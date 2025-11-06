# AI Workflow Chi Tiết - Từ Input Đến Response

## Tổng quan luồng xử lý

Khi người dùng nhập tin nhắn vào chatbot, hệ thống sẽ trải qua nhiều bước xử lý phức tạp để đảm bảo phản hồi chính xác và phù hợp nhất.

---

## 📝 WORKFLOW CHI TIẾT TỪNG BƯỚC

### **GIAI ĐOẠN 1: FRONTEND - Người dùng nhập tin nhắn**

#### Bước 1.1: User Interface (chatbot.js)
```
Người dùng gõ tin nhắn → Nhấn Enter/Click Send
↓
chatbot.js: sendMessage()
↓
- Lấy message từ input field
- Validate message (không rỗng)
- Display user message lên UI ngay lập tức
- Clear input field
- Show typing indicator
- Call sendMessageToApi()
```

**Code example:**
```javascript
async function sendMessage() {
    const message = chatInput.value.trim();
    if (!message) return;
    
    displayMessage('user', message);           // Hiển thị ngay
    chatInput.value = '';                     // Xóa input
    showTyping(true);                          // Show "AI đang gõ..."
    await sendMessageToApi(message);           // Gọi API
}
```

#### Bước 1.2: API Request (chatbot.js)
```
Fetch POST request to /api/ai/chat/enhanced
↓
Body: {
    message: "User's message",
    userId: USER_ID,
    sessionId: getCurrentSessionId()
}
↓
Headers: Content-Type: application/json
```

---

### **GIAI ĐOẠN 2: BACKEND - Xử lý request**

#### Bước 2.1: Controller Entry Point (EnhancedAIController.java)
```
Request đến → EnhancedAIController.chat()
↓
- Validate session authentication
- Extract userId từ HTTP session
- Validate request body
```

#### Bước 2.2: Rate Limiting Check
```java
// Kiểm tra rate limit cho userId
if (!rateLimitingService.isAllowed(userId, RateLimitType.AI_CHAT)) {
    return ResponseEntity.status(429).body(
        new ChatReply("Rate limit exceeded...", false, LocalDateTime.now())
    );
}
```

**Rate Limiting:** Giới hạn số lượng request để tránh spam và tối ưu server

#### Bước 2.3: Input Validation & Sanitization
```java
// Validate và sanitize input
ValidationResult validation = securityService.validateInput(
    req.message(), 
    InputType.MESSAGE
);

if (!validation.isValid()) {
    return ResponseEntity.badRequest().body(
        new ChatReply("❌ " + validation.getErrorMessage(), false, LocalDateTime.now())
    );
}
```

**Validation checks:**
- ✅ Không rỗng
- ✅ Độ dài hợp lệ (không quá 1000 ký tự)
- ✅ Không chứa script injection
- ✅ Không chứa SQL injection patterns
- ✅ Format hợp lệ

---

### **GIAI ĐOẠN 3: LANGUAGE PROCESSING - Xử lý ngôn ngữ**

#### Bước 3.1: Language Detection
```java
// Detect ngôn ngữ của tin nhắn
Language userLanguage = languageDetectionService.detectLanguage(req.message());
```

**Supported languages:** Vietnamese, English, Chinese, Japanese, Korean, French, German, Spanish, Thai, Indonesian, etc.

**Detection method:** Sử dụng library language detection dựa trên:
- Character patterns
- Common words
- Statistical analysis

#### Bước 3.2: Translation to Vietnamese
```java
// Dịch sang tiếng Việt để AI xử lý
String processedMessage = validation.getSanitizedInput();
if (userLanguage != Language.VIETNAMESE) {
    processedMessage = translationService.translateUserInput(processedMessage, userLanguage);
}
```

**Translation service:**
- Primary: LibreTranslate API
- Fallback: Multiple fallback URLs
- Cache: In-memory cache để tăng performance
- Async: Non-blocking translation

---

### **GIAI ĐOẠN 4: AI PROCESSING - Xử lý bằng AI**

