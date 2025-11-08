# Hướng dẫn cấu hình Google OAuth2 Login

## Tổng quan

Ứng dụng OpenEvent đã được tích hợp đăng nhập bằng Google OAuth2. Tài liệu này hướng dẫn cách cấu hình và sử dụng tính năng này.

## Các bước cấu hình

### 1. Tạo Google OAuth2 Credentials

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Tạo project mới hoặc chọn project hiện có
3. Bật **Google+ API**:
   - Vào **APIs & Services** > **Library**
   - Tìm "Google+ API" và bật
4. Tạo OAuth 2.0 Client ID:
   - Vào **APIs & Services** > **Credentials**
   - Click **Create Credentials** > **OAuth client ID**
   - Chọn **Web application**
   - Đặt tên cho client (ví dụ: "OpenEvent OAuth Client")
   - Thêm **Authorized redirect URIs**:
     - `http://localhost:8080/login/oauth2/code/google` (cho development)
     - `https://yourdomain.com/login/oauth2/code/google` (cho production)
   - Lưu **Client ID** và **Client Secret**

### 2. Cấu hình Environment Variables

Thêm các biến môi trường sau vào file `.env` hoặc cấu hình environment:

```bash
GOOGLE_OAUTH_CLIENT_ID=your-client-id-here
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret-here
```

### 3. Cập nhật Database Schema

Chạy migration script để cập nhật database:

```bash
mysql -u root -p openevent < add_oauth_support.sql
```

Hoặc chạy trực tiếp trong MySQL:

```sql
USE openevent;

-- Cập nhật cột password_hash để cho phép NULL (cho OAuth users)
ALTER TABLE account 
MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- Thêm cột oauth_provider
ALTER TABLE account 
ADD COLUMN oauth_provider VARCHAR(50) NULL;

-- Thêm cột oauth_provider_id
ALTER TABLE account 
ADD COLUMN oauth_provider_id VARCHAR(255) NULL;

-- Tạo index
CREATE INDEX idx_account_oauth_provider_id ON account(oauth_provider_id);
CREATE INDEX idx_account_oauth_provider ON account(oauth_provider, oauth_provider_id);
```

### 4. Khởi động ứng dụng

1. Đảm bảo đã cài đặt dependency OAuth2 Client (đã được thêm vào `pom.xml`)
2. Khởi động ứng dụng:
   ```bash
   mvn spring-boot:run
   ```
3. Truy cập trang đăng nhập: `http://localhost:8080/login`
4. Click vào biểu tượng Google để đăng nhập

## Cách hoạt động

### Flow đăng nhập bằng Google

1. User click vào nút Google trên trang đăng nhập
2. User được chuyển hướng đến Google để xác thực
3. Sau khi xác thực thành công, Google redirect về ứng dụng với authorization code
4. Spring Security exchange code lấy access token
5. `CustomOAuth2UserService` được gọi để lấy thông tin user từ Google
6. Hệ thống kiểm tra:
   - Nếu email chưa tồn tại: Tạo Account, User và Customer mới
   - Nếu email đã tồn tại: Cập nhật thông tin OAuth (nếu chưa có)
7. Lưu AccountId và Role vào session
8. Chuyển hướng user đến trang chủ hoặc URL đã lưu

### Xử lý tài khoản OAuth

- **Tài khoản mới**: Tự động tạo Account với `password_hash = NULL`, `oauth_provider = "GOOGLE"`, và `oauth_provider_id = <Google User ID>`
- **Tài khoản đã tồn tại**: Nếu user đã đăng ký bằng form trước đó, hệ thống sẽ liên kết OAuth info với account hiện có
- **Role mặc định**: User đăng nhập bằng Google sẽ có role **CUSTOMER** mặc định

## Lưu ý

1. **Password**: User đăng nhập bằng Google không có password. Nếu muốn đăng nhập bằng form, cần thêm tính năng "set password" hoặc "link account"
2. **Email**: Email từ Google được sử dụng làm username duy nhất
3. **Avatar**: Avatar từ Google được lưu vào trường `avatar` của User
4. **Name**: Tên từ Google được lưu vào trường `name` của User

## Troubleshooting

### Lỗi "Invalid client_id"

- Kiểm tra `GOOGLE_OAUTH_CLIENT_ID` trong environment variables
- Đảm bảo redirect URI trong Google Console khớp với URL của ứng dụng

### Lỗi "Redirect URI mismatch"

- Kiểm tra redirect URI trong Google Console
- Đảm bảo URI khớp chính xác: `http://localhost:8080/login/oauth2/code/google`

### User không được tạo trong database

- Kiểm tra logs để xem có lỗi gì không
- Đảm bảo database schema đã được cập nhật
- Kiểm tra quyền truy cập database

### Session không lưu AccountId

- Kiểm tra `CustomAuthenticationSuccessHandler` có được gọi không
- Kiểm tra logs để xem có lỗi trong quá trình xử lý OAuth2 user không

## Mở rộng

### Thêm Facebook OAuth

Để thêm Facebook OAuth, cần:

1. Thêm Facebook OAuth configuration vào `application.properties`
2. Tạo `CustomOAuth2UserService` riêng cho Facebook hoặc cập nhật service hiện tại
3. Thêm nút Facebook vào `login.html`

### Thêm tính năng "Link Account"

Cho phép user liên kết tài khoản Google với tài khoản hiện có bằng cách:
1. Yêu cầu user đăng nhập bằng form trước
2. Sau đó cho phép link với Google account
3. Cập nhật `oauth_provider` và `oauth_provider_id` vào account hiện có

## Tài liệu tham khảo

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Google Cloud Console](https://console.cloud.google.com/)

