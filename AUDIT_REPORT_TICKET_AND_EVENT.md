# BÁO CÁO RÀ SOÁT LOGIC NGHIỆP VỤ: MUA VÉ VÀ TẠO SỰ KIỆN

## 🎯 TỔNG QUAN
Báo cáo này rà soát chi tiết logic nghiệp vụ cho 2 tính năng chính:
1. **Mua vé sự kiện** (Ticket Purchasing)
2. **Tạo sự kiện** (Event Creation)

## ✅ CÁC LỖI ĐÃ ĐƯỢC SỬA

**Ngày sửa:** Hôm nay  
**Các lỗi đã sửa:**

1. ✅ **Race Condition mua vé** - Đã thêm pessimistic lock trong `TicketTypeServiceImpl.reserveTickets()` và `ITicketTypeRepo.findByIdForUpdate()`
2. ✅ **AI re-validate ticket** - Đã thêm validation trong `OrderAIService.confirmOrder()` để re-check ticket availability từ DB trước khi tạo order
3. ✅ **Host null check** - Đã sửa `AgentEventService.createEventByCustomer()` để auto-create host nếu user chưa có
4. ✅ **Place validation** - Đã sửa `EventAIAgent.processUserInput()` để return error thay vì break khi place không tìm thấy

**Các lỗi còn lại cần sửa:**
- ⚠️ Timeout pending orders (cần thêm scheduled task)
- ⚠️ Permission check khi tạo event (có thể optional)
- ⚠️ Input validation (có thể cải thiện sau)

---

## ❌ CÁC LỖI NGHIÊM TRỌNG PHÁT HIỆN

### 1. MUA VÉ - RACE CONDITION (LỖI NGHIÊM TRỌNG)

**Vị trí:** 
- `OrderServiceImpl.createOrderWithTicketTypes()` (dòng 78-158)
- `TicketType.increaseSoldQuantity()` (dòng 104-109)

**Vấn đề:**
- Method `increaseSoldQuantity()` dùng `synchronized` nhưng chỉ lock instance, không lock database row
- Nhiều user cùng lúc có thể reserve cùng một ticket type
- Race condition khi check `canPurchase()` và reserve ticket

**Ví dụ:**
```
User A: canPurchase() -> true (còn 1 vé)
User B: canPurchase() -> true (còn 1 vé)  
User A: increaseSoldQuantity() -> thành công
User B: increaseSoldQuantity() -> thành công (LỖI: đã hết vé nhưng vẫn reserve được)
```

**Giải pháp:**
```java
// Option 1: Pessimistic Lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM TicketType t WHERE t.ticketTypeId = :id")
Optional<TicketType> findByIdForUpdate(@Param("id") Long id);

// Option 2: Optimistic Lock với version
@Version
private Long version;

// Option 3: Database-level check
UPDATE ticket_type 
SET sold_quantity = sold_quantity + 1 
WHERE ticket_type_id = ? 
  AND sold_quantity < total_quantity;
```

---

### 2. MUA VÉ - KHÔNG CÓ TIMEOUT CHO PENDING ORDERS (LỖI NGHIÊM TRỌNG)

**Vị trí:**
- `OrderServiceImpl.createOrderWithTicketTypes()` tạo order với status PENDING
- Tickets được reserve ngay khi tạo order, không có timeout

**Vấn đề:**
- User tạo order PENDING nhưng không thanh toán
- Tickets bị "giữ" vô thời hạn, không có mechanism để release
- Nếu user không bao giờ thanh toán, tickets sẽ bị mắc kẹt

**Ví dụ:**
```
10:00 AM - User tạo order PENDING, reserve 5 vé VIP (còn 10 vé)
10:00 AM - Available: 10 -> Reserved: 5 -> Available: 5
... user không thanh toán ...
11:00 PM - Available vẫn là 5 (LỖI: đáng lẽ phải release về 10)
```