#### Bước 4.1: Chat Session Service
```java
// Tạo ChatRequest với message đã dịch
ChatRequest processedReq = new ChatRequest(
    processedMessage,      // Message đã translate
    userId,                // User ID
    sessionId              // Session ID
);

// Process chat
ChatReply reply = chatSessionService.chat(processedReq);
```

#### Bước 4.2: Event AI Agent Processing (EventAIAgent.java)

##### **4.2.1: Context Building**
```java
// Xây dựng context từ conversation history
List<Message> context = buildConversationContext(sessionId, userId);
```

**Context includes:**
- System prompt (instructions cho AI)
- Recent 10 messages từ conversation history
- Current date/time
- User information

##### **4.2.2: Intent Classification**
```java
// Tạo embedding cho user input
float[] userVector = embeddingService.getEmbedding(userInput);

// Classify intent
ActionType intent = classifier.classifyIntent(userInput, userVector);
```

**Possible intents:**
- `BUY_TICKET` - Mua vé
- `QUERY_TICKET_INFO` - Xem thông tin vé
- `ADD_EVENT` - Tạo sự kiện
- `UPDATE_EVENT` - Sửa sự kiện
- `DELETE_EVENT` - Xóa sự kiện
- `SET_REMINDER` - Đặt nhắc nhở
- `PROMPT_FREE_TIME` - Tìm thời gian rảnh
- `PROMPT_SUMMARY_TIME` - Tóm tắt lịch
- `CONFIRM_ORDER` - Xác nhận đơn hàng
- `CANCEL_ORDER` - Hủy đơn hàng
- `UNKNOWN` - Không rõ

##### **4.2.3: Special Flow Checks**

**A. Order Flow (Mua vé)**
```java
if (intent == ActionType.BUY_TICKET) {
    // Tìm sự kiện
    List<Event> foundEvents = eventVectorSearchService.searchEvents(userInput, userId, 1);
    
    // Start order creation
    return orderAIService.startOrderCreation(userId, eventName);
}

// Check pending order
if (orderAIService.hasPendingOrder(userId)) {
    PendingOrder pending = orderAIService.getPendingOrder(userId);
    
    switch (pending.getCurrentStep()) {
        case SELECT_TICKET_TYPE:
            return orderAIService.selectTicketType(userId, userInput);
        case PROVIDE_INFO:
            return orderAIService.provideInfo(userId, info);
        case CONFIRM_ORDER:
            return handleConfirmation(userId, userInput);
    }
}
```

**B. Out of Scope Check**
```java
// Kiểm tra câu hỏi có ngoài phạm vi không
if (isOutOfScope(userInput)) {
    return handleOutOfScopeQuestion();
}
```

**C. Weather Question Check**
```java
// Kiểm tra câu hỏi về thời tiết
if (isWeatherQuestion(userInput)) {
    return handleWeatherQuestion(userInput);
}
```

##### **4.2.4: LLM Processing**
```java
// Thêm user message vào context
context.add(new Message("user", userInput));

// Call LLM API để generate response
String aiResponse = llm.generateResponse(context);
```

**LLM Processing:**
- Model: Qwen/Qwen3-Embedding-8B
- API: HuggingFace API
- Input: Full conversation context
- Output: Natural language response

##### **4.2.5: Action Parsing**
```java
// Parse JSON actions từ LLM response
Pattern jsonPattern = Pattern.compile("(\\[\\s*\\{[\\s\\S]*?\\}\\s*\\])");
Matcher matcher = jsonPattern.matcher(aiResponse);
String jsonPart = matcher.find() ? matcher.group() : null;

// Parse actions
List<Action> actions = tryParseActions(jsonPart);
```

**Action types:**
- `ADD_EVENT` - Tạo sự kiện mới
- `UPDATE_EVENT` - Cập nhật sự kiện
- `DELETE_EVENT` - Xóa sự kiện
- `SET_REMINDER` - Đặt nhắc nhở

##### **4.2.6: Action Execution**

