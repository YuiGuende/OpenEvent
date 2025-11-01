package com.group02.openevent.model.notification;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.request.Request;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    private String fileURL;

    @ManyToOne
    private Account sender;

    private String title;

    @Column(length = 500, nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "target_url", length = 255)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    private Request relatedRequest;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationReceiver> receivers = new ArrayList<>();

    public Notification() {
    }

    public List<NotificationReceiver> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<NotificationReceiver> receivers) {
        this.receivers = receivers;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Account getSender() {
        return sender;
    }

    public void setSender(Account sender) {
        this.sender = sender;
    }



    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Request getRelatedRequest() {
        return relatedRequest;
    }

    public void setRelatedRequest(Request relatedRequest) {
        this.relatedRequest = relatedRequest;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", sender=" + sender.getEmail() +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                ", targetUrl='" + targetUrl + '\'' +
                ", type=" + type +
                ", createdAt=" + createdAt +
                ", relatedRequest=" + relatedRequest +
                '}';
    }
}
