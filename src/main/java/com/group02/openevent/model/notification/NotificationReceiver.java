package com.group02.openevent.model.notification;

import com.group02.openevent.model.account.Account;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_receiver")
public class NotificationReceiver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne
    @JoinColumn(name = "receiver_account_id", nullable = false)
    private Account receiver;

    @Column(name = "is_read")
    private boolean read = false;

    private LocalDateTime readAt;

    public NotificationReceiver() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Account getReceiver() {
        return receiver;
    }

    public void setReceiver(Account receiver) {
        this.receiver = receiver;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}