**Giải pháp:**
```java
// 1. Thêm scheduled task để cancel old pending orders
@Scheduled(fixedRate = 300000) // mỗi 5 phút
public void cancelExpiredPendingOrders() {
    LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(15);
    List<Order> expiredOrders = orderRepo.findPendingOrdersOlderThan(expiryTime);
    expiredOrders.forEach(order -> {
        orderService.cancelOrder(order.getOrderId());
    });
}

// 2. Hoặc dùng database cleanup job
```

---

### 3. MUA VÉ - AI KHÔNG RE-VALIDATE TICKET AVAILABILITY (LỖI NGHIÊM TRỌNG)

**Vị trí:**
- `OrderAIService.confirmOrder()` (dòng 174-286)
- `OrderAIService.selectTicketType()` (dòng 93-127)

**Vấn đề:**
- Khi AI confirm order, nó dùng `pendingOrder` từ memory (Map)
- Không re-check ticket availability từ database trước khi tạo order
- Có thể ticket đã bán hết từ khi user chọn đến lúc confirm

**Ví dụ:**
```
10:00 AM - User chọn vé VIP qua AI (còn 5 vé) -> pendingOrder trong memory
10:05 AM - Người khác mua hết 5 vé VIP qua web
10:10 AM - User confirm order qua AI -> LỖI: vẫn tạo order dù đã hết vé
```

**Giải pháp:**
```java
// Trong OrderAIService.confirmOrder()
// THÊM validation trước khi tạo order:
TicketType ticketType = ticketTypeRepo.findById(pendingOrder.getTicketType().getTicketTypeId())
    .orElseThrow(() -> new IllegalStateException("Ticket type not found"));

if (!ticketTypeService.canPurchaseTickets(ticketType.getTicketTypeId(), 1)) {
    pendingOrders.remove(userId);
    result.put("success", false);
    result.put("message", "❌ Loại vé này đã hết. Vui lòng chọn loại vé khác.");
    return result;
}
```

---

### 4. TẠO SỰ KIỆN - HOST CÓ THỂ NULL (LỖI NGHIÊM TRỌNG)

**Vị trí:**
- `AgentEventService.createEventByCustomer()` (dòng 252-330)

**Vấn đề:**
- Code lấy `Host h = user.getHost()` (dòng 289)
- Nếu user chưa là host, `h` sẽ là `null`
- Code vẫn set `finalEvent.setHost(h)` mà không check null (dòng 301)
- Event được tạo với host = null -> LỖI DATABASE CONSTRAINT hoặc BUSINESS LOGIC

**Code hiện tại:**
```java
Host h = user.getHost();
// ... (comment code không tạo host mới)
finalEvent.setHost(h); // LỖI: h có thể null
```

**Giải pháp:**
```java
Host h = user.getHost();
if (h == null) {
    // Option 1: Tự động tạo host cho user
    h = new Host();
    h.setUser(user);
    h = hostService.save(h);
    user.setHost(h);
    userService.save(user);
    log.info("Auto-created host for user {}", userId);
}
finalEvent.setHost(h);
```

---

### 5. TẠO SỰ KIỆN - KHÔNG CHECK PERMISSION (LỖI NGHIÊM TRỌNG)

**Vị trí:**
- `EventAIAgent.processUserInput()` xử lý ADD_EVENT (dòng 719-846)
- `AgentEventService.createEventByCustomer()` (dòng 252-330)

**Vấn đề:**
- AI có thể tạo event cho bất kỳ user nào mà không check:
  - User có phải host không?
  - User có quyền tạo event không?
  - Có giới hạn số event user có thể tạo không?

**Ví dụ:**
- User mới đăng ký chưa verify email vẫn có thể tạo event
- User bị ban vẫn có thể tạo event qua AI

