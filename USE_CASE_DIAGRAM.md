# Use Case Diagram - Hệ Thống Quản Lý Sự Kiện (OpenEvent)

## Tổng Quan
Tài liệu này mô tả Use Case Diagram đã được chỉnh sửa và cải thiện cho hệ thống quản lý sự kiện OpenEvent, bao gồm tất cả các chức năng và vai trò người dùng trong hệ thống.

## Các Vấn Đề Đã Được Sửa

### 1. **Thiếu Actors (Vai trò người dùng)**
- ❌ **Cũ**: Chỉ có User, Customer, Admin, Host, AI Agent, Payment Gateway
- ✅ **Mới**: Thêm **Department** (Phòng ban) - vai trò quản lý và duyệt sự kiện

### 2. **Thiếu Use Cases Quan Trọng**
- ❌ **Cũ**: Thiếu các chức năng về Volunteer, Voucher, Wallet, Event Forms
- ✅ **Mới**: Đã bổ sung đầy đủ các use case:
  - Volunteer Application & Management
  - Voucher System
  - Wallet & KYC
  - Event Forms (Feedback, Check-in, Volunteer forms)
  - Request Management
  - Event Approval Workflow

### 3. **Sai Quan Hệ Giữa Use Cases**
- ❌ **Cũ**: Một số quan hệ extend/include không chính xác
- ✅ **Mới**: Đã sửa lại các quan hệ theo đúng UML standards

---

## Actors (Vai Trò Người Dùng)

### 1. **User** (Người Dùng Chung)
- Actor cơ bản, có thể thực hiện xác thực

### 2. **Customer** (Khách Hàng)
- Kế thừa từ User
- Có thể mua vé, tham gia sự kiện, tương tác với AI

### 3. **Host** (Người Tổ Chức)
- Kế thừa từ User
- Tạo và quản lý sự kiện, quản lý volunteer, xem thống kê

### 4. **Admin** (Quản Trị Viên)
- Kế thừa từ User
- Quản lý toàn bộ hệ thống, người dùng, báo cáo

### 5. **Department** (Phòng Ban)
- Kế thừa từ User
- Duyệt sự kiện, quản lý yêu cầu, xem thống kê

### 6. **AI Agent** (Trợ Lý AI)
- Actor hệ thống tự động
- Hỗ trợ khách hàng tìm kiếm và mua vé qua chat

### 7. **Payment Gateway** (Cổng Thanh Toán)
- Actor hệ thống bên ngoài
- Xử lý thanh toán vé và đăng ký gói

---

## Use Cases Chi Tiết

### A. AUTHENTICATION & USER MANAGEMENT

#### UC-A1: Authentication (Tổng Quát)
- **Actors**: User
- **Mô tả**: Quá trình xác thực người dùng
- **Includes**: 
  - UC-A2: Sign Up
  - UC-A3: Log In
  - UC-A4: Log Out
  - UC-A5: Reset Password
  - UC-A6: Forgot Password
  - UC-A7: OAuth Login (Google)

#### UC-A2: Sign Up
- **Actors**: User
- **Mô tả**: Đăng ký tài khoản mới

#### UC-A3: Log In
- **Actors**: User
- **Mô tả**: Đăng nhập vào hệ thống
- **Extends**: UC-A7: OAuth Login

#### UC-A4: Log Out
- **Actors**: User
- **Mô tả**: Đăng xuất khỏi hệ thống

#### UC-A5: Reset Password
- **Actors**: User
- **Mô tả**: Đặt lại mật khẩu

#### UC-A6: Forgot Password
- **Actors**: User
- **Mô tả**: Quên mật khẩu và yêu cầu reset

#### UC-A7: OAuth Login
- **Actors**: User
- **Mô tả**: Đăng nhập bằng OAuth (Google)

---

### B. CUSTOMER USE CASES

#### UC-B1: View List Event
- **Actors**: Customer
- **Mô tả**: Xem danh sách sự kiện
- **Extends**: 
  - UC-B2: View Event Details
  - UC-B3: Search Events
  - UC-B4: Filter Events

#### UC-B2: View Event Details
- **Actors**: Customer
- **Mô tả**: Xem chi tiết sự kiện

#### UC-B3: Search Events
- **Actors**: Customer
- **Mô tả**: Tìm kiếm sự kiện

