# Tài liệu Tính năng Check-in - OpenEvent Project

## Tổng quan

Tính năng check-in trong dự án OpenEvent cho phép người tham dự sự kiện check-in/check-out thông qua:
- Form thủ công (nhập email, tên, số điện thoại)
- QR Code (quét mã để tự động check-in)
- Quản lý từ phía host (check-in thủ công cho người tham dự)

## 1. Data Models

### 1.1 EventAttendance Entity
**File:** `src/main/java/com/group02/openevent/model/attendance/EventAttendance.java`

```java
@Entity
@Table(name = "event_attendance")
public class EventAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendanceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;  // Link đến Order nếu người này mua vé
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    
    @Column(name = "email", length = 200)
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "organization", length = 200)
    private String organization;
    
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;  // Thời gian check-in
    
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;  // Thời gian check-out
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status = AttendanceStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "notes")
    private String notes;
    
    public enum AttendanceStatus {
        PENDING,      // Chưa check-in
        CHECKED_IN,   // Đã check-in
        CHECKED_OUT   // Đã check-out
    }
}
```

**Lưu ý quan trọng:**
- `EventAttendance` được tạo tự động khi Order được thanh toán (status = PAID)
- Mỗi email chỉ có thể check-in 1 lần cho 1 event (UNIQUE constraint: event_id, email)
- Có thể có `order = null` nếu là người được thêm thủ công bởi host

### 1.2 AttendanceRequest DTO
**File:** `src/main/java/com/group02/openevent/dto/attendance/AttendanceRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    private String fullName;
    private String email;        // Bắt buộc để tìm EventAttendance
    private String phone;
    private String organization;
}
```

## 2. Repository Layer

### 2.1 IEventAttendanceRepo
**File:** `src/main/java/com/group02/openevent/repository/IEventAttendanceRepo.java`

**Các method quan trọng:**
```java
// Tìm EventAttendance theo eventId và email
Optional<EventAttendance> findByEventIdAndEmail(Long eventId, String email);

// Tìm tất cả attendances của một event
List<EventAttendance> findByEventId(Long eventId);

// Kiểm tra email đã check-in chưa
boolean existsByEventIdAndEmailAndCheckedIn(Long eventId, String email);

// Đếm số người đã check-in
long countCheckedInByEventId(Long eventId);

// Đếm số người đã check-out
long countCheckedOutByEventId(Long eventId);

// Đếm số người đang có mặt (checked-in nhưng chưa check-out)
long countCurrentlyPresentByEventId(Long eventId);

// Tìm theo eventId và attendanceId (dùng cho admin check-in)
Optional<EventAttendance> findByEvent_IdAndAttendanceId(Long eventId, Long attendanceId);
```

## 3. Service Layer

### 3.1 EventAttendanceService Interface
**File:** `src/main/java/com/group02/openevent/service/EventAttendanceService.java`

**Các method chính:**
```java
// Check-in bằng email (cho người tham dự tự check-in)
EventAttendance checkIn(Long eventId, AttendanceRequest request);

// Check-out bằng email
EventAttendance checkOut(Long eventId, String email);

// Check-in từ phía admin (bằng attendanceId)
EventAttendance listCheckIn(Long eventId, Long attendanceId);

// Check-out từ phía admin
EventAttendance checkOut(Long eventId, Long attendanceId);

// Kiểm tra đã check-in chưa
boolean isAlreadyCheckedIn(Long eventId, String email);

// Tạo EventAttendance từ Order khi thanh toán thành công
EventAttendance createAttendanceFromOrder(Order order);
```

### 3.2 EventAttendanceServiceImpl - Logic Check-in
**File:** `src/main/java/com/group02/openevent/service/impl/EventAttendanceServiceImpl.java`

**Flow check-in chính (`checkIn` method):**

