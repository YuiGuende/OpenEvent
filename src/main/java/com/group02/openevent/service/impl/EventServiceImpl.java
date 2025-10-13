package com.group02.openevent.service.impl;

import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.request.*;
import com.group02.openevent.dto.request.create.*;
import com.group02.openevent.dto.request.update.*;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.*;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.AccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.group02.openevent.model.event.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventServiceImpl implements EventService {
    @Autowired
    OrderService orderService;
    IMusicEventRepo musicEventRepo;
    IWorkshopEventRepo iWorkshopEventRepo;
    IFestivalEventRepo iFestivalEventRepo;
    ICompetitionEventRepo iCompetitionEventRepo;
    IEventRepo eventRepo;
    EventMapper eventMapper;
    IOrganizationRepo organizationRepo;
    ITicketTypeRepo ticketTypeRepo;
    IHostRepo hostRepo;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public EventResponse saveEvent(EventCreationRequest request) {
        Event event;
        log.info("Saving event {}", request.getEventType());
        log.info("Saving event from DTO type: {}", request.getClass().getName());
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
        log.info("Saving event {}", event.getClass().getName());
        eventMapper.createEventFromRequest(request, event);
        final Event finalEvent = event;
        if (event.getSubEvents() != null) {
            event.getSubEvents().forEach(sub -> sub.setParentEvent(finalEvent));
        }

        event.setHost(hostRepo.getHostById(Long.parseLong("1")));

        return eventMapper.toEventResponse(eventRepo.save(event));
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventUpdateRequest request) {
        log.info("Request type: {}", request.getClass().getName());
        log.info("🔍 Raw request eventType = {}", request.getEventType());

        Event existing = eventRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id " + id));

        Event event = existing; // Dùng biến này để xử lý chung
        Long oldId = existing.getId(); // Giữ lại ID cũ để xóa // dùng biến này để xử lý chung

        // 🟡 Nếu người dùng đổi loại event
        if (!existing.getEventType().equals(request.getEventType())) {
            log.info("Changing event type from {} → {}", existing.getEventType(), request.getEventType());
            // Xóa bản cũ hoàn toàn khỏi DB (flush ngay)
            eventRepo.deleteById(oldId);
            eventRepo.flush();
            entityManager.clear();

            // Tạo instance mới theo loại event mới
            event = switch (request.getEventType()) {
                case MUSIC -> new MusicEvent();
                case WORKSHOP -> new WorkshopEvent();
                case FESTIVAL -> new FestivalEvent();
                case COMPETITION -> new CompetitionEvent();
                default -> new Event(); // fallback an toàn
            };

            // Giữ nguyên id & quan hệ
            event.setEventType(request.getEventType());
            event.setOrganization(existing.getOrganization());
            event.setHost(existing.getHost());
            event.setParentEvent(existing.getParentEvent());
            event.setVersion(0L); // reset version cho bản ghi mới

        }
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


        // 🟢 Map subclass-specific fields
        if (event instanceof MusicEvent musicEvent && request instanceof MusicEventUpdateRequest musicReq) {
            musicEvent.setMusicType(musicReq.getMusicType());
            musicEvent.setGenre(musicReq.getGenre());
            musicEvent.setPerformerCount(musicReq.getPerformerCount());
        } else if (event instanceof WorkshopEvent workshopEvent && request instanceof WorkshopEventUpdateRequest workReq) {
            workshopEvent.setMaterialsLink(workReq.getMaterialsLink());
            workshopEvent.setPrerequisites(workReq.getPrerequisites());
            workshopEvent.setMaxParticipants(workReq.getMaxParticipants());
            workshopEvent.setSkillLevel(workReq.getSkillLevel());
            workshopEvent.setTopic(workReq.getTopic());
        } else if (event instanceof FestivalEvent festivalEvent && request instanceof FestivalEventUpdateRequest festReq) {
            festivalEvent.setCulture(festReq.getCulture());
            festivalEvent.setHighlight(festReq.getHighlight());
        } else if (event instanceof CompetitionEvent competitionEvent && request instanceof CompetitionEventUpdateRequest comReq) {
            competitionEvent.setCompetitionType(comReq.getCompetitionType());
            competitionEvent.setRules(comReq.getRules());
            competitionEvent.setPrizePool(comReq.getPrizePool());
        }

        // 🟢 Speakers
        if (request.getSpeakers() != null && !request.getSpeakers().isEmpty()) {
            List<Speaker> speakers = new ArrayList<>();
            for (SpeakerRequest s : request.getSpeakers()) {
                Speaker sp = new Speaker();
                sp.setName(s.getName());
                sp.setProfile(s.getProfile());
                sp.setImageUrl(s.getImageUrl());
                sp.setDefaultRole(s.getDefaultRole());
                sp.setEvents(List.of(event));
                speakers.add(sp);
            }
            event.setSpeakers(speakers);
        }

        // ✅ Save cuối cùng
        Event saved = eventRepo.saveAndFlush(event);
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
    public List<Event> getEventByHostId(Long id) {
        return eventRepo.getEventByHostId(id);
    }

    @Override
    public Event getEventResponseById(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
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