**Example: ADD_EVENT action**
```java
case "ADD_EVENT" -> {
    Map<String, Object> args = action.getArgs();
    
    // Extract event information
    String title = getStr(args, "title", "event_title");
    LocalDateTime start = getTime(args, "start_time", "starts_at");
    LocalDateTime end = getTime(args, "end_time", "ends_at");
    String placeName = getStr(args, "place", "location");
    
    // Validation
    if (title == null || start == null || end == null) {
        systemResult.append("📝 Thiếu thông tin sự kiện");
        break;
    }
    
    // Check time conflict
    List<Event> conflicted = eventService.isTimeConflict(start, end, List.of(place));
    if (!conflicted.isEmpty()) {
        systemResult.append("⚠️ Sự kiện bị trùng thời gian");
        break;
    }
    
    // Weather check for outdoor events
    if (isOutdoorEvent) {
        String forecast = weatherService.getForecastNote(start, location);
        if (forecast.contains("rain")) {
            pendingEvents.put(sessionId, new PendingEvent(event));
            return "🌦 Thời tiết có thể mưa. Bạn có muốn tiếp tục?";
        }
    }
    
    // Create event
    Event saved = agentEventService.createEventByCustomer(userId, event, orgId);
    systemResult.append("✅ Đã thêm sự kiện: " + saved.getTitle());
}
```

**Example: SET_REMINDER action**
```java
case "SET_REMINDER" -> {
    Long remindMinutes = getLong(args, "remind_minutes");
    String eventTitle = getStr(args, "event_title");
    
    // Tìm sự kiện
    Optional<Event> targetEvent = findEventByName(eventTitle);
    
    // Get user email
    String userEmail = getCustomerEmail(userId);
    
    // Save reminder
    agentEventService.createOrUpdateEmailReminder(eventId, remindMinutes, userId);
    
    systemResult.append("✅ Đã đặt lịch nhắc nhở trước " + remindMinutes + " phút");
}
```

---

### **GIAI ĐOẠN 5: RESPONSE GENERATION - Tạo phản hồi**

#### Bước 5.1: Response Assembly
```java
// Combine text response và action results
String fullResponse = userVisibleText + "\n\n" + systemResult;
if (shouldReload) {
    fullResponse += "\n__RELOAD__";  // Flag để reload page
}
return fullResponse;
```

#### Bước 5.2: Translate Response
```java
// Dịch phản hồi về ngôn ngữ của user
String translatedResponse = reply.message();
if (userLanguage != Language.VIETNAMESE) {
    translatedResponse = translationService.translateAIResponse(
        reply.message(), 
        userLanguage
    );
}
```

#### Bước 5.3: Response Validation
```java
// Validate AI response
ValidationResult responseValidation = securityService.validateAIResponse(translatedResponse);
if (!responseValidation.isValid()) {
    translatedResponse = "❌ " + responseValidation.getErrorMessage();
}
```

#### Bước 5.4: Database Storage
```java
// Lưu user message vào database
ChatMessage userMessage = new ChatMessage();
userMessage.setSessionId(sessionId);
userMessage.setUserId(userId);
userMessage.setMessage(message);
userMessage.setIsFromUser(true);
userMessage.setTimestamp(LocalDateTime.now());
chatMessageRepo.save(userMessage);

// Lưu AI response vào database
ChatMessage aiMessage = new ChatMessage();
aiMessage.setSessionId(sessionId);
aiMessage.setUserId(userId);
aiMessage.setMessage(reply.message());
aiMessage.setIsFromUser(false);
aiMessage.setTimestamp(LocalDateTime.now());
chatMessageRepo.save(aiMessage);
```

---

### **GIAI ĐOẠN 6: FRONTEND - Hiển thị response**

#### Bước 6.1: Response Processing (chatbot.js)
```javascript
const data = await response.json();
showTyping(false);

// Kiểm tra nếu cần reload page
if (data.message && data.message.includes("__RELOAD__")) {
    const cleanMessage = data.message.replace("__RELOAD__", "").trim();
    displayMessage('bot', cleanMessage);
    setTimeout(() => location.reload(), 1500);
} else {
    displayMessage('bot', data.message || 'Xin lỗi...');
}
```

