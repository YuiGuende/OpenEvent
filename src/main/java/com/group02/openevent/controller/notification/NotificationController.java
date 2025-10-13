package com.group02.openevent.controller.notification;

// ... (Các imports giữ nguyên)

import com.group02.openevent.dto.notification.EventNotificationRequest;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.notification.Notification;
import com.group02.openevent.service.AccountService;
import com.group02.openevent.service.INotificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private AccountService accountService;

    @Autowired
    private INotificationService notificationService;


    @PostMapping("/event-participants")
    public ResponseEntity<?> sendEventNotification(@Valid @RequestBody EventNotificationRequest request, HttpSession session) {
        System.out.println("send event notification"+request);
        // Lấy thông tin người gửi (Host/Admin)
        Account sender = accountService.getCurrentAccount(session);
        System.out.println("sender"+sender);

        // **Bước mới: Khởi tạo đối tượng Notification từ Request và Sender**
        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setMessage(request.getMessage());
        notification.setTitle(request.getTitle());
        System.out.println("send notification"+notification);
        // Các trường khác của Notification được set tại đây

        // Gọi Service (chỉ truyền ID sự kiện và đối tượng Notification)
        Notification savedNotification = notificationService.sendNotificationToEventParticipants(
                request.getEventId(),
                notification
        );

        if (savedNotification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy người tham gia hoặc sự kiện không hợp lệ.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Đã gửi thông báo thành công tới các người tham gia sự kiện ID: " + request.getEventId());
    }
}