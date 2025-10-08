package com.group02.openevent.service;



import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.request.EventUpdateRequest;
import com.group02.openevent.model.event.*;
import com.group02.openevent.dto.request.EventCreationRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface EventService {
    CompetitionEvent saveCompetitionEvent(CompetitionEvent competitionEvent);
    FestivalEvent saveFestivalEvent(FestivalEvent festivalEvent);
    WorkshopEvent saveWorkshopEvent(WorkshopEvent workshopEvent);
    Optional<Event> getEventById(Long id);
    List<Event> getEventsByType(Class<? extends Event> eventType);
    Event saveEvent(Event event);
    Page<Event> listEvents(EventType eventType, EventStatus status, Pageable pageable);
    Event updateEventStatus(Long eventId, EventStatus status);
    Event approveEvent(Long eventId);
    long countEventsByStatus(EventStatus status);
    long countEventsByType(EventType eventType);
    long countTotalEvents();
    List<Event> getRecentEvents(int limit);
    List<EventCardDTO> getPosterEvents();
    List<EventCardDTO> getRecommendedEvents(int limit);
    EventCardDTO convertToDTO(Event event);
    EventResponse saveEvent(EventCreationRequest event);
    EventResponse updateEvent(Long id, EventUpdateRequest event);
    MusicEvent saveMusicEvent(MusicEvent musicEvent);
    List<EventCardDTO> getCustomerEvents(Long customerId);
    List<EventCardDTO> getLiveEvents(int i);
    Page<Event> getEventsByDepartment(Long departmentId, EventType eventType, EventStatus status, Pageable pageable);
}
