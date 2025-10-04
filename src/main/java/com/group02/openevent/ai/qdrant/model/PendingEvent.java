package com.group02.openevent.ai.qdrant.model;

import com.group02.openevent.model.event.Event;

public class PendingEvent {

    private Event event;

    public PendingEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public PendingEvent() {
    }
}