```java
@Transactional
public EventAttendance checkIn(Long eventId, AttendanceRequest request) {
    // 1. Validate event tồn tại
    Event event = eventRepo.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));
    
    // 2. Normalize email (lowercase, trim)
    String normalizedEmail = request.getEmail().trim().toLowerCase();
    
    // 3. Kiểm tra có Order đã thanh toán (PAID) không
    boolean hasPaidOrder = orderRepo.existsPaidByEventIdAndParticipantEmail(
        eventId, normalizedEmail);
    if (!hasPaidOrder) {
        throw new RuntimeException("Bạn không đăng ký sự kiện này");
    }
    
    // 4. Tìm EventAttendance đã tồn tại (được tạo khi order thanh toán)
    Optional<EventAttendance> existingOpt = attendanceRepo
        .findByEventIdAndEmail(eventId, normalizedEmail);
    
    if (existingOpt.isEmpty()) {
        throw new RuntimeException("Không tìm thấy thông tin đăng ký");
    }
    
    EventAttendance existing = existingOpt.get();
    
    // 5. Kiểm tra đã check-in chưa
    if (existing.getCheckInTime() != null) {
        throw new RuntimeException("Email này đã check-in lúc " + existing.getCheckInTime());
    }
    
    if (existing.getStatus() == AttendanceStatus.CHECKED_IN 
        || existing.getStatus() == AttendanceStatus.CHECKED_OUT) {
        throw new RuntimeException("Email này đã check-in rồi");
    }
    
    // 6. CẬP NHẬT EventAttendance (UPDATE, không tạo mới)
    existing.setCheckInTime(LocalDateTime.now());
    existing.setStatus(AttendanceStatus.CHECKED_IN);
    
    // 7. Cập nhật thông tin nếu có (optional)
    if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
        existing.setFullName(request.getFullName());
    }
    if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
        existing.setPhone(request.getPhone());
    }
    if (request.getOrganization() != null && !request.getOrganization().trim().isEmpty()) {
        existing.setOrganization(request.getOrganization());
    }
    
    return attendanceRepo.save(existing);
}
```

**Flow check-out:**
```java
@Transactional
public EventAttendance checkOut(Long eventId, String email) {
    String normalizedEmail = email.trim().toLowerCase();
    
    // Tìm attendance record
    EventAttendance attendance = attendanceRepo
        .findByEventIdAndEmail(eventId, normalizedEmail)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin check-in"));
    
    // Kiểm tra đã check-in chưa
    if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
        throw new RuntimeException("Bạn chưa check-in hoặc đã check-out rồi");
    }
    
    if (attendance.getCheckOutTime() != null) {
        throw new RuntimeException("Bạn đã check-out lúc " + attendance.getCheckOutTime());
    }
    
    // Cập nhật check-out
    attendance.setCheckOutTime(LocalDateTime.now());
    attendance.setStatus(AttendanceStatus.CHECKED_OUT);
    
    return attendanceRepo.save(attendance);
}
```

**Flow check-in từ admin (`listCheckIn` method):**
```java
public EventAttendance listCheckIn(Long eventId, Long attendanceId) {
    // Tìm EventAttendance
    EventAttendance attendance = attendanceRepo
        .findByEvent_IdAndAttendanceId(eventId, attendanceId)
        .orElseThrow(() -> new RuntimeException("Người tham dự không tìm thấy"));
    
    // Kiểm tra đã check-in chưa
    if (attendance.getCheckInTime() != null) {
        throw new RuntimeException("Người này đã check-in rồi");
    }
    
    // Cập nhật check-in
    attendance.setCheckInTime(LocalDateTime.now());
    attendance.setStatus(AttendanceStatus.CHECKED_IN);
    
    return attendanceRepo.save(attendance);
}
```

## 4. Controller Layer

### 4.1 EventAttendanceController - Check-in cho người tham dự
**File:** `src/main/java/com/group02/openevent/controller/attendance/EventAttendanceController.java`

**Các endpoint:**

1. **GET `/events/{eventId}/attendance`** - Trang hiển thị QR codes
   - Hiển thị 2 QR codes: check-in và check-out
   - Template: `event/event-checkin-page.html`

2. **GET `/events/{eventId}/qr-checkin`** - Redirect từ QR code check-in
   - Redirect đến `/login` với params: `checkin=true&eventId=...&action=checkin&redirectUrl=/forms/checkin/{eventId}`
   - Sau khi login, redirect đến form check-in

3. **GET `/events/{eventId}/checkin-form`** - Form check-in thủ công
   - Template: `event/checkin-form.html`
   - Yêu cầu authentication (Spring Security)

