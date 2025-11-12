# BDD Test Cases: Event Management AI — Quản lý sự kiện

## Feature: Tạo sự kiện từ câu lệnh tự nhiên bằng AI

---

## Scenario 1: BR-01 - User ID không hợp lệ (null)

**Given** hệ thống nhận request tạo sự kiện  
**And** `userId` trong request là `null`  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `400 Bad Request`  
**And** response body chứa message `"User ID không hợp lệ"`  
**And** không có event nào được tạo trong database

---

## Scenario 2: BR-01 - User ID không hợp lệ (zero)

**Given** hệ thống nhận request tạo sự kiện  
**And** `userId` trong request là `0`  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `400 Bad Request`  
**And** response body chứa message `"User ID không hợp lệ"`  
**And** không có event nào được tạo trong database

---

## Scenario 3: BR-01 - User ID không hợp lệ (negative)

**Given** hệ thống nhận request tạo sự kiện  
**And** `userId` trong request là `-1`  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `400 Bad Request`  
**And** response body chứa message `"User ID không hợp lệ"`  
**And** không có event nào được tạo trong database

---

## Scenario 4: BR-02 - Action null

**Given** hệ thống nhận request tạo sự kiện  
**And** `action` trong request là `null`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `400 Bad Request`  
**And** response body chứa message `"Action không được để trống"`  
**And** không có event nào được tạo trong database

---

## Scenario 5: BR-03 - Tool name không hợp lệ (UPDATE_EVENT)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"UPDATE_EVENT"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `400 Bad Request`  
**And** response body chứa message `"Chỉ hỗ trợ action ADD_EVENT"`  
**And** không có event nào được tạo trong database

---

## Scenario 6: BR-03 - Tool name không hợp lệ (DELETE_EVENT)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"DELETE_EVENT"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `400 Bad Request`  
**And** response body chứa message `"Chỉ hỗ trợ action ADD_EVENT"`  
**And** không có event nào được tạo trong database

---

## Scenario 7: BR-04 - Thiếu title

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa `start_time` và `end_time`  
**And** `action.args` không chứa `title`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống throw `RuntimeException`  
**And** exception message chứa thông tin về lỗi parse hoặc missing field  
**And** không có event nào được tạo trong database

---

## Scenario 8: BR-04 - Thiếu start_time

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa `title` và `end_time`  
**And** `action.args` không chứa `start_time`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống throw `RuntimeException`  
**And** exception message chứa thông tin về lỗi parse hoặc missing field  
**And** không có event nào được tạo trong database

---

## Scenario 9: BR-04 - Thiếu end_time

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa `title` và `start_time`  
**And** `action.args` không chứa `end_time`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống throw `RuntimeException`  
**And** exception message chứa thông tin về lỗi parse hoặc missing field  
**And** không có event nào được tạo trong database

---

## Scenario 10: BR-05 - Time window không hợp lệ (start >= end)

**Given** hệ thống nhận request tạo sự kiện từ AI Agent  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args.start_time` là `"2025-01-15T12:00"`  
**And** `action.args.end_time` là `"2025-01-15T10:00"` (end < start)  
**And** `action.args.title` là `"Workshop"`  
**And** `action.args.place` là `"Main Hall"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi `processUserInput(userInput, userId, sessionId, context, null)`  
**Then** hệ thống trả về message chứa `"Thời gian không hợp lệ"` hoặc `"bắt đầu phải trước kết thúc"`  
**And** không có event nào được tạo trong database

---

## Scenario 11: BR-05 - Time window không hợp lệ (start == end)

**Given** hệ thống nhận request tạo sự kiện từ AI Agent  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args.start_time` là `"2025-01-15T10:00"`  
**And** `action.args.end_time` là `"2025-01-15T10:00"` (end == start)  
**And** `action.args.title` là `"Workshop"`  
**And** `action.args.place` là `"Main Hall"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi `processUserInput(userInput, userId, sessionId, context, null)`  
**Then** hệ thống trả về message chứa `"Thời gian không hợp lệ"` hoặc `"bắt đầu phải trước kết thúc"`  
**And** không có event nào được tạo trong database

