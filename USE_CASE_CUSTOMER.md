# Use Case Diagram - CUSTOMER/GUEST (Khách Hàng/Khách)

## Tổng Quan
Tài liệu này mô tả Use Case Diagram chi tiết cho vai trò **Customer** (Khách Hàng) và **Guest** (Khách) trong hệ thống quản lý sự kiện OpenEvent.

## Actors
- **Customer**: Người dùng đã đăng ký, có thể mua vé, tham gia sự kiện, tương tác với AI
- **Guest**: Người dùng chưa đăng ký, chỉ có thể xem sự kiện (kế thừa từ Customer)

## Actors Liên Quan
- **Payment Gateway**: Xử lý thanh toán vé
- **AI Agent**: Hỗ trợ tìm kiếm và mua vé qua chat

---

## Use Cases Chi Tiết

### 1. XEM VÀ TÌM KIẾM SỰ KIỆN

#### UC-C1: View List Event (Xem Danh Sách Sự Kiện)
- **Actors**: Customer, Guest
- **Mô tả**: Xem danh sách các sự kiện có sẵn
- **Preconditions**: Không cần đăng nhập (Guest có thể xem)
- **Main Flow**:
  1. Người dùng truy cập trang chủ hoặc trang sự kiện
  2. Hệ thống hiển thị danh sách sự kiện
  3. Người dùng có thể xem thông tin cơ bản
- **Extends**:
  - UC-C2: View Event Details
  - UC-C3: Search Events
  - UC-C4: Filter Events

#### UC-C2: View Event Details (Xem Chi Tiết Sự Kiện)
- **Actors**: Customer, Guest
- **Mô tả**: Xem thông tin chi tiết về một sự kiện
- **Extends**: UC-C1: View List Event, UC-C3: Search Events
- **Main Flow**:
  1. Người dùng chọn sự kiện từ danh sách
  2. Hệ thống hiển thị thông tin chi tiết
  3. Người dùng có thể xem: mô tả, địa điểm, thời gian, giá vé, v.v.

#### UC-C3: Search Events (Tìm Kiếm Sự Kiện)
- **Actors**: Customer, Guest
- **Mô tả**: Tìm kiếm sự kiện theo từ khóa
- **Extends**: UC-C1: View List Event
- **Main Flow**:
  1. Người dùng nhập từ khóa tìm kiếm
  2. Hệ thống trả về kết quả phù hợp
  3. Người dùng có thể xem chi tiết từ kết quả

#### UC-C4: Filter Events (Lọc Sự Kiện)
- **Actors**: Customer, Guest
- **Mô tả**: Lọc sự kiện theo tiêu chí (loại, ngày, giá, địa điểm)
- **Extends**: UC-C1: View List Event

---

### 2. QUẢN LÝ VÉ

#### UC-C5: View Purchased Tickets (Xem Vé Đã Mua)
- **Actors**: Customer
- **Mô tả**: Xem danh sách vé đã mua
- **Preconditions**: Customer đã đăng nhập
- **Main Flow**:
  1. Customer truy cập trang "Vé của tôi"
  2. Hệ thống hiển thị danh sách vé đã mua
  3. Customer có thể xem chi tiết từng vé
- **Extends**:
  - UC-C6: View Ticket Types
  - UC-C7: View Ticket Details

#### UC-C6: View Ticket Types (Xem Loại Vé)
- **Actors**: Customer
- **Mô tả**: Xem các loại vé có sẵn cho sự kiện
- **Extends**: UC-C8: Buy Ticket

#### UC-C7: View Ticket Details (Xem Chi Tiết Vé)
- **Actors**: Customer
- **Mô tả**: Xem thông tin chi tiết về vé đã mua
- **Extends**: UC-C6: View Ticket Types, UC-C5: View Purchased Tickets

#### UC-C8: Buy Ticket (Mua Vé)
- **Actors**: Customer
- **Mô tả**: Mua vé cho sự kiện
- **Preconditions**: Customer đã đăng nhập
- **Main Flow**:
  1. Customer chọn sự kiện
  2. Customer chọn loại vé và số lượng
  3. Customer có thể áp dụng voucher
  4. Customer thực hiện thanh toán
  5. Hệ thống xác nhận mua vé
- **Includes**:
  - UC-C9: Process Ticket Payment
  - UC-C11: Confirm Ticket Purchase
- **Extends**:
  - UC-C6: View Ticket Types
  - UC-C10: Apply Voucher
  - UC-C12: Ticket Payment Failed

