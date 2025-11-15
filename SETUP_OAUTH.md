# Hướng dẫn cấu hình Google OAuth2

## Bước 1: Tạo file .env

1. Copy file `.env.example` thành `.env`:
   ```bash
   copy .env.example .env
   ```
   Hoặc trên PowerShell:
   ```powershell
   Copy-Item .env.example .env
   ```

2. Mở file `.env` và điền credentials của bạn:
   ```
   GOOGLE_OAUTH_CLIENT_ID=your-actual-client-id.apps.googleusercontent.com
   GOOGLE_OAUTH_CLIENT_SECRET=your-actual-client-secret
   ```

## Bước 2: Lấy credentials từ Google Cloud Console

1. Truy cập: https://console.cloud.google.com/apis/credentials
2. Click vào OAuth Client ID bạn vừa tạo
3. Copy **Your Client ID** và **Your Client Secret**
4. Dán vào file `.env`

## Bước 3: Chạy migration SQL

```bash
mysql -u root -p openevent < add_oauth_support.sql
```

## Bước 4: Khởi động ứng dụng

```bash
mvn spring-boot:run
```

## Bước 5: Test

1. Truy cập: http://localhost:8080/login
2. Click vào biểu tượng Google
3. Đăng nhập và kiểm tra

## Lưu ý

- File `.env` đã được thêm vào `.gitignore`, không lo commit nhầm credentials
- Không chia sẻ file `.env` với ai
- Nếu gặp lỗi "Client id must not be empty", kiểm tra lại file `.env`