#### Bước 6.2: Display Message
```javascript
function displayMessage(sender, message) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;
    
    // Format message (markdown support)
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.innerHTML = formatMessage(message);
    
    // Append to chat
    chatMessages.appendChild(messageDiv);
    scrollToBottom();
}

function formatMessage(message) {
    // Support markdown: **bold**, *italic*, newlines
    return message
        .replace(/\n/g, '<br>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>');
}
```

---

## 🎯 CÁC CÁCH AI PHẢN HỒI

Hệ thống AI có nhiều cách phản hồi khác nhau tùy thuộc vào intent và context:

### **1. PHẢN HỒI TỰ NHIÊN (Natural Conversation)**
```
User: "Xin chào"
AI: "Xin chào! Tôi là OpenEventAI, trợ lý thông minh của bạn. 
     Tôi có thể giúp bạn tìm kiếm sự kiện, đặt vé, và nhiều hơn nữa!"
```

### **2. PHẢN HỒI VỚI HÀNH ĐỘNG (Action-based)**
```
User: "Tạo sự kiện Music Night vào 20h ngày 15/12 tại Nhà văn hóa"
AI: "✅ Đã tạo sự kiện 'Music Night' vào 20:00 ngày 15/12/2024"
```

### **3. PHẢN HỒI VỚI XÁC NHẬN (Confirmation)**
```
User: "Tạo sự kiện ngoài trời"
AI: "🌦 Dự báo có thể mưa vào thời gian này. Bạn có muốn tiếp tục không?"
```

### **4. PHẢN HỒI THÔNG TIN (Information)**
```
User: "Xem vé sự kiện Music Night"
AI: "🎫 Thông tin vé cho sự kiện: Music Night
     • Vé thường: 100,000 VNĐ (Còn lại: 50 vé)
     • Vé VIP: 200,000 VNĐ (Còn lại: 10 vé)"
```

### **5. PHẢN HỒI HƯỚNG DẪN (Guidance)**
```
User: "Làm sao để mua vé?"
AI: "Em hướng dẫn bạn mua vé như sau:
     1️⃣ Cho tôi biết tên sự kiện muốn tham gia
     2️⃣ Tôi sẽ hiển thị tất cả loại vé với giá và số lượng
     3️⃣ Bạn chọn loại vé phù hợp
     4️⃣ Cung cấp thông tin: tên, email, số điện thoại
     5️⃣ Xác nhận đơn hàng
     6️⃣ Thanh toán qua PayOS"
```

### **6. PHẢN HỒI LỖI (Error)**
```
User: "Tạo sự kiện vào quá khứ"
AI: "⛔ Thời gian không hợp lệ: sự kiện không thể ở quá khứ"
```

### **7. PHẢN HỒI CẢNH BÁO (Warning)**
```
User: "Tạo sự kiện trùng thời gian"
AI: "⚠️ Sự kiện bị trùng thời gian với:
     - Sự kiện ABC (20:00 - 22:00)
     Bạn có muốn chọn thời gian khác không?"
```

### **8. PHẢN HỒI MULTI-STEP (Quy trình nhiều bước)**
```
Step 1 - User: "Mua vé"
         AI: "🎫 Sự kiện: Music Night
              Các loại vé có sẵn:
              • Vé thường: 100,000 VND
              • Vé VIP: 200,000 VND
              Bạn muốn chọn loại vé nào?"

Step 2 - User: "Vé VIP"
         AI: "✅ Đã chọn vé VIP - Giá: 200,000 VND
              Vui lòng cung cấp thông tin:
              - Tên người tham gia
              - Email
              - Số điện thoại"

Step 3 - User: "Tên: Nguyễn Văn A, Email: test@gmail.com"
         AI: "📋 Xác nhận thông tin đơn hàng:
              🎫 Sự kiện: Music Night
              🎟️ Loại vé: VIP
              💰 Giá: 200,000 VND
              👤 Tên: Nguyễn Văn A
              📧 Email: test@gmail.com
              💡 Xác nhận đặt vé? (Có/Không)"

Step 4 - User: "Có"
         AI: "✅ Đã tạo đơn hàng thành công!
              🔗 Link thanh toán: https://payos.com/..."
```

