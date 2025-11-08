package com.group02.openevent.model.notification;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
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
    private User receiver;

    @Column(name = "is_read")
    private boolean read = false;

    private LocalDateTime readAt;

    public NotificationReceiver() {
    }


}