# Decision Table Testing: Event Management AI — Quản lý sự kiện

## Feature được kiểm thử

**Tên feature/nghiệp vụ:** Event Management AI — Quản lý sự kiện
- Tạo sự kiện từ câu lệnh tự nhiên
- Tự động parse thông tin (tên, thời gian, địa điểm)
- Kiểm tra xung đột lịch
- Cảnh báo thời tiết cho sự kiện ngoài trời

**Mã/Link nguồn yêu cầu (BRD/SRS/User Story/AC/Jira):**
- Controller: `EventAIController` (`/api/ai/event/*`)
- Service: `EventAIAgent`, `AgentEventService`
- Weather Service: `WeatherService`
- Conflict Check: `EventService.isTimeConflict()`

---

## Luật nghiệp vụ (Business Rules)

**BR-01:** IF user chưa đăng nhập hoặc `userId` không hợp lệ THEN trả về 400 "User ID không hợp lệ"

**BR-02:** IF `action` null hoặc rỗng THEN trả về 400 "Action không được để trống"

**BR-03:** IF `toolName` không phải "ADD_EVENT" (khi tạo) THEN trả về 400 "Chỉ hỗ trợ action ADD_EVENT"

**BR-04:** IF thiếu thông tin bắt buộc (title, start_time, end_time) THEN không tạo event, trả về message "Thiếu thông tin sự kiện"

**BR-05:** IF `start_time >= end_time` THEN không tạo event, trả về "Thời gian không hợp lệ: bắt đầu phải trước kết thúc"

**BR-06:** IF địa điểm (place) không tồn tại trong hệ thống THEN không tạo event, trả về "Không tìm thấy địa điểm"

**BR-07:** IF phát hiện xung đột thời gian/địa điểm với event khác THEN không tự động tạo, hỏi lại user hoặc trả về danh sách xung đột

**BR-08:** IF sự kiện là outdoor activity VÀ thời tiết có cảnh báo mưa (rainChance > 50% hoặc condition chứa "rain") THEN tạm dừng tạo event, hỏi user xác nhận trước khi tiếp tục

**BR-09:** IF parse datetime thành công (theo các format: yyyy-MM-dd'T'HH:mm, yyyy-MM-dd HH:mm, dd/MM/yyyy HH:mm, dd-MM-yyyy HH:mm) THEN sử dụng datetime đã parse

**BR-10:** IF parse datetime thất bại THEN throw `IllegalArgumentException` "Không thể parse ngày giờ"

**BR-11:** IF tất cả điều kiện hợp lệ THEN tạo event thành công, trả về 200 với message "Đã tạo sự kiện thành công"

**BR-12:** IF kiểm tra conflict với `startTime`, `endTime`, `place` hợp lệ THEN trả về danh sách conflicts (có thể rỗng)

**BR-13:** IF kiểm tra conflict với format datetime không hợp lệ THEN trả về 500 Internal Server Error

**BR-14:** IF lấy thông tin thời tiết thành công VÀ có cảnh báo (forecast != null && !forecast.isEmpty() && chứa "rain" hoặc rainChance > 50%) THEN `hasWeatherWarning = true`

**BR-15:** IF lấy thông tin thời tiết thất bại hoặc không có cảnh báo THEN `hasWeatherWarning = false`

**BR-16:** IF user xác nhận tiếp tục tạo event sau cảnh báo thời tiết THEN tạo event bình thường

**BR-17:** IF user từ chối tạo event sau cảnh báo thời tiết THEN hủy tạo event, trả về "Đã hủy tạo sự kiện"

---

## Điều kiện (Conditions) – Cx

**C1 (Boolean):** User authenticated & userId valid?
- **Miền giá trị:** `true` (userId > 0), `false` (userId == null || userId <= 0)
- **Equivalence Classes:** Valid userId, Invalid userId

**C2 (Boolean):** Action provided & not null?
- **Miền giá trị:** `true` (action != null), `false` (action == null)
- **Equivalence Classes:** Action present, Action missing

**C3 (Enum):** Tool name valid?
- **Miền giá trị:** `ADD_EVENT`, `UPDATE_EVENT`, `DELETE_EVENT`, `INVALID` (khác)
- **Equivalence Classes:** Valid tool, Invalid tool

