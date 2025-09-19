package com.group02.openevent.service;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;

import java.util.Optional;

public interface EventService {
    Event saveEvent(Event event);
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    Optional<Event> getEventById(Integer id);
}