---

## Scenario 12: BR-06 - Place không tồn tại (tên place không tìm thấy)

**Given** hệ thống nhận request tạo sự kiện từ AI Agent  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa đầy đủ `title`, `start_time`, `end_time`  
**And** `action.args.place` là `"NonExistent Place"`  
**And** place `"NonExistent Place"` không tồn tại trong database  
**And** `userId` là `1` (hợp lệ)  
**When** gọi `processUserInput(userInput, userId, sessionId, context, null)`  
**Then** hệ thống trả về message chứa `"Không tìm thấy địa điểm"` hoặc `"địa điểm hợp lệ"`  
**And** không có event nào được tạo trong database

---

## Scenario 13: BR-06 - Place không tồn tại (place name rỗng)

**Given** hệ thống nhận request tạo sự kiện từ AI Agent  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa đầy đủ `title`, `start_time`, `end_time`  
**And** `action.args.place` là `null` hoặc rỗng  
**And** `userId` là `1` (hợp lệ)  
**When** gọi `processUserInput(userInput, userId, sessionId, context, null)`  
**Then** hệ thống trả về message chứa `"Vui lòng cung cấp tên địa điểm"`  
**And** không có event nào được tạo trong database

---

## Scenario 14: BR-07 - Phát hiện xung đột thời gian

**Given** hệ thống nhận request tạo sự kiện từ AI Agent  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa đầy đủ `title`, `start_time`, `end_time`, `place`  
**And** `action.args.start_time` là `"2025-01-15T10:00"`  
**And** `action.args.end_time` là `"2025-01-15T12:00"`  
**And** `action.args.place` là `"Main Hall"`  
**And** đã có event khác tồn tại tại `"Main Hall"` trong khoảng thời gian `10:00 - 12:00` ngày `2025-01-15`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi `processUserInput(userInput, userId, sessionId, context, null)`  
**Then** hệ thống trả về message chứa `"trùng thời gian"` hoặc `"xung đột"`  
**And** message chứa tên event bị xung đột  
**And** không có event mới nào được tạo trong database

---

## Scenario 15: BR-08 - Cảnh báo thời tiết cho outdoor event (có mưa)

**Given** hệ thống nhận request tạo sự kiện từ AI Agent  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa đầy đủ `title`, `start_time`, `end_time`, `place`  
**And** `action.args.event_type` là `"FESTIVAL"` (outdoor activity)  
**And** `action.args.start_time` là `"2025-01-20T14:00"`  
**And** `action.args.place` là `"Công viên"` (tồn tại)  
**And** không có xung đột thời gian  
**And** `classifier.classifyWeather()` trả về `"outdoor_activities"`  
**And** `weatherService.getForecastNote()` trả về forecast chứa `"mưa"` hoặc `rainChance > 50%`  
**And** `userId` là `1` (hợp lệ)  
**And** `sessionId` là `"session1"`  
**When** gọi `processUserInput(userInput, userId, sessionId, context, null)`  
**Then** hệ thống trả về message chứa cảnh báo thời tiết (có `"mưa"` hoặc `"rain"`)  
**And** message hỏi `"Bạn có muốn tiếp tục tạo sự kiện này không?"`  
**And** `PendingEvent` được lưu vào `pendingEvents` map với key là `sessionId`  
**And** không có event nào được tạo trong database (chờ user xác nhận)

---

