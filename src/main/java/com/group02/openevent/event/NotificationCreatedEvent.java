package com.group02.openevent.event;

import com.group02.openevent.model.notification.Notification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationCreatedEvent extends ApplicationEvent {
    private final Notification notification;
    private final Long actorId; // USER_ID from session (sender)

    public NotificationCreatedEvent(Object source, Notification notification, Long actorId) {
        super(source);
        this.notification = notification;
        this.actorId = actorId;
    }
}

