# 📋 LUỒNG XỬ LÝ KHI TẠO SỰ KIỆN QUA AI

## 🎯 Tổng quan

Tài liệu này mô tả chi tiết luồng xử lý từ khi người dùng yêu cầu tạo sự kiện qua chatbot AI cho đến khi sự kiện được lưu vào database.

---

## 🔄 LUỒNG XỬ LÝ CHI TIẾT

### **BƯỚC 1: Người dùng nhập yêu cầu**

```
Người dùng: "Tạo sự kiện Music Night vào 20h ngày 15/12 tại Nhà văn hóa"
```

**File liên quan:**
- `chatbot.js` (Frontend)
- Xử lý ở Frontend: gửi POST request đến `/api/ai/chat/enhanced`

---

### **BƯỚC 2: Controller nhận request**

**File:** `EnhancedAIController.java`

**Các bước xử lý:**
1. ✅ **Xác thực phiên đăng nhập** - Lấy `userId` từ HTTP session
2. ✅ **Rate Limiting** - Kiểm tra giới hạn số lượng request
3. ✅ **Input Validation** - Validate và sanitize input từ người dùng
   - Kiểm tra độ dài (không quá 1000 ký tự)
   - Kiểm tra SQL injection, XSS injection

**File:** `src/main/java/com/group02/openevent/controller/.../EnhancedAIController.java`

---

### **BƯỚC 3: Phát hiện và dịch ngôn ngữ**

**File:** `LanguageDetectionService.java`

1. **Phát hiện ngôn ngữ** - Xác định ngôn ngữ của tin nhắn (Vietnamese, English, Chinese, etc.)
2. **Dịch sang tiếng Việt** (nếu cần) - Dịch input sang tiếng Việt để AI xử lý
   - Sử dụng LibreTranslate API
   - Có cache để tối ưu performance

---

### **BƯỚC 4: ChatSessionService xử lý**

**File:** `ChatSessionService.java`

**Luồng:**
```java
ChatSessionService.chat(ChatRequest request)
↓
1. Lưu tin nhắn người dùng vào database (ChatMessage)
2. Gọi EventAIAgent.reply() để generate AI response
3. Lưu AI response vào database
4. Trả về ChatReply
```

---

### **BƯỚC 5: EventAIAgent xử lý logic chính**

**File:** `EventAIAgent.java`

#### **5.1: Xây dựng Context**
```java
buildConversationContext(sessionId, userId)
```
- Load system prompt (hướng dẫn cho AI)
- Load 10 tin nhắn gần nhất từ conversation history
- Thêm thông tin ngày hiện tại, thông tin user

#### **5.2: Kiểm tra các trường hợp đặc biệt**
- ✅ Kiểm tra câu hỏi ngoài phạm vi (`isOutOfScope()`)
- ✅ Kiểm tra câu hỏi về thời tiết (`isWeatherQuestion()`)
- ✅ Kiểm tra pending order (nếu đang mua vé)

#### **5.3: Intent Classification**
```java
float[] userVector = embeddingService.getEmbedding(userInput);
ActionType intent = classifier.classifyIntent(userInput, userVector);
```
- Tạo embedding vector từ user input
- Phân loại intent: `ADD_EVENT`, `BUY_TICKET`, `UPDATE_EVENT`, etc.

#### **5.4: Gọi LLM để generate response**
```java
context.add(new Message("user", userInput));
String aiResponse = llm.generateResponse(context);
```
- Model: Qwen/Qwen3-Embedding-8B
- Input: Full conversation context
- Output: Text response + JSON actions (nếu có)

#### **5.5: Parse JSON Actions từ LLM response**
```java
Pattern jsonPattern = Pattern.compile("(\\[\\s*\\{[\\s\\S]*?\\}\\s*\\])");
Matcher matcher = jsonPattern.matcher(aiResponse);
String jsonPart = matcher.find() ? matcher.group() : null;
List<Action> actions = tryParseActions(jsonPart);
```

**Ví dụ JSON từ LLM:**
```json
[
  {
    "toolName": "ADD_EVENT",
    "args": {
      "event_title": "Music Night",
      "starts_at": "2024-12-15T20:00",
      "ends_at": "2024-12-15T22:00",
      "place": "Nhà văn hóa",
      "description": "Sự kiện âm nhạc đêm",
      "event_type": "MUSIC"
    }
  }
]
```

