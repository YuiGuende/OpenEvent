package com.group02.openevent.event;

import com.group02.openevent.model.event.Event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventCancelledEvent extends ApplicationEvent {
    private final Event event;
    private final Long actorId; // USER_ID from session

    public EventCancelledEvent(Object source, Event event, Long actorId) {
        super(source);
        this.event = event;
        this.actorId = actorId;
    }
}

