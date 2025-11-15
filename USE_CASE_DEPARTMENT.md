# Use Case Diagram - DEPARTMENT (Phòng Ban)

## Tổng Quan
Tài liệu này mô tả Use Case Diagram chi tiết cho vai trò **Department** (Phòng Ban) trong hệ thống quản lý sự kiện OpenEvent. Department có trách nhiệm duyệt sự kiện, quản lý các yêu cầu từ Host, và theo dõi thống kê.

## Actor
- **Department**: Phòng ban có quyền duyệt sự kiện, quản lý yêu cầu, xem thống kê, và quản lý bài viết.

## Actors Liên Quan
- **Host**: Người tổ chức sự kiện (gửi yêu cầu duyệt đến Department)

---

## Use Cases Chi Tiết

### 1. DASHBOARD VÀ THỐNG KÊ

#### UC-E1: View Department Dashboard (Xem Dashboard Phòng Ban)
- **Mô tả**: Department xem dashboard tổng quan của phòng ban
- **Preconditions**: Department đã đăng nhập
- **Main Flow**:
  1. Department truy cập trang dashboard
  2. Hệ thống hiển thị các thống kê tổng quan
  3. Department có thể xem: tổng số sự kiện, yêu cầu chờ duyệt, sự kiện đang diễn ra, tổng người tham dự
- **Extends**:
  - UC-E2: View Department Statistics
  - UC-E3: View Approval Trends

#### UC-E2: View Department Statistics (Xem Thống Kê Phòng Ban)
- **Mô tả**: Department xem các thống kê chi tiết về phòng ban
- **Extends**: UC-E1: View Department Dashboard
- **Thông tin thống kê**:
  - Tổng số sự kiện
  - Số yêu cầu chờ duyệt
  - Sự kiện đang diễn ra
  - Tổng số người tham dự
  - Doanh thu
  - Thời gian duyệt trung bình
- **Extends**:
  - UC-E4: View Events By Month
  - UC-E5: View Events By Type
  - UC-E6: View Participants Trend
  - UC-E7: View Revenue Trend
  - UC-E8: View Order Status Distribution

#### UC-E3: View Approval Trends (Xem Xu Hướng Duyệt)
- **Mô tả**: Department xem xu hướng duyệt sự kiện theo thời gian
- **Extends**: UC-E1: View Department Dashboard
- **Main Flow**:
  1. Department chọn xem xu hướng duyệt
  2. Hệ thống hiển thị biểu đồ xu hướng
  3. Department có thể xem theo: ngày, tuần, tháng

#### UC-E4: View Events By Month (Xem Sự Kiện Theo Tháng)
- **Mô tả**: Department xem thống kê sự kiện được phân bổ theo tháng
- **Extends**: UC-E2: View Department Statistics
- **Main Flow**:
  1. Department chọn xem thống kê theo tháng
  2. Hệ thống hiển thị số lượng sự kiện mỗi tháng
  3. Department có thể xem biểu đồ phân bổ

#### UC-E5: View Events By Type (Xem Sự Kiện Theo Loại)
- **Mô tả**: Department xem thống kê sự kiện được phân loại theo loại (Music, Festival, Competition, Workshop)
- **Extends**: UC-E2: View Department Statistics
- **Main Flow**:
  1. Department chọn xem thống kê theo loại
  2. Hệ thống hiển thị số lượng sự kiện mỗi loại
  3. Department có thể xem biểu đồ phân bổ

#### UC-E6: View Participants Trend (Xem Xu Hướng Người Tham Dự)
- **Mô tả**: Department xem xu hướng số lượng người tham dự theo thời gian
- **Extends**: UC-E2: View Department Statistics
- **Main Flow**:
  1. Department chọn xem xu hướng người tham dự
  2. Hệ thống hiển thị biểu đồ xu hướng
  3. Department có thể phân tích tăng trưởng

#### UC-E7: View Revenue Trend (Xem Xu Hướng Doanh Thu)
- **Mô tả**: Department xem xu hướng doanh thu từ các sự kiện
- **Extends**: UC-E2: View Department Statistics
- **Main Flow**:
  1. Department chọn xem xu hướng doanh thu
  2. Hệ thống hiển thị biểu đồ doanh thu theo thời gian
  3. Department có thể phân tích xu hướng tài chính

#### UC-E8: View Order Status Distribution (Xem Phân Bổ Trạng Thái Đơn Hàng)
- **Mô tả**: Department xem phân bổ các đơn hàng theo trạng thái
- **Extends**: UC-E2: View Department Statistics
- **Main Flow**:
  1. Department chọn xem phân bổ đơn hàng
  2. Hệ thống hiển thị số lượng đơn hàng theo từng trạng thái
  3. Department có thể xem biểu đồ phân bổ

