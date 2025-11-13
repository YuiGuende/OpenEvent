# Use Case Diagram - HOST (Người Tổ Chức Sự Kiện)

## Tổng Quan
Tài liệu này mô tả Use Case Diagram chi tiết cho vai trò **Host** (Người Tổ Chức) trong hệ thống quản lý sự kiện OpenEvent.

## Actor
- **Host**: Người tổ chức sự kiện, có thể tạo và quản lý sự kiện, quản lý volunteer, xem thống kê, và các chức năng liên quan.

## Actors Liên Quan
- **Payment Gateway**: Xử lý thanh toán đăng ký gói
- **Department**: Duyệt sự kiện từ Host
- **AI Agent**: Hỗ trợ Host tư vấn về sự kiện

---

## Use Cases Chi Tiết

### 1. QUẢN LÝ SỰ KIỆN

#### UC-H1: Create Event (Tạo Sự Kiện)
- **Mô tả**: Host tạo sự kiện mới trong hệ thống
- **Preconditions**: Host đã đăng nhập
- **Main Flow**:
  1. Host chọn loại sự kiện (Music, Festival, Competition, Workshop)
  2. Host điền thông tin sự kiện
  3. Host chọn tạo dưới tên cá nhân hoặc tổ chức
  4. Hệ thống lưu sự kiện
  5. Host có thể gửi để duyệt
- **Includes**:
  - UC-H2: Create Event Under Organization
  - UC-H3: Create Event Under Host's Name
- **Extends**:
  - UC-H4: Submit Event for Approval
  - UC-H5: Consult AI for Event Suggestions

#### UC-H2: Create Event Under Organization
- **Mô tả**: Tạo sự kiện dưới tên tổ chức
- **Includes**: UC-H1: Create Event

#### UC-H3: Create Event Under Host's Name
- **Mô tả**: Tạo sự kiện dưới tên cá nhân Host
- **Includes**: UC-H1: Create Event

#### UC-H4: Submit Event for Approval
- **Mô tả**: Gửi sự kiện để Department duyệt
- **Extends**: UC-H1: Create Event
- **Actors**: Host, Department

#### UC-H5: Consult AI for Event Suggestions
- **Mô tả**: Tư vấn với AI về ý tưởng sự kiện
- **Extends**: UC-H1: Create Event
- **Actors**: Host, AI Agent

#### UC-H6: Edit Event (Chỉnh Sửa Sự Kiện)
- **Mô tả**: Host chỉnh sửa thông tin sự kiện đã tạo
- **Preconditions**: Sự kiện thuộc quyền quản lý của Host
- **Main Flow**:
  1. Host chọn sự kiện cần chỉnh sửa
  2. Host cập nhật thông tin
  3. Hệ thống lưu thay đổi
- **Extends**:
  - UC-H7: Archive Event

#### UC-H7: Archive Event
- **Mô tả**: Lưu trữ sự kiện (không xóa)
- **Extends**: UC-H6: Edit Event

#### UC-H8: Manage Events (Quản Lý Danh Sách Sự Kiện)
- **Mô tả**: Xem và quản lý tất cả sự kiện của Host
- **Extends**: UC-H6: Edit Event

---

### 2. QUẢN LÝ VÉ VÀ GIẢM GIÁ

#### UC-H9: Manage Ticket Types (Quản Lý Loại Vé)
- **Mô tả**: Host tạo và quản lý các loại vé cho sự kiện
- **Main Flow**:
  1. Host chọn sự kiện
  2. Host tạo/cập nhật/xóa loại vé
  3. Host thiết lập giá và số lượng
  4. Hệ thống lưu thay đổi

#### UC-H10: Manage Discounts/Promotions (Quản Lý Giảm Giá/Khuyến Mãi)
- **Mô tả**: Host tạo và quản lý mã giảm giá, khuyến mãi
- **Main Flow**:
  1. Host tạo mã giảm giá
  2. Host thiết lập điều kiện áp dụng
  3. Host thiết lập thời gian hiệu lực
  4. Hệ thống lưu thông tin

---

### 3. QUẢN LÝ NGƯỜI THAM DỰ

#### UC-H11: View Attendance (Xem Danh Sách Tham Dự)
- **Mô tả**: Host xem danh sách người tham dự sự kiện
- **Main Flow**:
  1. Host chọn sự kiện
  2. Hệ thống hiển thị danh sách người tham dự
  3. Host có thể xem chi tiết từng người
- **Extends**:
  - UC-H12: View Event Statistics
  - UC-H13: View Attendees
  - UC-H14: Export Attendee List

#### UC-H12: View Event Statistics (Xem Thống Kê Sự Kiện)
- **Mô tả**: Host xem các thống kê về sự kiện
- **Extends**: UC-H11: View Attendance
- **Thông tin thống kê**:
  - Số lượng người tham dự
  - Doanh thu
  - Tỷ lệ check-in
  - Phản hồi từ người tham dự

