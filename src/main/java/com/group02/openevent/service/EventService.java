package com.group02.openevent.service;

import com.group02.openevent.model.dto.request.EventCreationRequest;
import com.group02.openevent.model.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;

import java.util.Optional;

public interface EventService {
    EventResponse saveEvent(EventCreationRequest event);
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    Optional<Event> getEventById(Long id);
}
