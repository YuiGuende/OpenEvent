# 📝 TODO: Tích hợp Order AI vào EventAIAgent

## ✅ ĐÃ TẠO:

1. ✅ `PendingOrder.java` - DTO lưu trạng thái đơn hàng
2. ✅ `OrderAIService.java` - Service xử lý logic mua vé
3. ✅ `AI_ORDER_CREATION_GUIDE.md` - Tài liệu đầy đủ

---

## 🔧 CẦN LÀM TIẾP:

### **BƯỚC 1: Thêm OrderAIService vào EventAIAgent**

File: `EventAIAgent.java`

```java
@Service
@Slf4j
public class EventAIAgent implements Serializable {
    
    // ... existing fields ...
    private final OrderAIService orderAIService; // ← THÊM DÒNG NÀY
    
    public EventAIAgent(
        // ... existing parameters ...
        OrderAIService orderAIService // ← THÊM PARAMETER
    ) {
        // ... existing code ...
        this.orderAIService = orderAIService; // ← THÊM DÒNG NÀY
    }
}
```

---

### **BƯỚC 2: Update System Prompt**

File: `EventAIAgent.java` → method `initializeSystemMessage()`

Thêm vào cuối system prompt:

```java
systemPrompt.append("""

## XỬ LÝ MUA VÉ SỰ KIỆN:
1. Khi người dùng muốn mua vé (ví dụ: "Mua vé sự kiện X", "Đăng ký tham gia Y"):
   - Tìm sự kiện theo tên
   - Hiển thị danh sách loại vé có sẵn
   - Hướng dẫn người dùng chọn vé

2. Khi người dùng chọn loại vé:
   - Xác nhận loại vé đã chọn
   - Yêu cầu thông tin người tham gia (tên, email, SĐT)

3. Khi người dùng cung cấp thông tin:
   - Trích xuất: tên, email, số điện thoại
   - Hiển thị tóm tắt đơn hàng
   - Yêu cầu xác nhận (Có/Không)

4. Khi người dùng xác nhận:
   - Tạo đơn hàng trong hệ thống
   - Tạo payment link qua PayOS
   - Trả về link thanh toán cho người dùng

**LƯU Ý MUA VÉ:**
- KHÔNG hiển thị JSON cho người dùng
- Luôn hỏi xác nhận trước khi tạo đơn
- Nếu thiếu thông tin, hỏi lại người dùng
- Nếu vé hết, đề xuất loại vé khác
""");
```

---

### **BƯỚC 3: Add Intent Detection**

File: `EventAIAgent.java` → method `processUserInput()`

Thêm logic detect mua vé **TRƯỚC KHI** gọi LLM:

```java
public String processUserInput(String userInput, int userId, HttpServletResponse response) throws Exception {
    
    // ... existing pending event check ...
    
    // ← THÊM LOGIC NÀY
    // Check if user wants to buy tickets
    if (userInput.toLowerCase().contains("mua vé") || 
        userInput.toLowerCase().contains("mua ve") ||
        userInput.toLowerCase().contains("đăng ký") ||
        userInput.toLowerCase().contains("tham gia sự kiện")) {
        
        // Extract event name (simple extraction)
        String eventName = extractEventName(userInput);
        return orderAIService.startOrderCreation((long) userId, eventName);
    }
    
    // Check if user is in pending order flow
    if (orderAIService.hasPendingOrder((long) userId)) {
        PendingOrder pending = orderAIService.getPendingOrder((long) userId);
        
        // Handle based on current step
        switch (pending.getCurrentStep()) {
            case SELECT_TICKET_TYPE -> {
                // User is selecting ticket type
                return orderAIService.selectTicketType((long) userId, userInput);
            }
            case PROVIDE_INFO -> {
                // User is providing info
                Map<String, String> info = extractParticipantInfo(userInput);
                return orderAIService.provideInfo((long) userId, info);
            }
            case CONFIRM_ORDER -> {
                // User is confirming
                if (userInput.toLowerCase().contains("có") || 
                    userInput.toLowerCase().contains("yes") ||
                    userInput.toLowerCase().contains("ok") ||
                    userInput.toLowerCase().contains("xác nhận")) {
                    
                    Map<String, Object> result = orderAIService.confirmOrder((long) userId);
                    return (String) result.get("message");
                    
                } else if (userInput.toLowerCase().contains("không") || 
                          userInput.toLowerCase().contains("cancel") ||
                          userInput.toLowerCase().contains("hủy")) {
                    
                    return orderAIService.cancelOrder((long) userId);
                }
            }
        }
    }
    
    // ... existing code continues (LLM call, etc.) ...
}
```