**C4 (String):** Required fields present (title, start_time, end_time)?
- **Miền giá trị:** `COMPLETE` (cả 3 đều có), `INCOMPLETE` (thiếu ít nhất 1)
- **Equivalence Classes:** All present, Missing title, Missing start_time, Missing end_time

**C5 (DateTime Range):** Time window valid (start < end)?
- **Miền giá trị:** `VALID` (start < end), `INVALID` (start >= end)
- **Boundary:** start == end (invalid), start = end - 1 second (valid)

**C6 (String/Place):** Place exists in system?
- **Miền giá trị:** `EXISTS` (tìm thấy trong DB/Qdrant), `NOT_FOUND` (không tìm thấy)
- **Equivalence Classes:** Place found, Place not found, Place name empty

**C7 (Boolean):** Time conflict detected?
- **Miền giá trị:** `HAS_CONFLICT` (conflicts.size() > 0), `NO_CONFLICT` (conflicts.isEmpty())
- **Equivalence Classes:** Conflict exists, No conflict

**C8 (Enum):** Event type is outdoor activity?
- **Miền giá trị:** `OUTDOOR` (classifyWeather returns "outdoor_activities"), `INDOOR` (khác)
- **Equivalence Classes:** Outdoor event, Indoor event

**C9 (Boolean):** Weather warning exists (rain forecast)?
- **Miền giá trị:** `HAS_WARNING` (forecast != null && !forecast.isEmpty() && (contains "rain" || rainChance > 50%)), `NO_WARNING` (khác)
- **Equivalence Classes:** Warning present, No warning, Forecast null/empty

**C10 (DateTime Format):** DateTime parseable?
- **Miền giá trị:** `PARSEABLE` (khớp 1 trong 4 format), `NOT_PARSEABLE` (không khớp format nào)
- **Equivalence Classes:** Format yyyy-MM-dd'T'HH:mm, yyyy-MM-dd HH:mm, dd/MM/yyyy HH:mm, dd-MM-yyyy HH:mm, Invalid format

**C11 (Boolean):** User confirms after weather warning?
- **Miền giá trị:** `CONFIRMED` (userInput chứa "có"/"ok"/"tiếp tục"), `REJECTED` (chứa "không"), `UNCLEAR` (khác)
- **Equivalence Classes:** Confirmed, Rejected, Unclear response

---

## Hành động/Kết quả (Actions) – Ax

**A1:** Trả về 400 Bad Request với message "User ID không hợp lệ"

**A2:** Trả về 400 Bad Request với message "Action không được để trống"

**A3:** Trả về 400 Bad Request với message "Chỉ hỗ trợ action ADD_EVENT"

**A4:** Không tạo event, trả về message "Thiếu thông tin sự kiện (tiêu đề hoặc thời gian)"

**A5:** Không tạo event, trả về message "Thời gian không hợp lệ: bắt đầu phải trước kết thúc"

**A6:** Không tạo event, trả về message "Không tìm thấy địa điểm [placeName]" hoặc "Vui lòng cung cấp tên địa điểm"

**A7:** Không tự động tạo event, trả về danh sách conflicts và message "Sự kiện bị trùng thời gian/địa điểm với: [danh sách]"

**A8:** Tạm dừng tạo event, lưu vào `PendingEvent`, trả về cảnh báo thời tiết và hỏi "Bạn có muốn tiếp tục tạo sự kiện này không?"

**A9:** Parse datetime thành công, sử dụng `LocalDateTime` đã parse

**A10:** Throw `IllegalArgumentException` "Không thể parse ngày giờ: [input]"

**A11:** Tạo event thành công, lưu vào DB, tạo email reminder mặc định, trả về 200 với message "Đã tạo sự kiện thành công"

**A12:** Trả về 200 với danh sách conflicts (có thể rỗng), `hasConflict = !conflicts.isEmpty()`

**A13:** Trả về 500 Internal Server Error khi parse datetime thất bại trong check-conflict

**A14:** Trả về `hasWeatherWarning = true` trong response weather API

**A15:** Trả về `hasWeatherWarning = false` trong response weather API

