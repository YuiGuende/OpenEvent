# Use Case Diagrams - Mục Lục Tổng Hợp

## Tổng Quan
Tài liệu này là mục lục tổng hợp các Use Case Diagram chi tiết cho từng role trong hệ thống quản lý sự kiện OpenEvent.

---

## Danh Sách Tài Liệu

### 1. [USE_CASE_DIAGRAM.md](./USE_CASE_DIAGRAM.md)
**Use Case Diagram Tổng Quan**
- Tổng hợp tất cả các actors và use cases trong hệ thống
- Mô tả các vấn đề đã được sửa
- Sơ đồ tổng quan của toàn bộ hệ thống

### 2. [USE_CASE_HOST.md](./USE_CASE_HOST.md)
**Use Case Diagram - HOST (Người Tổ Chức)**
- Quản lý sự kiện (tạo, chỉnh sửa, lưu trữ)
- Quản lý vé và giảm giá
- Quản lý người tham dự và thống kê
- Quản lý volunteer
- Quản lý form (feedback, check-in, volunteer)
- Quản lý thông báo
- Quản lý tài khoản và gói dịch vụ

**Số lượng Use Cases**: 33 use cases

### 3. [USE_CASE_CUSTOMER.md](./USE_CASE_CUSTOMER.md)
**Use Case Diagram - CUSTOMER/GUEST (Khách Hàng/Khách)**
- Xem và tìm kiếm sự kiện
- Quản lý vé (mua, xem, hủy)
- Tham gia sự kiện (check-in, check-out)
- Đánh giá và phản hồi
- Đăng ký tình nguyện viên
- Điểm thưởng và xếp hạng
- Quản lý ví điện tử
- Tương tác với AI Agent

**Số lượng Use Cases**: 31 use cases

### 4. [USE_CASE_AUTHENTICATION.md](./USE_CASE_AUTHENTICATION.md)
**Use Case Diagram - AUTHENTICATION (Xác Thực)**
- Đăng ký (Customer, Owner)
- Đăng nhập (thông thường, OAuth Google)
- Quản lý mật khẩu (quên, reset)
- Đăng xuất
- Xem hồ sơ
- Xác minh Owner (Admin)

**Số lượng Use Cases**: 9 use cases

### 5. [USE_CASE_ADMIN.md](./USE_CASE_ADMIN.md)
**Use Case Diagram - ADMIN (Quản Trị Viên)**
- Dashboard và thống kê hệ thống
- Quản lý người dùng
- Quản lý sự kiện
- Quản lý thông báo
- Quản lý báo cáo
- Quản lý vé và hoàn tiền
- Quản lý tin tức
- Quản lý phản hồi
- Kiểm toán và log

**Số lượng Use Cases**: 20 use cases

### 6. [USE_CASE_DEPARTMENT.md](./USE_CASE_DEPARTMENT.md)
**Use Case Diagram - DEPARTMENT (Phòng Ban)**
- Dashboard và thống kê phòng ban
- Quản lý yêu cầu sự kiện (duyệt/từ chối)
- Quản lý sự kiện
- Quản lý đơn hàng
- Quản lý bài viết (articles)

**Số lượng Use Cases**: 26 use cases

---

## Tổng Hợp Số Lượng Use Cases

| Role | Số Use Cases | File |
|------|--------------|------|
| **Host** | 33 | [USE_CASE_HOST.md](./USE_CASE_HOST.md) |
| **Customer/Guest** | 31 | [USE_CASE_CUSTOMER.md](./USE_CASE_CUSTOMER.md) |
| **Department** | 26 | [USE_CASE_DEPARTMENT.md](./USE_CASE_DEPARTMENT.md) |
| **Admin** | 20 | [USE_CASE_ADMIN.md](./USE_CASE_ADMIN.md) |
| **Authentication** | 9 | [USE_CASE_AUTHENTICATION.md](./USE_CASE_AUTHENTICATION.md) |
| **Tổng cộng** | **119+** | (có một số use cases được chia sẻ) |

---

## Các Vấn Đề Đã Được Sửa

### 1. **HOST Use Cases**
- ✅ Bổ sung: Consult AI for Event Suggestions
- ✅ Bổ sung: Manage Ticket Types
- ✅ Bổ sung: Manage Discounts/Promotions
- ✅ Bổ sung: Export Attendee List
- ✅ Bổ sung: Send Notifications
- ✅ Bổ sung: Monitor Event Revenue
- ✅ Bổ sung: Archive Event
- ✅ Bổ sung: Update Payment/Contact/Organization Information