#### UC-E9: View Average Approval Time (Xem Thời Gian Duyệt Trung Bình)
- **Mô tả**: Department xem thời gian duyệt sự kiện trung bình
- **Main Flow**:
  1. Department truy cập thống kê thời gian duyệt
  2. Hệ thống tính toán và hiển thị thời gian trung bình
  3. Department có thể đánh giá hiệu quả xử lý

---

### 2. QUẢN LÝ YÊU CẦU SỰ KIỆN

#### UC-E10: View Event Requests (Xem Yêu Cầu Sự Kiện)
- **Mô tả**: Department xem danh sách các yêu cầu duyệt sự kiện từ Host
- **Preconditions**: Department đã đăng nhập
- **Main Flow**:
  1. Department truy cập trang quản lý yêu cầu
  2. Hệ thống hiển thị danh sách yêu cầu
  3. Department có thể lọc theo trạng thái (PENDING, APPROVED, REJECTED)
  4. Department có thể xem chi tiết từng yêu cầu
- **Extends**:
  - UC-E11: View Request Details
  - UC-E12: View Pending Requests

#### UC-E11: View Request Details (Xem Chi Tiết Yêu Cầu)
- **Mô tả**: Department xem thông tin chi tiết về một yêu cầu sự kiện
- **Extends**: UC-E10: View Event Requests
- **Main Flow**:
  1. Department chọn yêu cầu từ danh sách
  2. Hệ thống hiển thị thông tin chi tiết:
     - Thông tin sự kiện
     - Thông tin Host
     - Trạng thái yêu cầu
     - Thời gian tạo
  3. Department có thể duyệt hoặc từ chối

#### UC-E12: View Pending Requests (Xem Yêu Cầu Chờ Duyệt)
- **Mô tả**: Department xem danh sách các yêu cầu đang chờ duyệt
- **Extends**: UC-E10: View Event Requests
- **Main Flow**:
  1. Department lọc yêu cầu theo trạng thái PENDING
  2. Hệ thống hiển thị danh sách yêu cầu chờ duyệt
  3. Department có thể xử lý từng yêu cầu

#### UC-E13: Approve Event Request (Duyệt Yêu Cầu Sự Kiện)
- **Mô tả**: Department duyệt yêu cầu sự kiện từ Host
- **Preconditions**: Có yêu cầu ở trạng thái PENDING
- **Main Flow**:
  1. Department xem chi tiết yêu cầu
  2. Department kiểm tra thông tin sự kiện
  3. Department chọn "Duyệt"
  4. Department có thể thêm phản hồi (tùy chọn)
  5. Hệ thống cập nhật trạng thái sự kiện thành PUBLIC
  6. Host nhận thông báo duyệt thành công
- **Alternative Flow**:
  - Thông tin không hợp lệ → Department từ chối

#### UC-E14: Reject Event Request (Từ Chối Yêu Cầu Sự Kiện)
- **Mô tả**: Department từ chối yêu cầu sự kiện từ Host
- **Preconditions**: Có yêu cầu ở trạng thái PENDING
- **Main Flow**:
  1. Department xem chi tiết yêu cầu
  2. Department xác định lý do từ chối
  3. Department chọn "Từ chối"
  4. Department nhập lý do từ chối (bắt buộc)
  5. Hệ thống cập nhật trạng thái yêu cầu thành REJECTED
  6. Hệ thống cập nhật trạng thái sự kiện thành REJECTED
  7. Host nhận thông báo từ chối kèm lý do

---

### 3. QUẢN LÝ SỰ KIỆN

#### UC-E15: View Events (Xem Danh Sách Sự Kiện)
- **Mô tả**: Department xem danh sách tất cả sự kiện liên quan đến phòng ban
- **Main Flow**:
  1. Department truy cập trang quản lý sự kiện
  2. Hệ thống hiển thị danh sách sự kiện
  3. Department có thể:
     - Lọc theo loại sự kiện (Music, Festival, Competition, Workshop)
     - Lọc theo trạng thái (DRAFT, PENDING, PUBLIC, ONGOING, CANCEL)
     - Sắp xếp theo các tiêu chí khác nhau
     - Xem chi tiết từng sự kiện
- **Extends**:
  - UC-E16: View Event Details
  - UC-E17: View Upcoming Events

#### UC-E16: View Event Details (Xem Chi Tiết Sự Kiện)
- **Mô tả**: Department xem thông tin chi tiết về một sự kiện
- **Extends**: UC-E15: View Events
- **Main Flow**:
  1. Department chọn sự kiện từ danh sách
  2. Hệ thống hiển thị thông tin chi tiết:
     - Thông tin cơ bản (tên, mô tả, hình ảnh)
     - Thời gian và địa điểm
     - Thông tin Host/Organization
     - Trạng thái sự kiện
     - Số lượng người tham dự
     - Doanh thu