**A16:** Tiếp tục tạo event từ `PendingEvent`, lưu vào DB, trả về "Đã tạo sự kiện: [title]"

**A17:** Hủy `PendingEvent`, trả về "Đã hủy tạo sự kiện do bạn từ chối"

---

## Ưu tiên xung đột (Precedence/Priority)

1. **C1 (Authentication)** → A1 (kiểm tra đầu tiên)
2. **C2 (Action present)** → A2 (kiểm tra sau authentication)
3. **C3 (Tool name)** → A3 (kiểm tra tool name)
4. **C4 (Required fields)** → A4 (kiểm tra đầy đủ thông tin)
5. **C10 (DateTime parseable)** → A9/A10 (parse datetime trước khi validate time window)
6. **C5 (Time window)** → A5 (validate time window sau khi parse)
7. **C6 (Place exists)** → A6 (kiểm tra place sau khi validate time)
8. **C7 (Conflict)** → A7 (kiểm tra conflict sau khi có place)
9. **C8 (Outdoor) + C9 (Weather warning)** → A8 (kiểm tra weather nếu outdoor)
10. **C11 (User confirmation)** → A16/A17 (xử lý confirmation sau weather warning)
11. **Tất cả pass** → A11 (tạo event thành công)

---

## Tổ hợp không khả thi (Infeasible Combos)

**IC-01:** C1=false nhưng A11 (không thể tạo event khi chưa authenticate)

**IC-02:** C2=false nhưng A11 (không thể tạo event khi không có action)

**IC-03:** C3=INVALID nhưng A11 (không thể tạo event với tool name không hợp lệ)

**IC-04:** C4=INCOMPLETE nhưng A11 (không thể tạo event khi thiếu thông tin)

**IC-05:** C5=INVALID nhưng A11 (không thể tạo event với time window không hợp lệ)

**IC-06:** C6=NOT_FOUND nhưng A11 (không thể tạo event khi place không tồn tại)

**IC-07:** C10=NOT_PARSEABLE nhưng A9 (không thể parse datetime nếu format không hợp lệ)

**IC-08:** C8=INDOOR nhưng C9=HAS_WARNING (indoor event không cần kiểm tra weather warning)

**IC-09:** C9=NO_WARNING nhưng A8 (không thể cảnh báo weather nếu không có warning)

**IC-10:** C11=UNCLEAR nhưng A16/A17 (không thể xác nhận/hủy nếu user response không rõ)

---

## Fallback/Default

- **Nếu không khớp luật nào:** Trả về 500 Internal Server Error với message "Lỗi khi tạo sự kiện: [exception message]"
- **Nếu parse datetime thất bại:** Throw `IllegalArgumentException` và catch ở controller, trả về 500
- **Nếu weather service lỗi:** Log error, tiếp tục tạo event (không block), không hiển thị weather warning
- **Nếu conflict check lỗi:** Log error, tiếp tục tạo event (không block), không hiển thị conflict warning

---

## Ràng buộc kỹ thuật & bối cảnh test

**Vai trò/Phân quyền liên quan:**
- User phải đăng nhập (có `userId` hợp lệ)
- User có thể là Customer (sẽ tự động promote thành Host khi tạo event đầu tiên)

**Tiền điều kiện (Preconditions):**
- User đã đăng nhập, có `userId` trong session
- Database có sẵn bảng `events`, `places`, `hosts`, `customers`
- Weather API key đã được cấu hình trong `ConfigLoader`
- Qdrant service đang chạy (nếu dùng vector search cho place)

**Hậu điều kiện (Postconditions):**
- Event được lưu vào DB với status `DRAFT`
- Host được tạo tự động nếu Customer chưa có Host
- Email reminder mặc định được tạo (5 phút trước event)
- `PendingEvent` được xóa khỏi session sau khi xác nhận/hủy

**Dữ liệu cần có:**
- Bảng `events`: id, title, starts_at, ends_at, status, host_id, place_id
- Bảng `places`: id, place_name
- Bảng `hosts`: id, customer_id
- Bảng `customers`: id, account_id
- Seed data: ít nhất 1 Place để test conflict

