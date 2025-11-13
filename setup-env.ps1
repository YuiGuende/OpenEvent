# Script để tạo file .env từ .env.example
# Chạy script này: .\setup-env.ps1

Write-Host "=== Setup Google OAuth2 Environment ===" -ForegroundColor Green
Write-Host ""

# Kiểm tra file .env đã tồn tại chưa
if (Test-Path ".env") {
    Write-Host "File .env đã tồn tại!" -ForegroundColor Yellow
    $overwrite = Read-Host "Bạn có muốn ghi đè không? (y/n)"
    if ($overwrite -ne "y") {
        Write-Host "Hủy bỏ." -ForegroundColor Red
        exit
    }
}

# Tạo file .env từ .env.example
if (Test-Path ".env.example") {
    Copy-Item ".env.example" ".env"
    Write-Host "Đã tạo file .env từ .env.example" -ForegroundColor Green
} else {
    Write-Host "Tạo file .env mới..." -ForegroundColor Yellow
    @"
# Google OAuth2 Configuration
GOOGLE_OAUTH_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret-here
"@ | Out-File -FilePath ".env" -Encoding UTF8
    Write-Host "Đã tạo file .env" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Hướng dẫn tiếp theo ===" -ForegroundColor Cyan
Write-Host "1. Mở file .env và điền Google OAuth credentials của bạn"
Write-Host "2. Lấy credentials từ: https://console.cloud.google.com/apis/credentials"
Write-Host "3. Chạy migration SQL: mysql -u root -p openevent < add_oauth_support.sql"
Write-Host "4. Khởi động ứng dụng: mvn spring-boot:run"
Write-Host ""