#### UC-B4: Filter Events
- **Actors**: Customer
- **Mô tả**: Lọc sự kiện theo tiêu chí

#### UC-B5: View Purchased Tickets
- **Actors**: Customer
- **Mô tả**: Xem vé đã mua
- **Extends**:
  - UC-B6: View Ticket Types
  - UC-B7: View Ticket Details

#### UC-B6: View Ticket Types
- **Actors**: Customer
- **Mô tả**: Xem các loại vé

#### UC-B7: View Ticket Details
- **Actors**: Customer
- **Mô tả**: Xem chi tiết vé

#### UC-B8: Buy Ticket
- **Actors**: Customer
- **Mô tả**: Mua vé sự kiện
- **Includes**:
  - UC-B9: Process Ticket Payment
  - UC-B10: Apply Voucher
  - UC-B11: Confirm Ticket Purchase
- **Extends**:
  - UC-B12: Ticket Payment Failed

#### UC-B9: Process Ticket Payment
- **Actors**: Customer, Payment Gateway
- **Mô tả**: Xử lý thanh toán vé

#### UC-B10: Apply Voucher
- **Actors**: Customer
- **Mô tả**: Áp dụng mã giảm giá
- **Extends**: UC-B8: Buy Ticket

#### UC-B11: Confirm Ticket Purchase
- **Actors**: Customer
- **Mô tả**: Xác nhận mua vé

#### UC-B12: Ticket Payment Failed
- **Actors**: Customer
- **Mô tả**: Thanh toán thất bại

#### UC-B13: Cancel Ticket
- **Actors**: Customer
- **Mô tả**: Hủy vé đã mua

#### UC-B14: Check-in Event
- **Actors**: Customer
- **Mô tả**: Check-in vào sự kiện
- **Extends**: UC-B15: Submit Check-in Form

#### UC-B15: Submit Check-in Form
- **Actors**: Customer
- **Mô tả**: Điền form check-in

#### UC-B16: Check-out Event
- **Actors**: Customer
- **Mô tả**: Check-out khỏi sự kiện

#### UC-B17: Rate Event
- **Actors**: Customer
- **Mô tả**: Đánh giá sự kiện
- **Extends**: UC-B18: Submit Feedback Form

#### UC-B18: Submit Feedback Form
- **Actors**: Customer
- **Mô tả**: Gửi form phản hồi

#### UC-B19: Apply for Volunteer
- **Actors**: Customer
- **Mô tả**: Đăng ký làm tình nguyện viên
- **Extends**: UC-B20: Submit Volunteer Form

#### UC-B20: Submit Volunteer Form
- **Actors**: Customer
- **Mô tả**: Điền form đăng ký tình nguyện viên

#### UC-B21: View My Requests
- **Actors**: Customer
- **Mô tả**: Xem các yêu cầu của mình (volunteer applications, feedback forms)

#### UC-B22: Earn Points After Event
- **Actors**: Customer
- **Mô tả**: Nhận điểm sau khi tham gia sự kiện

#### UC-B23: View Leaderboard
- **Actors**: Customer
- **Mô tả**: Xem bảng xếp hạng điểm

#### UC-B24: Manage Wallet
- **Actors**: Customer
- **Mô tả**: Quản lý ví điện tử
- **Extends**:
  - UC-B25: View Wallet Balance
  - UC-B26: Complete KYC
  - UC-B27: View Transaction History

#### UC-B25: View Wallet Balance
- **Actors**: Customer
- **Mô tả**: Xem số dư ví

#### UC-B26: Complete KYC
- **Actors**: Customer
- **Mô tả**: Hoàn thành xác minh danh tính (KYC)

#### UC-B27: View Transaction History
- **Actors**: Customer
- **Mô tả**: Xem lịch sử giao dịch

---

### C. AI AGENT USE CASES

#### UC-C1: Chat with AI Agent
- **Actors**: Customer, AI Agent
- **Mô tả**: Trò chuyện với AI Agent
- **Extends**:
  - UC-C2: Get Event Suggestions
  - UC-C3: Buy Ticket via Chat
  - UC-C4: Multi-language Support

#### UC-C2: Get Event Suggestions
- **Actors**: Customer, AI Agent
- **Mô tả**: Nhận gợi ý sự kiện từ AI

