package com.group02.openevent.event;

import com.group02.openevent.model.request.Request;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventApprovalRequestedEvent extends ApplicationEvent {
    private final Request request;
    private final Long actorId; // USER_ID from session (sender)

    public EventApprovalRequestedEvent(Object source, Request request, Long actorId) {
        super(source);
        this.request = request;
        this.actorId = actorId;
    }
}

