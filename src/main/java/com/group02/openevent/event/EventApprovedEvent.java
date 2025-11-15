package com.group02.openevent.event;

import com.group02.openevent.model.event.Event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventApprovedEvent extends ApplicationEvent {
    private final Event event;
    private final Long actorId; // USER_ID from session (approver)

    public EventApprovedEvent(Object source, Event event, Long actorId) {
        super(source);
        this.event = event;
        this.actorId = actorId;
    }
}