---

### **BƯỚC 6: Thực thi Action ADD_EVENT**

**File:** `EventAIAgent.java` - Phương thức `processUserInput()`

#### **6.1: Extract thông tin từ Action**
```java
case "ADD_EVENT" -> {
    Map<String, Object> args = action.getArgs();
    String title = getStr(args, "title", "event_title", "name");
    LocalDateTime start = getTime(args, "start_time", "starts_at", "start");
    LocalDateTime end = getTime(args, "end_time", "ends_at", "end");
    String placeName = getStr(args, "place", "location");
}
```

#### **6.2: Validation thông tin**
- ✅ Kiểm tra title, start, end không null
- ✅ Kiểm tra start phải trước end
- ✅ Kiểm tra địa điểm hợp lệ

#### **6.3: Tìm kiếm Place (Vector Search)**
```java
float[] placeVec = embeddingService.getEmbedding(placeName);
List<Map<String, Object>> searchResults = qdrantService.searchPlacesByVector(placeVec, 1);
```
- Tạo embedding vector cho tên địa điểm
- Tìm kiếm trong Qdrant vector database
- Fallback: Tìm trong database thông thường nếu không tìm thấy

**File:** `QdrantService.java`
- Sử dụng vector search với filter `kind: "place"`
- Trả về place_id từ payload

#### **6.4: Kiểm tra trùng thời gian/địa điểm**
```java
List<Event> conflicted = eventService.isTimeConflict(start, end, List.of(place));
if (!conflicted.isEmpty()) {
    // Báo lỗi trùng thời gian
}
```

#### **6.5: Kiểm tra thời tiết (cho sự kiện ngoài trời)**
```java
String intentWeather = classifier.classifyWeather(userInput, userVector);
if ("outdoor_activities".equals(intentWeather)) {
    String forecastNote = weatherService.getForecastNote(start, "Da Nang");
    if (forecastNote.contains("rain")) {
        // Lưu vào pendingEvents và hỏi xác nhận từ user
        pendingEvents.put(sessionId, new PendingEvent(event));
        return "🌦 Dự báo có mưa. Bạn có muốn tiếp tục?";
    }
}
```

#### **6.6: Tạo EventItem object**
```java
EventItem event = new EventItem();
event.setTitle(title);
event.setStartsAt(start);
event.setEndsAt(end);
event.setCreatedAt(LocalDateTime.now());
event.setEnrollDeadline(defaultDeadline);
event.setEventStatus(EventStatus.DRAFT);
event.setEventType(EventType.MUSIC); // hoặc từ args
event.setPlace(List.of(place));
```

#### **6.7: Gọi AgentEventService để lưu**
```java
Event saved = agentEventService.createEventByCustomer(userId, event, orgId);
```

---

### **BƯỚC 7: AgentEventService tạo sự kiện**

**File:** `AgentEventService.java` - Phương thức `createEventByCustomer()`

#### **7.1: Load hoặc tạo Customer**
```java
Customer c = customerService.getOrCreateByUserId(userId);
```

#### **7.2: Tạo Event object theo loại**
```java
EventType draftType = draft.getEventType();
switch (draftType) {
    case WORKSHOP -> event = new WorkshopEvent();
    case MUSIC -> event = new MusicEvent();
    case FESTIVAL -> event = new FestivalEvent();
    case COMPETITION -> event = new CompetitionEvent();
    default -> event = new Event();
}
```

#### **7.3: Map dữ liệu từ EventItem sang Event**
```java
AIEventMapper.createEventFromRequest(draft, event);
```
**File:** `AIEventMapper.java`
- Copy title, description, startsAt, endsAt, etc.
- Set places, enrollDeadline, status, eventType

#### **7.4: Tìm hoặc tạo Host**
```java
Host h = c.getHost();
if (h == null) {
    h = hostService.findByCustomerId(c.getCustomerId())
        .orElseGet(() -> {
            Host nh = new Host();
            nh.setCustomer(c);
            return hostService.save(nh);
        });
}
event.setHost(h);
```
- Customer tự động được promote thành Host khi tạo sự kiện