**Giải pháp:**
```java
// Trong EventAIAgent.processUserInput() hoặc AgentEventService.createEventByCustomer()
// THÊM permission check:
User user = userService.getUserById(userId);
if (user == null) {
    throw new IllegalArgumentException("User not found");
}

// Check nếu user cần phải là host
if (!user.hasHostRole() && !hostService.isUserHost(userId)) {
    // Auto-create host hoặc reject
    throw new SecurityException("User must be a host to create events");
}

// Optional: Check rate limit
int eventCount = eventService.getEventCountByHostId(hostId);
if (eventCount >= MAX_EVENTS_PER_HOST) {
    throw new IllegalStateException("Maximum events per host reached");
}
```

---

### 6. TẠO SỰ KIỆN - PLACE VALIDATION KHÔNG ĐẦY ĐỦ (LỖI TRUNG BÌNH)

**Vị trí:**
- `EventAIAgent.processUserInput()` xử lý ADD_EVENT (dòng 735-783)

**Vấn đề:**
- Nếu place không tìm thấy, code vẫn break nhưng không rollback
- Event có thể được tạo với place = null hoặc empty list
- Có thể tạo event mà không có địa điểm (vi phạm business rule)

**Code hiện tại:**
```java
if (placeOpt.isEmpty()) {
    systemResult.append("⛔ Để tạo sự kiện, bạn cần cung cấp địa điểm hợp lệ.");
    break; // Chỉ break, không throw exception
}
// ... sau đó vẫn có thể tạo event ở đâu đó
```

**Giải pháp:**
```java
if (placeOpt.isEmpty()) {
    String errorMsg = "⛔ Để tạo sự kiện, bạn cần cung cấp địa điểm hợp lệ.";
    if (placeNameRaw != null && !placeNameRaw.isBlank()) {
        errorMsg += " Không tìm thấy địa điểm \"" + placeNameRaw + "\".";
    } else {
        errorMsg += " Vui lòng cung cấp tên địa điểm.";
    }
    return errorMsg; // RETURN ngay, không break
}
```

---

### 7. AI RESPONSE - HALLUCINATION PREVENTION CHƯA HOÀN THIỆN (LỖI TRUNG BÌNH)

**Vị trí:**
- `EventAIAgent.processUserInput()` (dòng 987-1022)

**Vấn đề:**
- Code có cơ chế chống hallucination nhưng chỉ cho PROMPT_SUMMARY_TIME và QUERY_TICKET_INFO
- Các intent khác vẫn có thể hallucinate
- LLM response có thể chứa thông tin sai mà không được validate

**Ví dụ:**
- User hỏi "Xem vé sự kiện ABC" -> AI có thể tự bịa giá vé, thời gian
- User hỏi "Tôi có bao nhiêu đơn hàng?" -> AI có thể tự bịa số lượng

**Giải pháp:**
```java
// THÊM validation cho tất cả các AI responses liên quan đến data
// Luôn query DB trước khi trả lời
if (userInput.contains("vé") || userInput.contains("ticket")) {
    // ALWAYS query từ DB, không trust LLM
    return handleTicketInfoQuery(userInput, userVector);
}
if (userInput.contains("đơn hàng") || userInput.contains("order")) {
    // ALWAYS query từ DB
    return handleOrderQuery(userId);
}
```

---

## ⚠️ CÁC VẤN ĐỀ KHÁC (ÍT NGHIÊM TRỌNG)

### 8. MUA VÉ - DUPLICATE PENDING ORDER HANDLING

**Vị trí:**
- `OrderController.createWithTicketTypes()` (dòng 88-97)

**Vấn đề:**
- Code cancel old pending order nếu có, nhưng không check xem có order nào khác đang pending không
- Race condition: 2 requests cùng lúc có thể tạo 2 pending orders

**Giải pháp:**
```java
// Dùng database unique constraint hoặc pessimistic lock
@Transactional
public Order createOrderWithTicketTypes(...) {
    // Lock customer row để prevent concurrent orders
    Customer customer = customerRepo.findByIdForUpdate(customerId);
    Optional<Order> pendingOrder = getPendingOrderForEvent(...);
    // ...
}
```

---

### 9. TẠO SỰ KIỆN - THIẾU VALIDATION INPUT