4. **POST `/events/{eventId}/checkin`** - Xử lý check-in
   ```java
   @PostMapping("/{eventId}/checkin")
   public String processCheckin(
       @PathVariable Long eventId,
       @ModelAttribute AttendanceRequest request,
       RedirectAttributes redirectAttributes) {
       
       try {
           EventAttendance attendance = attendanceService.checkIn(eventId, request);
           redirectAttributes.addFlashAttribute("successMessage", 
               "Check-in thành công! Chào mừng " + attendance.getFullName());
           redirectAttributes.addFlashAttribute("checkInTime", attendance.getCheckInTime());
           return "redirect:/events/" + eventId + "/checkin-form";
       } catch (Exception e) {
           redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
           return "redirect:/events/" + eventId + "/checkin-form";
       }
   }
   ```

5. **GET `/events/qr-code/generate?url=...`** - Generate QR code image
   - Trả về PNG image của QR code

### 4.2 EventAttendeesController - Quản lý từ phía host
**File:** `src/main/java/com/group02/openevent/controller/attendance/EventAttendeesController.java`

**Các endpoint:**

1. **POST `/event/{eventId}/attendees/{attendeeId}/check-in`** - Admin check-in
   ```java
   @PostMapping("/{attendeeId}/check-in")
   @ResponseBody
   public ResponseEntity<?> checkIn(
       @PathVariable Long eventId,
       @PathVariable Long attendeeId) {
       try {
           EventAttendance attendance = attendanceService.listCheckIn(eventId, attendeeId);
           return ResponseEntity.ok(attendance);
       } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.BAD_REQUEST)
               .body("{\"error\": \"" + e.getMessage() + "\"}");
       }
   }
   ```

2. **POST `/event/{eventId}/attendees/{attendeeId}/check-out`** - Admin check-out

### 4.3 EventFormController - Check-in qua Form
**File:** `src/main/java/com/group02/openevent/controller/form/EventFormController.java`

- Khi submit form check-in (FormType.CHECKIN), tự động gọi `attendanceService.checkIn()`
- URL: `/forms/checkin/{eventId}`

## 5. Frontend Templates

### 5.1 Check-in Form
**File:** `src/main/resources/templates/event/checkin-form.html`

- Form HTML với các field: fullName, email, phone, organization
- Submit POST đến `/events/{eventId}/checkin`
- Hiển thị success/error messages

### 5.2 QR Code Page
**File:** `src/main/resources/templates/event/event-checkin-page.html`

- Hiển thị 2 QR codes: check-in và check-out
- QR code được generate từ URL: `/events/qr-code/generate?url=...`

### 5.3 Event Attendees Management
**File:** `src/main/resources/templates/fragments/event-attendees.html`

- Danh sách người tham dự với filter theo check-in status
- Nút check-in/check-out cho từng người
- JavaScript: `event-attendees.js` xử lý AJAX calls

## 6. JavaScript Functions

### 6.1 event-attendees.js
**File:** `src/main/resources/static/js/event-attendees.js`

**Function check-in:**
```javascript
function checkIn(attendanceId) {
    if (!confirm("Xác nhận check-in người tham dự này?")) return;
    const eventId = getEventId();
    
    fetch(`/event/${eventId}/attendees/${attendanceId}/check-in`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
    })
    .then(response => {
        if (response.ok) {
            showAlert("Check-in thành công", "success");
            setTimeout(() => reloadAttendeesFragment(), 500);
        } else {
            return response.json().then(data => {
                throw new Error(data.error || "Lỗi khi check-in");
            });
        }
    })
    .catch(error => {
        showAlert(error.message || "Lỗi khi check-in", "error");
    });
}
```

## 7. Database Schema

**Table: `event_attendance`**

```sql
CREATE TABLE event_attendance (
    attendance_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT DEFAULT NULL,           -- FK to orders
    event_id        BIGINT NOT NULL,                -- FK to event
    customer_id     BIGINT DEFAULT NULL,            -- FK to customer
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(50),
    organization    VARCHAR(200),
    check_in_time   DATETIME(6) DEFAULT NULL,       -- Thời gian check-in
    check_out_time  DATETIME(6) DEFAULT NULL,       -- Thời gian check-out
    status          ENUM('PENDING','CHECKED_IN','CHECKED_OUT') NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    notes           TEXT,
    
    -- Constraints
    UNIQUE KEY uk_email_event (event_id, email),    -- 1 email chỉ check-in 1 lần/event
    INDEX idx_event_status (event_id, status),
    INDEX idx_checkin_time (check_in_time)
);
```