#### UC-C3: Buy Ticket via Chat
- **Actors**: Customer, AI Agent
- **Mô tả**: Mua vé qua chat với AI
- **Includes**: UC-B9: Process Ticket Payment

#### UC-C4: Multi-language Support
- **Actors**: Customer, AI Agent
- **Mô tả**: Hỗ trợ đa ngôn ngữ trong chat

---

### D. HOST USE CASES

#### UC-D1: Create Event
- **Actors**: Host
- **Mô tả**: Tạo sự kiện mới
- **Includes**:
  - UC-D2: Create Event Under Organization
  - UC-D3: Create Event Under Host's Name
- **Extends**: UC-D4: Submit Event for Approval

#### UC-D2: Create Event Under Organization
- **Actors**: Host
- **Mô tả**: Tạo sự kiện dưới tên tổ chức

#### UC-D3: Create Event Under Host's Name
- **Actors**: Host
- **Mô tả**: Tạo sự kiện dưới tên cá nhân

#### UC-D4: Submit Event for Approval
- **Actors**: Host
- **Mô tả**: Gửi sự kiện để duyệt

#### UC-D5: Edit Event
- **Actors**: Host
- **Mô tả**: Chỉnh sửa sự kiện

#### UC-D6: View Attendance
- **Actors**: Host
- **Mô tả**: Xem danh sách tham dự
- **Extends**: UC-D7: View Event Statistics

#### UC-D7: View Event Statistics
- **Actors**: Host
- **Mô tả**: Xem thống kê sự kiện

#### UC-D8: Manage Volunteer Applications
- **Actors**: Host
- **Mô tả**: Quản lý đơn đăng ký tình nguyện viên
- **Includes**:
  - UC-D9: View Volunteer Applications
  - UC-D10: Approve Volunteer Application
  - UC-D11: Reject Volunteer Application

#### UC-D9: View Volunteer Applications
- **Actors**: Host
- **Mô tả**: Xem danh sách đơn đăng ký tình nguyện viên

#### UC-D10: Approve Volunteer Application
- **Actors**: Host
- **Mô tả**: Duyệt đơn đăng ký tình nguyện viên

#### UC-D11: Reject Volunteer Application
- **Actors**: Host
- **Mô tả**: Từ chối đơn đăng ký tình nguyện viên

#### UC-D12: Create Event Form
- **Actors**: Host
- **Mô tả**: Tạo form cho sự kiện
- **Includes**:
  - UC-D13: Create Feedback Form
  - UC-D14: Create Check-in Form
  - UC-D15: Create Volunteer Form

#### UC-D13: Create Feedback Form
- **Actors**: Host
- **Mô tả**: Tạo form phản hồi

#### UC-D14: Create Check-in Form
- **Actors**: Host
- **Mô tả**: Tạo form check-in

#### UC-D15: Create Volunteer Form
- **Actors**: Host
- **Mô tả**: Tạo form đăng ký tình nguyện viên

#### UC-D16: View Form Responses
- **Actors**: Host
- **Mô tả**: Xem phản hồi từ các form

#### UC-D17: Subscribe to Plan
- **Actors**: Host
- **Mô tả**: Đăng ký gói dịch vụ
- **Includes**:
  - UC-D18: Process Subscription Payment
  - UC-D19: Confirm Subscription
- **Extends**: UC-D20: Subscription Failed

#### UC-D18: Process Subscription Payment
- **Actors**: Host, Payment Gateway
- **Mô tả**: Xử lý thanh toán đăng ký

#### UC-D19: Confirm Subscription
- **Actors**: Host
- **Mô tả**: Xác nhận đăng ký

#### UC-D20: Subscription Failed
- **Actors**: Host
- **Mô tả**: Đăng ký thất bại

#### UC-D21: Update Profile
- **Actors**: Host
- **Mô tả**: Cập nhật thông tin cá nhân/tổ chức

#### UC-D22: Manage Events
- **Actors**: Host
- **Mô tả**: Quản lý danh sách sự kiện
- **Extends**: UC-D5: Edit Event

---

### E. DEPARTMENT USE CASES

#### UC-E1: View Event Requests
- **Actors**: Department
- **Mô tả**: Xem danh sách yêu cầu duyệt sự kiện
- **Extends**: UC-E2: View Request Details

