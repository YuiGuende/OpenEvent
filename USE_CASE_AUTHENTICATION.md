# Use Case Diagram - AUTHENTICATION (Xác Thực)

## Tổng Quan
Tài liệu này mô tả Use Case Diagram chi tiết cho module **Authentication** (Xác Thực) trong hệ thống quản lý sự kiện OpenEvent.

## Actors
- **User**: Người dùng chung, có thể thực hiện các thao tác xác thực
- **Admin**: Quản trị viên, có thể xác minh owner (chủ sở hữu)

## Actors Liên Quan
- **OAuth Provider** (Google): Cung cấp dịch vụ đăng nhập OAuth

---

## Use Cases Chi Tiết

### 1. ĐĂNG KÝ

#### UC-A1: Register Customer (Đăng Ký Khách Hàng)
- **Actors**: User
- **Mô tả**: Người dùng đăng ký tài khoản mới với vai trò Customer
- **Preconditions**: Người dùng chưa có tài khoản
- **Main Flow**:
  1. Người dùng truy cập trang đăng ký
  2. Người dùng điền thông tin (email, password, tên, số điện thoại)
  3. Hệ thống kiểm tra email chưa tồn tại
  4. Hệ thống tạo tài khoản và gửi email xác nhận
  5. Người dùng đăng nhập được
- **Alternative Flow**:
  - Email đã tồn tại → Thông báo lỗi
  - Password không đủ mạnh → Yêu cầu password mạnh hơn

#### UC-A2: Register Owner (Đăng Ký Chủ Sở Hữu)
- **Actors**: User
- **Mô tả**: Người dùng đăng ký tài khoản với vai trò Owner/Host
- **Preconditions**: Người dùng chưa có tài khoản
- **Main Flow**:
  1. Người dùng truy cập trang đăng ký Owner
  2. Người dùng điền thông tin cơ bản
  3. Người dùng điền thông tin tổ chức (nếu có)
  4. Hệ thống tạo tài khoản với trạng thái chờ duyệt
  5. Admin xác minh owner
  6. Tài khoản được kích hoạt
- **Includes**:
  - UC-A9: Verify Owner
- **Alternative Flow**:
  - Admin từ chối → Tài khoản không được kích hoạt

---

### 2. ĐĂNG NHẬP

#### UC-A3: Login (Đăng Nhập)
- **Actors**: User
- **Mô tả**: Người dùng đăng nhập vào hệ thống
- **Preconditions**: Người dùng đã có tài khoản
- **Main Flow**:
  1. Người dùng truy cập trang đăng nhập
  2. Người dùng nhập email và password
  3. Hệ thống xác thực thông tin
  4. Hệ thống tạo session
  5. Người dùng được chuyển đến trang chủ
- **Extends**:
  - UC-A4: Login Google Account
- **Alternative Flow**:
  - Thông tin sai → Hiển thị lỗi
  - Tài khoản bị khóa → Thông báo và yêu cầu liên hệ admin

#### UC-A4: Login Google Account (Đăng Nhập Bằng Google)
- **Actors**: User, OAuth Provider
- **Mô tả**: Người dùng đăng nhập bằng tài khoản Google
- **Extends**: UC-A3: Login
- **Main Flow**:
  1. Người dùng chọn "Đăng nhập bằng Google"
  2. Hệ thống chuyển đến Google OAuth
  3. Người dùng xác nhận quyền truy cập
  4. Google trả về thông tin người dùng
  5. Hệ thống tạo hoặc cập nhật tài khoản
  6. Người dùng được đăng nhập
- **Alternative Flow**:
  - Người dùng từ chối → Quay lại trang đăng nhập

---

### 3. QUẢN LÝ MẬT KHẨU

#### UC-A5: Forgot Password (Quên Mật Khẩu)
- **Actors**: User
- **Mô tả**: Người dùng yêu cầu reset mật khẩu khi quên
- **Preconditions**: Người dùng có tài khoản
- **Main Flow**:
  1. Người dùng truy cập trang "Quên mật khẩu"
  2. Người dùng nhập email
  3. Hệ thống gửi email chứa link reset password
  4. Người dùng nhận email và click link
  5. Người dùng được chuyển đến trang đặt lại mật khẩu
- **Includes**:
  - UC-A6: Reset Password

#### UC-A6: Reset Password (Đặt Lại Mật Khẩu)
- **Actors**: User
- **Mô tả**: Người dùng đặt lại mật khẩu mới
- **Includes**: UC-A5: Forgot Password
- **Preconditions**: Người dùng có link reset hợp lệ
- **Main Flow**:
  1. Người dùng nhập mật khẩu mới
  2. Người dùng xác nhận mật khẩu mới
  3. Hệ thống kiểm tra tính hợp lệ
  4. Hệ thống cập nhật mật khẩu
  5. Người dùng có thể đăng nhập với mật khẩu mới
- **Alternative Flow**:
  - Link hết hạn → Yêu cầu gửi lại email
  - Mật khẩu không khớp → Yêu cầu nhập lại

---

### 4. ĐĂNG XUẤT