## 8. Business Logic Flow

### 8.1 Flow Check-in từ người tham dự:

1. **Người tham dự quét QR code hoặc truy cập form check-in**
   - URL: `/events/{eventId}/qr-checkin` hoặc `/events/{eventId}/checkin-form`

2. **Nếu chưa login → Redirect đến `/login`**
   - Sau khi login → Redirect về form check-in

3. **Điền form check-in** (fullName, email, phone, organization)
   - Submit POST đến `/events/{eventId}/checkin`

4. **Service layer xử lý:**
   - Validate event tồn tại
   - Kiểm tra có Order PAID không
   - Tìm EventAttendance theo email
   - Kiểm tra chưa check-in
   - Cập nhật `checkInTime` và `status = CHECKED_IN`

5. **Redirect về form với success message**

### 8.2 Flow Check-in từ admin:

1. **Admin xem danh sách attendees** (`/manage/event/check-in`)
2. **Click nút "Check-in"** cho một attendee
3. **JavaScript gọi API:** `POST /event/{eventId}/attendees/{attendeeId}/check-in`
4. **Service layer:** `listCheckIn(eventId, attendanceId)`
5. **Cập nhật UI** với kết quả

## 9. Validation Rules

1. **Email phải có Order PAID** cho event đó
2. **Mỗi email chỉ check-in 1 lần** cho 1 event (UNIQUE constraint)
3. **Phải check-in trước khi check-out**
4. **Không thể check-in 2 lần** (kiểm tra `checkInTime != null`)

## 10. Integration với Order System

- Khi Order được thanh toán (status = PAID), tự động tạo `EventAttendance`
- Method: `createAttendanceFromOrder(Order order)` được gọi từ PaymentController
- EventAttendance được tạo với status = PENDING, chờ check-in

## 11. API Endpoints Summary

### Public Endpoints (cần authentication):
- `GET /events/{eventId}/attendance` - Trang QR codes
- `GET /events/{eventId}/qr-checkin` - Redirect từ QR check-in
- `GET /events/{eventId}/checkin-form` - Form check-in
- `POST /events/{eventId}/checkin` - Xử lý check-in
- `GET /events/{eventId}/checkout-form` - Form check-out
- `POST /events/{eventId}/checkout` - Xử lý check-out
- `GET /events/qr-code/generate?url=...` - Generate QR code

### Admin Endpoints:
- `POST /event/{eventId}/attendees/{attendeeId}/check-in` - Admin check-in
- `POST /event/{eventId}/attendees/{attendeeId}/check-out` - Admin check-out
- `GET /fragments/attendees?eventId=...` - Danh sách attendees

## 12. Điểm cần lưu ý cho Face Recognition Check-in

1. **Cần lưu trữ face data:**
   - Có thể thêm field `face_encoding` hoặc `face_image_url` vào `EventAttendance`
   - Hoặc tạo bảng riêng `face_registrations` link đến `EventAttendance`

2. **Flow check-in bằng khuôn mặt:**
   - Capture ảnh từ camera
   - Extract face encoding (dùng thư viện như face_recognition, OpenCV, hoặc cloud API)
   - So sánh với database để tìm match
   - Nếu match → gọi `attendanceService.checkIn(eventId, request)` với email tìm được

3. **Cần API mới:**
   - `POST /events/{eventId}/face-checkin` - Nhận ảnh, xử lý face recognition, check-in
   - `POST /events/{eventId}/register-face` - Đăng ký khuôn mặt cho attendee (có thể làm khi check-in lần đầu)

4. **Security:**
   - Validate ảnh là khuôn mặt thật (liveness detection)
   - Rate limiting để tránh spam
   - Logging để audit

## 13. Dependencies liên quan

- Spring Boot (Web, Data JPA, Security)
- Thymeleaf (templates)
- ZXing (QR code generation)
- Apache POI (Excel export)
- Lombok (code generation)

---

**Tài liệu này cung cấp đầy đủ thông tin về tính năng check-in hiện tại. ChatGPT có thể sử dụng để hiểu rõ flow và implement face recognition check-in.**