#### UC-H13: View Attendees (Xem Chi Tiết Người Tham Dự)
- **Mô tả**: Host xem thông tin chi tiết từng người tham dự
- **Extends**: UC-H11: View Attendance

#### UC-H14: Export Attendee List (Xuất Danh Sách Tham Dự)
- **Mô tả**: Host xuất danh sách tham dự ra file (Excel, PDF)
- **Extends**: UC-H11: View Attendance

#### UC-H15: Monitor Event Revenue (Theo Dõi Doanh Thu)
- **Mô tả**: Host theo dõi doanh thu từ sự kiện
- **Main Flow**:
  1. Host chọn sự kiện
  2. Hệ thống hiển thị thống kê doanh thu
  3. Host có thể xem theo thời gian, loại vé

---

### 4. QUẢN LÝ VOLUNTEER

#### UC-H16: Manage Volunteer Applications (Quản Lý Đơn Đăng Ký Tình Nguyện Viên)
- **Mô tả**: Host quản lý các đơn đăng ký làm tình nguyện viên
- **Main Flow**:
  1. Host xem danh sách đơn đăng ký
  2. Host duyệt hoặc từ chối đơn
  3. Host có thể gửi phản hồi
- **Includes**:
  - UC-H17: View Volunteer Applications
  - UC-H18: Approve Volunteer Application
  - UC-H19: Reject Volunteer Application

#### UC-H17: View Volunteer Applications
- **Mô tả**: Xem danh sách đơn đăng ký tình nguyện viên
- **Includes**: UC-H16: Manage Volunteer Applications

#### UC-H18: Approve Volunteer Application
- **Mô tả**: Duyệt đơn đăng ký tình nguyện viên
- **Includes**: UC-H16: Manage Volunteer Applications

#### UC-H19: Reject Volunteer Application
- **Mô tả**: Từ chối đơn đăng ký tình nguyện viên
- **Includes**: UC-H16: Manage Volunteer Applications

---

### 5. QUẢN LÝ FORM

#### UC-H20: Create Event Form (Tạo Form Cho Sự Kiện)
- **Mô tả**: Host tạo các form cho sự kiện
- **Main Flow**:
  1. Host chọn sự kiện
  2. Host chọn loại form cần tạo
  3. Host thiết kế form với các câu hỏi
  4. Hệ thống lưu form
- **Includes**:
  - UC-H21: Create Feedback Form
  - UC-H22: Create Check-in Form
  - UC-H23: Create Volunteer Form

#### UC-H21: Create Feedback Form
- **Mô tả**: Tạo form phản hồi sau sự kiện
- **Includes**: UC-H20: Create Event Form

#### UC-H22: Create Check-in Form
- **Mô tả**: Tạo form check-in cho người tham dự
- **Includes**: UC-H20: Create Event Form

#### UC-H23: Create Volunteer Form
- **Mô tả**: Tạo form đăng ký tình nguyện viên
- **Includes**: UC-H20: Create Event Form

#### UC-H24: View Form Responses (Xem Phản Hồi Từ Form)
- **Mô tả**: Host xem các phản hồi từ form đã tạo
- **Main Flow**:
  1. Host chọn form
  2. Hệ thống hiển thị danh sách phản hồi
  3. Host có thể xem chi tiết từng phản hồi

---

### 6. QUẢN LÝ THÔNG BÁO

#### UC-H25: Send Notifications (Gửi Thông Báo)
- **Mô tả**: Host gửi thông báo đến người tham dự
- **Main Flow**:
  1. Host chọn sự kiện
  2. Host soạn thông báo
  3. Host chọn đối tượng nhận (tất cả hoặc nhóm cụ thể)
  4. Hệ thống gửi thông báo

---

### 7. QUẢN LÝ TÀI KHOẢN VÀ GÓI DỊCH VỤ

#### UC-H26: Update Profile (Cập Nhật Hồ Sơ)
- **Mô tả**: Host cập nhật thông tin cá nhân/tổ chức
- **Main Flow**:
  1. Host truy cập trang profile
  2. Host cập nhật thông tin
  3. Hệ thống lưu thay đổi
- **Extends**:
  - UC-H27: Update Payment Information
  - UC-H28: Update Contact Information
  - UC-H29: Update Organization Details

#### UC-H27: Update Payment Information
- **Mô tả**: Cập nhật thông tin thanh toán
- **Extends**: UC-H26: Update Profile

#### UC-H28: Update Contact Information
- **Mô tả**: Cập nhật thông tin liên hệ
- **Extends**: UC-H26: Update Profile

#### UC-H29: Update Organization Details
- **Mô tả**: Cập nhật thông tin tổ chức
- **Extends**: UC-H26: Update Profile

#### UC-H30: Subscribe to Plan (Đăng Ký Gói Dịch Vụ)
- **Mô tả**: Host đăng ký gói dịch vụ để có thêm tính năng
- **Main Flow**:
  1. Host xem các gói dịch vụ
  2. Host chọn gói phù hợp
  3. Host thực hiện thanh toán
  4. Hệ thống kích hoạt gói dịch vụ