#### UC-A7: Log Out (Đăng Xuất)
- **Actors**: User
- **Mô tả**: Người dùng đăng xuất khỏi hệ thống
- **Preconditions**: Người dùng đã đăng nhập
- **Main Flow**:
  1. Người dùng chọn "Đăng xuất"
  2. Hệ thống xóa session
  3. Người dùng được chuyển đến trang đăng nhập
  4. Người dùng phải đăng nhập lại để truy cập

---

### 5. XEM HỒ SƠ

#### UC-A8: View User Profile (Xem Hồ Sơ Người Dùng)
- **Actors**: User
- **Mô tả**: Người dùng xem thông tin hồ sơ của mình
- **Preconditions**: Người dùng đã đăng nhập
- **Main Flow**:
  1. Người dùng truy cập trang profile
  2. Hệ thống hiển thị thông tin hồ sơ
  3. Người dùng có thể xem: tên, email, số điện thoại, vai trò, v.v.
- **Note**: Use case này thường được thực hiện sau khi đăng nhập thành công, nhưng là một use case độc lập

---

### 6. XÁC MINH OWNER (Admin)

#### UC-A9: Verify Owner (Xác Minh Chủ Sở Hữu)
- **Actors**: Admin
- **Mô tả**: Admin xác minh tài khoản Owner đã đăng ký
- **Preconditions**: Có yêu cầu đăng ký Owner chờ duyệt
- **Main Flow**:
  1. Admin xem danh sách yêu cầu đăng ký Owner
  2. Admin xem thông tin chi tiết yêu cầu
  3. Admin kiểm tra tính hợp lệ
  4. Admin duyệt hoặc từ chối
  5. Hệ thống cập nhật trạng thái tài khoản
  6. Người dùng nhận thông báo kết quả
- **Includes**: UC-A2: Register Owner
- **Alternative Flow**:
  - Admin từ chối → Tài khoản không được kích hoạt, người dùng nhận thông báo

---

## Sơ Đồ Use Case (Text Representation)

```
┌─────────────────────────────────────────────────────────────┐
│              AUTHENTICATION USE CASES                        │
└─────────────────────────────────────────────────────────────┘

[User] ───────────────────────────────────────────────────────

ĐĂNG KÝ:
  ├── Register Customer
  └── Register Owner
      └── Verify Owner (includes) → [Admin]

ĐĂNG NHẬP:
  └── Login
      └── Login Google Account (extends) → [OAuth Provider]

QUẢN LÝ MẬT KHẨU:
  ├── Forgot Password
  │   └── Reset Password (includes)
  └── Reset Password

ĐĂNG XUẤT:
  └── Log Out

XEM HỒ SƠ:
  └── View User Profile

[Admin] ──────────────────────────────────────────────────────

XÁC MINH:
  └── Verify Owner
      └── Register Owner (includes)
```

---

## Quan Hệ Giữa Use Cases

### Include (Bắt Buộc)
- Register Owner → Verify Owner
- Forgot Password → Reset Password

### Extend (Tùy Chọn)
- Login → Login Google Account

### Generalization (Kế Thừa)
- Login Google Account → Login (theo nghĩa là một cách đăng nhập)

---

## So Sánh Với Use Case Cũ

### Các Lỗi Đã Sửa:

1. **Sửa Quan Hệ Generalization**:
   - ❌ **Cũ**: Login → Login Google Account (sai hướng)
   - ✅ **Mới**: Login Google Account extends Login (đúng)

2. **Sửa Quan Hệ Extend**:
   - ❌ **Cũ**: View User Profile extends Login (không hợp lý)
   - ✅ **Mới**: View User Profile là use case độc lập, không extend Login

3. **Bổ Sung Use Cases**:
   - ✅ Register Customer (tách riêng với Register Owner)
   - ✅ View User Profile (use case độc lập)

4. **Làm Rõ Quan Hệ Include**:
   - ✅ Register Owner includes Verify Owner (bắt buộc)
   - ✅ Forgot Password includes Reset Password (bắt buộc)

---

## Luồng Xử Lý Chính

### Luồng Đăng Ký Customer:
1. User → Register Customer
2. Hệ thống tạo tài khoản
3. User có thể Login ngay

### Luồng Đăng Ký Owner:
1. User → Register Owner
2. Hệ thống tạo tài khoản (chờ duyệt)
3. Admin → Verify Owner
4. Tài khoản được kích hoạt
5. User có thể Login

### Luồng Đăng Nhập:
1. User → Login
   - Option 1: Login thông thường (email/password)
   - Option 2: Login Google Account (OAuth)
2. Hệ thống xác thực
3. Hệ thống tạo session
4. User có thể View User Profile

### Luồng Quên Mật Khẩu:
1. User → Forgot Password
2. Hệ thống gửi email reset
3. User → Reset Password
4. Mật khẩu được cập nhật
5. User có thể Login với mật khẩu mới

---

## Ghi Chú

- **Include**: Use case được include luôn được thực thi khi use case chính được gọi
- **Extend**: Use case được extend chỉ thực thi trong điều kiện nhất định
- **Verify Owner** chỉ được thực hiện bởi Admin, không phải User
- **Login Google Account** sử dụng OAuth 2.0 flow
- Tất cả use cases đều có thể truy cập công khai (không cần đăng nhập), trừ View User Profile và Log Out


