package com.group02.openevent.service.impl;

import com.group02.openevent.model.dto.home.EventCardDTO;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.model.dto.request.EventCreationRequest;
import com.group02.openevent.model.dto.response.EventResponse;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import com.group02.openevent.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.group02.openevent.model.event.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventServiceImpl implements EventService {
    @Autowired
    OrderService orderService;
    IMusicEventRepo musicEventRepo;
    IEventRepo eventRepo;
    EventMapper eventMapper;

    @Override
    public EventResponse saveEvent(EventCreationRequest request) {
        // đảm bảo schedule biết event cha
        Event event = eventMapper.toEvent(request);

        if (event.getSchedules() != null) {
            event.getSchedules().forEach(s -> s.setEvent(event));
        }
        event.setSpeakers(request.getSpeakers());
        event.setPlaces(request.getPlaces());
        return eventMapper.toEventResponse(eventRepo.save(event));
    }

    @Override
    public MusicEvent saveMusicEvent(MusicEvent musicEvent) {
        return null;
    }

    @Override
    public List<EventCardDTO> getCustomerEvents(Long customerId) {
        List<Event> events = orderService.findConfirmedEventsByCustomerId(customerId);
        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventCardDTO> getLiveEvents(int i) {
        List<Event> events = eventRepo.findRecommendedEvents(
                EventStatus.ONGOING,
                PageRequest.of(0, i)
        );
        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CompetitionEvent saveCompetitionEvent(CompetitionEvent competitionEvent) {
        if (competitionEvent.getSchedules() != null) {
            competitionEvent.getSchedules().forEach(s -> s.setEvent(competitionEvent));
        }
        return eventRepo.save(competitionEvent);
    }

    @Override
    public FestivalEvent saveFestivalEvent(FestivalEvent festivalEvent) {
        if (festivalEvent.getSchedules() != null) {
            festivalEvent.getSchedules().forEach(s -> s.setEvent(festivalEvent));
        }
        return eventRepo.save(festivalEvent);
    }

    @Override
    public WorkshopEvent saveWorkshopEvent(WorkshopEvent workshopEvent) {
        if (workshopEvent.getSchedules() != null) {
            workshopEvent.getSchedules().forEach(s -> s.setEvent(workshopEvent));
        }
        return eventRepo.save(workshopEvent);
    }

    @Override
    public Optional<Event> getEventById(Long id) {
        return eventRepo.findById(id);
    }

    @Override
    public List<Event> getEventsByType(Class<? extends Event> eventType) {
        return eventRepo.findByEventType(eventType);
    }

    @Override
    public Event saveEvent(Event event) {
        return eventRepo.save(event);
    }

    @Override
    public Page<Event> listEvents(EventType eventType, EventStatus status, Pageable pageable) {
        if (eventType != null && status != null) {
            return eventRepo.findByEventTypeAndStatus(eventType, status, pageable);
        } else if (eventType != null) {
            return eventRepo.findByEventType(eventType, pageable);
        } else if (status != null) {
            return eventRepo.findByStatus(status, pageable);
        } else {
            return eventRepo.findAll(pageable);
        }
    }
    @Override
    public List<Event> isTimeConflict(LocalDateTime start, LocalDateTime end, List<Place> places) {
        return eventRepo.findConflictedEvents(start, end, places);
    }

    @Override
    public boolean removeEvent(Long id) {
        if (eventRepo.existsById(id)) {
            eventRepo.deleteById(id);
            return true; // ✅ xóa thành công
        } else {
            return false; // ✅ không tìm thấy
        }
    }

    @Override
    public boolean deleteByTitle(String title) {
        List<Event> events = eventRepo.findByTitle(title);
        if (events.isEmpty()) {
            return false; // ✅ không tìm thấy sự kiện
        }
        eventRepo.deleteAll(events);
        return true; // ✅ xóa thành công
    }

    @Override
    public List<Event> findByTitle(String title) {
        return eventRepo.findByTitle(title);
    }

    @Override
    public List<Event> getAllEvents() {
        return eventRepo.findAll();
    }

//    @Override
//    public List<Event> getEventByUserId(Integer userId) {
//        return eventRepo.getEventByUserId(userId);
//    }

    @Override
    public Optional<Event> getEventByEventId(Long eventId) {
        return eventRepo.findById(eventId);
    }

    @Override
    public Optional<Event> getFirstEventByTitle(String title) {
        List<Event> events = eventRepo.findByTitle(title);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(events.get(0)); // trả về sự kiện đầu tiên
    }
//    @Override
//    public Optional<Event> getNextUpcomingEventByUserId(int userId) {
//        return eventRepo.findNextUpcomingEventByUserId(userId, LocalDateTime.now());
//    }
    @Override
    public List<Event> getEventsByPlace(int placeId) {
        return eventRepo.findByPlaceId(placeId);
    }

    @Override
    public List<Event> getEventsBetween(LocalDateTime start, LocalDateTime end, Long userId) {
        // TODO: Implement proper filtering by date range and user
        // For now, return all events filtered by date range
        return eventRepo.findAll().stream()
                .filter(event -> event.getStartsAt().isAfter(start) || event.getStartsAt().isEqual(start))
                .filter(event -> event.getEndsAt().isBefore(end) || event.getEndsAt().isEqual(end))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Event> getEventByUserId(Long userId) {
        // TODO: Implement proper user-based filtering
        // For now, return all events
        return eventRepo.findAll();
    }

    @Override
    public Event updateEventStatus(Long eventId, EventStatus status) {
        Optional<Event> eventOpt = eventRepo.findById(eventId);
        if (eventOpt.isPresent()) {
            Event event = eventOpt.get();
            event.setStatus(status);
            return eventRepo.save(event);
        }
        throw new RuntimeException("Event not found with id: " + eventId);
    }

    @Override
    public Event approveEvent(Long eventId) {
        return updateEventStatus(eventId, EventStatus.PUBLIC);
    }

    @Override
    public long countEventsByStatus(EventStatus status) {
        return eventRepo.findByStatus(status).size();
    }

    @Override
    public long countEventsByType(EventType eventType) {
        return eventRepo.findByEventType(eventType, PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
    }

    @Override
    public long countTotalEvents() {
        return eventRepo.count();
    }

    @Override
    public List<Event> getRecentEvents(int limit) {
        return eventRepo.findAll(PageRequest.of(0, limit)).getContent();
    }

    @Override
    public List<EventCardDTO> getPosterEvents() {
        List<Event> posterEvents = eventRepo.findByPosterTrueAndStatus(EventStatus.PUBLIC);
        System.out.println("posterEvents size(in service): " + posterEvents.size());
        return posterEvents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventCardDTO> getRecommendedEvents(int limit) {
        List<Event> events = eventRepo.findRecommendedEvents(
                EventStatus.PUBLIC,
                PageRequest.of(0, limit)
        );
        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Override
    public EventCardDTO convertToDTO(Event event) {
        String organizer = event.getOrganization() != null && event.getOrganization().getOrgName() != null
                ? event.getOrganization().getOrgName()
                : (event.getHost() != null ? event.getHost().getHostName() : "Unknown");
        String city = event.getPlaces() != null && !event.getPlaces().isEmpty()
                ? event.getPlaces().get(0).getPlaceName()
                : "TBA";
        Integer registered = 0;
        registered = orderService.countUniqueParticipantsByEventId(event.getId());
        return EventCardDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .imageUrl(event.getImageUrl())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .startsAt(event.getStartsAt())
                .endsAt(event.getEndsAt())
                .enrollDeadline(event.getEnrollDeadline())
                .capacity(event.getCapacity())
                .registered(registered) // TODO: Get actual registration count from Order/Registration table
                .city(city) // TODO: Extract from Places
                .organizer(organizer) // TODO: Get from Host/Organizer
                .maxPrice(event.getMaxTicketPice())
                .minPrice(event.getMinTicketPice())// TODO: Get from ticket pricing
                .poster(event.isPoster())
                .build();
    }


}