#### UC-E17: View Upcoming Events (Xem Sự Kiện Sắp Tới)
- **Mô tả**: Department xem danh sách các sự kiện sắp diễn ra
- **Extends**: UC-E15: View Events
- **Main Flow**:
  1. Department chọn xem sự kiện sắp tới
  2. Hệ thống hiển thị danh sách sự kiện có ngày bắt đầu trong tương lai
  3. Sự kiện được sắp xếp theo thời gian gần nhất

#### UC-E18: Update Event Status (Cập Nhật Trạng Thái Sự Kiện)
- **Mô tả**: Department cập nhật trạng thái sự kiện (thủ công)
- **Main Flow**:
  1. Department chọn sự kiện
  2. Department chọn trạng thái mới
  3. Department xác nhận thay đổi
  4. Hệ thống cập nhật trạng thái
  5. Host nhận thông báo (nếu có thay đổi)

#### UC-E19: View Featured Events (Xem Sự Kiện Nổi Bật)
- **Mô tả**: Department xem danh sách các sự kiện nổi bật
- **Main Flow**:
  1. Department truy cập trang sự kiện nổi bật
  2. Hệ thống hiển thị các sự kiện được đánh dấu nổi bật
  3. Department có thể xem thống kê từng sự kiện

---

### 4. QUẢN LÝ ĐƠN HÀNG

#### UC-E20: View Department Orders (Xem Đơn Hàng Phòng Ban)
- **Mô tả**: Department xem danh sách đơn hàng liên quan đến các sự kiện của phòng ban
- **Main Flow**:
  1. Department truy cập trang quản lý đơn hàng
  2. Hệ thống hiển thị danh sách đơn hàng
  3. Department có thể:
     - Lọc theo trạng thái đơn hàng
     - Xem chi tiết từng đơn hàng
     - Xuất danh sách đơn hàng
- **Extends**:
  - UC-E21: View Order Details

#### UC-E21: View Order Details (Xem Chi Tiết Đơn Hàng)
- **Mô tả**: Department xem thông tin chi tiết về một đơn hàng
- **Extends**: UC-E20: View Department Orders
- **Main Flow**:
  1. Department chọn đơn hàng từ danh sách
  2. Hệ thống hiển thị thông tin chi tiết:
     - Thông tin khách hàng
     - Thông tin sự kiện
     - Loại vé và số lượng
     - Giá trị đơn hàng
     - Trạng thái thanh toán
     - Thời gian tạo

---

### 5. QUẢN LÝ BÀI VIẾT (ARTICLES)

#### UC-E22: View Articles (Xem Danh Sách Bài Viết)
- **Mô tả**: Department xem danh sách các bài viết của phòng ban
- **Main Flow**:
  1. Department truy cập trang quản lý bài viết
  2. Hệ thống hiển thị danh sách bài viết
  3. Department có thể lọc theo trạng thái (DRAFT, PUBLISHED)
- **Extends**:
  - UC-E23: Create Article
  - UC-E24: Edit Article

#### UC-E23: Create Article (Tạo Bài Viết)
- **Mô tả**: Department tạo bài viết mới
- **Main Flow**:
  1. Department chọn "Tạo bài viết mới"
  2. Department điền thông tin:
     - Tiêu đề
     - Nội dung
     - Hình ảnh (tùy chọn)
  3. Department lưu bài viết (DRAFT hoặc PUBLISH)
  4. Hệ thống lưu bài viết
- **Includes**: UC-E22: View Articles

#### UC-E24: Edit Article (Chỉnh Sửa Bài Viết)
- **Mô tả**: Department chỉnh sửa bài viết đã tạo
- **Extends**: UC-E22: View Articles
- **Main Flow**:
  1. Department chọn bài viết cần chỉnh sửa
  2. Department cập nhật thông tin
  3. Department lưu thay đổi
  4. Hệ thống cập nhật bài viết

#### UC-E25: Delete Article (Xóa Bài Viết)
- **Mô tả**: Department xóa bài viết
- **Main Flow**:
  1. Department chọn bài viết cần xóa
  2. Department xác nhận xóa
  3. Hệ thống xóa bài viết
  4. Bài viết được loại bỏ khỏi danh sách

#### UC-E26: Publish Article (Xuất Bản Bài Viết)
- **Mô tả**: Department xuất bản bài viết (chuyển từ DRAFT sang PUBLISHED)
- **Main Flow**:
  1. Department chọn bài viết ở trạng thái DRAFT
  2. Department chọn "Xuất bản"
  3. Hệ thống cập nhật trạng thái bài viết thành PUBLISHED
  4. Bài viết được hiển thị công khai

---

## Sơ Đồ Use Case (Text Representation)

