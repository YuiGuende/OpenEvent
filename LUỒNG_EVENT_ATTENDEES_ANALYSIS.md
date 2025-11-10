# Phân Tích Luồng Event Attendees Fragment

## Tổng Quan
Luồng hoạt động của fragment `event-attendees` từ điều hướng trong `app.js` đến controller, service, repository và database.

## 1. Luồng Điều Hướng (Routing)

### 1.1. Từ app.js
- **File**: `src/main/resources/static/js/app.js`
- **Route**: `/manage/event/${eventId}/attendees`
- **Fragment URL**: `/fragments/attendees?id=${eventId}`
- **Cơ chế**: SpaRouter tự động fetch fragment từ URL và inject vào `#main-content`

### 1.2. Fragment Endpoint
- **Controller**: `EventManageController`
- **Method**: `@GetMapping("/fragments/attendees")`
- **Path**: `/fragments/attendees`
- **Parameters**: 
  - `id` (Long) - Event ID
  - `page` (int, default=0) - Số trang
  - `size` (int, default=10) - Kích thước trang
  - `search` (String, optional) - Tìm kiếm
  - `ticketTypeFilter` (Long, optional) - Lọc loại vé
  - `paymentStatusFilter` (String, optional) - Lọc trạng thái thanh toán
  - `checkinStatusFilter` (String, optional) - Lọc trạng thái check-in

## 2. Luồng Xử Lý Dữ Liệu

### 2.1. Controller Layer (`EventManageController.attendees()`)
```java
@GetMapping("/fragments/attendees")
public String attendees(@RequestParam Long id, ...)
```

**Logic:**
1. Tạo `Pageable` từ `page` và `size`
2. Kiểm tra điều kiện:
   - Nếu có `search`: Gọi `eventAttendance.searchAttendees()`
   - Nếu có filter: Gọi `eventAttendance.filterAttendees()`
   - Ngược lại: Gọi `eventAttendance.getAttendeesByEvent()`
3. Load event và ticket types
4. Thêm attributes vào Model
5. Return fragment: `"fragments/event-attendees :: content"`

### 2.2. Service Layer (`EventAttendanceServiceImpl`)

#### a. `getAttendeesByEvent(Long eventId, Pageable pageable)`
- Gọi `attendanceRepo.findByOrder_Event_EventId(eventId, pageable)`
- Trả về `Page<EventAttendance>`

#### b. `searchAttendees(Long eventId, String search, Pageable pageable)`
- Gọi `attendanceRepo.searchAttendees(eventId, search, pageable)`
- Query tìm theo: `participantName`, `participantEmail`, `participantPhone`

#### c. `filterAttendees(...)`
- Gọi `attendanceRepo.filterAttendees(...)`
- Filter theo: `ticketType`, `paymentStatus`, `checkinStatus`

### 2.3. Repository Layer (`IEventAttendanceRepo`)

#### a. `findByOrder_Event_EventId(Long eventId, Pageable pageable)`
- Query: `SELECT ea FROM EventAttendance ea WHERE ea.order.event.id = :eventId`
- Trả về `Page<EventAttendance>`

#### b. `searchAttendees(...)`
- Query tìm kiếm trong `order.participantName`, `order.participantEmail`, `order.participantPhone`

#### c. `filterAttendees(...)`
- Query với các điều kiện:
  - `ticketTypeId`: Filter theo loại vé
  - `paymentStatus`: Filter theo trạng thái thanh toán (PAID, PENDING, CANCELLED)
  - `checkinStatus`: Filter theo trạng thái check-in (CHECKED_IN, NOT_CHECKED_IN)

## 3. Luồng CRUD Operations

### 3.1. Add Attendee

**Frontend (event-attendees.js):**
```javascript
fetch(`/event/${eventId}/attendees/add`, {
    method: "POST",
    body: params.toString()
})
```

**Backend:**
- **Controller**: `EventAttendanceController.addAttendee()`
- **Path**: `POST /event/{eventId}/attendees/add`
- **Service**: `EventAttendanceServiceImpl.addAttendee()`

**Logic:**
1. Validate event và ticket type
2. Tìm hoặc tạo Customer:
   - Tìm Account bằng email
   - Nếu có Account: Tìm Customer, nếu không có thì tạo
   - Nếu không có Account: Tạo Account và Customer mới
3. Tạo Order với customer, event, ticketType
4. Set status = PAID (thêm thủ công)
5. Calculate total amount
6. Tạo EventAttendance với order và event
7. Lưu vào database

### 3.2. Edit Attendee

**Frontend:**
```javascript
fetch(`/event/${eventId}/attendees/${attendanceId}/edit`, {
    method: "PUT",
    body: params.toString()
})
```

**Backend:**
- **Controller**: `EventAttendanceController.editAttendee()`
- **Path**: `PUT /event/{eventId}/attendees/{attendeeId}/edit`
- **Service**: `EventAttendanceServiceImpl.updateAttendee()`

**Logic:**
1. Tìm EventAttendance theo ID
2. Cập nhật Order: name, email, phone, organization
3. Lưu Order

### 3.3. Delete Attendee

**Frontend:**
```javascript
fetch(`/event/${eventId}/attendees/${attendanceId}`, {
    method: "DELETE"
})
```

**Backend:**
- **Controller**: `EventAttendanceController.deleteAttendee()`
- **Path**: `DELETE /event/{eventId}/attendees/{attendeeId}`
- **Service**: `EventAttendanceServiceImpl.deleteAttendee()`

