# THÔNG TIN TÍNH NĂNG: TẠO EVENT BẰNG AI

## 1. TỔNG QUAN TÍNH NĂNG

Tính năng cho phép người dùng tạo sự kiện (Event) thông qua tương tác với AI Agent bằng ngôn ngữ tự nhiên. AI sẽ phân tích yêu cầu của người dùng, trích xuất thông tin và tạo sự kiện trong hệ thống.

## 2. LUỒNG XỬ LÝ CHÍNH

### 2.1. Entry Point
- **API Endpoint**: `POST /api/ai/event/create`
- **Controller**: `EventAIController.createEvent()`
- **Service**: `AgentEventService.saveEventFromAction()`
- **AI Agent**: `EventAIAgent.processUserInput()`

### 2.2. Quy trình xử lý

1. **Người dùng gửi yêu cầu** (qua chat hoặc API)
   - Input: Câu nói tự nhiên (VD: "Tạo sự kiện Music Night vào 20h ngày 15/12 tại Nhà văn hóa")
   - Hoặc: JSON Action với toolName="ADD_EVENT"

2. **AI xử lý input**
   - Phân tích intent (VectorIntentClassifier)
   - Trích xuất thông tin từ ngôn ngữ tự nhiên
   - Tạo Action JSON với toolName="ADD_EVENT"

3. **Validation**
   - Kiểm tra action không null
   - Kiểm tra userId hợp lệ (> 0)
   - Kiểm tra toolName = "ADD_EVENT"
   - Kiểm tra các trường bắt buộc: title, start_time, end_time
   - Kiểm tra thời gian: start < end
   - Kiểm tra địa điểm tồn tại trong hệ thống
   - Kiểm tra xung đột thời gian/địa điểm

4. **Xử lý đặc biệt**
   - Nếu sự kiện ngoài trời: Kiểm tra thời tiết, hỏi xác nhận nếu có mưa
   - Nếu có xung đột: Thông báo và dừng tạo sự kiện

5. **Tạo Event**
   - Tạo EventItem từ Action args
   - Map sang Event entity (theo EventType)
   - Gán Host từ User
   - Gán Organization (nếu có)
   - Set default values (status=DRAFT, eventType=OTHERS nếu không có)
   - Lưu vào database
   - Tạo email reminder mặc định (5 phút trước)

6. **Response**
   - Success: Trả về message thành công và eventTitle
   - Error: Trả về error message

## 3. CẤU TRÚC DỮ LIỆU

### 3.1. Request Format

