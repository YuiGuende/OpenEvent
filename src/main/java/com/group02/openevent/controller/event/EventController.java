package com.group02.openevent.controller.event;

import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.request.PlaceUpdateRequest;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.response.ApiResponse;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.*;
import com.group02.openevent.dto.request.TicketUpdateRequest;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IImageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.repository.IEventImageRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.TicketTypeService;
import com.group02.openevent.service.impl.HostServiceImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.group02.openevent.model.event.Event;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/events")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventController {


    private final EventService eventService;

    private final IImageService imageService;

    private final ITicketTypeRepo ticketTypeRepo;

    private  final TicketTypeService ticketTypeService;

    private  final HostServiceImpl hostService;
    
    private final IEventImageRepo eventImageRepo;
    
    private final IEventRepo eventRepo;
    
    @Autowired
    public EventController(EventService eventService, IImageService imageService, ITicketTypeRepo ticketTypeRepo, TicketTypeService ticketTypeService, HostServiceImpl hostService, IEventImageRepo eventImageRepo, IEventRepo eventRepo) {
        this.eventService = eventService;
        this.imageService = imageService;
        this.ticketTypeRepo = ticketTypeRepo;
        this.ticketTypeService = ticketTypeService;
        this.hostService = hostService;
        this.eventImageRepo = eventImageRepo;
        this.eventRepo = eventRepo;
    }

    private Long getAccountIdFromSession(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            throw new RuntimeException("User not logged in");
        }
        return accountId;
    }

    // POST - create event
    @PostMapping("save/music")
    @ResponseBody
    public MusicEvent saveMusic(@RequestBody MusicEvent event) {
        return eventService.saveMusicEvent(event);
    }

    @PostMapping("save/festival")
    @ResponseBody
    public FestivalEvent saveFestival(@RequestBody FestivalEvent event) {
        return eventService.saveFestivalEvent(event);
    }

    @PostMapping("save/competition")
    @ResponseBody
    public CompetitionEvent saveCompetition(@RequestBody CompetitionEvent event) {
        return eventService.saveCompetitionEvent(event);
    }

    @PostMapping("save/workshop")
    @ResponseBody
    public WorkshopEvent saveWorkshop(@RequestBody WorkshopEvent event) {
        return eventService.saveWorkshopEvent(event);
    }

    @PostMapping("/saveEvent")
    public String createEvent(@ModelAttribute("eventForm") EventCreationRequest request, HttpSession session) {
        Long hostId = hostService.findHostIdByAccountId(getAccountIdFromSession(session));
        log.info("startsAt = {}", request.getStartsAt());
        log.info("endsAt = {}", request.getEndsAt());
        EventResponse savedEvent =  eventService.saveEvent(request,hostId);
        log.info(String.valueOf(savedEvent.getEventType()));
        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-stared";
    }

    // GET - get event by id
    @GetMapping("/{id}")
    @ResponseBody
    public Optional<Event> getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    @GetMapping("/{id}/participants/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getParticipantsCount(@PathVariable Long id) {
        try {
            long count = eventService.countUniqueParticipantsByEventId(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting participants count for event {}: {}", id, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("count", 0);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/update/{id}")
    public String updateEvent(@PathVariable("id") Long id,
                              @ModelAttribute EventUpdateRequest request,
                              @RequestParam(value = "placesJson", required = false) String placesJson,
                              @RequestParam(value = "ticketsJson", required = false) String ticketsJson,
                              Model model){

        try {
            // Process places from JSON if provided
            if (placesJson != null && !placesJson.trim().isEmpty()) {
                log.info("Processing places JSON: {}", placesJson);
                ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                List<PlaceUpdateRequest> placeRequests = objectMapper.readValue(placesJson, new TypeReference<List<PlaceUpdateRequest>>() {});

                // Convert PlaceUpdateRequest to Place entities (keep all places, including deleted ones)
                List<Place> places = placeRequests.stream()
                        .map(pr -> {
                            Place place = new Place();
                            place.setId(pr.getId());
                            place.setPlaceName(pr.getPlaceName());
                            place.setBuilding(pr.getBuilding());
                            // Store isDeleted flag in a custom field or handle it in service layer
                            return place;
                        })
                        .collect(Collectors.toList());


                request.setPlaces(places);
                request.setPlaceUpdateRequests(placeRequests);
                log.info("Parsed {} places from JSON (including deleted places)", places.size());
            }
            EventResponse updated = eventService.updateEvent(id, request);
            model.addAttribute("updated", updated);
            model.addAttribute("message", "Cập nhật thành công!");

        } catch (Exception e) {
            log.error("Error updating event: ", e);
            model.addAttribute("error", "Có lỗi xảy ra khi cập nhật sự kiện: " + e.getMessage());
        }

        return "fragments/getting-started :: content";
    }

    // Lightweight endpoint: update only tickets for an event
    @PostMapping("/update-tickets/{id}")
    @ResponseBody
    public ApiResponse<Void> updateTicketsOnly(@PathVariable("id") Long id,
                                               @RequestParam(value = "ticketsJson", required = false) String ticketsJson) {
        try {
            if (ticketsJson != null && !ticketsJson.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                List<TicketUpdateRequest> ticketRequests =
                        mapper.readValue(ticketsJson, new TypeReference<>() {});
                ticketTypeService.updateTickets(id, ticketRequests);
            }
            return ApiResponse.<Void>builder().message("Tickets updated").build();
        } catch (Exception e) {
            log.error("Error updating tickets only:", e);
            return ApiResponse.<Void>builder().message("Error: " + e.getMessage()).build();
        }
    }

    // GET - get event by type
    @GetMapping("/type/{type}")
    @ResponseBody
    public List<Event> getEventsByType(@PathVariable String type) {
        Class<? extends Event> eventType = getEventTypeClass(type);
        return eventService.getEventsByType(eventType);
    }

    private Class<? extends Event> getEventTypeClass(String type) {
        switch (type.toUpperCase()) {
            case "MUSIC":
                return MusicEvent.class;
            case "WORKSHOP":
                return WorkshopEvent.class;
            case "FESTIVAL":
                return FestivalEvent.class;
            case "COMPETITION":
                return CompetitionEvent.class;
            case "OTHERS":
                return Event.class;
            default:
                throw new IllegalArgumentException("Unknown event type: " + type);
        }
    }

    @PostMapping("/upload/multiple-images")
    @ResponseBody
    public ResponseEntity<?> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("orderIndexes") int[] orderIndexes,
            @RequestParam("mainPosters") boolean[] mainPosters,
            @RequestParam("eventId") Long eventId) {
        try {
            Optional<Event> optionalEvent = eventService.getEventById(eventId);
            if (!optionalEvent.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Event not found"));
            }

            Event event = optionalEvent.get();

            // Validate arrays length
            if (files.length != orderIndexes.length || files.length != mainPosters.length) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid input: arrays have mismatched lengths"));
            }

            if (event.getEventImages() == null)
                event.setEventImages(new HashSet<>()); // tránh NullPointerException

            // Process each file
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                int orderIndex = orderIndexes[i];
                boolean mainPoster = mainPosters[i];

                if (file.isEmpty()) continue;

                String url = imageService.saveImage(file);
                EventImage eventImage = new EventImage(url, orderIndex, mainPoster, event);
                event.getEventImages().add(eventImage);
            }

            Event savedEvent = eventService.saveEvent(event);
            
            // Update event imageUrl from poster with mainPoster = true and orderIndex = 0
            updateEventImageUrlFromPoster(eventId);
            
            return ResponseEntity.ok(savedEvent);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }




    // Delete a single ticket type by id (direct hard delete)
    @DeleteMapping("/ticket/{ticketTypeId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable("ticketTypeId") Long ticketTypeId) {
        try {
            ticketTypeService.deleteTicketType(ticketTypeId);
            ApiResponse<Void> ok = ApiResponse.<Void>builder().message("Đã xóa vé").build();
            return org.springframework.http.ResponseEntity.ok(ok);
        } catch (IllegalStateException ex) {
            // Vi phạm nghiệp vụ (ví dụ: đã có order tham chiếu)
            ApiResponse<Void> bad = ApiResponse.<Void>builder().message(ex.getMessage()).build();
            return org.springframework.http.ResponseEntity.badRequest().body(bad);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Vi phạm ràng buộc FK ở DB
            ApiResponse<Void> bad = ApiResponse.<Void>builder()
                    .message("Không thể xóa vé vì đã có đơn hàng tham chiếu")
                    .build();
            return org.springframework.http.ResponseEntity.badRequest().body(bad);
        } catch (Exception e) {
            log.error("Error deleting ticket {}", ticketTypeId, e);
            ApiResponse<Void> err = ApiResponse.<Void>builder().message("Lỗi hệ thống khi xóa vé").build();
            return org.springframework.http.ResponseEntity.status(500).body(err);
        }
    }

    /**
     * Update event imageUrl from the poster with mainPoster = true and orderIndex = 0
     */
    private void updateEventImageUrlFromPoster(Long eventId) {
        log.info("Updating event imageUrl for eventId: {}", eventId);
        
        // Find all main posters for this event
        List<EventImage> mainPosters = eventImageRepo.findByEventIdAndMainPoster(eventId, true);
        
        // Find the poster with mainPoster = true and orderIndex = 0
        EventImage mainPoster = mainPosters.stream()
                .filter(img -> img.isMainPoster() && img.getOrderIndex() == 0)
                .findFirst()
                .orElse(null);
        
        if (mainPoster != null) {
            Optional<Event> eventOpt = eventRepo.findById(eventId);
            if (eventOpt.isPresent()) {
                Event event = eventOpt.get();
                event.setImageUrl(mainPoster.getUrl());
                eventRepo.save(event);
                log.info("Updated event imageUrl to: {} for eventId: {}", mainPoster.getUrl(), eventId);
            } else {
                log.warn("Event not found with id: {}", eventId);
            }
        } else {
            log.info("No main poster with orderIndex = 0 found for eventId: {}", eventId);
        }
    }

}