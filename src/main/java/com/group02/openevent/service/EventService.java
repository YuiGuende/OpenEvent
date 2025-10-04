package com.group02.openevent.service;


import com.group02.openevent.model.event.*;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventService {
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    CompetitionEvent saveCompetitionEvent(CompetitionEvent competitionEvent);
    FestivalEvent saveFestivalEvent(FestivalEvent festivalEvent);
    WorkshopEvent saveWorkshopEvent(WorkshopEvent workshopEvent);
    Optional<Event> getEventById(Integer id);
    List<Event> getEventsByType(Class<? extends Event> eventType);
    Event saveEvent(Event event);
    Page<Event> listEvents(EventType eventType, EventStatus status, Pageable pageable);
    List<Event> isTimeConflict(LocalDateTime start, LocalDateTime end, List<Place> places);
    boolean removeEvent(int id);
    boolean deleteByTitle(String title);
    List<Event> findByTitle(String title);
    List<Event> getAllEvents();
    Optional<Event> getEventByEventId(Integer eventId);
    Optional<Event> getFirstEventByTitle(String title);
//  Optional<Event> getNextUpcomingEventByUserId(int userId);
    List<Event> getEventsByPlace(int placeId);
    
    // Methods for AI support
    List<Event> getEventsBetween(LocalDateTime start, LocalDateTime end, Integer userId);
    List<Event> getEventByUserId(Integer userId);
}
