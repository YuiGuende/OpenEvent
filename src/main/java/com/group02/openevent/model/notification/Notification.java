package com.group02.openevent.model.notification;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.user.User;
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
    private User sender;

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

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationReceiver> receivers = new ArrayList<>();

    public Notification() {
    }


    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", sender=" + sender.getAccount().getEmail() +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                ", targetUrl='" + targetUrl + '\'' +
                ", type=" + type +
                ", createdAt=" + createdAt +
                ", relatedRequest=" + relatedRequest +
                '}';
    }
}
