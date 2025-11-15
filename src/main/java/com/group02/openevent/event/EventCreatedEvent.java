package com.group02.openevent.event;

import com.group02.openevent.model.event.Event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventCreatedEvent extends ApplicationEvent {
    private final Event event;
    private final Long actorId; // USER_ID from session

    public EventCreatedEvent(Object source, Event event, Long actorId) {
        super(source);
        this.event = event;
        this.actorId = actorId;
    }
}