#### UC-E2: View Request Details
- **Actors**: Department
- **Mô tả**: Xem chi tiết yêu cầu

#### UC-E3: Approve Event Request
- **Actors**: Department
- **Mô tả**: Duyệt yêu cầu sự kiện

#### UC-E4: Reject Event Request
- **Actors**: Department
- **Mô tả**: Từ chối yêu cầu sự kiện

#### UC-E5: View Department Dashboard
- **Actors**: Department
- **Mô tả**: Xem dashboard phòng ban
- **Extends**:
  - UC-E6: View Department Statistics
  - UC-E7: View Approval Trends

#### UC-E6: View Department Statistics
- **Actors**: Department
- **Mô tả**: Xem thống kê phòng ban

#### UC-E7: View Approval Trends
- **Actors**: Department
- **Mô tả**: Xem xu hướng duyệt sự kiện

#### UC-E8: View Department Orders
- **Actors**: Department
- **Mô tả**: Xem đơn hàng liên quan đến phòng ban

#### UC-E9: View Featured Events
- **Actors**: Department
- **Mô tả**: Xem các sự kiện nổi bật

---

### F. ADMIN USE CASES

#### UC-F1: View Admin Dashboard
- **Actors**: Admin
- **Mô tả**: Xem dashboard quản trị
- **Extends**: UC-F2: View System Statistics

#### UC-F2: View System Statistics
- **Actors**: Admin
- **Mô tả**: Xem thống kê hệ thống

#### UC-F3: View User List
- **Actors**: Admin
- **Mô tả**: Xem danh sách người dùng
- **Extends**: UC-F4: View User Detail

#### UC-F4: View User Detail
- **Actors**: Admin
- **Mô tả**: Xem chi tiết người dùng

#### UC-F5: View Request Event
- **Actors**: Admin
- **Mô tả**: Xem yêu cầu sự kiện

#### UC-F6: View Notification
- **Actors**: Admin
- **Mô tả**: Xem thông báo hệ thống

#### UC-F7: View Report List
- **Actors**: Admin
- **Mô tả**: Xem danh sách báo cáo

#### UC-F8: View Refund Ticket
- **Actors**: Admin
- **Mô tả**: Xem yêu cầu hoàn tiền vé

#### UC-F9: View Audit Logs
- **Actors**: Admin
- **Mô tả**: Xem nhật ký kiểm toán

---

## Quan Hệ Giữa Use Cases

### Generalization (Kế Thừa)
- Customer, Host, Admin, Department → User

### Include (Bắt Buộc)
- Buy Ticket → Process Ticket Payment
- Buy Ticket → Confirm Ticket Purchase
- Subscribe to Plan → Process Subscription Payment
- Subscribe to Plan → Confirm Subscription
- Manage Volunteer Applications → View Volunteer Applications
- Create Event Form → Create Feedback Form / Create Check-in Form / Create Volunteer Form

### Extend (Tùy Chọn)
- View List Event → View Event Details
- View Purchased Tickets → View Ticket Details
- Buy Ticket → Ticket Payment Failed
- Subscribe to Plan → Subscription Failed
- Check-in Event → Submit Check-in Form
- Rate Event → Submit Feedback Form
- Apply for Volunteer → Submit Volunteer Form
- Chat with AI Agent → Get Event Suggestions
- Chat with AI Agent → Buy Ticket via Chat

---

## Sơ Đồ Use Case (Text Representation)