**API/Endpoint liên quan:**
- `POST /api/ai/event/create` - Tạo event từ AI action
- `POST /api/ai/event/check-conflict` - Kiểm tra xung đột
- `POST /api/ai/utility/weather` - Lấy thông tin thời tiết
- Request body mẫu:
  ```json
  {
    "action": {
      "toolName": "ADD_EVENT",
      "args": {
        "title": "Workshop Python",
        "start_time": "2025-01-15T10:00",
        "end_time": "2025-01-15T12:00",
        "place": "Main Hall",
        "description": "Workshop về Python",
        "event_type": "WORKSHOP"
      }
    },
    "userId": 1
  }
  ```

**Ngưỡng, tần suất, rate-limit:**
- Không có rate limit cụ thể cho Event AI (có thể áp dụng rate limit chung cho AI endpoints)
- Weather API có giới hạn request (tùy plan của weatherapi.com)

**Timezone/Locale/I18n:**
- Sử dụng server local timezone (thường là UTC hoặc Asia/Ho_Chi_Minh)
- Hỗ trợ parse datetime theo nhiều format (dd/MM/yyyy, yyyy-MM-dd, etc.)

---

## Ví dụ cụ thể (ít nhất 3–5 mẫu)

### Ví dụ 1: Happy Path - Tạo event thành công
**Input:**
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Workshop Python",
      "start_time": "2025-01-15T10:00",
      "end_time": "2025-01-15T12:00",
      "place": "Main Hall",
      "description": "Workshop về Python",
      "event_type": "WORKSHOP"
    }
  },
  "userId": 1
}
```
**Output:** 200 OK
```json
{
  "success": true,
  "message": "✅ Đã tạo sự kiện thành công",
  "eventTitle": "Workshop Python"
}
```
**DB State:** Event được lưu với status DRAFT, Host được tạo nếu chưa có

---

### Ví dụ 2: Thiếu thông tin bắt buộc
**Input:**
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Workshop Python",
      "start_time": "2025-01-15T10:00"
      // Thiếu end_time
    }
  },
  "userId": 1
}
```
**Output:** Message "📝 Thiếu thông tin sự kiện (tiêu đề hoặc thời gian)."
**DB State:** Không có event mới được tạo

---

### Ví dụ 3: Time window không hợp lệ
**Input:**
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Workshop Python",
      "start_time": "2025-01-15T12:00",
      "end_time": "2025-01-15T10:00",  // end < start
      "place": "Main Hall"
    }
  },
  "userId": 1
}
```
**Output:** Message "⛔ Thời gian không hợp lệ: bắt đầu phải trước kết thúc."
**DB State:** Không có event mới được tạo

---

### Ví dụ 4: Phát hiện xung đột thời gian
**Input:**
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Workshop Python",
      "start_time": "2025-01-15T10:00",
      "end_time": "2025-01-15T12:00",
      "place": "Main Hall"  // Đã có event khác ở đây trong khoảng thời gian này
    }
  },
  "userId": 1
}
```
**Output:** Message "⚠️ Sự kiện bị trùng thời gian/địa điểm với:\n - [Event Name] (2025-01-15T10:00 - 2025-01-15T12:00)"
**DB State:** Không có event mới được tạo

---

### Ví dụ 5: Cảnh báo thời tiết cho outdoor event
**Input:**
```json
{
  "action": {
    "toolName": "ADD_EVENT",
    "args": {
      "title": "Festival ngoài trời",
      "start_time": "2025-01-20T14:00",
      "end_time": "2025-01-20T18:00",
      "place": "Công viên",
      "event_type": "FESTIVAL"
    }
  },
  "userId": 1
}
```
**Weather API Response:** `rainChance = 60%`, `condition = "Heavy rain"`
**Output:** "🌦 Dự báo ngày 2025-01-20T14:00 tại Da Nang: Heavy rain 🌧 (khả năng mưa: 60%)\n❓Bạn có muốn tiếp tục tạo sự kiện này không?"
**DB State:** `PendingEvent` được lưu trong session, chờ user xác nhận

**Sau khi user xác nhận "có":**
**Output:** "📅 Đã tạo sự kiện: Festival ngoài trời"
**DB State:** Event được lưu vào DB

---