**Logic:**
1. Tìm EventAttendance
2. Xóa EventAttendance
3. Xóa Order (nếu có)

### 3.4. Check-in

**Frontend:**
```javascript
fetch(`/event/${eventId}/attendees/${attendanceId}/check-in`, {
    method: "POST"
})
```

**Backend:**
- **Controller**: `EventAttendanceController.checkIn()`
- **Path**: `POST /event/{eventId}/attendees/{attendeeId}/check-in`
- **Service**: `EventAttendanceServiceImpl.listCheckIn()`

**Logic:**
1. Tìm EventAttendance
2. Kiểm tra đã check-in chưa
3. Set `checkInTime = LocalDateTime.now()`
4. Lưu

### 3.5. Check-out

**Frontend:**
```javascript
fetch(`/event/${eventId}/attendees/${attendanceId}/check-out`, {
    method: "POST"
})
```

**Backend:**
- **Controller**: `EventAttendanceController.checkOut()`
- **Path**: `POST /event/{eventId}/attendees/{attendeeId}/check-out`
- **Service**: `EventAttendanceServiceImpl.checkOut()`

**Logic:**
1. Tìm EventAttendance
2. Kiểm tra đã check-in chưa
3. Kiểm tra đã check-out chưa
4. Set `checkOutTime = LocalDateTime.now()`
5. Lưu

### 3.6. Export Excel

**Frontend:**
```html
<a th:href="@{/event/{eventId}/attendees/export/excel(eventId=${event.eventId})}">
```

**Backend:**
- **Controller**: `EventAttendanceController.exportToExcel()`
- **Path**: `GET /event/{eventId}/attendees/export/excel`
- **Service**: `EventAttendanceServiceImpl.filterAttendees()` (không có pagination)

**Logic:**
1. Lấy danh sách attendees (có thể filter)
2. Tạo Excel workbook với Apache POI
3. Điền dữ liệu vào sheet
4. Trả về file Excel

## 4. Các Vấn Đề Đã Sửa

### ✅ Vấn đề 1: Check-in Endpoint Path
- **Lỗi**: `/{eventId}/{attendeeId}/check-in` → Tạo path trùng lặp
- **Sửa**: `/{attendeeId}/check-in`
- **Kết quả**: `/event/{eventId}/attendees/{attendeeId}/check-in` ✅

### ✅ Vấn đề 2: Add Attendee thiếu Event
- **Lỗi**: Order và EventAttendance không có event
- **Sửa**: 
  - Set `order.setEvent(event)`
  - Set `attendance.setEvent(event)`
  - Validate ticket type thuộc event

### ✅ Vấn đề 3: Add Attendee thiếu Customer
- **Lỗi**: Order yêu cầu customer (nullable=false) nhưng không set
- **Sửa**: 
  - Tìm hoặc tạo Account bằng email
  - Tìm hoặc tạo Customer cho Account
  - Set `order.setCustomer(customer)`

## 5. Database Schema

### EventAttendance Table
- `attendance_id` (PK)
- `order_id` (FK → orders)
- `event_id` (FK → event, NOT NULL)
- `customer_id` (FK → customer, nullable)
- `check_in_time`, `check_out_time`
- `status` (PENDING, CHECKED_IN, CHECKED_OUT)

### Order Table
- `order_id` (PK)
- `customer_id` (FK → customer, NOT NULL)
- `event_id` (FK → event, NOT NULL)
- `ticket_type_id` (FK → ticket_type, NOT NULL)
- `participant_name`, `participant_email`, `participant_phone`, `participant_organization`
- `status` (PENDING, PAID, CANCELLED)

### Relationship
- `EventAttendance` → `Order` (Many-to-One)
- `EventAttendance` → `Event` (Many-to-One)
- `Order` → `Customer` (Many-to-One, NOT NULL)
- `Order` → `Event` (Many-to-One, NOT NULL)

## 6. Frontend Flow

### 6.1. Fragment Loading
1. User click vào "Người tham dự" trong sidebar
2. SpaRouter navigate đến `/manage/event/{eventId}/attendees`
3. Fetch `/fragments/attendees?id={eventId}`
4. Controller trả về HTML fragment
5. Inject vào `#main-content`
6. Load script `event-attendees.js` (nếu có)

### 6.2. Form Submission
- Search form: GET `/event/{eventId}/attendees?search=...`
- Filter form: GET `/event/{eventId}/attendees?ticketTypeFilter=...&...`
- Add modal: POST `/event/{eventId}/attendees/add`
- Edit modal: PUT `/event/{eventId}/attendees/{attendeeId}/edit`
- Delete: DELETE `/event/{eventId}/attendees/{attendeeId}`
- Check-in: POST `/event/{eventId}/attendees/{attendeeId}/check-in`
- Check-out: POST `/event/{eventId}/attendees/{attendeeId}/check-out`

## 7. Kết Luận

Luồng hoạt động đã được kiểm tra và sửa các vấn đề:
- ✅ Routing từ app.js đến fragment
- ✅ Controller endpoints đúng
- ✅ Service logic xử lý đúng
- ✅ Repository queries đúng
- ✅ Database relationships đúng
- ✅ CRUD operations hoạt động đúng
- ✅ Check-in/Check-out hoạt động đúng
- ✅ Export Excel hoạt động đúng

**Các vấn đề đã sửa:**
1. Check-in endpoint path trùng lặp
2. Add attendee thiếu event
3. Add attendee thiếu customer (tìm hoặc tạo tự động)

