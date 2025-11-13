package com.group02.openevent.event;

import com.group02.openevent.model.user.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserCreatedEvent extends ApplicationEvent {
    private final User user;
    private final Long actorId; // USER_ID from session (null if self-registration)
    private final String userType; // "CUSTOMER" or "HOST"

    public UserCreatedEvent(Object source, User user, Long actorId, String userType) {
        super(source);
        this.user = user;
        this.actorId = actorId; // Can be null for self-registration
        this.userType = userType;
    }
}

