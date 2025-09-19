package com.group02.openevent.service;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EventService {
    Event saveEvent(Event event);
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    Optional<Event> getEventById(Integer id);
    List<Event> getEventsByType(Class<? extends Event> eventType);

    Page<Event> listEvents(EventType eventType, EventStatus status, Pageable pageable);
}