#### **7.5: Set Organization (nếu có)**
```java
if (organizationId != null) {
    Organization org = organizationService.findById(organizationId)
        .orElseThrow(...);
    event.setOrganization(org);
}
```

#### **7.6: Set default values**
```java
if (event.getStatus() == null) event.setStatus(EventStatus.DRAFT);
if (event.getEventType() == null) event.setEventType(EventType.OTHERS);
if (event.getCreatedAt() == null) event.setCreatedAt(LocalDateTime.now());
```

#### **7.7: Lưu Event vào Database**
```java
Event savedEvent = eventRepo.save(event);
```

#### **7.8: Tạo Email Reminder mặc định**
```java
createOrUpdateEmailReminder(savedEvent.getId(), 5, userId);
```
- Tạo reminder 5 phút trước khi sự kiện bắt đầu
- Lưu vào bảng `EmailReminder`

---

### **BƯỚC 8: Response về Frontend**

**File:** `EventAIAgent.java`

```java
systemResult.append("✅ Đã thêm sự kiện: ").append(saved.getTitle()).append("\n");
String fullResponse = userVisibleText + "\n\n" + systemResult.toString();
return fullResponse;
```

**Ví dụ response:**
```
✅ Đã thêm sự kiện: Music Night

Sự kiện của bạn đã được tạo thành công và lưu vào hệ thống!
```

#### **8.1: Dịch response về ngôn ngữ gốc**
```java
if (userLanguage != Language.VIETNAMESE) {
    translatedResponse = translationService.translateAIResponse(reply.message(), userLanguage);
}
```

#### **8.2: Validate response**
```java
ValidationResult responseValidation = securityService.validateAIResponse(translatedResponse);
```

---

### **BƯỚC 9: Frontend hiển thị kết quả**

**File:** `chatbot.js`

1. Nhận response từ API
2. Hiển thị message lên UI
3. Có thể reload page nếu cần (flag `__RELOAD__`)

---

## 📊 SƠ ĐỒ LUỒNG TỔNG QUAN

```
User Input
    ↓
Frontend (chatbot.js)
    ↓
POST /api/ai/chat/enhanced
    ↓
EnhancedAIController
    ├─ Rate Limiting
    ├─ Input Validation
    └─ Language Detection & Translation
        ↓
ChatSessionService.chat()
    ├─ Save user message to DB
    └─ Call EventAIAgent.reply()
        ↓
EventAIAgent.processUserInput()
    ├─ Build Context (system prompt + history)
    ├─ Check special cases (out of scope, weather)
    ├─ Intent Classification (Vector Embedding)
    ├─ Call LLM (generate response + JSON actions)
    ├─ Parse JSON Actions
    └─ Execute ADD_EVENT action
        ├─ Extract event info (title, time, place)
        ├─ Validate info
        ├─ Vector Search Place in Qdrant
        ├─ Check time conflict
        ├─ Check weather (for outdoor events)
        ├─ Create EventItem
        └─ Call AgentEventService.createEventByCustomer()
            ├─ Get/Create Customer
            ├─ Create Event object (by type)
                    ├─ Map EventItem → Event
                    ├─ Get/Create Host
                    ├─ Set Organization (optional)
                    ├─ Set defaults
                    ├─ Save to Database (eventRepo.save())
                    └─ Create Email Reminder (5 min before)
            ↓
Response Assembly
    ├─ Combine text + system result
    └─ Translate (if needed)
        ↓
Save AI response to DB
    ↓
Return to Frontend
    ↓
Display to User
```

---

## 🔑 CÁC THÀNH PHẦN CHÍNH

### **1. EventAIAgent**
- **Vai trò:** Xử lý logic AI, phân tích intent, gọi LLM, thực thi actions
- **File:** `src/main/java/com/group02/openevent/ai/service/EventAIAgent.java`

### **2. AgentEventService**
- **Vai trò:** Xử lý business logic tạo/sửa/xóa sự kiện
- **File:** `src/main/java/com/group02/openevent/ai/service/AgentEventService.java`

### **3. QdrantService**
- **Vai trò:** Tương tác với Qdrant vector database để tìm kiếm Place
- **File:** `src/main/java/com/group02/openevent/ai/qdrant/service/QdrantService.java`

