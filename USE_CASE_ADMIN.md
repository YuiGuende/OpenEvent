# Use Case Diagram - ADMIN (Quản Trị Viên)

## Tổng Quan
Tài liệu này mô tả Use Case Diagram chi tiết cho vai trò **Admin** (Quản Trị Viên) trong hệ thống quản lý sự kiện OpenEvent.

## Actor
- **Admin**: Quản trị viên hệ thống, có quyền quản lý toàn bộ hệ thống, người dùng, sự kiện, và các báo cáo.

## Actors Liên Quan
- **Host**: Người tổ chức sự kiện (Admin có thể xem và quản lý)

---

## Use Cases Chi Tiết

### 1. DASHBOARD VÀ THỐNG KÊ

#### UC-F1: View Admin Dashboard (Xem Dashboard Quản Trị)
- **Mô tả**: Admin xem dashboard tổng quan của hệ thống
- **Preconditions**: Admin đã đăng nhập
- **Main Flow**:
  1. Admin truy cập trang dashboard
  2. Hệ thống hiển thị các thống kê tổng quan
  3. Admin có thể xem: tổng doanh thu, số sự kiện, số người dùng, v.v.
- **Extends**:
  - UC-F2: View System Statistics

#### UC-F2: View System Statistics (Xem Thống Kê Hệ Thống)
- **Mô tả**: Admin xem các thống kê chi tiết về hệ thống
- **Extends**: UC-F1: View Admin Dashboard
- **Thông tin thống kê**:
  - Tổng doanh thu hệ thống
  - Số lượng sự kiện (theo trạng thái)
  - Số lượng người dùng (theo vai trò)
  - Số lượng đơn hàng
  - Xu hướng theo thời gian

---

### 2. QUẢN LÝ NGƯỜI DÙNG

#### UC-F3: View User List (Xem Danh Sách Người Dùng)
- **Mô tả**: Admin xem danh sách tất cả người dùng trong hệ thống
- **Main Flow**:
  1. Admin truy cập trang quản lý người dùng
  2. Hệ thống hiển thị danh sách người dùng
  3. Admin có thể tìm kiếm, lọc theo vai trò
  4. Admin có thể xem chi tiết từng người dùng
- **Extends**:
  - UC-F4: View User Detail

#### UC-F4: View User Detail (Xem Chi Tiết Người Dùng)
- **Mô tả**: Admin xem thông tin chi tiết về một người dùng
- **Extends**: UC-F3: View User List
- **Main Flow**:
  1. Admin chọn người dùng từ danh sách
  2. Hệ thống hiển thị thông tin chi tiết
  3. Admin có thể xem: thông tin cá nhân, vai trò, lịch sử hoạt động
- **Extends**:
  - UC-F5: View User Attendance
  - UC-F6: Change User Status

#### UC-F5: View User Attendance (Xem Lịch Sử Tham Dự)
- **Mô tả**: Admin xem lịch sử tham dự sự kiện của người dùng
- **Extends**: UC-F4: View User Detail
- **Main Flow**:
  1. Admin xem chi tiết người dùng
  2. Admin chọn tab "Lịch sử tham dự"
  3. Hệ thống hiển thị danh sách sự kiện đã tham dự

#### UC-F6: Change User Status (Thay Đổi Trạng Thái Người Dùng)
- **Mô tả**: Admin thay đổi trạng thái người dùng (kích hoạt, khóa, v.v.)
- **Extends**: UC-F4: View User Detail
- **Main Flow**:
  1. Admin xem chi tiết người dùng
  2. Admin chọn thay đổi trạng thái
  3. Admin xác nhận thay đổi
  4. Hệ thống cập nhật trạng thái
  5. Người dùng nhận thông báo (nếu bị khóa)

#### UC-F7: View User (Xem Người Dùng - Tổng Quát)
- **Mô tả**: Use case tổng quát để xem thông tin người dùng
- **Extends**:
  - UC-F8: View Attendance List

#### UC-F8: View Attendance List (Xem Danh Sách Tham Dự)
- **Mô tả**: Admin xem danh sách tham dự của một sự kiện hoặc tất cả sự kiện
- **Extends**: UC-F7: View User
- **Main Flow**:
  1. Admin chọn sự kiện hoặc xem tổng quan
  2. Hệ thống hiển thị danh sách người tham dự
  3. Admin có thể xuất danh sách

---

### 3. QUẢN LÝ SỰ KIỆN

#### UC-F9: View Request Event (Xem Yêu Cầu Sự Kiện)
- **Mô tả**: Admin xem các yêu cầu về sự kiện (tạo mới, chỉnh sửa, v.v.)
- **Main Flow**:
  1. Admin truy cập trang quản lý yêu cầu
  2. Hệ thống hiển thị danh sách yêu cầu
  3. Admin có thể xem chi tiết từng yêu cầu