## Scenario 16: BR-09 - Parse datetime thành công (format yyyy-MM-dd'T'HH:mm)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.args.start_time` là `"2025-01-15T10:00"` (format ISO)  
**And** `action.args.end_time` là `"2025-01-15T12:00"` (format ISO)  
**And** `action.args.title` là `"Workshop"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống parse thành công `start_time` và `end_time` thành `LocalDateTime`  
**And** không throw exception  
**And** event được tạo thành công trong database

---

## Scenario 17: BR-09 - Parse datetime thành công (format yyyy-MM-dd HH:mm)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.args.start_time` là `"2025-01-15 10:00"` (format với space)  
**And** `action.args.end_time` là `"2025-01-15 12:00"` (format với space)  
**And** `action.args.title` là `"Workshop"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống parse thành công `start_time` và `end_time` thành `LocalDateTime`  
**And** không throw exception  
**And** event được tạo thành công trong database

---

## Scenario 18: BR-09 - Parse datetime thành công (format dd/MM/yyyy HH:mm)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.args.start_time` là `"15/01/2025 10:00"` (format Việt Nam)  
**And** `action.args.end_time` là `"15/01/2025 12:00"` (format Việt Nam)  
**And** `action.args.title` là `"Workshop"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống parse thành công `start_time` và `end_time` thành `LocalDateTime`  
**And** không throw exception  
**And** event được tạo thành công trong database

---

## Scenario 19: BR-09 - Parse datetime thành công (format dd-MM-yyyy HH:mm)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.args.start_time` là `"15-01-2025 10:00"` (format với dấu gạch ngang)  
**And** `action.args.end_time` là `"15-01-2025 12:00"` (format với dấu gạch ngang)  
**And** `action.args.title` là `"Workshop"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống parse thành công `start_time` và `end_time` thành `LocalDateTime`  
**And** không throw exception  
**And** event được tạo thành công trong database

---

## Scenario 20: BR-10 - Parse datetime thất bại (format không hợp lệ)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.args.start_time` là `"invalid-format"` (format không hợp lệ)  
**And** `action.args.end_time` là `"2025-01-15T12:00"`  
**And** `action.args.title` là `"Workshop"`  
**And** `userId` là `1` (hợp lệ)  
**When** gọi service `saveEventFromAction(action, userId)`  
**Then** hệ thống throw `RuntimeException`  
**And** exception message chứa `"Không thể parse ngày giờ"` hoặc `"IllegalArgumentException"`  
**And** không có event nào được tạo trong database

---

## Scenario 21: BR-11 - Tạo event thành công (Happy Path)

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa đầy đủ `title`, `start_time`, `end_time`, `place`  
**And** `action.args.title` là `"Workshop Python"`  
**And** `action.args.start_time` là `"2025-01-15T10:00"`  
**And** `action.args.end_time` là `"2025-01-15T12:00"`  
**And** `action.args.place` là `"Main Hall"` (tồn tại)  
**And** `start_time < end_time`  
**And** không có xung đột thời gian  
**And** không phải outdoor event hoặc không có cảnh báo thời tiết  
**And** `userId` là `1` (hợp lệ)  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `200 OK`  
**And** response body chứa `"success": true`  
**And** response body chứa message `"Đã tạo sự kiện thành công"`  
**And** event được lưu vào database với status `DRAFT`  
**And** Host được tạo tự động nếu Customer chưa có Host  
**And** Email reminder mặc định được tạo (5 phút trước event)

---

## Scenario 22: BR-11 - Service exception khi tạo event

**Given** hệ thống nhận request tạo sự kiện  
**And** `action.toolName` là `"ADD_EVENT"`  
**And** `action.args` chứa đầy đủ thông tin hợp lệ  
**And** `userId` là `1` (hợp lệ)  
**And** `agentEventService.saveEventFromAction()` throw `RuntimeException`  
**When** gọi API `POST /api/ai/event/create`  
**Then** hệ thống trả về HTTP status `500 Internal Server Error`  
**And** response body chứa `"success": false`  
**And** response body chứa message lỗi từ exception

---

## Scenario 23: BR-12 - Kiểm tra conflict thành công (có conflict)