#### UC-C9: Process Ticket Payment (Xử Lý Thanh Toán Vé)
- **Actors**: Customer, Payment Gateway
- **Mô tả**: Xử lý thanh toán cho vé
- **Includes**: UC-C8: Buy Ticket
- **Main Flow**:
  1. Customer chọn phương thức thanh toán
  2. Hệ thống chuyển đến Payment Gateway
  3. Payment Gateway xử lý thanh toán
  4. Kết quả được trả về hệ thống
- **Extends**: UC-C12: Ticket Payment Failed

#### UC-C10: Apply Voucher (Áp Dụng Mã Giảm Giá)
- **Actors**: Customer
- **Mô tả**: Áp dụng mã giảm giá khi mua vé
- **Extends**: UC-C8: Buy Ticket
- **Main Flow**:
  1. Customer nhập mã voucher
  2. Hệ thống kiểm tra tính hợp lệ
  3. Hệ thống áp dụng giảm giá nếu hợp lệ

#### UC-C11: Confirm Ticket Purchase (Xác Nhận Mua Vé)
- **Actors**: Customer
- **Mô tả**: Xác nhận mua vé thành công
- **Includes**: UC-C8: Buy Ticket
- **Main Flow**:
  1. Thanh toán thành công
  2. Hệ thống tạo order và ticket
  3. Hệ thống gửi email xác nhận
  4. Customer nhận vé trong tài khoản

#### UC-C12: Ticket Payment Failed (Thanh Toán Thất Bại)
- **Actors**: Customer
- **Mô tả**: Xử lý khi thanh toán thất bại
- **Extends**: UC-C9: Process Ticket Payment
- **Main Flow**:
  1. Thanh toán không thành công
  2. Hệ thống thông báo lỗi
  3. Customer có thể thử lại hoặc hủy

#### UC-C13: Cancel Ticket (Hủy Vé)
- **Actors**: Customer
- **Mô tả**: Hủy vé đã mua (nếu được phép)
- **Preconditions**: Vé có thể hủy (theo chính sách)
- **Main Flow**:
  1. Customer chọn vé cần hủy
  2. Customer xác nhận hủy
  3. Hệ thống xử lý hoàn tiền (nếu có)

---

### 3. THAM GIA SỰ KIỆN

#### UC-C14: Check-in Event (Check-in Sự Kiện)
- **Actors**: Customer
- **Mô tả**: Check-in vào sự kiện khi đến
- **Preconditions**: Customer đã mua vé và sự kiện đang diễn ra
- **Main Flow**:
  1. Customer đến địa điểm sự kiện
  2. Customer quét QR code hoặc nhập mã
  3. Hệ thống xác nhận check-in
  4. Customer có thể điền form check-in (nếu có)
- **Extends**: UC-C15: Submit Check-in Form

#### UC-C15: Submit Check-in Form (Gửi Form Check-in)
- **Actors**: Customer
- **Mô tả**: Điền và gửi form check-in
- **Extends**: UC-C14: Check-in Event

#### UC-C16: Check-out Event (Check-out Sự Kiện)
- **Actors**: Customer
- **Mô tả**: Check-out khỏi sự kiện khi rời đi
- **Extends**: UC-C14: Check-in Event
- **Main Flow**:
  1. Customer rời sự kiện
  2. Customer thực hiện check-out
  3. Hệ thống ghi nhận thời gian tham dự

---

### 4. ĐÁNH GIÁ VÀ PHẢN HỒI

#### UC-C17: Rate Event (Đánh Giá Sự Kiện)
- **Actors**: Customer
- **Mô tả**: Đánh giá sự kiện sau khi tham dự
- **Preconditions**: Customer đã tham dự sự kiện
- **Main Flow**:
  1. Customer chọn sự kiện đã tham dự
  2. Customer đánh giá (sao, nhận xét)
  3. Customer có thể điền form phản hồi
  4. Hệ thống lưu đánh giá
- **Extends**: UC-C18: Submit Feedback Form

#### UC-C18: Submit Feedback Form (Gửi Form Phản Hồi)
- **Actors**: Customer
- **Mô tả**: Điền và gửi form phản hồi về sự kiện
- **Extends**: UC-C17: Rate Event

---

### 5. TÌNH NGUYỆN VIÊN

#### UC-C19: Apply for Volunteer (Đăng Ký Làm Tình Nguyện Viên)
- **Actors**: Customer
- **Mô tả**: Đăng ký làm tình nguyện viên cho sự kiện
- **Preconditions**: Sự kiện có form đăng ký volunteer
- **Main Flow**:
  1. Customer chọn sự kiện
  2. Customer truy cập form đăng ký volunteer
  3. Customer điền thông tin
  4. Customer gửi đơn đăng ký
- **Extends**: UC-C20: Submit Volunteer Form