- **Extends**:
  - UC-F10: Change Event Status

#### UC-F10: Change Event Status (Thay Đổi Trạng Thái Sự Kiện)
- **Mô tả**: Admin thay đổi trạng thái sự kiện (duyệt, từ chối, khóa, v.v.)
- **Extends**: UC-F9: View Request Event
- **Main Flow**:
  1. Admin xem yêu cầu sự kiện
  2. Admin quyết định duyệt hoặc từ chối
  3. Hệ thống cập nhật trạng thái sự kiện
  4. Host nhận thông báo kết quả

---

### 4. QUẢN LÝ THÔNG BÁO

#### UC-F11: View Notification (Xem Thông Báo)
- **Mô tả**: Admin xem các thông báo hệ thống
- **Main Flow**:
  1. Admin truy cập trang thông báo
  2. Hệ thống hiển thị danh sách thông báo
  3. Admin có thể xem chi tiết, đánh dấu đã đọc
- **Extends**:
  - UC-F9: View Request Event (thông báo về yêu cầu sự kiện)

#### UC-F12: Send Notification (Gửi Thông Báo)
- **Mô tả**: Admin gửi thông báo đến người dùng hoặc nhóm người dùng
- **Main Flow**:
  1. Admin soạn thông báo
  2. Admin chọn đối tượng nhận (tất cả, nhóm cụ thể, cá nhân)
  3. Admin gửi thông báo
  4. Hệ thống gửi đến người nhận

---

### 5. QUẢN LÝ BÁO CÁO

#### UC-F13: View Report List (Xem Danh Sách Báo Cáo)
- **Mô tả**: Admin xem danh sách các báo cáo trong hệ thống
- **Main Flow**:
  1. Admin truy cập trang báo cáo
  2. Hệ thống hiển thị danh sách báo cáo
  3. Admin có thể xem chi tiết, xuất báo cáo
- **Extends**:
  - UC-F14: Update Report

#### UC-F14: Update Report (Cập Nhật Báo Cáo)
- **Mô tả**: Admin cập nhật hoặc tạo báo cáo mới
- **Extends**: UC-F13: View Report List
- **Main Flow**:
  1. Admin chọn tạo hoặc cập nhật báo cáo
  2. Admin điền thông tin báo cáo
  3. Hệ thống lưu báo cáo

---

### 6. QUẢN LÝ VÉ VÀ HOÀN TIỀN

#### UC-F15: View Ticket (Xem Vé)
- **Mô tả**: Admin xem thông tin về vé trong hệ thống
- **Main Flow**:
  1. Admin truy cập trang quản lý vé
  2. Hệ thống hiển thị danh sách vé
  3. Admin có thể tìm kiếm, lọc theo sự kiện
- **Extends**:
  - UC-F16: View Refund Ticket

#### UC-F16: View Refund Ticket (Xem Vé Hoàn Tiền)
- **Mô tả**: Admin xem các yêu cầu hoàn tiền vé
- **Extends**: UC-F15: View Ticket
- **Main Flow**:
  1. Admin xem danh sách vé
  2. Admin chọn tab "Hoàn tiền"
  3. Hệ thống hiển thị danh sách yêu cầu hoàn tiền
  4. Admin có thể duyệt hoặc từ chối
- **Extends**:
  - UC-F17: View Escalated Ticket

#### UC-F17: View Escalated Ticket (Xem Vé Được Nâng Cấp)
- **Mô tả**: Admin xem các vé có vấn đề cần xử lý đặc biệt
- **Extends**: UC-F16: View Refund Ticket
- **Main Flow**:
  1. Admin xem danh sách hoàn tiền
  2. Hệ thống hiển thị các vé được đánh dấu "escalated"
  3. Admin xử lý các trường hợp đặc biệt

---

### 7. QUẢN LÝ TIN TỨC

#### UC-F18: Create News (Tạo Tin Tức)
- **Mô tả**: Admin tạo tin tức, thông báo cho hệ thống
- **Main Flow**:
  1. Admin truy cập trang quản lý tin tức
  2. Admin soạn tin tức (tiêu đề, nội dung, hình ảnh)
  3. Admin chọn đối tượng hiển thị
  4. Hệ thống lưu và hiển thị tin tức

---

### 8. QUẢN LÝ PHẢN HỒI