**Given** hệ thống nhận request kiểm tra xung đột  
**And** `startTime` là `"2025-01-15T10:00"`  
**And** `endTime` là `"2025-01-15T12:00"`  
**And** `place` là `"Main Hall"`  
**And** đã có event khác tồn tại tại `"Main Hall"` trong khoảng thời gian này  
**When** gọi API `POST /api/ai/event/check-conflict`  
**Then** hệ thống trả về HTTP status `200 OK`  
**And** response body chứa `"hasConflict": true`  
**And** response body chứa `"conflicts"` array không rỗng  
**And** response body chứa `"conflictCount" > 0`  
**And** response body chứa message `"Phát hiện X xung đột thời gian"`

---

## Scenario 24: BR-12 - Kiểm tra conflict thành công (không có conflict)

**Given** hệ thống nhận request kiểm tra xung đột  
**And** `startTime` là `"2025-01-15T10:00"`  
**And** `endTime` là `"2025-01-15T12:00"`  
**And** `place` là `"Main Hall"`  
**And** không có event nào tồn tại tại `"Main Hall"` trong khoảng thời gian này  
**When** gọi API `POST /api/ai/event/check-conflict`  
**Then** hệ thống trả về HTTP status `200 OK`  
**And** response body chứa `"hasConflict": false`  
**And** response body chứa `"conflicts"` array rỗng  
**And** response body chứa `"conflictCount": 0`  
**And** response body chứa message `"Không có xung đột thời gian"`

---

## Scenario 25: BR-13 - Kiểm tra conflict với format datetime không hợp lệ

**Given** hệ thống nhận request kiểm tra xung đột  
**And** `startTime` là `"invalid-format"` (format không hợp lệ)  
**And** `endTime` là `"2025-01-15T12:00"`  
**And** `place` là `"Main Hall"`  
**When** gọi API `POST /api/ai/event/check-conflict`  
**Then** hệ thống trả về HTTP status `500 Internal Server Error`  
**And** response body chứa message lỗi về parse datetime

---

## Scenario 26: BR-14 - Weather API có cảnh báo mưa

**Given** hệ thống nhận request lấy thông tin thời tiết  
**And** `location` là `"Da Nang"`  
**And** `date` là `"2025-01-20T14:00"`  
**And** Weather API trả về forecast chứa `"rain"` hoặc `rainChance > 50%`  
**When** gọi API `POST /api/ai/utility/weather`  
**Then** hệ thống trả về HTTP status `200 OK`  
**And** response body chứa `"hasWeatherWarning": true`  
**And** response body chứa `"forecast"` không rỗng  
**And** forecast chứa thông tin về mưa

---

## Scenario 27: BR-15 - Weather API không có cảnh báo

**Given** hệ thống nhận request lấy thông tin thời tiết  
**And** `location` là `"Da Nang"`  
**And** `date` là `"2025-01-20T14:00"`  
**And** Weather API trả về forecast không chứa `"rain"` và `rainChance <= 50%`  
**When** gọi API `POST /api/ai/utility/weather`  
**Then** hệ thống trả về HTTP status `200 OK`  
**And** response body chứa `"hasWeatherWarning": false`  
**And** response body chứa `"forecast"` (có thể rỗng hoặc không)

---

## Scenario 28: BR-16 - User xác nhận tiếp tục sau cảnh báo thời tiết

**Given** hệ thống đã tạo `PendingEvent` do cảnh báo thời tiết  
**And** `PendingEvent` được lưu trong `pendingEvents` map với key là `sessionId`  
**And** `sessionId` là `"session1"`  
**And** user gửi input `"có"` hoặc `"ok"` hoặc `"tiếp tục"`  
**When** gọi `processUserInput("có", userId, "session1", context, null)`  
**Then** hệ thống lấy `PendingEvent` từ map  
**And** hệ thống tạo event từ `PendingEvent`  
**And** hệ thống xóa `PendingEvent` khỏi map  
**And** hệ thống trả về message `"Đã tạo sự kiện: [title]"`  
**And** event được lưu vào database

---

## Scenario 29: BR-17 - User từ chối sau cảnh báo thời tiết

