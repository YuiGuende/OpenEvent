@echo off
REM Script để tạo file .env từ .env.example
REM Chạy script này: setup-env.bat

echo === Setup Google OAuth2 Environment ===
echo.

REM Kiểm tra file .env đã tồn tại chưa
if exist .env (
    echo File .env đã tồn tại!
    set /p overwrite="Bạn có muốn ghi đè không? (y/n): "
    if /i not "%overwrite%"=="y" (
        echo Hủy bỏ.
        exit /b
    )
)

REM Tạo file .env từ .env.example
if exist .env.example (
    copy .env.example .env >nul
    echo Đã tạo file .env từ .env.example
) else (
    echo Tạo file .env mới...
    (
        echo # Google OAuth2 Configuration
        echo GOOGLE_OAUTH_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
        echo GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret-here
    ) > .env
    echo Đã tạo file .env
)

echo.
echo === Hướng dẫn tiếp theo ===
echo 1. Mở file .env và điền Google OAuth credentials của bạn
echo 2. Lấy credentials từ: https://console.cloud.google.com/apis/credentials
echo 3. Chạy migration SQL: mysql -u root -p openevent ^< add_oauth_support.sql
echo 4. Khởi động ứng dụng: mvn spring-boot:run
echo.

pause






