package com.group02.openevent.util;

import com.group02.openevent.model.notification.Notification;
import com.group02.openevent.model.notification.NotificationReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketUtil.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketUtil(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Gửi notification realtime đến tất cả receivers
     * @param notification Notification đã được lưu
     */
    public void sendNotificationToReceivers(Notification notification) {
        if (notification == null || notification.getReceivers() == null) {
            logger.warn("Cannot send notification: notification or receivers is null");
            return;
        }

        // Chuẩn bị payload để gửi
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", notification.getNotificationId());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("type", notification.getType().name());
        payload.put("fileURL", notification.getFileURL());
        payload.put("createdAt", notification.getCreatedAt().toString());
        payload.put("senderEmail", notification.getSender() != null ? notification.getSender().getEmail() : null);

        // Gửi đến từng receiver qua WebSocket
        int sentCount = 0;
        for (NotificationReceiver receiver : notification.getReceivers()) {
            if (receiver.getReceiver() != null && receiver.getReceiver().getAccountId() != null) {
                Long accountId = receiver.getReceiver().getAccountId();
                try {
                    // Gửi đến queue riêng của từng user: /queue/notifications/{accountId}
                    String destination = "/queue/notifications/" + accountId;
                    messagingTemplate.convertAndSend(destination, payload);
                    sentCount++;
                    logger.info("Sent notification {} to user {}", notification.getNotificationId(), accountId);
                } catch (Exception e) {
                    logger.error("Error sending notification to user {}: {}", accountId, e.getMessage());
                }
            }
        }

        logger.info("Sent notification {} to {} receivers via WebSocket", notification.getNotificationId(), sentCount);
    }
}