### Ví dụ 6: Check conflict API
**Input:**
```json
{
  "startTime": "2025-01-15T10:00",
  "endTime": "2025-01-15T12:00",
  "place": "Main Hall"
}
```
**Output:** 200 OK
```json
{
  "hasConflict": true,
  "conflicts": [
    {
      "id": 1,
      "title": "Existing Event",
      "startsAt": "2025-01-15T10:00",
      "endsAt": "2025-01-15T12:00"
    }
  ],
  "conflictCount": 1,
  "message": "⚠️ Phát hiện 1 xung đột thời gian"
}
```

---

### Ví dụ 7: Weather API
**Input:**
```json
{
  "location": "Da Nang",
  "date": "2025-01-20T14:00"
}
```
**Output:** 200 OK
```json
{
  "success": true,
  "location": "Da Nang",
  "date": "2025-01-20T14:00",
  "forecast": "Dự báo ngày 2025-01-20T14:00 tại Da Nang: Heavy rain 🌧 (khả năng mưa: 60%)",
  "hasWeatherWarning": true
}
```

---

## Giới hạn & giả định

**Giả định:**
- Weather API (weatherapi.com) luôn available (hoặc có fallback khi lỗi)
- Qdrant service available (nếu dùng vector search cho place)
- User có quyền tạo event (không cần check permission phức tạp)
- Datetime format từ LLM luôn theo một trong 4 format hỗ trợ

**Không thuộc phạm vi:**
- Validation phức tạp về capacity, ticket types (thuộc Event Management thông thường)
- Email reminder scheduling logic chi tiết (thuộc Email Reminder feature)
- Vector search algorithm chi tiết (thuộc Qdrant service)
- LLM prompt engineering (thuộc AI Agent logic)

---

## Tài liệu đính kèm

**Link code:**
- Controller: `src/main/java/com/group02/openevent/ai/controller/EventAIController.java`
- Service: `src/main/java/com/group02/openevent/ai/service/AgentEventService.java`
- AI Agent: `src/main/java/com/group02/openevent/ai/service/EventAIAgent.java`
- Weather Service: `src/main/java/com/group02/openevent/ai/service/WeatherService.java`
- Event Service: `src/main/java/com/group02/openevent/service/EventService.java` (method `isTimeConflict`)

**Link test:**
- `src/test/java/com/group02/openevent/ai/controller/EventAIControllerTest.java`
- `src/test/java/com/group02/openevent/ai/service/AgentEventServiceTest.java`
- `src/test/java/com/group02/openevent/ai/controller/AIUtilityControllerWeatherTest.java`
- `src/test/java/com/group02/openevent/service/EventServiceTest.java` (test `isTimeConflict`)

**Lược đồ DB:**
- `events` (id, title, starts_at, ends_at, status, host_id, created_at)
- `places` (id, place_name)
- `hosts` (id, customer_id)
- `customers` (id, account_id)
- `email_reminders` (id, event_id, user_id, remind_minutes, sent, created_at)

---

## Mapping Decision Table → Unit/Integration Tests

### BR-01 (User ID validation)
**Test:** `EventAIControllerTest.createEvent_toolValidation` (cần bổ sung test cho userId null/<=0)
- **Đề xuất thêm:** `createEvent_invalidUserId_400()`

### BR-02 (Action null)
**Test:** `EventAIControllerTest.createEvent_toolValidation` (cần bổ sung test cho action null)
- **Đề xuất thêm:** `createEvent_nullAction_400()`

### BR-03 (Tool name validation)
**Test:** `EventAIControllerTest.createEvent_toolValidation` - ✅ Đã có
- Test với `UPDATE_EVENT`, `DELETE_EVENT`, `FOO` → expect 400

### BR-04 (Required fields)
**Test:** `AgentEventServiceTest.saveEventFromAction_happy` (cần bổ sung test cho missing fields)
- **Đề xuất thêm:** `saveEventFromAction_missingTitle_throwsException()`
- **Đề xuất thêm:** `saveEventFromAction_missingStartTime_throwsException()`

### BR-05 (Time window validation)
**Test:** Cần bổ sung test trong `EventAIAgent` hoặc `AgentEventService`
- **Đề xuất thêm:** `saveEventFromAction_invalidTimeWindow_throwsException()`