**Given** hệ thống đã tạo `PendingEvent` do cảnh báo thời tiết  
**And** `PendingEvent` được lưu trong `pendingEvents` map với key là `sessionId`  
**And** `sessionId` là `"session1"`  
**And** user gửi input `"không"`  
**When** gọi `processUserInput("không", userId, "session1", context, null)`  
**Then** hệ thống xóa `PendingEvent` khỏi map  
**And** hệ thống trả về message `"Đã hủy tạo sự kiện do bạn từ chối"`  
**And** không có event nào được tạo trong database

---

## Scenario 30: BR-17 - User response không rõ sau cảnh báo thời tiết

**Given** hệ thống đã tạo `PendingEvent` do cảnh báo thời tiết  
**And** `PendingEvent` được lưu trong `pendingEvents` map với key là `sessionId`  
**And** `sessionId` là `"session1"`  
**And** user gửi input không chứa `"có"`, `"ok"`, `"tiếp tục"`, hoặc `"không"`  
**When** gọi `processUserInput("maybe", userId, "session1", context, null)`  
**Then** hệ thống trả về message `"Bạn có thể xác nhận lại: có/không?"`  
**And** `PendingEvent` vẫn còn trong map (chưa bị xóa)  
**And** không có event nào được tạo trong database

---

## Tổng kết

### Test Cases theo Business Rules:

| BR | Scenario | Test Method | Status |
|----|----------|-------------|--------|
| BR-01 | Scenarios 1-3 | `createEvent_invalidUserId_*` | ✅ |
| BR-02 | Scenario 4 | `createEvent_nullAction_400` | ✅ |
| BR-03 | Scenarios 5-6 | `createEvent_toolValidation` | ✅ |
| BR-04 | Scenarios 7-9 | `saveEventFromAction_missing*_throwsException` | ✅ |
| BR-05 | Scenarios 10-11 | `processUserInput_invalidTimeWindow_returnsError` | ✅ |
| BR-06 | Scenarios 12-13 | `processUserInput_placeNotFound_returnsError` | ✅ |
| BR-07 | Scenario 14 | `processUserInput_timeConflict_returnsConflictMessage` | ✅ |
| BR-08 | Scenario 15 | `processUserInput_outdoorEventWithRainWarning_pausesCreation` | ✅ |
| BR-09 | Scenarios 16-19 | `saveEventFromAction_validDateTimeFormats_success` | ✅ |
| BR-10 | Scenario 20 | `saveEventFromAction_invalidDateTimeFormat_throwsException` | ✅ |
| BR-11 | Scenarios 21-22 | `createEvent_happyPath_200`, `createEvent_serviceException_500` | ✅ |
| BR-12 | Scenarios 23-24 | `checkConflict_ok` | ✅ |
| BR-13 | Scenario 25 | `checkConflict_invalidTimeFormat_500` | ✅ |
| BR-14 | Scenario 26 | `AIUtilityControllerWeatherTest` | ✅ |
| BR-15 | Scenario 27 | `AIUtilityControllerWeatherTest` | ✅ |
| BR-16 | Scenario 28 | `processUserInput_weatherWarningConfirmed_createsEvent` | ✅ |
| BR-17 | Scenarios 29-30 | `processUserInput_weatherWarningRejected_cancelsEvent` | ✅ |

### Tổng số test cases: 30 scenarios

### Coverage:
- ✅ Validation: 9 scenarios (BR-01 đến BR-04, BR-10)
- ✅ Business Logic: 8 scenarios (BR-05 đến BR-08, BR-16, BR-17)
- ✅ Happy Path: 1 scenario (BR-11)
- ✅ Error Handling: 2 scenarios (BR-11 exception, BR-13)
- ✅ API Endpoints: 4 scenarios (BR-12, BR-14, BR-15)
- ✅ Data Parsing: 4 scenarios (BR-09)
- ✅ Edge Cases: 2 scenarios (BR-17 unclear response)












