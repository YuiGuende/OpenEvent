# Phân Tích Các Chức Năng Còn Thiếu

## Tổng Quan
Dựa trên danh sách chức năng bạn cung cấp và codebase hiện tại, dưới đây là phân tích chi tiết về các chức năng đã có và còn thiếu.

---

## ✅ CÁC CHỨC NĂNG ĐÃ CÓ (Đã Implement)

### 1. Authentication & User Management
- ✅ **Home Page** - `HomeController.java`
- ✅ **Search Events** - `SearchController.java`
- ✅ **Event Details Page** - `EventDetailController.java`
- ✅ **Register Account** - `AuthController.java` (`/api/auth/register`)
- ✅ **Login** - `AuthController.java` (`/api/auth/login`)
- ✅ **Register for Event** - `OrderController.java` (`/api/orders/create-with-ticket-types`)
- ✅ **View My Events** - `HomeController.java` (trong home page)
- ✅ **View Purchased Tickets** - `UserOrderController.java` (`/orders`)
- ✅ **View Order Details** - `UserOrderController.java` (`/orders/{orderId}`)

### 2. Event Management
- ✅ **Check-in (QR)** - `EventAttendanceController.java` (`/events/{eventId}/qr-checkin`)
- ✅ **Check-out (QR)** - `EventAttendanceController.java` (`/events/{eventId}/qr-checkout`)
- ✅ **View Attendance List** - `EventAttendanceController.java` (`/events/{eventId}/attendances`)
- ✅ **Event Check-in/Check-out List** - `EventAttendanceController.java` (`/events/{eventId}/manage-attendance`)
- ✅ **View Public Event List** - `EventController.java` (various endpoints)
- ✅ **View Event as Host** - `HostController.java` (`/fragment/events`)
- ✅ **View Event Status** - `EventController.java`
- ✅ **Cancel Event** - `EventController.java` (`DELETE /api/events/{id}`)
- ✅ **Order Products** - `OrderController.java` (`POST /api/orders`)
- ✅ **View Event Statistics** - `DashboardApiController.java` (`/api/dashboard/event/{eventId}/stats`)

### 3. AI & Chat
- ✅ **Chat with AI Agent** - `EnhancedAIController.java`, `ChatApiController.java`
- ✅ **Search for Event** - `SearchController.java` (`/search`)
- ✅ **AI Event Suggestions** - Có trong AI service (cần verify endpoint)

### 4. Forms & Feedback
- ✅ **Submit Event Form** - `EventFormController.java` (`POST /forms/feedback/submit`)
- ✅ **View Event Forms** - `EventFormController.java` (`/forms/{eventId}`)
- ✅ **Create Event Forms** - `EventFormController.java` (`/forms/create/{eventId}`)
- ✅ **Submit Service Feedback** - `EventFormController.java` (feedback form)

### 5. Payment & Orders
- ✅ **Process Payment** - `PaymentController.java` (`POST /api/payments/create-for-order/{orderId}`)
- ✅ **Payment History** - `PaymentController.java` (`GET /api/payments/history`)
- ✅ **Request Refund** - `RequestController.java` (RequestType.REFUND)
- ✅ **View Refund Tickets** - Có thể thông qua Request system

### 6. Vouchers & Points
- ✅ **Voucher Validation** - `VoucherController.java` (`GET /api/vouchers/validate/{voucherCode}`)
- ✅ **Earn Points** - Customer entity có field `points`, logic cộng điểm có thể trong service

### 7. Admin Features
- ✅ **Admin Dashboard** - `AdminController.java` (`/admin/dashboard`)
- ✅ **Event Revenue Monitoring** - `DashboardApiController.java`
- ✅ **Export Attendee List** - `EventAttendeesController.java` (`/export/excel`)
- ✅ **View Ticket Sales Stats** - `DashboardApiController.java`

### 8. Department Features
- ✅ **Department Dashboard** - `DepartmentController.java` (`/department/dashboard`)
- ✅ **Manage Articles** - `DepartmentController.java` (`/department/articles`)
- ✅ **Create Department** - Có thể thông qua registration flow
- ✅ **View Service/Dept Details** - `DepartmentController.java`

### 9. Host Features
- ✅ **Event Dashboard** - `HostController.java` (`/fragment/dashboard`)
- ✅ **New Event Page** - `EventManageController.java`
- ✅ **Update Event Page** - `EventManageController.java` (`/fragments/update-event`)

### 10. Notifications
- ✅ **Notifications** - `NotificationController.java` (`/api/notifications/my-notifications`)

---

## ❌ CÁC CHỨC NĂNG CÒN THIẾU (Chưa Implement)

### 1. User Profile Management
- ❌ **User Profile** - Không có controller/view riêng để xem profile
- ❌ **Edit Profile** - Không có endpoint để edit user profile (name, phone, avatar, etc.)
- ❌ **Change Password** - Không có endpoint để đổi mật khẩu
- ❌ **Delete Account** - Không có endpoint để xóa tài khoản

**Gợi ý Implementation:**
```java
// Cần tạo UserProfileController với các endpoints:
GET  /api/user/profile          // Xem profile
PUT  /api/user/profile          // Cập nhật profile
PUT  /api/user/change-password  // Đổi mật khẩu
DELETE /api/user/account        // Xóa tài khoản
```

### 2. Password Reset
- ❌ **Forgot Password** - Có trong use case docs nhưng chưa implement
- ❌ **Reset Password** - Chưa có endpoint

**Gợi ý Implementation:**
```java
// Cần thêm vào AuthController:
POST /api/auth/forgot-password   // Gửi email reset
POST /api/auth/reset-password    // Reset với token
```

