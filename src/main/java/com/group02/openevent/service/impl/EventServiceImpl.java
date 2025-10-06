package com.group02.openevent.service.impl;

import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.request.*;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.SpeakerRole;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.*;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.group02.openevent.model.event.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    IOrganizationRepo organizationRepo;
    ITicketTypeRepo ticketTypeRepo;
    IHostRepo hostRepo;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public EventResponse saveEvent(EventCreationRequest request) {
        Event event = eventMapper.toEvent(request);

        // Gán host, org, parent event

        if (event.getSubEvents() != null) {
            event.getSubEvents().forEach(sub -> sub.setParentEvent(event));
        }
        Event saved = eventRepo.save(event);

        return eventMapper.toEventResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventUpdateRequest request) {
        Event event;
        if (request instanceof WorkshopEventUpdateRequest) {
            event = entityManager.find(WorkshopEvent.class, id);
        } else if (request instanceof MusicEventUpdateRequest) {
            event = entityManager.find(MusicEvent.class, id);
        } else if (request instanceof FestivalEventUpdateRequest) {
            event = entityManager.find(FestivalEvent.class, id);
        } else if (request instanceof CompetitionEventUpdateRequest) {
            event = entityManager.find(CompetitionEvent.class, id);
        } else {
            event = entityManager.find(Event.class, id);
        }
        log.info("Request type: {}", request.getClass().getName());

        // 👇 Load lại đúng subclass thật (MusicEvent, WorkshopEvent,...)
        // Handle schedules FIRST - before mapper runs
//        if (request.getSchedules() != null) {
//            event.getSchedules().clear();
//            for (EventSchedule s : request.getSchedules()) {
//                s.setEvent(event);
//                event.getSchedules().add(s);
//            }
//        }

//        if (request.getTicketTypes() != null) {
//            event.getTicketTypes().clear();
//            for (TicketType t : request.getTicketTypes()) {
//                t.setEvent(event);
//                event.getTicketTypes().add(t);
//            }
//        }

        // Map common fields
        eventMapper.updateEventFromRequest(request, event);

        // Update organization, host, and parent event
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

        if (event.getEventImages() != null) {
            for (EventImage img : event.getEventImages()) {
                img.setEvent(event);
            }
        }

        if (event.getSubEvents() != null) {
            for (Event sub : event.getSubEvents()) {
                sub.setParentEvent(event);
            }
        }

        // Handle speakers
//        if (request.getSpeakers() != null && !request.getSpeakers().isEmpty()) {
//            List<Speaker> speakers = new ArrayList<>();
//            for (SpeakerRequest s : request.getSpeakers()) {
//                Speaker sp = new Speaker();
//                sp.setName(s.getName());
//                sp.setProfile(s.getProfile());
//                sp.setImageUrl(s.getImageUrl());
//                sp.setDefaultRole(s.getDefaultRole());
//                sp.setEvents(List.of(event));
//                speakers.add(sp);
//            }
//            event.setSpeakers(speakers);
//        }

        // ✅ Handle subclass-specific fields (now works!)
        if (event instanceof MusicEvent musicEvent && request instanceof MusicEventUpdateRequest musicReq) {
            musicEvent.setMusicType(musicReq.getMusicType());
            musicEvent.setGenre(musicReq.getGenre());
            musicEvent.setPerformerCount(musicReq.getPerformerCount());
        }
        else if (event instanceof FestivalEvent festivalEvent && request instanceof FestivalEventUpdateRequest festReq) {
            festivalEvent.setCulture(festReq.getCulture());
            festivalEvent.setHighlight(festReq.getHighlight());
        }
        else if (event instanceof CompetitionEvent competitionEvent && request instanceof CompetitionEventUpdateRequest comReq) {
            competitionEvent.setCompetitionType(comReq.getCompetitionType());
            competitionEvent.setRules(comReq.getRules());
            competitionEvent.setPrizePool(comReq.getPrizePool());
        }
        else if (event instanceof WorkshopEvent workshopEvent && request instanceof WorkshopEventUpdateRequest workReq) {
            workshopEvent.setMaterialsLink(workReq.getMaterialsLink());
            workshopEvent.setPrerequisites(workReq.getPrerequisites());
            workshopEvent.setMaxParticipants(workReq.getMaxParticipants());
            workshopEvent.setSkillLevel(workReq.getSkillLevel());
            workshopEvent.setTopic(workReq.getTopic());
            log.info(workReq.getSkillLevel());
        }

        Event saved = eventRepo.save(event);
        return eventMapper.toEventResponse(saved);
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
        List<Event> posterEvents = eventRepo.findByPosterTrue();
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