#### UC-C20: Submit Volunteer Form (Gửi Form Tình Nguyện Viên)
- **Actors**: Customer
- **Mô tả**: Điền và gửi form đăng ký tình nguyện viên
- **Extends**: UC-C19: Apply for Volunteer

#### UC-C21: View My Requests (Xem Yêu Cầu Của Tôi)
- **Actors**: Customer
- **Mô tả**: Xem các yêu cầu đã gửi (volunteer applications, feedback forms)
- **Main Flow**:
  1. Customer truy cập trang "Yêu cầu của tôi"
  2. Hệ thống hiển thị danh sách yêu cầu
  3. Customer có thể xem trạng thái từng yêu cầu

---

### 6. ĐIỂM THƯỞNG VÀ XẾP HẠNG

#### UC-C22: Earn Points After Event (Nhận Điểm Sau Sự Kiện)
- **Actors**: Customer
- **Mô tả**: Nhận điểm thưởng sau khi tham dự sự kiện
- **Preconditions**: Customer đã check-in và tham dự sự kiện
- **Main Flow**:
  1. Customer tham dự sự kiện
  2. Hệ thống tự động cộng điểm
  3. Customer nhận thông báo về điểm thưởng

#### UC-C23: View Leaderboard (Xem Bảng Xếp Hạng)
- **Actors**: Customer
- **Mô tả**: Xem bảng xếp hạng điểm của người dùng
- **Main Flow**:
  1. Customer truy cập trang leaderboard
  2. Hệ thống hiển thị bảng xếp hạng
  3. Customer có thể xem vị trí của mình

---

### 7. VÍ ĐIỆN TỬ

#### UC-C24: Manage Wallet (Quản Lý Ví Điện Tử)
- **Actors**: Customer
- **Mô tả**: Quản lý ví điện tử của Customer
- **Preconditions**: Customer đã đăng nhập
- **Main Flow**:
  1. Customer truy cập trang ví
  2. Customer có thể xem số dư, lịch sử giao dịch
  3. Customer có thể nạp tiền, rút tiền
- **Extends**:
  - UC-C25: View Wallet Balance
  - UC-C26: Complete KYC
  - UC-C27: View Transaction History

#### UC-C25: View Wallet Balance (Xem Số Dư Ví)
- **Actors**: Customer
- **Mô tả**: Xem số dư hiện tại trong ví
- **Extends**: UC-C24: Manage Wallet

#### UC-C26: Complete KYC (Hoàn Thành Xác Minh Danh Tính)
- **Actors**: Customer
- **Mô tả**: Hoàn thành quy trình KYC để sử dụng ví
- **Extends**: UC-C24: Manage Wallet
- **Main Flow**:
  1. Customer điền thông tin KYC
  2. Customer upload giấy tờ
  3. Hệ thống xác minh
  4. KYC được duyệt hoặc từ chối

#### UC-C27: View Transaction History (Xem Lịch Sử Giao Dịch)
- **Actors**: Customer
- **Mô tả**: Xem lịch sử các giao dịch trong ví
- **Extends**: UC-C24: Manage Wallet

---

### 8. TƯƠNG TÁC VỚI AI

#### UC-C28: Chat with AI Agent (Trò Chuyện Với AI)
- **Actors**: Customer, AI Agent
- **Mô tả**: Trò chuyện với AI Agent để được hỗ trợ
- **Preconditions**: Customer đã đăng nhập
- **Main Flow**:
  1. Customer mở chat với AI
  2. Customer đặt câu hỏi hoặc yêu cầu
  3. AI Agent trả lời và hỗ trợ
  4. Customer có thể tiếp tục trò chuyện
- **Extends**:
  - UC-C29: Get Event Suggestions
  - UC-C30: Buy Ticket via Chat
  - UC-C31: Multi-language Support

#### UC-C29: Get Event Suggestions (Nhận Gợi Ý Sự Kiện)
- **Actors**: Customer, AI Agent
- **Mô tả**: Nhận gợi ý sự kiện từ AI dựa trên sở thích
- **Extends**: UC-C28: Chat with AI Agent
- **Main Flow**:
  1. Customer yêu cầu gợi ý sự kiện
  2. AI Agent phân tích sở thích
  3. AI Agent đề xuất các sự kiện phù hợp

#### UC-C30: Buy Ticket via Chat (Mua Vé Qua Chat)
- **Actors**: Customer, AI Agent
- **Mô tả**: Mua vé trực tiếp qua chat với AI
- **Extends**: UC-C28: Chat with AI Agent
- **Includes**: UC-C9: Process Ticket Payment
- **Main Flow**:
  1. Customer yêu cầu mua vé qua chat
  2. AI Agent hỗ trợ chọn sự kiện và vé
  3. AI Agent hướng dẫn thanh toán
  4. Thanh toán được xử lý

