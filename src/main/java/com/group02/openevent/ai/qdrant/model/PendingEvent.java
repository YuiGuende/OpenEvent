package com.group02.openevent.ai.qdrant.model;

import com.group02.openevent.ai.dto.EventItem;

public class PendingEvent {

    private EventItem eventItem;

    public PendingEvent(EventItem eventItem) {
        this.eventItem = eventItem;
    }

    public EventItem getEventItem() {
        return eventItem;
    }

    public void setEventItem(EventItem eventItem) {
        this.eventItem = eventItem;
    }

    public PendingEvent() {
    }
}