**API Request:**
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Tên sự kiện",
      "event_title": "Tên sự kiện (alias)",
      "start_time": "2024-12-15T20:00",
      "starts_at": "2024-12-15T20:00 (alias)",
      "end_time": "2024-12-15T22:00",
      "ends_at": "2024-12-15T22:00 (alias)",
      "place": "Tên địa điểm",
      "location": "Tên địa điểm (alias)",
      "description": "Mô tả sự kiện (optional)",
      "event_type": "MUSIC|WORKSHOP|FESTIVAL|COMPETITION|OTHERS",
      "organization_id": 123 (optional),
      "enroll_deadline": "2024-12-15T19:00" (optional),
      "capacity": 100 (optional)
    }
  },
  "userId": 1
}
```

**Natural Language Input:**
- "Tạo sự kiện Music Night vào 20h ngày 15/12 tại Nhà văn hóa"
- "Lên sự kiện workshop Python ngày mai lúc 14h"
- "Tạo festival văn hóa cuối tuần này"

### 3.2. Response Format

**Success Response:**
```json
{
  "success": true,
  "message": "✅ Đã tạo sự kiện thành công",
  "eventTitle": "Tên sự kiện"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "❌ Lỗi khi tạo sự kiện: [chi tiết lỗi]"
}
```

### 3.3. EventItem DTO
- `id`: Long (optional, null khi tạo mới)
- `title`: String (required)
- `description`: String (optional)
- `startsAt`: LocalDateTime (required)
- `endsAt`: LocalDateTime (required)
- `place`: List<Place> (required)
- `enrollDeadline`: LocalDateTime (optional, default = startsAt - 1 hour)
- `createdAt`: LocalDateTime (auto-generated)
- `eventType`: EventType enum (optional, default = OTHERS)
- `eventStatus`: EventStatus enum (default = DRAFT)

### 3.4. Event Entity
- `id`: Long (auto-generated)
- `title`: String (required, max 150 chars)
- `description`: String (optional, TEXT)
- `startsAt`: LocalDateTime (required)
- `endsAt`: LocalDateTime (required)
- `places`: List<Place> (ManyToMany)
- `host`: Host (ManyToOne, required)
- `organization`: Organization (ManyToOne, optional)
- `status`: EventStatus (default = DRAFT)
- `eventType`: EventType (required)
- `enrollDeadline`: LocalDateTime (optional)
- `createdAt`: LocalDateTime (auto-generated)
- `capacity`: Integer (optional)
- `imageUrl`: String (optional)
- `benefits`: String (optional)
- `learningObjects`: String (optional)
- `points`: Integer (optional)

### 3.5. Enums

**EventType:**
- MUSIC
- WORKSHOP
- COMPETITION
- FESTIVAL
- OTHERS (default)

**EventStatus:**
- DRAFT (default khi tạo)
- PUBLIC
- ONGOING
- CANCEL
- FINISH

## 4. VALIDATION RULES

### 4.1. Input Validation
- ✅ `action` không được null
- ✅ `userId` không được null và phải > 0
- ✅ `toolName` phải = "ADD_EVENT"
- ✅ `title` không được null hoặc empty
- ✅ `start_time` hoặc `starts_at` không được null
- ✅ `end_time` hoặc `ends_at` không được null
- ✅ `start_time` phải < `end_time`
- ✅ `place` hoặc `location` phải tồn tại trong hệ thống
- ✅ Địa điểm và thời gian không được trùng với sự kiện khác

### 4.2. Business Rules
- Event mặc định có status = DRAFT
- Event mặc định có eventType = OTHERS nếu không chỉ định
- EnrollDeadline mặc định = startsAt - 1 hour
- Tự động tạo email reminder (5 phút trước) khi tạo event
- User phải có Host (tự động lấy từ User)
- Organization phải tồn tại nếu organization_id được cung cấp

### 4.3. Date/Time Formats Supported
- `yyyy-MM-dd'T'HH:mm` (ISO format)
- `yyyy-MM-dd HH:mm`
- `dd/MM/yyyy HH:mm`
- `dd-MM-yyyy HH:mm`

## 5. ERROR CASES

### 5.1. Validation Errors
- ❌ Action không được để trống → 400 Bad Request
- ❌ User ID không hợp lệ → 400 Bad Request
- ❌ Chỉ hỗ trợ action ADD_EVENT → 400 Bad Request
- ❌ Thiếu thông tin sự kiện (tiêu đề hoặc thời gian) → Error message
- ❌ Thời gian không hợp lệ (start >= end) → Error message
- ❌ Địa điểm không hợp lệ hoặc không tìm thấy → Error message
- ❌ Sự kiện bị trùng thời gian/địa điểm → Warning message, không tạo

### 5.2. Business Logic Errors
- ❌ User không tồn tại → Exception
- ❌ Organization không tồn tại (nếu có orgId) → IllegalArgumentException
- ❌ Không thể parse ngày giờ → IllegalArgumentException
- ❌ Lỗi khi lưu vào database → RuntimeException

### 5.3. System Errors
- ❌ Database connection error → 500 Internal Server Error
- ❌ Embedding service không khả dụng → Fallback to name search
- ❌ Qdrant service error → Fallback to database search

## 6. EDGE CASES

### 6.1. Weather Check
- Nếu sự kiện ngoài trời và thời tiết có mưa:
  - Hệ thống hỏi xác nhận
  - Nếu user xác nhận "có" → Tạo event
  - Nếu user từ chối → Không tạo event

### 6.2. Time Conflict
- Kiểm tra xung đột thời gian/địa điểm trước khi tạo
- Nếu có xung đột → Thông báo và không tạo event
- User phải chọn thời gian/địa điểm khác

### 6.3. Place Search
- Tìm kiếm địa điểm bằng vector embedding (Qdrant)
- Fallback: Tìm kiếm bằng tên trong database
- Nếu không tìm thấy → Error

### 6.4. Event Type Handling
- Nếu eventType null → Default to OTHERS
- Nếu eventType không hợp lệ → Default to OTHERS
- Tạo đúng loại Event entity (WorkshopEvent, MusicEvent, etc.)

### 6.5. Organization
- Organization ID optional
- Nếu có organizationId nhưng không tồn tại → Exception
- Nếu không có organizationId → Set null

### 6.6. Pending Events
- Hệ thống lưu pending events theo sessionId
- Nếu có pending event và user xác nhận → Tạo event
- Nếu user từ chối → Xóa pending event

## 7. DEPENDENCIES

### 7.1. Services
- `AgentEventService`: Xử lý logic tạo event
- `EventService`: Quản lý events, kiểm tra xung đột
- `PlaceService`: Tìm kiếm địa điểm
- `UserService`: Lấy thông tin user và host
- `OrganizationService`: Quản lý organizations
- `EmailReminderService`: Tạo email reminders
- `WeatherService`: Kiểm tra thời tiết
- `EmbeddingService`: Tạo vector embeddings
- `QdrantService`: Vector search
- `VectorIntentClassifier`: Phân loại intent

### 7.2. Repositories
- `IEventRepo`: Lưu event vào database
- `IEmailReminderRepo`: Lưu email reminders

### 7.3. External Services
- LLM (Language Model): Xử lý ngôn ngữ tự nhiên
- Qdrant: Vector database cho semantic search
- Weather API: Lấy thông tin thời tiết

## 8. TESTING SCENARIOS

### 8.1. Happy Path
1. Tạo event với đầy đủ thông tin hợp lệ
2. Tạo event với thông tin tối thiểu (chỉ title, start, end, place)
3. Tạo event với organization
4. Tạo event với các loại eventType khác nhau
5. Tạo event với description và các trường optional

### 8.2. Validation Tests
1. Test với action null
2. Test với userId null hoặc <= 0
3. Test với toolName khác "ADD_EVENT"
4. Test với title null/empty
5. Test với start_time null
6. Test với end_time null
7. Test với start_time >= end_time
8. Test với place không tồn tại
9. Test với date format không hợp lệ

### 8.3. Business Logic Tests
1. Test xung đột thời gian/địa điểm
2. Test tạo event với eventType null → default OTHERS
3. Test tạo event với eventType không hợp lệ → default OTHERS
4. Test enrollDeadline tự động = startsAt - 1 hour
5. Test email reminder tự động tạo (5 phút trước)
6. Test organization không tồn tại
7. Test user không có host

### 8.4. Edge Cases
1. Test weather check cho sự kiện ngoài trời
2. Test pending event confirmation flow
3. Test place search với vector embedding
4. Test place search fallback
5. Test với các date format khác nhau
6. Test với timezone khác nhau
7. Test với title dài (max 150 chars)
8. Test với description rất dài

### 8.5. Error Handling Tests
1. Test database connection error
2. Test embedding service không khả dụng
3. Test Qdrant service error
4. Test exception khi save event
5. Test exception khi tạo email reminder

### 8.6. Integration Tests
1. Test end-to-end flow từ API đến database
2. Test với real LLM response
3. Test với real Qdrant search
4. Test với real weather service
5. Test với multiple concurrent requests

## 9. TESTING TECHNIQUES CẦN ÁP DỤNG

### 9.1. Unit Testing
- Test từng method riêng lẻ
- Mock các dependencies
- Test validation logic
- Test business rules

### 9.2. Integration Testing
- Test API endpoints
- Test service interactions
- Test database operations
- Test external service calls

### 9.3. Functional Testing
- Test các use cases chính
- Test các edge cases
- Test error handling
- Test user flows

### 9.4. Performance Testing
- Test response time
- Test concurrent requests
- Test với large datasets
- Test database query performance

### 9.5. Security Testing
- Test input validation
- Test SQL injection
- Test XSS attacks
- Test authorization

### 9.6. Usability Testing
- Test với natural language inputs khác nhau
- Test với các ngôn ngữ khác nhau
- Test với các format date/time khác nhau
- Test error messages rõ ràng

## 10. MOCK DATA EXAMPLES

### 10.1. Valid Request
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Music Night 2024",
      "start_time": "2024-12-15T20:00",
      "end_time": "2024-12-15T22:00",
      "place": "Nhà văn hóa Đà Nẵng",
      "description": "Đêm nhạc acoustic với các nghệ sĩ địa phương",
      "event_type": "MUSIC"
    }
  },
  "userId": 1
}
```

