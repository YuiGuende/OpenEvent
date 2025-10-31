# 🛒 Hướng dẫn: Tạo Đơn Hàng bằng AI

## 🎯 Tổng quan

Tính năng AI Order Creation cho phép người dùng mua vé sự kiện thông qua chat với AI, với quy trình hoàn chỉnh:

```
1. Chọn sự kiện
2. Chọn loại vé
3. Điền thông tin
4. Xác nhận và thanh toán
```

---

## 🗣️ CÁC CÂU LỆNH AI

### 1. **Bắt đầu mua vé:**
```
"Tôi muốn mua vé sự kiện Music Festival"
"Mua vé Workshop AI"
"Đăng ký tham gia event Competition 2024"
```

### 2. **Chọn loại vé:**
```
"Chọn vé VIP"
"Tôi muốn vé Standard"
"Lấy vé Early Bird"
```

### 3. **Cung cấp thông tin:**
```
"Tên: Nguyễn Văn A, Email: test@gmail.com, SĐT: 0123456789"
"Họ tên: Trần Thị B, Email: example@email.com"
```

### 4. **Xác nhận:**
```
"Có"
"Xác nhận"
"OK"
"Đồng ý"
```

### 5. **Hủy đơn:**
```
"Hủy đơn hàng"
"Không mua nữa"
"Cancel"
```

---

## 📋 QUY TRÌNH CHI TIẾT

### **BƯỚC 1: CHỌN SỰ KIỆN**

**User input:**
```
"Tôi muốn mua vé sự kiện Music Festival"
```

**AI response:**
```
🎫 Sự kiện: **Music Festival 2024**

📅 Thời gian: 2024-12-20 18:00

Các loại vé có sẵn:

• **VIP**
  - Giá: 500,000 VND
  - Còn lại: 50 vé
  - Mô tả: Ghế ngồi hạng sang, free drinks

• **Standard**
  - Giá: 200,000 VND
  - Còn lại: 200 vé
  - Mô tả: Vé thường

• **Early Bird**
  - Giá: 150,000 VND
  - Còn lại: 0 vé
  - Mô tả: Vé ưu đãi sớm

💡 Bạn muốn chọn loại vé nào?
```

---

### **BƯỚC 2: CHỌN LOẠI VÉ**

**User input:**
```
"Chọn vé VIP"
```

**AI response:**
```
✅ Đã chọn vé **VIP** - Giá: 500,000 VND

📝 Vui lòng cung cấp thông tin:
- Tên người tham gia
- Email
- Số điện thoại (tùy chọn)

Ví dụ: 'Tên: Nguyễn Văn A, Email: test@gmail.com, SĐT: 0123456789'
```

---

### **BƯỚC 3: ĐIỀN THÔNG TIN**

**User input:**
```
"Tên: Nguyễn Văn A, Email: nguyenvana@gmail.com, SĐT: 0912345678"
```

**AI response:**
```
📋 **Xác nhận thông tin đơn hàng:**

🎫 Sự kiện: Music Festival 2024
🎟️ Loại vé: VIP
💰 Giá: 500,000 VND

👤 Thông tin người tham gia:
- Tên: Nguyễn Văn A
- Email: nguyenvana@gmail.com
- SĐT: 0912345678

💡 Xác nhận đặt vé? (Có/Không)
```

---

### **BƯỚC 4: XÁC NHẬN VÀ TẠO ĐƠN**

**User input:**
```
"Có"
```

**AI response:**
```
✅ Đã tạo đơn hàng thành công!
🔗 Link thanh toán: https://payos.vn/payment/xxxxx

💡 Vui lòng thanh toán để hoàn tất đăng ký.
```

---

## 🔧 KIẾN TRÚC TECHNICAL

### **1. Components:**

#### a. **PendingOrder (DTO)**
```java
@Data
public class PendingOrder {
    private Event event;
    private TicketType ticketType;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private OrderStep currentStep;
    
    public enum OrderStep {
        SELECT_EVENT,
        SELECT_TICKET_TYPE,
        PROVIDE_INFO,
        CONFIRM_ORDER
    }
}
```

#### b. **OrderAIService**
```java
@Service
public class OrderAIService {
    // Store pending orders
    Map<Long, PendingOrder> pendingOrders;
    
    // Core methods
    String startOrderCreation(Long userId, String eventQuery);
    String selectTicketType(Long userId, String ticketTypeName);
    String provideInfo(Long userId, Map<String, String> info);
    Map<String, Object> confirmOrder(Long userId);
    String cancelOrder(Long userId);
}
```

#### c. **EventAIAgent Integration**
```java
// AI Agent detects ORDER_CREATE intent
switch (intent) {
    case ORDER_CREATE -> {
        // Use OrderAIService
        String response = orderAIService.startOrderCreation(userId, userInput);
        systemResult.append(response);
    }
}
```

---

## 🎭 STATE MANAGEMENT

### **Pending Order States:**

```
┌──────────────┐
│ SELECT_EVENT │
└──────┬───────┘
       │ User: "Mua vé event X"
       ▼
┌──────────────────┐
│SELECT_TICKET_TYPE│
└──────┬───────────┘
       │ User: "Chọn vé VIP"
       ▼
┌──────────────┐
│ PROVIDE_INFO │
└──────┬───────┘
       │ User: "Tên: A, Email: a@b.c"
       ▼
┌──────────────┐
│CONFIRM_ORDER │
└──────┬───────┘
       │ User: "Có"
       ▼
  ┌─────────┐
  │ PAYMENT │
  └─────────┘
```

