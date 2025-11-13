package com.group02.openevent.service.impl;

import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.request.PlaceUpdateRequest;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.SpeakerRole;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.*;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.event.EventCancelledEvent;
import com.group02.openevent.event.EventCreatedEvent;
import com.group02.openevent.event.EventUpdatedEvent;
import com.group02.openevent.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import com.group02.openevent.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.AccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.enums.Building;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventServiceImpl implements EventService {
    @Autowired
    @Lazy
    OrderService orderService;
    IEventRepo eventRepo;
    EventMapper eventMapper;
    IOrganizationRepo organizationRepo;
    IHostRepo hostRepo;
    IPlaceRepo placeRepo;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public EventResponse saveEvent(EventCreationRequest request,Long hostId) {
        Event event;
        log.info("Saving event {}", request.getEventType());
        log.info("Saving event from DTO type: {}", request.getClass().getName());
        EventType type = request.getEventType();
        if (type == null) {
            log.warn("Null EventType received. Defaulting to generic Event.");
            event = new Event();
        }else {
            switch (request.getEventType()) {
                case WORKSHOP:
                    event = new WorkshopEvent();
                    break;
                case MUSIC:
                    event = new MusicEvent();
                    break;
                case FESTIVAL:
                    event = new FestivalEvent();
                    break;
                case COMPETITION:
                    event = new CompetitionEvent();
                    break;
                default:
                    // Khối này chỉ dành cho trường hợp EventType không hợp lệ hoặc không có
                    log.warn("Unknown or null EventType received. Defaulting to generic Event.");
                    event = new Event();
                    break;
            }
        }
        log.info("Saving event {}", event.getClass().getName());
        eventMapper.createEventFromRequest(request, event);
        final Event finalEvent = event;
        if (event.getSubEvents() != null) {
            event.getSubEvents().forEach(sub -> sub.setParentEvent(finalEvent));
        }
        event.setHost(hostRepo.getHostById(hostId));
        Event savedEvent = eventRepo.save(event);
        
        // Publish EventCreatedEvent for audit log
        try {
            Long userId = savedEvent.getHost() != null && savedEvent.getHost().getUser() != null
                    ? savedEvent.getHost().getUser().getUserId()
                    : null;
            if (userId != null) {
                eventPublisher.publishEvent(new EventCreatedEvent(this, savedEvent, userId));
            }
        } catch (Exception e) {
            log.error("Error publishing EventCreatedEvent: {}", e.getMessage(), e);
        }
        
        return eventMapper.toEventResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventUpdateRequest request) {

        Event existing = eventRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id " + id));

        Event event = existing; // Dùng biến này để xử lý chung
        eventMapper.updateEventFromRequest(request, event);

        // 🟢 Update organization, host, parent
        if (request.getOrganizationId() != null) {
            Organization org = organizationRepo.findById(request.getOrganizationId())
                    .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
            event.setOrganization(org);
        }

        if (request.getHostId() != null) {
            Host host = hostRepo.findById(request.getHostId())
                    .orElseThrow(() -> new EntityNotFoundException("Host not found"));
            event.setHost(host);
        }

        if (request.getParentEventId() != null) {
            Event parent = eventRepo.findById(request.getParentEventId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent event not found"));
            event.setParentEvent(parent);
        }

        switch (String.valueOf(request.getEventType())) {
            case "COMPETITION" -> {
                CompetitionEvent comp = (CompetitionEvent) existing;
                comp.setCompetitionType(request.getCompetitionType());
                comp.setRules(request.getRules());
                comp.setPrizePool(request.getPrizePool());
            }
            case "MUSIC" -> {
                MusicEvent mus = (MusicEvent) existing;
                mus.setMusicType(request.getMusicType());
                mus.setGenre(request.getGenre());
                mus.setPerformerCount(request.getPerformerCount());
            }
            case "WORKSHOP" -> {
                WorkshopEvent ws = (WorkshopEvent) existing;
                ws.setTopic(request.getTopic());
                ws.setSkillLevel(request.getSkillLevel());
                ws.setMaxParticipants(request.getMaxParticipants());
            }
            case "FESTIVAL" -> {
                FestivalEvent fe = (FestivalEvent) existing;
                fe.setCulture(request.getCulture());
                fe.setHighlight(request.getHighlight());
            }
        }

        // 🟢 Places - Handle places from JSON data with proper deletion support
        if (request.getPlaceUpdateRequests() != null && !request.getPlaceUpdateRequests().isEmpty()) {
            event.getPlaces().clear();


            for (PlaceUpdateRequest placeUpdateRequest : request.getPlaceUpdateRequests()) {
                // Skip deleted places
                if (Boolean.TRUE.equals(placeUpdateRequest.getIsDeleted())) {
                    continue;
                }

                Place place;

                if (placeUpdateRequest.getId() != null) {
                    place = placeRepo.findById(placeUpdateRequest.getId())
                            .orElseThrow(() -> new EntityNotFoundException("Place not found with id " + placeUpdateRequest.getId()));
                    if (!place.getPlaceName().equals(placeUpdateRequest.getPlaceName()) || 
                        !place.getBuilding().equals(placeUpdateRequest.getBuilding())) {
                        place.setPlaceName(placeUpdateRequest.getPlaceName());
                        place.setBuilding(placeUpdateRequest.getBuilding());
                        place = placeRepo.save(place);
                    }
                } else {
                    place = new Place();
                    place.setPlaceName(placeUpdateRequest.getPlaceName());
                    place.setBuilding(placeUpdateRequest.getBuilding());
                    place = placeRepo.save(place);
                }

                event.getPlaces().add(place);

            }

        } else if (request.getPlaces() != null) {
            event.getPlaces().clear();

            for (Place placeRequest : request.getPlaces()) {
                Place place;

                if (placeRequest.getId() != null) {
                    // Existing place - find by ID
                    place = placeRepo.findById(placeRequest.getId())
                            .orElseThrow(() -> new EntityNotFoundException("Place not found with id " + placeRequest.getId()));
                } else {
                    place = new Place();
                    place.setPlaceName(placeRequest.getPlaceName());
                    place.setBuilding(placeRequest.getBuilding());
                    place = placeRepo.save(place);
                }
                event.getPlaces().add(place);
            }

            log.info("Updated event with {} places (fallback mode)", event.getPlaces().size());
        }
        // ✅ Save cuối cùng
        log.info("Saving event with {} places to database", event.getPlaces().size());
        Event saved = eventRepo.saveAndFlush(event);
        
        // Publish EventUpdatedEvent for audit log
        try {
            Long userId = saved.getHost() != null && saved.getHost().getUser() != null
                    ? saved.getHost().getUser().getUserId()
                    : null;
            if (userId != null) {
                eventPublisher.publishEvent(new EventUpdatedEvent(this, saved, userId));
            }
        } catch (Exception e) {
            log.error("Error publishing EventUpdatedEvent: {}", e.getMessage(), e);
        }

        return eventMapper.toEventResponse(saved);
    }


    @Override
    public MusicEvent saveMusicEvent(MusicEvent musicEvent) {
        return null;
    }

    public List<Event> getEventsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return eventRepo.findAllById(ids);
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
    public List<Event> getEventByHostId(Long id) {
        return eventRepo.getEventByHostId(id);
    }


    @Override
    public Event getEventResponseById(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
    }

    @Override
    public Page<Event> getEventsByDepartment(Long departmentId, EventType eventType, EventStatus status, Pageable pageable) {
        if (eventType != null && status != null) {
            return eventRepo.findByDepartment_AccountIdAndEventTypeAndStatus(departmentId, eventType, status, pageable);
        } else if (eventType != null) {
            return eventRepo.findByDepartment_AccountIdAndEventType(departmentId, eventType, pageable);
        } else if (status != null) {
            return eventRepo.findByDepartment_AccountIdAndStatus(departmentId, status, pageable);
        } else {
            return eventRepo.findByDepartment_AccountId(departmentId, pageable);
        }
    }


    @Override
    public List<EventCardDTO> searchEvents(String keyword, String type,
                                           LocalDate startDate, LocalDate endDate) {

        LocalDateTime fromDateTime = (startDate != null)
                ? startDate.atStartOfDay()
                : null;
        LocalDateTime toDateTime = (endDate != null)
                ? endDate.atTime(23, 59, 59)
                : null;

        EventType eventType = null;
        if (type != null && !type.isBlank()) {
            try {
                eventType = EventType.valueOf(type.toUpperCase()); // convert String -> Enum
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Invalid event type: " + type);
            }
        }

        List<Event> events = eventRepo.searchEvents(
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                eventType,
                fromDateTime,
                toDateTime
        );

        return events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long countUniqueParticipantsByEventId(Long id) {
      return  orderService.countUniqueParticipantsByEventId(id);
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
    public List<Event> findByTitleAndPublicStatus(String title) {
        return eventRepo.findByTitleAndPublicStatus(title);
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
        if (eventId == null) return Optional.empty();
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

    @Override
    public Optional<Event> getFirstPublicEventByTitle(String title) {
        List<Event> events = eventRepo.findByTitleAndPublicStatus(title);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(events.get(0)); // trả về sự kiện PUBLIC đầu tiên
    }
//    @Override
//    public Optional<Event> getNextUpcomingEventByUserId(int userId) {
//        return eventRepo.findNextUpcomingEventByUserId(userId, LocalDateTime.now());
//    }
    @Override
    public List<Event> getEventsByPlace(Long placeId) {
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

    public Optional<Event> getNextUpcomingEventByUserId(Long userId) {
        // Gọi repository với điều kiện: sự kiện PHẢI BẮT ĐẦU sau thời điểm hiện tại
        return eventRepo.findNextUpcomingEventByUserId(userId, LocalDateTime.now());
    }

    @Override
    public List<Event> getEventByUserId(Long userId) {
        // TODO: Implement proper user-based filtering
        // For now, return all events
        return eventRepo.findAll();
    }

    @Override
    public Event updateEventStatus(Long eventId, EventStatus status) {
        return updateEventStatus(eventId, status, null);
    }
    
    // Overload method với userId parameter
    public Event updateEventStatus(Long eventId, EventStatus status, Long userId) {
        Optional<Event> eventOpt = eventRepo.findById(eventId);
        if (eventOpt.isPresent()) {
            Event event = eventOpt.get();
            EventStatus oldStatus = event.getStatus();
            event.setStatus(status);
            Event savedEvent = eventRepo.save(event);
            
            // Publish events for audit log
            try {
                // Determine userId: use parameter if provided, otherwise try to get from event host
                Long actorId = userId;
                if (actorId == null && savedEvent.getHost() != null && savedEvent.getHost().getUser() != null) {
                    actorId = savedEvent.getHost().getUser().getUserId();
                }
                
                if (actorId != null) {
                    // Publish EventCancelledEvent if status changed to CANCEL
                    if (status == EventStatus.CANCEL) {
                        eventPublisher.publishEvent(new EventCancelledEvent(this, savedEvent, actorId));
                    }
                    // Note: EventApprovedEvent is published from RequestServiceImpl when request is approved
                }
            } catch (Exception e) {
                log.error("Error publishing event status change event: {}", e.getMessage(), e);
            }
            
            return savedEvent;
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
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return eventRepo.findAll(pageRequest).getContent();
    }

    @Override
    public List<EventCardDTO> getPosterEvents() {
        List<Event> posterEvents = eventRepo.findByPosterTrue();
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
        // Avoid touching Host->Customer->Account to prevent EntityNotFound on bad data
        String organizer = null;
        if (event.getOrganization() != null && event.getOrganization().getOrgName() != null) {
            organizer = event.getOrganization().getOrgName();
        } else if (event.getHost() != null && event.getHost().getOrganization() != null
                && event.getHost().getOrganization().getOrgName() != null) {
            organizer = event.getHost().getOrganization().getOrgName();
        } else {
            organizer = "Unknown";
        }
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
                .registered(registered)
                .city(city)
                .organizer(organizer)
                .maxPrice(event.getMaxTicketPice())
                .minPrice(event.getMinTicketPice())
                .poster(event.isPoster())
                .benefits(event.getBenefits())
                .build();
    }


}