### 10.2. Minimal Request
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Workshop Python",
      "start_time": "2024-12-20T14:00",
      "end_time": "2024-12-20T17:00",
      "place": "Trung tâm đào tạo"
    }
  },
  "userId": 1
}
```

### 10.3. Invalid Requests
```json
// Missing title
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "start_time": "2024-12-15T20:00",
      "end_time": "2024-12-15T22:00"
    }
  },
  "userId": 1
}

// Invalid time (start >= end)
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Test Event",
      "start_time": "2024-12-15T22:00",
      "end_time": "2024-12-15T20:00"
    }
  },
  "userId": 1
}

// Invalid userId
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Test Event",
      "start_time": "2024-12-15T20:00",
      "end_time": "2024-12-15T22:00"
    }
  },
  "userId": -1
}
```

## 11. EXPECTED BEHAVIORS

### 11.1. Success Behaviors
- Event được tạo thành công trong database
- Status = DRAFT
- Host được gán từ User
- Email reminder được tạo (5 phút trước)
- Response trả về success message và eventTitle

### 11.2. Error Behaviors
- Validation errors trả về 400 Bad Request với error message
- Business logic errors trả về error message trong response
- System errors trả về 500 Internal Server Error
- Tất cả errors đều có message rõ ràng, dễ hiểu

### 11.3. Warning Behaviors
- Xung đột thời gian: Thông báo và không tạo event
- Thời tiết xấu: Hỏi xác nhận trước khi tạo
- Place không tìm thấy: Thông báo và không tạo event

## 12. TESTING CHECKLIST

### 12.1. Input Validation
- [ ] Action null
- [ ] UserId null/zero/negative
- [ ] ToolName khác "ADD_EVENT"
- [ ] Title null/empty
- [ ] Start time null/invalid format
- [ ] End time null/invalid format
- [ ] Start >= End
- [ ] Place null/not found

### 12.2. Business Logic
- [ ] Event được tạo với status DRAFT
- [ ] EventType default OTHERS khi null
- [ ] EnrollDeadline = startsAt - 1 hour
- [ ] Email reminder được tạo
- [ ] Host được gán từ User
- [ ] Organization được gán nếu có
- [ ] Time conflict detection
- [ ] Weather check cho outdoor events

### 12.3. Edge Cases
- [ ] Pending event confirmation
- [ ] Multiple date formats
- [ ] Long title (150 chars)
- [ ] Very long description
- [ ] Special characters in title
- [ ] Unicode characters
- [ ] Empty description
- [ ] Null organization

### 12.4. Error Handling
- [ ] Database errors
- [ ] Service unavailable errors
- [ ] Network errors
- [ ] Invalid data format
- [ ] Missing required fields
- [ ] Concurrent requests

### 12.5. Integration
- [ ] API endpoint works
- [ ] Database save works
- [ ] Email reminder creation
- [ ] Place search works
- [ ] Time conflict check works
- [ ] Weather service integration

---

**Tài liệu này cung cấp đầy đủ thông tin để tạo bảng Testing Techniques cho tính năng Tạo Event bằng AI.**

