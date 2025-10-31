# 📧 Hướng dẫn Cấu hình Email Reminder với AI

## 🎯 Tổng quan

Hệ thống AI Email Reminder cho phép:
- Người dùng yêu cầu AI nhắc nhở về sự kiện qua email
- AI lưu yêu cầu nhắc nhở vào database
- Scheduler tự động kiểm tra và gửi email đúng thời điểm

---

## 📝 Các bước Setup

### 1. Cấu hình Gmail App Password

#### a. Bật 2-Factor Authentication trên Gmail:
1. Truy cập: https://myaccount.google.com/security
2. Bật "2-Step Verification"

#### b. Tạo App Password:
1. Truy cập: https://myaccount.google.com/apppasswords
2. Chọn "Mail" và "Other (Custom name)"
3. Nhập tên: "OpenEvent Email Service"
4. Copy mã 16 ký tự được tạo

### 2. Cập nhật .env file

Thêm vào file `.env` (hoặc tạo mới nếu chưa có):

```properties
# Email Configuration
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-16-digit-app-password
```

**Ví dụ:**
```properties
EMAIL_USERNAME=openevent.system@gmail.com
EMAIL_PASSWORD=abcd efgh ijkl mnop
```

### 3. Verify cấu hình

File `application.properties` đã được cấu hình:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
```

---

## 🚀 Cách sử dụng

### 1. Qua AI Chat:

**Ví dụ câu lệnh:**
```
"Nhắc tôi về sự kiện Đại hội sinh viên trước 30 phút"
"Gửi email nhắc tôi về event Workshop AI trước 1 giờ"
"Reminder sự kiện Music Festival trước 15 phút"
```

### 2. AI xử lý:

AI sẽ:
1. Phân tích yêu cầu
2. Tìm sự kiện phù hợp
3. Lưu EmailReminder vào database với:
   - `event_id`: ID sự kiện
   - `user_id`: ID người dùng
   - `remind_minutes`: Số phút nhắc trước (ví dụ: 30)
   - `is_sent`: false (chưa gửi)

### 3. Scheduler tự động gửi:

**EmailReminderScheduler** chạy mỗi 5 phút:
```java
@Scheduled(fixedRate = 300000) // 5 minutes
public void checkAndSendReminders()
```

Khi đến thời điểm nhắc:
- Lấy tất cả reminders chưa gửi (`is_sent = false`)
- Kiểm tra thời gian: `event.startsAt - remindMinutes <= now`
- Gửi email HTML đẹp mắt
- Đánh dấu `is_sent = true`

---

## 📧 Email Template

Email được gửi có format HTML chuyên nghiệp với:
- 🔔 Header gradient đẹp mắt
- 📅 Thông tin chi tiết sự kiện
- 🕐 Thời gian bắt đầu/kết thúc
- 📝 Mô tả sự kiện
- 🔗 Link đến trang chi tiết
- 💡 Lưu ý và footer

---

## ⚙️ Cấu hình Scheduler

### Gửi Email Reminders:
- **Tần suất:** Mỗi 5 phút (300,000 ms)
- **Thời gian:** 24/7 tự động

### Cleanup Old Reminders:
- **Tần suất:** Mỗi ngày lúc 2:00 AM
- **Xóa:** Reminders đã gửi quá 30 ngày

---

## 🧪 Testing

### Test gửi email đơn giản:

Tạo controller test:
```java
@RestController
@RequestMapping("/api/test")
public class EmailTestController {
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/send-email")
    public String testEmail() {
        emailService.sendSimpleEmail(
            "recipient@example.com",
            "Test Email",
            "This is a test email from OpenEvent"
        );
        return "Email sent!";
    }
}
```

### Test qua AI:

1. Đăng nhập vào hệ thống
2. Tạo một sự kiện với thời gian bắt đầu trong vài phút tới
3. Chat với AI: "Nhắc tôi về [tên sự kiện] trước 5 phút"
4. Đợi 5 phút và kiểm tra email

---

## 🔍 Troubleshooting

### Email không gửi?

**1. Kiểm tra logs:**
```bash
# Tìm "email" hoặc "reminder" trong logs
grep -i "email\|reminder" logs/application.log
```

**2. Kiểm tra Gmail App Password:**
- Đảm bảo 2FA đã bật
- App Password đúng format (16 ký tự)
- Không có khoảng trắng thừa

**3. Kiểm tra firewall:**
```bash
# Kiểm tra có connect được SMTP không
telnet smtp.gmail.com 587
```

**4. Kiểm tra database:**
```sql
-- Xem reminders chưa gửi
SELECT * FROM email_reminder WHERE is_sent = false;

-- Xem reminders đã gửi
SELECT * FROM email_reminder WHERE is_sent = true;
```

### Lỗi "Authentication failed"?

- Kiểm tra EMAIL_USERNAME có đúng không
- Kiểm tra EMAIL_PASSWORD là App Password (không phải password thường)
- Gmail có thể block "Less secure apps" - dùng App Password để tránh

### Scheduler không chạy?

Kiểm tra `@EnableScheduling` trong `OpenEventApplication.java`:
```java
@SpringBootApplication
@EnableScheduling  // <-- Phải có dòng này
public class OpenEventApplication {
    // ...
}
```

---

## 📊 Database Schema

```sql
CREATE TABLE email_reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    remind_minutes INT NOT NULL,
    created_at DATETIME NOT NULL,
    is_sent BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (event_id) REFERENCES event(id)
);
```

---

## ✅ Checklist

- [ ] Tạo Gmail App Password
- [ ] Cập nhật .env với EMAIL_USERNAME và EMAIL_PASSWORD
- [ ] Verify application.properties có cấu hình email
- [ ] Verify @EnableScheduling đã bật
- [ ] Test gửi email đơn giản
- [ ] Test qua AI chat
- [ ] Kiểm tra logs
- [ ] Kiểm tra email inbox

---

## 🎉 Kết luận

Sau khi setup xong, hệ thống sẽ:
1. ✅ Nhận yêu cầu nhắc nhở từ AI
2. ✅ Lưu vào database
3. ✅ Tự động gửi email đúng thời điểm
4. ✅ Tự động cleanup reminders cũ

**Happy coding! 🚀**



