```
┌─────────────────────────────────────────────────────────────┐
│              DEPARTMENT USE CASES                            │
└─────────────────────────────────────────────────────────────┘

[Department] ──────────────────────────────────────────────────

DASHBOARD VÀ THỐNG KÊ:
  ├── View Department Dashboard
  │   ├── View Department Statistics (extends)
  │   │   ├── View Events By Month (extends)
  │   │   ├── View Events By Type (extends)
  │   │   ├── View Participants Trend (extends)
  │   │   ├── View Revenue Trend (extends)
  │   │   └── View Order Status Distribution (extends)
  │   └── View Approval Trends (extends)
  └── View Average Approval Time

QUẢN LÝ YÊU CẦU SỰ KIỆN:
  ├── View Event Requests
  │   ├── View Request Details (extends)
  │   └── View Pending Requests (extends)
  ├── Approve Event Request
  └── Reject Event Request

QUẢN LÝ SỰ KIỆN:
  ├── View Events
  │   ├── View Event Details (extends)
  │   └── View Upcoming Events (extends)
  ├── Update Event Status
  └── View Featured Events

QUẢN LÝ ĐƠN HÀNG:
  └── View Department Orders
      └── View Order Details (extends)

QUẢN LÝ BÀI VIẾT:
  ├── View Articles
  │   ├── Create Article (includes)
  │   └── Edit Article (extends)
  ├── Delete Article
  └── Publish Article
```

---

## Quan Hệ Giữa Use Cases

### Include (Bắt Buộc)
- View Articles → Create Article (khi tạo bài viết mới)

### Extend (Tùy Chọn)
- View Department Dashboard → View Department Statistics
- View Department Dashboard → View Approval Trends
- View Department Statistics → View Events By Month
- View Department Statistics → View Events By Type
- View Department Statistics → View Participants Trend
- View Department Statistics → View Revenue Trend
- View Department Statistics → View Order Status Distribution
- View Event Requests → View Request Details
- View Event Requests → View Pending Requests
- View Events → View Event Details
- View Events → View Upcoming Events
- View Department Orders → View Order Details
- View Articles → Edit Article

---

## So Sánh Với Use Case Cũ

### Các Cải Tiến:

1. **Bổ Sung Use Cases**:
   - ✅ View Events By Month
   - ✅ View Events By Type
   - ✅ View Participants Trend
   - ✅ View Revenue Trend
   - ✅ View Order Status Distribution
   - ✅ View Average Approval Time
   - ✅ View Pending Requests
   - ✅ View Upcoming Events
   - ✅ Update Event Status
   - ✅ View Order Details
   - ✅ Quản lý Articles (View, Create, Edit, Delete, Publish)

2. **Sửa Quan Hệ**:
   - ✅ Sửa quan hệ extend cho đúng hướng
   - ✅ Bổ sung quan hệ include cho Create Article

3. **Nhóm Use Cases**:
   - ✅ Nhóm theo chức năng rõ ràng
   - ✅ Dễ hiểu và dễ bảo trì

4. **Chi Tiết Hóa**:
   - ✅ Mô tả đầy đủ preconditions, main flow, alternative flow
   - ✅ Làm rõ các thống kê và báo cáo

---

## Luồng Xử Lý Chính

### Luồng Duyệt Sự Kiện:
1. Host → Submit Event for Approval
2. Department → View Event Requests
3. Department → View Request Details
4. Department → Approve Event Request hoặc Reject Event Request
5. Host nhận thông báo kết quả

### Luồng Xem Dashboard:
1. Department → View Department Dashboard
2. Department → View Department Statistics
3. Department có thể xem các thống kê chi tiết:
   - View Events By Month
   - View Events By Type
   - View Participants Trend
   - View Revenue Trend
   - View Order Status Distribution

### Luồng Quản Lý Bài Viết:
1. Department → View Articles
2. Department → Create Article (tạo mới)
3. Department → Edit Article (chỉnh sửa)
4. Department → Publish Article (xuất bản)

---

## Ghi Chú

- Tất cả use cases yêu cầu Department đã đăng nhập
- Department có quyền duyệt/từ chối sự kiện từ Host
- Department có thể quản lý bài viết để thông tin về các sự kiện
- Các thống kê giúp Department đánh giá hiệu quả và xu hướng
- View Average Approval Time giúp Department cải thiện thời gian xử lý

---

## Đặc Điểm Nổi Bật

1. **Quyền Duyệt Sự Kiện**: Department là cấp duyệt sự kiện từ Host
2. **Thống Kê Chi Tiết**: Có nhiều loại thống kê để phân tích
3. **Quản Lý Bài Viết**: Department có thể tạo và quản lý bài viết
4. **Theo Dõi Đơn Hàng**: Department có thể theo dõi đơn hàng từ các sự kiện
5. **Quản Lý Sự Kiện**: Department có thể xem và cập nhật trạng thái sự kiện