```
┌─────────────────────────────────────────────────────────────────┐
│                    OPENEVENT SYSTEM                             │
└─────────────────────────────────────────────────────────────────┘

ACTORS:
  User (General)
    ├── Customer (inherits)
    ├── Host (inherits)
    ├── Admin (inherits)
    └── Department (inherits)
  
  AI Agent (System)
  Payment Gateway (External)

USE CASES BY ACTOR:

[User]
  └── Authentication
      ├── Sign Up
      ├── Log In
      │   └── OAuth Login (extends)
      ├── Log Out
      ├── Reset Password
      └── Forgot Password

[Customer]
  ├── View List Event
  │   ├── View Event Details (extends)
  │   ├── Search Events (extends)
  │   └── Filter Events (extends)
  ├── View Purchased Tickets
  │   ├── View Ticket Types (extends)
  │   └── View Ticket Details (extends)
  ├── Buy Ticket
  │   ├── Process Ticket Payment (includes) → [Payment Gateway]
  │   ├── Apply Voucher (extends)
  │   ├── Confirm Ticket Purchase (includes)
  │   └── Ticket Payment Failed (extends)
  ├── Cancel Ticket
  ├── Check-in Event
  │   └── Submit Check-in Form (extends)
  ├── Check-out Event
  ├── Rate Event
  │   └── Submit Feedback Form (extends)
  ├── Apply for Volunteer
  │   └── Submit Volunteer Form (extends)
  ├── View My Requests
  ├── Earn Points After Event
  ├── View Leaderboard
  ├── Manage Wallet
  │   ├── View Wallet Balance (extends)
  │   ├── Complete KYC (extends)
  │   └── View Transaction History (extends)
  └── Chat with AI Agent
      ├── Get Event Suggestions (extends) → [AI Agent]
      ├── Buy Ticket via Chat (extends) → [AI Agent]
      └── Multi-language Support (extends) → [AI Agent]

[Host]
  ├── Create Event
  │   ├── Create Event Under Organization (includes)
  │   ├── Create Event Under Host's Name (includes)
  │   └── Submit Event for Approval (extends) → [Department]
  ├── Edit Event
  ├── View Attendance
  │   └── View Event Statistics (extends)
  ├── Manage Volunteer Applications
  │   ├── View Volunteer Applications (includes)
  │   ├── Approve Volunteer Application (includes)
  │   └── Reject Volunteer Application (includes)
  ├── Create Event Form
  │   ├── Create Feedback Form (includes)
  │   ├── Create Check-in Form (includes)
  │   └── Create Volunteer Form (includes)
  ├── View Form Responses
  ├── Subscribe to Plan
  │   ├── Process Subscription Payment (includes) → [Payment Gateway]
  │   ├── Confirm Subscription (includes)
  │   └── Subscription Failed (extends)
  ├── Update Profile
  └── Manage Events
      └── Edit Event (extends)

[Department]
  ├── View Event Requests
  │   └── View Request Details (extends)
  ├── Approve Event Request
  ├── Reject Event Request
  ├── View Department Dashboard
  │   ├── View Department Statistics (extends)
  │   └── View Approval Trends (extends)
  ├── View Department Orders
  └── View Featured Events

[Admin]
  ├── View Admin Dashboard
  │   └── View System Statistics (extends)
  ├── View User List
  │   └── View User Detail (extends)
  ├── View Request Event
  ├── View Notification
  ├── View Report List
  ├── View Refund Ticket
  └── View Audit Logs

[AI Agent]
  ├── Get Event Suggestions
  ├── Buy Ticket via Chat
  └── Multi-language Support

[Payment Gateway]
  ├── Process Ticket Payment
  └── Process Subscription Payment
```

---

## So Sánh Với Use Case Cũ

### Các Cải Tiến Chính:

1. **Bổ Sung Actor**: Thêm Department role
2. **Bổ Sung Use Cases**:
   - Volunteer Management (Apply, Approve, Reject)
   - Voucher System (Apply Voucher)
   - Wallet & KYC Management
   - Event Forms (Feedback, Check-in, Volunteer forms)
   - Request Management
   - Department-specific features
3. **Sửa Quan Hệ**: 
   - Điều chỉnh include/extend relationships
   - Thêm các quan hệ còn thiếu
4. **Cải Thiện Cấu Trúc**:
   - Nhóm use cases theo chức năng
   - Làm rõ luồng xử lý
   - Bổ sung các use case mở rộng

---

## Ghi Chú Kỹ Thuật

- **Include**: Quan hệ bắt buộc, use case được include luôn được thực thi
- **Extend**: Quan hệ tùy chọn, use case được extend chỉ thực thi trong điều kiện nhất định
- **Generalization**: Quan hệ kế thừa, actor con có tất cả quyền của actor cha
- **Association**: Quan hệ giữa actor và use case

---

## Kết Luận

Use Case Diagram mới này đã:
- ✅ Bổ sung đầy đủ các actors và use cases
- ✅ Sửa các quan hệ sai
- ✅ Phản ánh đúng chức năng của hệ thống
- ✅ Dễ hiểu và dễ bảo trì

Tài liệu này có thể được sử dụng làm cơ sở cho việc phát triển và kiểm thử hệ thống.