#### UC-C31: Multi-language Support (Hỗ Trợ Đa Ngôn Ngữ)
- **Actors**: Customer, AI Agent
- **Mô tả**: Chat với AI bằng nhiều ngôn ngữ
- **Extends**: UC-C28: Chat with AI Agent

---

## Sơ Đồ Use Case (Text Representation)

```
┌─────────────────────────────────────────────────────────────┐
│              CUSTOMER/GUEST USE CASES                       │
└─────────────────────────────────────────────────────────────┘

[Customer] ───────────────────────────────────────────────────
[Guest] (inherits from Customer, limited access)

XEM VÀ TÌM KIẾM SỰ KIỆN:
  ├── View List Event
  │   ├── View Event Details (extends)
  │   ├── Search Events (extends)
  │   └── Filter Events (extends)
  └── View Event Details (also extends Search Events)

QUẢN LÝ VÉ:
  ├── View Purchased Tickets
  │   ├── View Ticket Types (extends)
  │   └── View Ticket Details (extends)
  ├── Buy Ticket
  │   ├── View Ticket Types (extends)
  │   ├── Apply Voucher (extends)
  │   ├── Process Ticket Payment (includes) → [Payment Gateway]
  │   ├── Confirm Ticket Purchase (includes)
  │   └── Ticket Payment Failed (extends)
  └── Cancel Ticket

THAM GIA SỰ KIỆN:
  ├── Check-in Event
  │   └── Submit Check-in Form (extends)
  └── Check-out Event

ĐÁNH GIÁ VÀ PHẢN HỒI:
  └── Rate Event
      └── Submit Feedback Form (extends)

TÌNH NGUYỆN VIÊN:
  ├── Apply for Volunteer
  │   └── Submit Volunteer Form (extends)
  └── View My Requests

ĐIỂM THƯỞNG:
  ├── Earn Points After Event
  └── View Leaderboard

VÍ ĐIỆN TỬ:
  └── Manage Wallet
      ├── View Wallet Balance (extends)
      ├── Complete KYC (extends)
      └── View Transaction History (extends)

TƯƠNG TÁC VỚI AI:
  └── Chat with AI Agent
      ├── Get Event Suggestions (extends) → [AI Agent]
      ├── Buy Ticket via Chat (extends) → [AI Agent]
      └── Multi-language Support (extends) → [AI Agent]
```

---

## Quan Hệ Giữa Use Cases

### Generalization (Kế Thừa)
- Guest → Customer (Guest có quyền hạn hạn chế hơn)

### Include (Bắt Buộc)
- Buy Ticket → Process Ticket Payment
- Buy Ticket → Confirm Ticket Purchase
- Buy Ticket via Chat → Process Ticket Payment

### Extend (Tùy Chọn)
- View List Event → View Event Details
- View List Event → Search Events
- View List Event → Filter Events
- Search Events → View Event Details
- Buy Ticket → View Ticket Types
- Buy Ticket → Apply Voucher
- Buy Ticket → Ticket Payment Failed
- Process Ticket Payment → Ticket Payment Failed
- View Purchased Tickets → View Ticket Types
- View Ticket Types → View Ticket Details
- Check-in Event → Submit Check-in Form
- Check-out Event → Check-in Event
- Rate Event → Submit Feedback Form
- Apply for Volunteer → Submit Volunteer Form
- Manage Wallet → View Wallet Balance
- Manage Wallet → Complete KYC
- Manage Wallet → View Transaction History
- Chat with AI Agent → Get Event Suggestions
- Chat with AI Agent → Buy Ticket via Chat
- Chat with AI Agent → Multi-language Support

---

## So Sánh Với Use Case Cũ

### Các Cải Tiến:

1. **Bổ Sung Use Cases**:
   - ✅ Filter Events
   - ✅ Apply Voucher
   - ✅ View My Requests
   - ✅ Manage Wallet (và các use case con)
   - ✅ Complete KYC
   - ✅ View Transaction History
   - ✅ Multi-language Support

2. **Sửa Quan Hệ**:
   - ✅ Sửa quan hệ include/extend cho đúng
   - ✅ Bổ sung quan hệ giữa View Event Details và Search Events
   - ✅ Sửa quan hệ Process Ticket Payment và Confirm Ticket Purchase

3. **Bổ Sung Guest Role**:
   - ✅ Thêm Guest như một actor riêng với quyền hạn hạn chế

---

## Ghi Chú

- **Guest** chỉ có thể xem sự kiện, không thể mua vé hoặc thực hiện các hành động khác
- **Customer** có đầy đủ quyền sau khi đăng nhập
- Tất cả use cases liên quan đến mua vé, thanh toán, quản lý ví đều yêu cầu Customer đã đăng nhập