- **Includes**:
  - UC-H31: Process Subscription Payment
  - UC-H32: Confirm Subscription
- **Extends**: UC-H33: Subscription Failed
- **Actors**: Host, Payment Gateway

#### UC-H31: Process Subscription Payment
- **Mô tả**: Xử lý thanh toán đăng ký gói
- **Includes**: UC-H30: Subscribe to Plan
- **Actors**: Host, Payment Gateway

#### UC-H32: Confirm Subscription
- **Mô tả**: Xác nhận đăng ký thành công
- **Includes**: UC-H30: Subscribe to Plan

#### UC-H33: Subscription Failed
- **Mô tả**: Đăng ký thất bại (thanh toán không thành công)
- **Extends**: UC-H30: Subscribe to Plan

---

## Sơ Đồ Use Case (Text Representation)

```
┌─────────────────────────────────────────────────────────────┐
│                    HOST USE CASES                            │
└─────────────────────────────────────────────────────────────┘

[Host] ────────────────────────────────────────────────────────

QUẢN LÝ SỰ KIỆN:
  ├── Create Event
  │   ├── Create Event Under Organization (includes)
  │   ├── Create Event Under Host's Name (includes)
  │   ├── Submit Event for Approval (extends) → [Department]
  │   └── Consult AI for Event Suggestions (extends) → [AI Agent]
  ├── Edit Event
  │   └── Archive Event (extends)
  └── Manage Events
      └── Edit Event (extends)

QUẢN LÝ VÉ VÀ GIẢM GIÁ:
  ├── Manage Ticket Types
  └── Manage Discounts/Promotions

QUẢN LÝ NGƯỜI THAM DỰ:
  ├── View Attendance
  │   ├── View Event Statistics (extends)
  │   ├── View Attendees (extends)
  │   └── Export Attendee List (extends)
  └── Monitor Event Revenue

QUẢN LÝ VOLUNTEER:
  └── Manage Volunteer Applications
      ├── View Volunteer Applications (includes)
      ├── Approve Volunteer Application (includes)
      └── Reject Volunteer Application (includes)

QUẢN LÝ FORM:
  ├── Create Event Form
  │   ├── Create Feedback Form (includes)
  │   ├── Create Check-in Form (includes)
  │   └── Create Volunteer Form (includes)
  └── View Form Responses

QUẢN LÝ THÔNG BÁO:
  └── Send Notifications

QUẢN LÝ TÀI KHOẢN:
  ├── Update Profile
  │   ├── Update Payment Information (extends)
  │   ├── Update Contact Information (extends)
  │   └── Update Organization Details (extends)
  └── Subscribe to Plan
      ├── Process Subscription Payment (includes) → [Payment Gateway]
      ├── Confirm Subscription (includes)
      └── Subscription Failed (extends)
```

---

## Quan Hệ Giữa Use Cases

### Include (Bắt Buộc)
- Create Event → Create Event Under Organization
- Create Event → Create Event Under Host's Name
- Manage Volunteer Applications → View Volunteer Applications
- Manage Volunteer Applications → Approve Volunteer Application
- Manage Volunteer Applications → Reject Volunteer Application
- Create Event Form → Create Feedback Form
- Create Event Form → Create Check-in Form
- Create Event Form → Create Volunteer Form
- Subscribe to Plan → Process Subscription Payment
- Subscribe to Plan → Confirm Subscription

### Extend (Tùy Chọn)
- Create Event → Submit Event for Approval
- Create Event → Consult AI for Event Suggestions
- Edit Event → Archive Event
- View Attendance → View Event Statistics
- View Attendance → View Attendees
- View Attendance → Export Attendee List
- Update Profile → Update Payment Information
- Update Profile → Update Contact Information
- Update Profile → Update Organization Details
- Subscribe to Plan → Subscription Failed

---

## So Sánh Với Use Case Cũ

### Các Cải Tiến:

1. **Bổ Sung Use Cases**:
   - ✅ Consult AI for Event Suggestions
   - ✅ Manage Ticket Types
   - ✅ Manage Discounts/Promotions
   - ✅ Export Attendee List
   - ✅ Send Notifications
   - ✅ Monitor Event Revenue
   - ✅ Archive Event
   - ✅ Update Payment Information
   - ✅ Update Contact Information
   - ✅ Update Organization Details

2. **Sửa Quan Hệ**:
   - ✅ Sửa quan hệ include/extend cho đúng
   - ✅ Bổ sung quan hệ với AI Agent và Department

3. **Nhóm Use Cases**:
   - ✅ Nhóm theo chức năng rõ ràng
   - ✅ Dễ hiểu và dễ bảo trì

---

## Ghi Chú

- **Include**: Use case được include luôn được thực thi khi use case chính được gọi
- **Extend**: Use case được extend chỉ thực thi trong điều kiện nhất định
- Tất cả use cases yêu cầu Host đã đăng nhập (trừ các use case authentication)


