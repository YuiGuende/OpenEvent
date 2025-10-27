package com.group02.openevent.service;



import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.model.event.*;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.user.Host;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.time.LocalDate;
import java.time.LocalDateTime;
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
    List<Event> isTimeConflict(LocalDateTime start, LocalDateTime end, List<Place> places);
    boolean removeEvent(Long id);
    boolean deleteByTitle(String title);
    List<Event> findByTitle(String title);
    List<Event> findByTitleAndPublicStatus(String title);
    List<Event> getAllEvents();
    Optional<Event> getEventByEventId(Long eventId);
    Optional<Event> getFirstEventByTitle(String title);
    Optional<Event> getFirstPublicEventByTitle(String title);
//  Optional<Event> getNextUpcomingEventByUserId(int userId);
    List<Event> getEventsByPlace(Long placeId);

    // Methods for AI support


    List<Event> getEventsByIds(List<Long> ids);
    List<Event> getEventsBetween(LocalDateTime start, LocalDateTime end, Long userId);
    List<Event> getEventByUserId(Long userId);
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
    List<Event> getEventByHostId(Long id);
     Event getEventResponseById(Long id);
    Page<Event> getEventsByDepartment(Long departmentId, EventType eventType, EventStatus status, Pageable pageable);
    Optional<Event> getNextUpcomingEventByUserId(Long userId);

    List<EventCardDTO> searchEvents(String keyword, String type, LocalDate startDate, LocalDate endDate);

    long countUniqueParticipantsByEventId(Long id);
}
