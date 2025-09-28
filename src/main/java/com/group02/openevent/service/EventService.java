package com.group02.openevent.service;


import com.group02.openevent.model.dto.MusicEventDetailDTO;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface EventService {
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    CompetitionEvent saveCompetitionEvent(CompetitionEvent competitionEvent);
    FestivalEvent saveFestivalEvent(FestivalEvent festivalEvent);
    WorkshopEvent saveWorkshopEvent(WorkshopEvent workshopEvent);
    Optional<Event> getEventById(Integer id);

    Optional<Event> getEventById(Long id);

    List<Event> getEventsByType(Class<? extends Event> eventType);
    Event saveEvent(Event event);
    Page<Event> listEvents(EventType eventType, EventStatus status, Pageable pageable);
}