**Vị trí:**
- `AgentEventService.createEventByCustomer()` (dòng 252-330)

**Vấn đề:**
- Không validate:
  - Title không được empty
  - Start time phải sau current time
  - End time phải sau start time
  - Capacity phải > 0

**Giải pháp:**
```java
// THÊM validation:
if (draft.getTitle() == null || draft.getTitle().trim().isEmpty()) {
    throw new IllegalArgumentException("Event title is required");
}
if (draft.getStartsAt() == null || draft.getStartsAt().isBefore(LocalDateTime.now())) {
    throw new IllegalArgumentException("Event start time must be in the future");
}
if (draft.getEndsAt() == null || !draft.getEndsAt().isAfter(draft.getStartsAt())) {
    throw new IllegalArgumentException("Event end time must be after start time");
}
```

---

### 10. MUA VÉ - VOLUNTEER CHECK KHÔNG ĐẦY ĐỦ

**Vị trí:**
- `OrderController.createWithTicketTypes()` (dòng 74-79)

**Vấn đề:**
- Check volunteer nhưng không check:
  - Volunteer status (APPROVED vs PENDING)
  - Event đã bắt đầu chưa (không thể mua vé sau khi event đã bắt đầu)

**Giải pháp:**
```java
// Check event status
Event event = eventRepo.findById(request.getEventId())
    .orElseThrow(() -> new IllegalArgumentException("Event not found"));

if (event.getStatus() != EventStatus.PUBLIC) {
    return ResponseEntity.badRequest().body(Map.of(
        "success", false,
        "message", "Event is not open for registration"
    ));
}

if (event.getStartsAt().isBefore(LocalDateTime.now())) {
    return ResponseEntity.badRequest().body(Map.of(
        "success", false,
        "message", "Cannot register for event that has already started"
    ));
}

if (event.getEnrollDeadline() != null && 
    event.getEnrollDeadline().isBefore(LocalDateTime.now())) {
    return ResponseEntity.badRequest().body(Map.of(
        "success", false,
        "message", "Registration deadline has passed"
    ));
}
```

---

## ✅ CÁC ĐIỂM TỐT ĐÃ LÀM

1. ✅ **Transaction Management:** Sử dụng `@Transactional` đúng chỗ
2. ✅ **Error Handling:** Có try-catch và error messages
3. ✅ **Hallucination Prevention:** Đã có cơ chế chống hallucination cho một số cases
4. ✅ **Business Logic:** Có check volunteer, duplicate registration
5. ✅ **Event Conflicts:** Có check time conflict khi tạo event

---

## 📋 KHUYẾN NGHỊ ƯU TIÊN SỬA

### 🔴 ƯU TIÊN CAO (Phải sửa ngay):
1. **Race condition mua vé** (Lỗi #1)
2. **Timeout pending orders** (Lỗi #2)
3. **AI re-validate ticket** (Lỗi #3)
4. **Host null check** (Lỗi #4)
5. **Permission check** (Lỗi #5)

### 🟡 ƯU TIÊN TRUNG BÌNH (Nên sửa):
6. **Place validation** (Lỗi #6)
7. **Hallucination prevention** (Lỗi #7)
8. **Duplicate pending order** (Lỗi #8)

### 🟢 ƯU TIÊN THẤP (Có thể sửa sau):
9. **Input validation** (Lỗi #9)
10. **Volunteer check** (Lỗi #10)

---

## 🔧 HƯỚNG DẪN SỬA CHỮA

Tất cả các lỗi trên cần được sửa trước khi deploy production. 
Đặc biệt là các lỗi race condition và timeout pending orders vì chúng ảnh hưởng trực tiếp đến tính chính xác của dữ liệu và trải nghiệm người dùng.

**Khuyến nghị:** Test kỹ các scenarios:
- Multiple users mua cùng lúc
- User tạo order nhưng không thanh toán
- AI tạo event cho user chưa là host
- AI mua vé khi ticket đã hết

