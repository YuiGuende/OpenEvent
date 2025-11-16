package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Implementation of Email Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void sendEventReminderEmail(String userEmail, Event event, int minutesBefore) {
        // Validate email format
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (!isValidEmailFormat(userEmail)) {
            throw new IllegalArgumentException("Invalid email format: " + userEmail);
        }
        
        // Validate event
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        if (event.getStartsAt() == null || event.getEndsAt() == null) {
            throw new IllegalArgumentException("Event start/end time cannot be null");
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(userEmail);
            helper.setSubject("🔔 Nhắc nhở: Sự kiện \"" + event.getTitle() + "\" sắp diễn ra!");
            
            String content = buildReminderEmailContent(event, minutesBefore);
            helper.setText(content, true); // true = HTML
            
            mailSender.send(message);
            log.info("✅ Sent reminder email to {} for event: {}", userEmail, event.getTitle());
            
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Basic email format validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        // Validate email format
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (!isValidEmailFormat(to)) {
            throw new IllegalArgumentException("Invalid email format: " + to);
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("✅ Sent email to {}: {}", to, subject);
            
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Build HTML email content for event reminder
     */
    private String buildReminderEmailContent(Event event, int minutesBefore) {
        // Validate event and times
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        if (event.getStartsAt() == null || event.getEndsAt() == null) {
            throw new IllegalArgumentException("Event start/end time cannot be null");
        }
        
        String startTime = event.getStartsAt().format(DATE_FORMATTER);
        String endTime = event.getEndsAt().format(DATE_FORMATTER);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                              color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .event-details { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .detail-row { margin: 10px 0; padding: 10px; border-left: 3px solid #667eea; }
                    .label { font-weight: bold; color: #667eea; }
                    .button { background: #667eea; color: white; padding: 12px 30px; 
                             text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                    .footer { text-align: center; margin-top: 20px; color: #888; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔔 Nhắc nhở sự kiện</h1>
                        <p>Sự kiện của bạn sắp bắt đầu trong %d phút!</p>
                    </div>
                    <div class="content">
                        <div class="event-details">
                            <h2 style="color: #667eea; margin-top: 0;">📅 %s</h2>
                            
                            <div class="detail-row">
                                <span class="label">🕐 Thời gian bắt đầu:</span><br>
                                %s
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">🕐 Thời gian kết thúc:</span><br>
                                %s
                            </div>
                            
                            <div class="detail-row">
                                <span class="label">📝 Mô tả:</span><br>
                                %s
                            </div>
                        </div>
                        
                        <p style="text-align: center;">
                            <a href="http://localhost:8080/events/%d" class="button">
                                Xem chi tiết sự kiện
                            </a>
                        </p>
                        
                        <p style="margin-top: 20px; padding: 15px; background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;">
                            <strong>💡 Lưu ý:</strong> Hãy chuẩn bị sẵn sàng để tham gia sự kiện!
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p>Email này được gửi tự động từ OpenEvent</p>
                        <p>© 2024 OpenEvent - Event Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                minutesBefore,
                event.getTitle(),
                startTime,
                endTime,
                event.getDescription() != null ? event.getDescription() : "Không có mô tả",
                event.getId()
            );
    }
}