---

### **BƯỚC 4: Add Helper Methods**

File: `EventAIAgent.java`

```java
/**
 * Extract event name from user input
 */
private String extractEventName(String userInput) {
    // Simple extraction - remove common words
    String cleaned = userInput
        .replaceAll("(?i)(mua vé|mua ve|đăng ký|tham gia|sự kiện|event)", "")
        .trim();
    return cleaned;
}

/**
 * Extract participant information from user input
 */
private Map<String, String> extractParticipantInfo(String userInput) {
    Map<String, String> info = new HashMap<>();
    
    // Extract name
    Pattern namePattern = Pattern.compile("(?:tên|họ tên|name)\\s*:?\\s*([^,]+)", Pattern.CASE_INSENSITIVE);
    Matcher nameMatcher = namePattern.matcher(userInput);
    if (nameMatcher.find()) {
        info.put("name", nameMatcher.group(1).trim());
    }
    
    // Extract email
    Pattern emailPattern = Pattern.compile("(?:email|mail)\\s*:?\\s*([^,\\s]+@[^,\\s]+)", Pattern.CASE_INSENSITIVE);
    Matcher emailMatcher = emailPattern.matcher(userInput);
    if (emailMatcher.find()) {
        info.put("email", emailMatcher.group(1).trim());
    }
    
    // Extract phone
    Pattern phonePattern = Pattern.compile("(?:sđt|sdt|phone|số điện thoại)\\s*:?\\s*([0-9]+)", Pattern.CASE_INSENSITIVE);
    Matcher phoneMatcher = phonePattern.matcher(userInput);
    if (phoneMatcher.find()) {
        info.put("phone", phoneMatcher.group(1).trim());
    }
    
    return info;
}
```

---

## 📋 IMPLEMENTATION CHECKLIST:

- [ ] Add `OrderAIService` field to `EventAIAgent`
- [ ] Add to constructor and inject
- [ ] Update `initializeSystemMessage()` with order prompt
- [ ] Add intent detection for "mua vé"
- [ ] Add pending order flow handling
- [ ] Add `extractEventName()` helper method
- [ ] Add `extractParticipantInfo()` helper method
- [ ] Test full flow
- [ ] Handle edge cases (event not found, ticket sold out, etc.)

---

## 🧪 TESTING COMMANDS:

```bash
# Test 1: Start order
"Mua vé sự kiện Music Festival"

# Test 2: Select ticket
"Chọn vé VIP"

# Test 3: Provide info
"Tên: Nguyễn Văn A, Email: test@gmail.com, SĐT: 0123456789"

# Test 4: Confirm
"Có"

# Test 5: Cancel
"Không" hoặc "Hủy đơn hàng"
```

---

## ⚠️ LƯU Ý:

1. **Thứ tự xử lý:**
   - Check mua vé TRƯỚC khi call LLM
   - Check pending order state TRƯỚC khi parse intent
   - Fallback to LLM nếu không match

2. **Error handling:**
   - Event không tồn tại
   - Vé đã hết
   - Thiếu thông tin
   - User không login

3. **State management:**
   - Pending orders tự động expire sau 30 phút
   - Clear pending order sau khi confirm hoặc cancel

---

**Priority:** 🔥 HIGH
**Complexity:** ⭐⭐⭐ (Medium)
**Estimated Time:** 2-3 hours

