### **4. EmbeddingService**
- **Vai trò:** Tạo embedding vectors từ text (cho vector search)
- **File:** `src/main/java/com/group02/openevent/ai/service/EmbeddingService.java`

### **5. VectorIntentClassifier**
- **Vai trò:** Phân loại intent từ user input bằng vector similarity
- **File:** `src/main/java/com/group02/openevent/ai/qdrant/service/VectorIntentClassifier.java`

---

## ⚠️ CÁC TRƯỜNG HỢP ĐẶC BIỆT

### **1. Trùng thời gian/địa điểm**
```java
List<Event> conflicted = eventService.isTimeConflict(start, end, places);
if (!conflicted.isEmpty()) {
    // Báo lỗi và không tạo sự kiện
}
```

### **2. Thời tiết có mưa (sự kiện ngoài trời)**
```java
// Lưu vào pendingEvents, hỏi xác nhận từ user
pendingEvents.put(sessionId, new PendingEvent(event));
return "🌦 Dự báo có mưa. Bạn có muốn tiếp tục?";
```

### **3. Thiếu thông tin**
```java
if (title == null || start == null || end == null) {
    systemResult.append("📝 Thiếu thông tin sự kiện...");
    break; // Không tạo sự kiện
}
```

### **4. Không tìm thấy địa điểm**
```java
if (placeOpt.isEmpty()) {
    systemResult.append("⛔ Không tìm thấy địa điểm...");
    break;
}
```

---

## 💾 LƯU TRỮ DỮ LIỆU

### **1. ChatMessage**
- Lưu tin nhắn của user và AI response
- **Table:** `chat_message`
- **Columns:** `session_id`, `user_id`, `message`, `is_from_user`, `timestamp`

### **2. Event**
- Lưu thông tin sự kiện
- **Table:** `event` (có các subclass: `music_event`, `workshop_event`, etc.)
- **Columns:** `id`, `title`, `description`, `starts_at`, `ends_at`, `host_id`, `organization_id`, etc.

### **3. EmailReminder**
- Lưu lịch nhắc nhở email
- **Table:** `email_reminder`
- **Columns:** `id`, `event_id`, `user_id`, `remind_minutes`, `sent`, `created_at`

### **4. Qdrant (Vector Database)**
- Lưu embeddings cho Place và Event
- **Collection:** Configurable (default: `events_collection`)
- **Payload:** `kind`, `place_id`, `event_id`, `startsAt`, etc.

---

## 🎯 KẾT QUẢ CUỐI CÙNG

Sau khi hoàn thành luồng xử lý:
1. ✅ Sự kiện được lưu vào database với status `DRAFT`
2. ✅ Customer được tự động promote thành Host (nếu chưa là Host)
3. ✅ Email reminder mặc định được tạo (5 phút trước khi sự kiện bắt đầu)
4. ✅ Response được gửi về frontend thông báo thành công
5. ✅ Tất cả messages được lưu vào ChatMessage table

---

## 📝 LƯU Ý QUAN TRỌNG

1. **Transaction:** Toàn bộ quá trình tạo sự kiện được wrap trong `@Transactional` để đảm bảo data consistency
2. **Error Handling:** Nếu có lỗi ở bất kỳ bước nào, hệ thống sẽ:
   - Log lỗi chi tiết
   - Trả về message lỗi thân thiện cho user
   - Không rollback transaction nếu lỗi không nghiêm trọng (ví dụ: tạo reminder thất bại)
3. **Vector Search:** Hệ thống sử dụng vector similarity search để tìm kiếm Place, cho phép tìm kiếm linh hoạt hơn so với exact match
4. **Pending Events:** Sự kiện có thể được lưu tạm trong `pendingEvents` map nếu cần xác nhận từ user (ví dụ: thời tiết có mưa)

---

## 🔍 TÀI LIỆU THAM KHẢO

- `AI_WORKFLOW_DETAILED.md` - Tài liệu tổng quan về AI workflow
- `EventAIAgent.java` - File xử lý logic AI chính
- `AgentEventService.java` - File xử lý business logic tạo event
- `QdrantService.java` - File tương tác với vector database


