package com.group02.openevent.service;

import com.group02.openevent.model.event.Event;

import java.util.Optional;

public interface EventService {
    Event saveEvent(Event event);
    Optional<Event> getEventById(Integer id);
}