---

## 💾 DATA FLOW

### **1. Start Order:**
```
User → AI → OrderAIService.startOrderCreation()
         → EventService.findByTitle()
         → TicketTypeService.getTicketTypesByEvent()
         → Create PendingOrder
         → Return ticket list
```

### **2. Select Ticket:**
```
User → AI → OrderAIService.selectTicketType()
         → Find TicketType from PendingOrder.event
         → Check availability
         → Update PendingOrder.ticketType
         → Ask for participant info
```

### **3. Provide Info:**
```
User → AI → OrderAIService.provideInfo()
         → Parse user input (name, email, phone)
         → Update PendingOrder fields
         → Validate completeness
         → Show confirmation summary
```

### **4. Confirm:**
```
User → AI → OrderAIService.confirmOrder()
         → Get Customer from userId
         → Create CreateOrderWithTicketTypeRequest
         → OrderService.createOrderWithTicketTypes()
         → PaymentService.createPaymentLinkForOrder()
         → Clear PendingOrder
         → Return payment URL
```

---

## 🧠 AI INTEGRATION

### **Update System Prompt:**

Add to `EventAIAgent.initializeSystemMessage()`:

```java
systemPrompt.append("""
## XỬ LÝ MUA VÉ:
1. Khi người dùng muốn mua vé:
   - Phân tích tên sự kiện
   - Hiển thị danh sách vé
   - Hướng dẫn chọn vé

2. Khi chọn vé:
   - Xác nhận loại vé
   - Yêu cầu thông tin người tham gia

3. Khi cung cấp thông tin:
   - Trích xuất: tên, email, SĐT
   - Hiển thị summary
   - Yêu cầu xác nhận

4. Khi xác nhận:
   - Tạo đơn hàng
   - Tạo payment link
   - Trả về link thanh toán
""");
```

### **Add Intent Detection:**

```java
// In EventAIAgent.processUserInput()
if (userInput.contains("mua vé") || 
    userInput.contains("đăng ký") ||
    userInput.contains("tham gia")) {
    
    // Start order process
    String response = orderAIService.startOrderCreation(userId, extractEventName(userInput));
    return response;
}
```

---

## ⚠️ ERROR HANDLING

### **1. Event not found:**
```
❌ Không tìm thấy sự kiện "XYZ". Vui lòng kiểm tra lại tên sự kiện.
```

### **2. Ticket sold out:**
```
❌ Loại vé "VIP" đã hết. Vui lòng chọn loại vé khác.
```

### **3. Missing information:**
```
⚠️ Còn thiếu thông tin:
- Tên người tham gia
- Email

Vui lòng cung cấp đầy đủ thông tin.
```

### **4. Order creation failed:**
```
❌ Lỗi khi tạo đơn hàng: [error message]
Vui lòng thử lại sau.
```

---

## 🧪 TESTING

### **Test Flow:**

```bash
# 1. Start order
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Mua vé Music Festival",
    "userId": 1
  }'

# 2. Select ticket
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Chọn vé VIP",
    "userId": 1
  }'

# 3. Provide info
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tên: Test User, Email: test@test.com",
    "userId": 1
  }'

# 4. Confirm
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Có",
    "userId": 1
  }'
```

---

## 🔒 SECURITY

### **1. User Authentication:**
```java
// Verify user owns the order
if (!order.getCustomer().getAccount().getAccountId().equals(userId)) {
    throw new SecurityException("Unauthorized");
}
```

### **2. Ticket Availability:**
```java
// Check ticket still available before creating order
if (!ticketType.isAvailable()) {
    throw new IllegalStateException("Ticket sold out");
}
```

### **3. Session Timeout:**
```java
// Pending orders expire after 30 minutes
if (pendingOrder.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
    pendingOrders.remove(userId);
}
```

---

## 📊 DATABASE

### **Tables Involved:**

```sql
-- Existing tables used
event (id, title, starts_at, ...)
ticket_type (ticket_type_id, event_id, name, price, ...)
orders (order_id, customer_id, event_id, ticket_type_id, ...)
payments (payment_id, order_id, checkout_url, ...)
```

**No new tables needed!** Uses existing schema.

---

## ✅ CHECKLIST

### Implementation:
- [x] Create PendingOrder DTO
- [x] Create OrderAIService
- [ ] Integrate with EventAIAgent
- [ ] Update AI system prompt
- [ ] Add intent detection
- [ ] Test full flow

### Features:
- [x] Select event
- [x] Select ticket type
- [x] Provide participant info
- [x] Confirm and create order
- [x] Generate payment link
- [x] Cancel order

### Error Handling:
- [x] Event not found
- [x] Ticket not available
- [x] Missing information
- [x] Order creation failed

---

## 🚀 NEXT STEPS

### Phase 1: Basic Implementation ✅
- PendingOrder DTO
- OrderAIService core methods

### Phase 2: AI Integration 🔄
- Update EventAIAgent
- Add to system prompt
- Intent detection

### Phase 3: Enhanced Features 🔮
- Multiple ticket quantities
- Apply vouchers via AI
- Order history lookup
- Order cancellation

---

**Status:** ✅ Core Services Ready - Need AI Integration
**Last Updated:** 2024-10-11
**Version:** 1.0.0



































