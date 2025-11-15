# Hướng dẫn cấu hình Facebook OAuth2 Login

## Tổng quan

Ứng dụng OpenEvent đã được tích hợp đăng nhập bằng Facebook OAuth2. Tài liệu này hướng dẫn cách cấu hình và sử dụng tính năng này.

## Các bước cấu hình

### 1. Tạo Facebook App

1. Truy cập [Facebook Developers](https://developers.facebook.com/)
2. Đăng nhập bằng tài khoản Facebook của bạn
3. Click vào **My Apps** > **Create App**
4. Chọn loại app: **Consumer** hoặc **Business**
5. Điền thông tin:
   - **App Name**: Tên ứng dụng (ví dụ: "OpenEvent")
   - **App Contact Email**: Email liên hệ
   - **Business Account** (tùy chọn): Chọn nếu có
6. Click **Create App**

### 2. Thêm Facebook Login Product

1. Trong Facebook App Dashboard, vào **Add Product**
2. Tìm và click **Set Up** cho **Facebook Login**
3. Chọn **Web** làm platform
4. Cấu hình **Settings**:
   - **Site URL**: `http://localhost:8080` (cho development)
   - **Valid OAuth Redirect URIs**: 
     - `http://localhost:8080/login/oauth2/code/facebook` (cho development)
     - `https://yourdomain.com/login/oauth2/code/facebook` (cho production)

### 3. Lấy App ID và App Secret

1. Vào **Settings** > **Basic** trong Facebook App Dashboard
2. Lưu **App ID** và **App Secret**
3. **Lưu ý**: App Secret cần được bảo mật, không chia sẻ công khai

### 4. Cấu hình Permissions (Quyền)

1. Vào **Products** > **Facebook Login** > **Settings**
2. Trong **User & Friend Permissions**, thêm:
   - `email` - Để lấy địa chỉ email của user
   - `public_profile` - Để lấy thông tin công khai (name, picture)
3. **Lưu ý**: Facebook có thể không trả về email nếu user không cho phép. Hệ thống sẽ tự động tạo email tạm thời từ Facebook ID.

### 5. Cấu hình Environment Variables

Thêm các biến môi trường sau vào file `.env` hoặc cấu hình environment:

```bash
FACEBOOK_OAUTH_CLIENT_ID=your-app-id-here
FACEBOOK_OAUTH_CLIENT_SECRET=your-app-secret-here
```

Hoặc thêm vào file `setup-env.ps1` hoặc `setup-env.bat`:

```powershell
$env:FACEBOOK_OAUTH_CLIENT_ID="your-app-id-here"
$env:FACEBOOK_OAUTH_CLIENT_SECRET="your-app-secret-here"
```

### 6. Cập nhật Database Schema

Database schema đã được cập nhật trong `add_oauth_support.sql`. Nếu chưa chạy, chạy script:

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

### 7. Khởi động ứng dụng

1. Đảm bảo đã cài đặt dependency OAuth2 Client (đã được thêm vào `pom.xml`)
2. Khởi động ứng dụng:
   ```bash
   mvn spring-boot:run
   ```
3. Truy cập trang đăng nhập: `http://localhost:8080/login`
4. Click vào biểu tượng Facebook để đăng nhập

## Cách hoạt động

### Flow đăng nhập bằng Facebook

1. User click vào nút Facebook trên trang đăng nhập
2. User được chuyển hướng đến Facebook để xác thực
3. User cho phép ứng dụng truy cập thông tin (email, public profile)
4. Sau khi xác thực thành công, Facebook redirect về ứng dụng với authorization code
5. Spring Security exchange code lấy access token
6. `CustomOAuth2UserService` được gọi để lấy thông tin user từ Facebook Graph API
7. Hệ thống kiểm tra:
   - Nếu email chưa tồn tại: Tạo Account, User và Customer mới
   - Nếu email đã tồn tại: Cập nhật thông tin OAuth (nếu chưa có)
   - Nếu không có email: Tạo email tạm từ Facebook ID
8. Lưu AccountId và Role vào session
9. Chuyển hướng user đến trang chủ hoặc URL đã lưu

### Xử lý tài khoản OAuth

- **Tài khoản mới**: Tự động tạo Account với `password_hash = NULL`, `oauth_provider = "FACEBOOK"`, và `oauth_provider_id = <Facebook User ID>`
- **Tài khoản đã tồn tại**: Nếu user đã đăng ký bằng form trước đó, hệ thống sẽ liên kết OAuth info với account hiện có
- **Role mặc định**: User đăng nhập bằng Facebook sẽ có role **CUSTOMER** mặc định
- **Email**: 
  - Nếu Facebook trả về email: Sử dụng email đó
  - Nếu không có email: Tạo email tạm thời dạng `{facebook_id}@facebook.oauth`

### Xử lý dữ liệu từ Facebook

Facebook OAuth cung cấp:
- ✅ **ID**: Facebook User ID (luôn có)
- ✅ **Name**: Tên đầy đủ (luôn có)
- ✅ **Email**: Địa chỉ email (có thể null nếu user không cho phép)
- ✅ **Picture**: URL ảnh đại diện (nested object: `picture.data.url`)
- ❌ **Phone Number**: Facebook OAuth không cung cấp số điện thoại

**Lưu ý về số điện thoại**: 
- Facebook OAuth không trả về số điện thoại
- Số điện thoại sẽ được để `NULL` trong database
- Có thể yêu cầu user nhập số điện thoại sau khi đăng nhập lần đầu (tính năng tùy chọn)

## Lưu ý

1. **Password**: User đăng nhập bằng Facebook không có password. Nếu muốn đăng nhập bằng form, cần thêm tính năng "set password" hoặc "link account"

2. **Email**: 
   - Email từ Facebook được sử dụng làm username duy nhất (nếu có)
   - Nếu không có email, hệ thống tự động tạo email tạm từ Facebook ID

3. **Avatar**: Avatar từ Facebook được lưu vào trường `avatar` của User

4. **Name**: Tên từ Facebook được lưu vào trường `name` của User

5. **App Review**: 
   - Trong môi trường development, chỉ có thể test với tài khoản Facebook của chính bạn (admin/developer của app)
   - Để app hoạt động với tất cả user, cần submit app để Facebook review (cho production)

## Troubleshooting

### Lỗi "Invalid OAuth access token"

- Kiểm tra `FACEBOOK_OAUTH_CLIENT_ID` và `FACEBOOK_OAUTH_CLIENT_SECRET` trong environment variables
- Đảm bảo App ID và App Secret đúng

### Lỗi "Redirect URI mismatch"

- Kiểm tra redirect URI trong Facebook App Settings
- Đảm bảo URI khớp chính xác: `http://localhost:8080/login/oauth2/code/facebook`
- Lưu ý: Facebook yêu cầu URI phải khớp chính xác, kể cả `http` vs `https`

### Lỗi "Email permission not granted"

- User có thể từ chối cấp quyền email
- Hệ thống sẽ tự động tạo email tạm thời từ Facebook ID
- Log sẽ hiển thị warning: "Email not provided by facebook OAuth, using generated identifier"

### User không được tạo trong database

- Kiểm tra logs để xem có lỗi gì không
- Đảm bảo database schema đã được cập nhật
- Kiểm tra quyền truy cập database

### Session không lưu AccountId

- Kiểm tra `CustomAuthenticationSuccessHandler` có được gọi không
- Kiểm tra logs để xem có lỗi trong quá trình xử lý OAuth2 user không

### App chỉ hoạt động với tài khoản của bạn

- Đây là hành vi bình thường trong môi trường development
- Chỉ admin/developer của Facebook App mới có thể đăng nhập
- Để app hoạt động với tất cả user, cần:
  1. Submit app để Facebook review
  2. Được Facebook approve
  3. App phải ở chế độ "Live" (không phải "Development")

## So sánh Google vs Facebook OAuth

| Tính năng | Google OAuth | Facebook OAuth |
|----------|--------------|---------------|
| Email | ✅ Luôn có | ⚠️ Có thể null |
| Name | ✅ Luôn có | ✅ Luôn có |
| Picture | ✅ URL string | ✅ Nested object |
| Phone | ❌ Không có | ❌ Không có |
| ID Format | `sub` | `id` |
| User Info URI | `/oauth2/v3/userinfo` | `/v18.0/me?fields=...` |

## Mở rộng

### Thêm tính năng "Link Account"

Cho phép user liên kết tài khoản Facebook với tài khoản hiện có bằng cách:
1. Yêu cầu user đăng nhập bằng form trước
2. Sau đó cho phép link với Facebook account
3. Cập nhật `oauth_provider` và `oauth_provider_id` vào account hiện có

### Yêu cầu số điện thoại sau đăng nhập

Nếu cần số điện thoại, có thể:
1. Sau khi đăng nhập bằng Facebook lần đầu
2. Hiển thị form yêu cầu user nhập số điện thoại
3. Cập nhật vào bảng `user.phone_number`

## Tài liệu tham khảo

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Facebook Login Documentation](https://developers.facebook.com/docs/facebook-login/)
- [Facebook Graph API](https://developers.facebook.com/docs/graph-api)
- [Facebook App Review](https://developers.facebook.com/docs/app-review)

