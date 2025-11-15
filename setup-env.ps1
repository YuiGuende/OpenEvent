# Script để tạo file .env từ .env.example
# Chạy script này: .\setup-env.ps1

Write-Host "=== Setup OAuth2 Environment (Google & Facebook) ===" -ForegroundColor Green
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

# Facebook OAuth2 Configuration
FACEBOOK_OAUTH_CLIENT_ID=your-facebook-app-id-here
FACEBOOK_OAUTH_CLIENT_SECRET=your-facebook-app-secret-here
"@ | Out-File -FilePath ".env" -Encoding UTF8
    Write-Host "Đã tạo file .env" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Hướng dẫn tiếp theo ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Google OAuth:" -ForegroundColor Yellow
Write-Host "   - Mở file .env và điền Google OAuth credentials"
Write-Host "   - Lấy từ: https://console.cloud.google.com/apis/credentials"
Write-Host ""
Write-Host "2. Facebook OAuth:" -ForegroundColor Yellow
Write-Host "   - Mở file .env và điền Facebook OAuth credentials"
Write-Host "   - Lấy từ: https://developers.facebook.com/apps/"
Write-Host "   - Vào Settings > Basic để lấy App ID và App Secret"
Write-Host ""
Write-Host "3. Chạy migration SQL:" -ForegroundColor Yellow
Write-Host "   mysql -u root -p openevent < add_oauth_support.sql"
Write-Host ""
Write-Host "4. Khởi động ứng dụng:" -ForegroundColor Yellow
Write-Host "   mvn spring-boot:run"
Write-Host ""