### BR-06 (Place not found)
**Test:** `EventAIControllerTest.freeTime_placeNotFound_400` - ✅ Đã có (cho free-time, cần tương tự cho create)
- **Đề xuất thêm:** `createEvent_placeNotFound_returnsError()`

### BR-07 (Time conflict)
**Test:** `EventServiceTest.testIsTimeConflict` - ✅ Đã có
- **Test:** `EventAIControllerTest.checkConflict_ok` - ✅ Đã có
- **Đề xuất thêm:** `saveEventFromAction_hasConflict_returnsConflictMessage()`

### BR-08 (Weather warning for outdoor)
**Test:** `AIUtilityControllerWeatherTest.weather_ok_and_malformed_date_500` - ✅ Đã có
- **Đề xuất thêm:** `saveEventFromAction_outdoorWithRainWarning_pausesCreation()`

### BR-09/BR-10 (DateTime parsing)
**Test:** Cần bổ sung test trong `AgentEventService` hoặc `EventAIAgent`
- **Đề xuất thêm:** `tryParseDateTime_validFormats_returnsLocalDateTime()`
- **Đề xuất thêm:** `tryParseDateTime_invalidFormat_throwsException()`

### BR-11 (Create success)
**Test:** `AgentEventServiceTest.saveEventFromAction_happy` - ✅ Đã có
- **Test:** `AgentEventServiceTest.createEventByCustomer_withOrganization` - ✅ Đã có

### BR-12 (Check conflict API)
**Test:** `EventAIControllerTest.checkConflict_ok` - ✅ Đã có
- **Test:** `EventServiceTest.testIsTimeConflict` - ✅ Đã có

### BR-13 (Check conflict invalid format)
**Test:** `EventAIControllerTest.checkConflict_invalidTimeFormat_500` - ✅ Đã có

### BR-14/BR-15 (Weather warning flag)
**Test:** `AIUtilityControllerWeatherTest.weather_ok_and_malformed_date_500` - ✅ Đã có
- **Test:** `AIUtilityControllerTest.classifyIntent_weatherIntent` - ✅ Đã có

### BR-16/BR-17 (User confirmation after weather warning)
**Test:** Cần bổ sung test trong `EventAIAgent`
- **Đề xuất thêm:** `processUserInput_weatherWarningConfirmed_createsEvent()`
- **Đề xuất thêm:** `processUserInput_weatherWarningRejected_cancelsEvent()`

---

## Gợi ý Test Cases cần bổ sung

1. **`EventAIControllerTest.createEvent_invalidUserId_400()`**
   - Test với userId = null, userId = 0, userId = -1

2. **`EventAIControllerTest.createEvent_nullAction_400()`**
   - Test với action = null trong request body

3. **`AgentEventServiceTest.saveEventFromAction_missingRequiredFields_throwsException()`**
   - Test với missing title, start_time, end_time

4. **`AgentEventServiceTest.saveEventFromAction_invalidTimeWindow_throwsException()`**
   - Test với start_time >= end_time

5. **`EventAIAgentTest.processUserInput_weatherWarning_pausesCreation()`**
   - Test flow: outdoor event → weather warning → pause → user confirms → create

6. **`EventAIAgentTest.processUserInput_weatherWarning_rejectsCreation()`**
   - Test flow: outdoor event → weather warning → pause → user rejects → cancel

7. **`AgentEventServiceTest.tryParseDateTime_multipleFormats_success()`**
   - Test parse với 4 format khác nhau

8. **`AgentEventServiceTest.tryParseDateTime_invalidFormat_throwsException()`**
   - Test parse với format không hợp lệ

---

## Checklist xác nhận

- [x] Tất cả điều kiện đã có miền giá trị & điểm biên
- [x] Mỗi hành động được định nghĩa rõ, không mơ hồ
- [x] Có thứ tự ưu tiên khi xung đột
- [x] Đã liệt kê tổ hợp không khả thi
- [x] Có ví dụ thực tế kèm dữ liệu (7 ví dụ)
- [x] Đã map với test cases hiện có
- [x] Đã đề xuất test cases cần bổ sung












