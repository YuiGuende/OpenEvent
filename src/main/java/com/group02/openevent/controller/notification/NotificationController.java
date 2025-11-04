package com.group02.openevent.controller.notification;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.notification.Notification;
import com.group02.openevent.repository.INotificationRepo;
import com.group02.openevent.service.AccountService;
import com.group02.openevent.service.INotificationService;
import com.group02.openevent.util.CloudinaryUtil;
import com.group02.openevent.util.WebSocketUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    @Autowired
    private AccountService accountService;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private CloudinaryUtil cloudinaryUtil;

    @Autowired
    private WebSocketUtil webSocketUtil;

    @Autowired
    private INotificationRepo notificationRepo;

    @Autowired
    private com.group02.openevent.repository.INotificationReceiverRepo notificationReceiverRepo;

    @Autowired
    private com.group02.openevent.repository.ICustomerRepo customerRepo;

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        try {
            Account account = accountService.getCurrentAccount(session);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Not authenticated"));
            }

            long count = notificationReceiverRepo.countUnreadByReceiverAccountId(account.getAccountId());
            return ResponseEntity.ok(Map.of("success", true, "count", count));
        } catch (Exception e) {
            logger.error("Error getting unread count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "count", 0));
        }
    }

    @GetMapping("/my-notifications")
    public ResponseEntity<?> getMyNotifications(HttpSession session) {
        try {
            Account account = accountService.getCurrentAccount(session);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Not authenticated"));
            }

            List<com.group02.openevent.model.notification.NotificationReceiver> receivers = 
                    notificationReceiverRepo.findByReceiverAccountIdOrderByCreatedAtDesc(account.getAccountId());

            List<Map<String, Object>> notificationDTOs = receivers.stream()
                    .map(nr -> {
                        Notification n = nr.getNotification();
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("notificationId", n.getNotificationId());
                        dto.put("title", n.getTitle());
                        dto.put("message", n.getMessage());
                        dto.put("fileURL", n.getFileURL());
                        dto.put("createdAt", n.getCreatedAt().toString());
                        dto.put("isRead", nr.isRead());
                        dto.put("readAt", nr.getReadAt() != null ? nr.getReadAt().toString() : null);
                        
                        // Get event info directly from notification
                        String eventTitle = "Unknown Event";
                        Long eventId = null;
                        
                        if (n.getEvent() != null) {
                            eventTitle = n.getEvent().getTitle();
                            eventId = n.getEvent().getId();
                        }
                        
                        // Get host name from sender
                        String hostName = "Unknown Host";
                        if (n.getSender() != null) {
                            com.group02.openevent.model.user.Customer senderCustomer = 
                                    customerRepo.findByAccount_AccountId(n.getSender().getAccountId()).orElse(null);
                            
                            if (senderCustomer != null && senderCustomer.getHost() != null) {
                                com.group02.openevent.model.user.Host host = senderCustomer.getHost();
                                hostName = host.getHostName();
                            } else {
                                // Fallback: use sender email
                                hostName = n.getSender().getEmail();
                            }
                        }
                        
                        dto.put("hostName", hostName);
                        dto.put("eventTitle", eventTitle);
                        dto.put("eventId", eventId);
                        
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "notifications", notificationDTOs));
        } catch (Exception e) {
            logger.error("Error getting notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "notifications", List.of()));
        }
    }

    @PostMapping("/{notificationId}/mark-read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId, HttpSession session) {
        try {
            Account account = accountService.getCurrentAccount(session);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Not authenticated"));
            }

            Optional<com.group02.openevent.model.notification.NotificationReceiver> receiverOpt = 
                    notificationReceiverRepo.findByReceiver_AccountIdAndNotification_NotificationId(
                            account.getAccountId(), notificationId);

            if (receiverOpt.isPresent()) {
                com.group02.openevent.model.notification.NotificationReceiver receiver = receiverOpt.get();
                receiver.setRead(true);
                receiver.setReadAt(java.time.LocalDateTime.now());
                notificationReceiverRepo.save(receiver);
                
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Notification not found"));
            }
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false));
        }
    }

    @PostMapping("/event-participants")
    public ResponseEntity<?> sendEventNotification(
            @RequestParam("eventId") Long eventId,
            @RequestParam("title") String title,
            @RequestParam("message") String message,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpSession session) {
        
        try {
            logger.info("Sending notification for event {}: title={}, message length={}, hasFile={}", 
                    eventId, title, message.length(), file != null && !file.isEmpty());

            // Lấy thông tin người gửi (Host/Admin)
            Account sender = accountService.getCurrentAccount(session);
            if (sender == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng chưa đăng nhập");
            }

            // Upload file nếu có
            String fileURL = null;
            if (file != null && !file.isEmpty()) {
                try {
                    fileURL = cloudinaryUtil.uploadFile(file);
                    logger.info("File uploaded successfully: {}", fileURL);
                } catch (IOException e) {
                    logger.error("Error uploading file: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Lỗi khi upload file: " + e.getMessage());
                }
            }

            // Khởi tạo đối tượng Notification từ Request và Sender
            Notification notification = new Notification();
            notification.setSender(sender);
            notification.setMessage(message);
            notification.setTitle(title);
            notification.setFileURL(fileURL);

            // Gọi Service (chỉ truyền ID sự kiện và đối tượng Notification)
            Notification savedNotification = notificationService.sendNotificationToEventParticipants(
                    eventId,
                    notification
            );

            if (savedNotification == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy người tham gia hoặc sự kiện không hợp lệ.");
            }

            // Gửi notification realtime qua WebSocket
            try {
                webSocketUtil.sendNotificationToReceivers(savedNotification);
                logger.info("Notification {} sent via WebSocket to {} receivers", 
                        savedNotification.getNotificationId(), 
                        savedNotification.getReceivers() != null ? savedNotification.getReceivers().size() : 0);
            } catch (Exception e) {
                logger.error("Error sending notification via WebSocket: {}", e.getMessage());
                // Không fail request nếu WebSocket có lỗi
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Đã gửi thông báo thành công tới các người tham gia sự kiện ID: " + eventId);
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getNotificationsByEvent(@PathVariable Long eventId) {
        try {
            // Lấy tất cả notifications của event này (filter trực tiếp bằng eventId)
            List<Notification> notifications = notificationRepo.findAll().stream()
                    .filter(n -> n.getEvent() != null && 
                            n.getEvent().getId() != null &&
                            n.getEvent().getId().equals(eventId))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Sắp xếp mới nhất trước
                    .collect(Collectors.toList());

            // Convert to DTO
            List<Map<String, Object>> notificationDTOs = notifications.stream()
                    .map(n -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("notificationId", n.getNotificationId());
                        dto.put("title", n.getTitle());
                        dto.put("message", n.getMessage());
                        dto.put("fileURL", n.getFileURL());
                        dto.put("createdAt", n.getCreatedAt().toString());
                        dto.put("receiverCount", n.getReceivers() != null ? n.getReceivers().size() : 0);
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("notifications", notificationDTOs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting notifications for event {}: {}", eventId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Lỗi khi lấy lịch sử thông báo: " + e.getMessage());
            response.put("notifications", List.of());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}