### 2. **CUSTOMER Use Cases**
- ✅ Bổ sung: Filter Events
- ✅ Bổ sung: Apply Voucher
- ✅ Bổ sung: View My Requests
- ✅ Bổ sung: Manage Wallet và các use case con
- ✅ Bổ sung: Complete KYC
- ✅ Bổ sung: View Transaction History
- ✅ Bổ sung: Multi-language Support
- ✅ Sửa quan hệ Process Ticket Payment và Confirm Ticket Purchase

### 3. **AUTHENTICATION Use Cases**
- ✅ Sửa quan hệ generalization: Login Google Account extends Login
- ✅ Tách riêng Register Customer và Register Owner
- ✅ Bổ sung View User Profile (use case độc lập)
- ✅ Sửa quan hệ include/extend cho đúng

### 4. **ADMIN Use Cases**
- ✅ Bổ sung: View System Statistics
- ✅ Bổ sung: View User Attendance
- ✅ Bổ sung: Change User Status
- ✅ Bổ sung: Change Event Status
- ✅ Bổ sung: Send Notification
- ✅ Bổ sung: Update Report
- ✅ Bổ sung: View Escalated Ticket
- ✅ Bổ sung: Create News
- ✅ Bổ sung: View Feedback
- ✅ Bổ sung: View Audit Logs
- ✅ Sửa quan hệ extend cho đúng hướng

### 5. **DEPARTMENT Use Cases**
- ✅ Bổ sung: View Events By Month
- ✅ Bổ sung: View Events By Type
- ✅ Bổ sung: View Participants Trend
- ✅ Bổ sung: View Revenue Trend
- ✅ Bổ sung: View Order Status Distribution
- ✅ Bổ sung: View Average Approval Time
- ✅ Bổ sung: View Pending Requests
- ✅ Bổ sung: View Upcoming Events
- ✅ Bổ sung: Update Event Status
- ✅ Bổ sung: View Order Details
- ✅ Bổ sung: Quản lý Articles (View, Create, Edit, Delete, Publish)
- ✅ Sửa quan hệ extend cho đúng hướng

---

## Cấu Trúc Quan Hệ

### Include (Bắt Buộc)
- Use case được include luôn được thực thi khi use case chính được gọi
- Ví dụ: Buy Ticket includes Process Ticket Payment

### Extend (Tùy Chọn)
- Use case được extend chỉ thực thi trong điều kiện nhất định
- Ví dụ: Buy Ticket extends Ticket Payment Failed (chỉ khi thanh toán thất bại)

### Generalization (Kế Thừa)
- Actor con kế thừa tất cả quyền của actor cha
- Ví dụ: Guest → Customer → User

---

## Hướng Dẫn Sử Dụng

1. **Để xem tổng quan**: Đọc [USE_CASE_DIAGRAM.md](./USE_CASE_DIAGRAM.md)
2. **Để xem chi tiết từng role**: Đọc file tương ứng:
   - Host: [USE_CASE_HOST.md](./USE_CASE_HOST.md)
   - Customer/Guest: [USE_CASE_CUSTOMER.md](./USE_CASE_CUSTOMER.md)
   - Department: [USE_CASE_DEPARTMENT.md](./USE_CASE_DEPARTMENT.md)
   - Admin: [USE_CASE_ADMIN.md](./USE_CASE_ADMIN.md)
   - Authentication: [USE_CASE_AUTHENTICATION.md](./USE_CASE_AUTHENTICATION.md)

3. **Để tìm use case cụ thể**: Sử dụng chức năng tìm kiếm trong file

---

## Lưu Ý

- Tất cả các use case diagrams đã được kiểm tra và sửa các lỗi từ phiên bản cũ
- Các quan hệ include/extend đã được điều chỉnh cho đúng với UML standards
- Các use cases đã được bổ sung dựa trên codebase thực tế của hệ thống
- Mỗi use case đều có mô tả chi tiết về preconditions, main flow, và alternative flow

---

## Cập Nhật

- **Ngày tạo**: Hôm nay
- **Phiên bản**: 2.0 (Đã sửa và bổ sung đầy đủ)
- **Trạng thái**: Hoàn thiện

---

## Liên Kết Nhanh

- [Use Case Diagram Tổng Quan](./USE_CASE_DIAGRAM.md)
- [Host Use Cases](./USE_CASE_HOST.md)
- [Customer/Guest Use Cases](./USE_CASE_CUSTOMER.md)
- [Department Use Cases](./USE_CASE_DEPARTMENT.md)
- [Admin Use Cases](./USE_CASE_ADMIN.md)
- [Authentication Use Cases](./USE_CASE_AUTHENTICATION.md)