### 3. Wishlist
- ❌ **Save Event to Wishlist** - Không có entity/model cho wishlist
- ❌ **Remove Event From Wishlist** - Không có endpoint

**Gợi ý Implementation:**
```java
// Cần tạo:
- Entity: Wishlist (user_id, event_id, created_at)
- Repository: IWishlistRepo
- Service: WishlistService
- Controller: WishlistController với:
  POST   /api/wishlist/{eventId}      // Thêm vào wishlist
  DELETE /api/wishlist/{eventId}      // Xóa khỏi wishlist
  GET    /api/wishlist                // Xem danh sách wishlist
```

### 4. Leaderboard
- ❌ **Leaderboard** - Có trong use case docs nhưng chưa implement

**Gợi ý Implementation:**
```java
// Cần tạo LeaderboardController:
GET /api/leaderboard              // Top users by points
GET /api/leaderboard/my-rank      // Rank của user hiện tại
```

### 5. Points Management
- ❌ **View Points Balance** - Customer có field `points` nhưng chưa có endpoint riêng để xem

**Gợi ý Implementation:**
```java
// Có thể thêm vào UserProfileController hoặc tạo riêng:
GET /api/user/points              // Xem số điểm hiện tại
GET /api/user/points/history      // Lịch sử tích điểm
```

### 6. Admin Features
- ❌ **Ban User Accounts** - Không có endpoint để ban/unban users

**Gợi ý Implementation:**
```java
// Thêm vào AdminController:
POST /api/admin/users/{userId}/ban      // Ban user
POST /api/admin/users/{userId}/unban    // Unban user
GET  /api/admin/users                   // List users với filter
```

### 7. Chat Features
- ❌ **Chat with Host** - Có AI chat nhưng chưa có direct chat với host

**Gợi ý Implementation:**
```java
// Cần tạo messaging system:
- Entity: Message (sender_id, receiver_id, event_id, content, created_at)
- Repository: IMessageRepo
- Service: MessageService
- Controller: MessageController với:
  POST /api/messages/send              // Gửi message
  GET  /api/messages/with-host/{hostId} // Lấy conversation với host
  GET  /api/messages/conversations      // List conversations
```

### 8. Order Feedback
- ❌ **Give Order Feedback** - Có event feedback nhưng chưa có order-specific feedback

**Gợi ý Implementation:**
```java
// Có thể thêm vào OrderController:
POST /api/orders/{orderId}/feedback     // Gửi feedback cho order
GET  /api/orders/{orderId}/feedback    // Xem feedback của order
```

### 9. Promotions Management
- ❌ **Manage Promotions** - Không có system quản lý promotions (khác với vouchers)

**Gợi ý Implementation:**
```java
// Cần tạo Promotion system:
- Entity: Promotion (code, discount_type, discount_value, start_date, end_date, etc.)
- Repository: IPromotionRepo
- Service: PromotionService
- Controller: PromotionController (cho admin/host)
```

### 10. Services Management
- ❌ **Create Services** - Không rõ "services" ở đây là gì (có thể là services của department?)

**Cần làm rõ:** Services có phải là các dịch vụ của Department không? Nếu vậy có thể đã có trong DepartmentController.

### 11. Appointment Reports
- ❌ **View Appointment Reports** - Không rõ "appointment" trong context này

**Cần làm rõ:** Appointment có phải là các cuộc hẹn/meetings không? Có thể liên quan đến event scheduling?

### 12. Menu List
- ❌ **View Menu List** - Không rõ "menu" trong context này

**Cần làm rõ:** Menu có phải là menu của event (food/drinks) không? Hoặc menu navigation?

---

## 📊 TỔNG KẾT

### Đã có: ~45/58 chức năng (77.6%)
### Còn thiếu: ~13/58 chức năng (22.4%)

### Priority Implementation:

#### **High Priority** (Quan trọng cho user experience):
1. ✅ User Profile Management (View, Edit, Change Password)
2. ✅ Forgot/Reset Password
3. ✅ View Points Balance
4. ✅ Wishlist (Save/Remove Events)

#### **Medium Priority** (Nice to have):
5. ✅ Leaderboard
6. ✅ Chat with Host
7. ✅ Give Order Feedback
8. ✅ Ban User Accounts (Admin)

#### **Low Priority** (Cần làm rõ requirements):
9. ⚠️ Manage Promotions (khác với Vouchers?)
10. ⚠️ Create Services (là gì?)
11. ⚠️ View Appointment Reports (là gì?)
12. ⚠️ View Menu List (là gì?)
13. ⚠️ Delete Account (có thể cần soft delete)

---

## 🔍 NOTES

1. **Forgot Password**: Có trong use case docs nhưng chưa implement. Cần thêm email service để gửi reset link.

2. **Wishlist**: Hoàn toàn chưa có. Cần tạo entity và full CRUD.

3. **Leaderboard**: Có trong use case nhưng chưa implement. Customer có points field, có thể dùng để build leaderboard.

4. **User Profile**: User entity đã có các fields (name, phone, avatar, etc.) nhưng chưa có UI/endpoints để edit.

5. **Points System**: Customer có points field và logic cộng điểm có thể đã có trong service, nhưng chưa có endpoint riêng để view points balance.

6. **Chat with Host**: Có AI chat nhưng chưa có direct messaging giữa user và host.

---

## 📝 RECOMMENDATIONS

1. **Tạo UserProfileController** để quản lý profile, đổi mật khẩu
2. **Implement Forgot/Reset Password** với email service
3. **Tạo Wishlist system** (entity, service, controller)
4. **Implement Leaderboard** dựa trên Customer.points
5. **Tạo Messaging system** cho chat với host
6. **Làm rõ requirements** cho các chức năng còn mơ hồ (Services, Appointments, Menu)