### **9. PHẢN HỒI VỚI LINK (Payment/Rerload)**
```
User: "Xác nhận mua vé"
AI: "✅ Đã tạo đơn hàng thành công!
     🔗 Link thanh toán: https://payos.com/checkout/12345
     
     💡 Vui lòng thanh toán để hoàn tất đăng ký."
```

### **10. PHẢN HỒI NGOÀI PHẠM VI (Out of Scope)**
```
User: "Lịch sử Việt Nam như thế nào?"
AI: "Xin lỗi, tôi chỉ có thể hỗ trợ về hệ thống OpenEvent và các sự kiện thôi.
     
     Em có thể giúp bạn:
     ✅ Tìm kiếm sự kiện
     ✅ Mua vé sự kiện
     ✅ Tạo và quản lý sự kiện
     ✅ Xem thông tin về speakers và địa điểm
     
     Bạn cần hỗ trợ gì về OpenEvent ạ? 😊"
```

### **11. PHẢN HỒI VỚI LOADER (Reload Page)**
```
User: "Tạo sự kiện và reload"
AI: "✅ Đã tạo sự kiện thành công!
     [Auto reload page after 1.5s]"
```

---

## 🔄 ERROR HANDLING & RETRY MECHANISMS

### **Retry Strategy**
```javascript
async function sendMessageToApi(message, retryCount = 0) {
    try {
        const response = await fetch(API_ENDPOINT, {...});
        // Success
    } catch (error) {
        if (retryCount < 2 && !isAuthError) {
            // Retry with exponential backoff
            setTimeout(() => {
                sendMessageToApi(message, retryCount + 1);
            }, 2000 * (retryCount + 1));
        } else {
            // Show error with retry button
            displayErrorWithRetryButton();
        }
    }
}
```

### **Error Types**
- **400**: Invalid data → "Dữ liệu không hợp lệ"
- **401**: Unauthenticated → "Phiên đăng nhập đã hết hạn"
- **403**: Forbidden → "Không có quyền truy cập"
- **429**: Rate limit → "Bạn đã gửi quá nhiều tin nhắn"
- **500**: Server error → "Lỗi máy chủ"
- **503**: Service unavailable → "Dịch vụ tạm thời không khả dụng"

---

## 📊 PERFORMANCE METRICS

- **Response Time**: ~1-3 seconds (average)
- **Translation Time**: ~200-500ms (cached)
- **LLM Processing**: ~1-2 seconds
- **Database Operations**: ~50-100ms

---

## 🔐 SECURITY FEATURES

1. **Rate Limiting**: Prevent spam và abuse
2. **Input Validation**: Sanitize user input
3. **SQL Injection Prevention**: Parameterized queries
4. **XSS Prevention**: Content sanitization
5. **Session Management**: Secure session handling
6. **Authentication**: Session-based auth

---

## 💡 TÓM TẮT

**Luồng xử lý hoàn chỉnh:**
```
User Input → Frontend Validation → API Call → Rate Limiting → 
Input Sanitization → Language Detection → Translation → 
AI Context Building → Intent Classification → LLM Processing → 
Action Parsing & Execution → Response Generation → Translation → 
Database Storage → Frontend Display
```

**Các cách AI phản hồi:**
1. ✅ Natural conversation
2. ✅ Action-based responses
3. ✅ Confirmation dialogues
4. ✅ Information display
5. ✅ Guidance & instructions
6. ✅ Error messages
7. ✅ Warning alerts
8. ✅ Multi-step workflows
9. ✅ Payment links
10. ✅ Out-of-scope responses
11. ✅ Page reload triggers