#### UC-F19: View Feedback (Xem Phản Hồi)
- **Mô tả**: Admin xem các phản hồi từ người dùng về sự kiện
- **Main Flow**:
  1. Admin truy cập trang quản lý phản hồi
  2. Hệ thống hiển thị danh sách phản hồi
  3. Admin có thể xem chi tiết, phản hồi lại
  4. Admin có thể xử lý các phản hồi tiêu cực

---

### 9. KIỂM TOÁN VÀ LOG

#### UC-F20: View Audit Logs (Xem Nhật Ký Kiểm Toán)
- **Mô tả**: Admin xem nhật ký các hoạt động trong hệ thống
- **Main Flow**:
  1. Admin truy cập trang audit logs
  2. Hệ thống hiển thị danh sách log
  3. Admin có thể lọc theo: thời gian, người dùng, hành động
  4. Admin có thể xuất log để phân tích

---

## Sơ Đồ Use Case (Text Representation)

```
┌─────────────────────────────────────────────────────────────┐
│                    ADMIN USE CASES                         │
└─────────────────────────────────────────────────────────────┘

[Admin] ───────────────────────────────────────────────────────

DASHBOARD VÀ THỐNG KÊ:
  └── View Admin Dashboard
      └── View System Statistics (extends)

QUẢN LÝ NGƯỜI DÙNG:
  ├── View User List
  │   └── View User Detail (extends)
  │       ├── View User Attendance (extends)
  │       └── Change User Status (extends)
  └── View User
      └── View Attendance List (extends)

QUẢN LÝ SỰ KIỆN:
  └── View Request Event
      └── Change Event Status (extends)

QUẢN LÝ THÔNG BÁO:
  ├── View Notification
  │   └── View Request Event (extends)
  └── Send Notification

QUẢN LÝ BÁO CÁO:
  └── View Report List
      └── Update Report (extends)

QUẢN LÝ VÉ:
  └── View Ticket
      └── View Refund Ticket (extends)
          └── View Escalated Ticket (extends)

QUẢN LÝ TIN TỨC:
  └── Create News

QUẢN LÝ PHẢN HỒI:
  └── View Feedback

KIỂM TOÁN:
  └── View Audit Logs
```

---

## Quan Hệ Giữa Use Cases

### Extend (Tùy Chọn)
- View Admin Dashboard → View System Statistics
- View User List → View User Detail
- View User Detail → View User Attendance
- View User Detail → Change User Status
- View User → View Attendance List
- View Notification → View Request Event
- View Request Event → Change Event Status
- View Report List → Update Report
- View Ticket → View Refund Ticket
- View Refund Ticket → View Escalated Ticket

---

## So Sánh Với Use Case Cũ

### Các Cải Tiến:

1. **Sửa Quan Hệ**:
   - ✅ Sửa quan hệ extend cho đúng hướng
   - ✅ Bổ sung các quan hệ còn thiếu

2. **Bổ Sung Use Cases**:
   - ✅ View System Statistics (chi tiết hơn)
   - ✅ View User Attendance
   - ✅ Change User Status
   - ✅ Change Event Status
   - ✅ Send Notification
   - ✅ Update Report
   - ✅ View Escalated Ticket
   - ✅ Create News
   - ✅ View Feedback
   - ✅ View Audit Logs

3. **Nhóm Use Cases**:
   - ✅ Nhóm theo chức năng rõ ràng
   - ✅ Dễ hiểu và dễ bảo trì

4. **Sửa Lỗi**:
   - ❌ **Cũ**: Host actor kết nối trực tiếp với Admin actor (sai)
   - ✅ **Mới**: Host chỉ xuất hiện trong context, không có quan hệ trực tiếp với Admin actor

---

## Luồng Xử Lý Chính

### Luồng Quản Lý Người Dùng:
1. Admin → View User List
2. Admin → View User Detail (chọn người dùng)
3. Admin có thể:
   - View User Attendance
   - Change User Status

### Luồng Quản Lý Sự Kiện:
1. Admin → View Request Event
2. Admin → Change Event Status (duyệt/từ chối)

### Luồng Quản Lý Hoàn Tiền:
1. Admin → View Ticket
2. Admin → View Refund Ticket
3. Admin → View Escalated Ticket (nếu có)

### Luồng Xem Dashboard:
1. Admin → View Admin Dashboard
2. Admin → View System Statistics (xem chi tiết)

---

## Ghi Chú

- Tất cả use cases yêu cầu Admin đã đăng nhập
- Admin có quyền cao nhất trong hệ thống
- Một số use cases có thể yêu cầu quyền đặc biệt (super admin)
- View Audit Logs giúp Admin theo dõi và kiểm tra các hoạt động trong hệ thống
- Change User Status và Change Event Status là các hành động quan trọng, cần xác nhận kỹ


