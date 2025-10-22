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
import java.util.List;
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

    @Autowired
    public EventController(EventService eventService, IImageService imageService, ITicketTypeRepo ticketTypeRepo) {
        this.eventService = eventService;
        this.imageService = imageService;
        this.ticketTypeRepo = ticketTypeRepo;
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
    public String createEvent(@ModelAttribute("eventForm") EventCreationRequest request, Model model) {
        log.info("startsAt = {}", request.getStartsAt());
        log.info("endsAt = {}", request.getEndsAt());
        EventResponse savedEvent =  eventService.saveEvent(request);
        log.info(String.valueOf(savedEvent.getEventType()));
        return "redirect:/manage/event/" + savedEvent.getId()+ "/getting-stared";
    }

    // GET - get event by id
    @GetMapping("/{id}")
    @ResponseBody
    public Optional<Event> getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    @PostMapping("/update/{id}")
    public String updateEvent(@PathVariable("id") Long id,
                             @ModelAttribute EventUpdateRequest request,
                             @RequestParam(value = "placesJson", required = false) String placesJson,
                             @RequestParam(value = "ticketsJson", required = false) String ticketsJson,
                             Model model){
        log.info("🔍 EventController: updateEvent called with ID: {}", id);
        log.info("🔍 EventController: Event Type: {}", request.getEventType());
        log.info("🔍 EventController: placesJson received: {}", placesJson);
        log.info("🔍 EventController: ticketsJson received: {}", ticketsJson);

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

                // Store the original PlaceUpdateRequest list for service layer to process deletions
                request.setPlaces(places);
                // Also store the original requests to handle deletions properly
                request.setPlaceUpdateRequests(placeRequests);
                log.info("Parsed {} places from JSON (including deleted places)", places.size());
            }

            // Process tickets from JSON if provided
            if (ticketsJson != null && !ticketsJson.trim().isEmpty()) {
                log.info("Processing tickets JSON: {}", ticketsJson);
                ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                List<TicketUpdateRequest> ticketRequests = objectMapper.readValue(ticketsJson, new TypeReference<List<TicketUpdateRequest>>() {});

                // Process each ticket
                for (TicketUpdateRequest ticketRequest : ticketRequests) {
                    if (Boolean.TRUE.equals(ticketRequest.getIsDeleted())) {
                        // Delete ticket
                        if (ticketRequest.getTicketTypeId() != null) {
                            ticketTypeRepo.deleteById(ticketRequest.getTicketTypeId());
                            log.info("Deleted ticket with ID: {}", ticketRequest.getTicketTypeId());
                        }
                    } else if (Boolean.TRUE.equals(ticketRequest.getIsNew())) {
                        // Create new ticket
                        TicketType newTicket = new TicketType();
                        newTicket.setName(ticketRequest.getName());
                        newTicket.setDescription(ticketRequest.getDescription());
                        newTicket.setPrice(ticketRequest.getPrice());
                        newTicket.setTotalQuantity(ticketRequest.getTotalQuantity());
                        newTicket.setSoldQuantity(ticketRequest.getSoldQuantity() != null ? ticketRequest.getSoldQuantity() : 0);
                        newTicket.setStartSaleDate(ticketRequest.getStartSaleDate());
                        newTicket.setEndSaleDate(ticketRequest.getEndSaleDate());
                        newTicket.setSale(ticketRequest.getSale());

                        // Set event (you'll need to get the event)
                        Event event = eventService.getEventById(id).orElseThrow(() -> new RuntimeException("Event not found"));
                        newTicket.setEvent(event);

                        ticketTypeRepo.save(newTicket);
                        log.info("Created new ticket: {}", newTicket.getName());
                    } else {
                        // Update existing ticket
                        if (ticketRequest.getTicketTypeId() != null) {
                            TicketType existingTicket = ticketTypeRepo.findById(ticketRequest.getTicketTypeId())
                                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

                            existingTicket.setName(ticketRequest.getName());
                            existingTicket.setDescription(ticketRequest.getDescription());
                            existingTicket.setPrice(ticketRequest.getPrice());
                            existingTicket.setTotalQuantity(ticketRequest.getTotalQuantity());
                            existingTicket.setSoldQuantity(ticketRequest.getSoldQuantity() != null ? ticketRequest.getSoldQuantity() : 0);
                            existingTicket.setStartSaleDate(ticketRequest.getStartSaleDate());
                            existingTicket.setEndSaleDate(ticketRequest.getEndSaleDate());
                            existingTicket.setSale(ticketRequest.getSale());

                            ticketTypeRepo.save(existingTicket);
                            log.info("Updated ticket with ID: {}", ticketRequest.getTicketTypeId());
                        }
                    }
                }

                log.info("Processed {} tickets from JSON", ticketRequests.size());
            }

        log.info("🔍 EventController: About to call eventService.updateEvent()");
        EventResponse updated = eventService.updateEvent(id, request);
        log.info("🔍 EventController: eventService.updateEvent() completed");
        log.info("Event updated successfully with type: {}", updated.getEventType());

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
                ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                List<TicketUpdateRequest> ticketRequests = objectMapper.readValue(ticketsJson, new TypeReference<List<TicketUpdateRequest>>() {});

                for (TicketUpdateRequest ticketRequest : ticketRequests) {
                    if (Boolean.TRUE.equals(ticketRequest.getIsDeleted())) {
                        if (ticketRequest.getTicketTypeId() != null) {
                            ticketTypeRepo.deleteById(ticketRequest.getTicketTypeId());
                        }
                    } else if (Boolean.TRUE.equals(ticketRequest.getIsNew())) {
                        TicketType newTicket = new TicketType();
                        newTicket.setName(ticketRequest.getName());
                        newTicket.setDescription(ticketRequest.getDescription());
                        newTicket.setPrice(ticketRequest.getPrice());
                        newTicket.setTotalQuantity(ticketRequest.getTotalQuantity());
                        newTicket.setSoldQuantity(ticketRequest.getSoldQuantity() != null ? ticketRequest.getSoldQuantity() : 0);
                        newTicket.setStartSaleDate(ticketRequest.getStartSaleDate());
                        newTicket.setEndSaleDate(ticketRequest.getEndSaleDate());
                        newTicket.setSale(ticketRequest.getSale());
                        Event event = eventService.getEventById(id).orElseThrow(() -> new RuntimeException("Event not found"));
                        newTicket.setEvent(event);
                        ticketTypeRepo.save(newTicket);
                    } else {
                        if (ticketRequest.getTicketTypeId() != null) {
                            TicketType existingTicket = ticketTypeRepo.findById(ticketRequest.getTicketTypeId())
                                    .orElseThrow(() -> new RuntimeException("Ticket not found"));
                            existingTicket.setName(ticketRequest.getName());
                            existingTicket.setDescription(ticketRequest.getDescription());
                            existingTicket.setPrice(ticketRequest.getPrice());
                            existingTicket.setTotalQuantity(ticketRequest.getTotalQuantity());
                            existingTicket.setSoldQuantity(ticketRequest.getSoldQuantity() != null ? ticketRequest.getSoldQuantity() : 0);
                            existingTicket.setStartSaleDate(ticketRequest.getStartSaleDate());
                            existingTicket.setEndSaleDate(ticketRequest.getEndSaleDate());
                            existingTicket.setSale(ticketRequest.getSale());
                            ticketTypeRepo.save(existingTicket);
                        }
                    }
                }
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

    @PostMapping("/upload/image")
    @ResponseBody
    public ResponseEntity<Event> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("index") int index,
            @RequestParam("mainPoster") boolean mainPoster,
            @RequestParam("eventId") Long eventId) throws IOException {
        String url = imageService.saveImage(file);
        Optional<Event> optionalEvent = eventService.getEventById(eventId);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            EventImage eventImage = new EventImage(url, index, mainPoster, event);

            optionalEvent.get().getEventImages().add(eventImage);


            return ResponseEntity.ok(eventService.saveEvent(event));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/upload/multiple-images")
    @ResponseBody
    public ResponseEntity<Event> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("orderIndexes") int[] orderIndexes,
            @RequestParam("mainPosters") boolean[] mainPosters,
            @RequestParam("eventId") Long eventId) throws IOException {

        Optional<Event> optionalEvent = eventService.getEventById(eventId);
        if (!optionalEvent.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Event event = optionalEvent.get();

        // Validate arrays length
        if (files.length != orderIndexes.length || files.length != mainPosters.length) {
            return ResponseEntity.badRequest().build();
        }

        // Process each file
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            int orderIndex = orderIndexes[i];
            boolean mainPoster = mainPosters[i];

            // Validate file
            if (file.isEmpty()) continue;

            // Save image and get URL
            String url = imageService.saveImage(file);

            // Create EventImage
            EventImage eventImage = new EventImage(url, orderIndex, mainPoster, event);
            event.getEventImages().add(eventImage);
        }

        // Save event with new images
        Event savedEvent = eventService.saveEvent(event);
        return ResponseEntity.ok(savedEvent);
    }

    @PostMapping("/upload/images-batch")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> uploadImagesBatch(
            @RequestParam("eventId") Long eventId,
            @RequestParam("images") String imagesJson) {

        try {
            // Parse images data from JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> imagesData =
                    mapper.readValue(imagesJson, java.util.List.class);

            Optional<Event> optionalEvent = eventService.getEventById(eventId);
            if (!optionalEvent.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Event event = optionalEvent.get();

            // Process each image data
            for (java.util.Map<String, Object> imageData : imagesData) {
                // This would need to be implemented based on your image service
                // For now, we'll create a placeholder
                String url = "placeholder_url_" + System.currentTimeMillis();
                int orderIndex = (Integer) imageData.get("orderIndex");
                boolean mainPoster = (Boolean) imageData.get("mainPoster");

                EventImage eventImage = new EventImage(url, orderIndex, mainPoster, event);
                event.getEventImages().add(eventImage);
            }

            // Save event
            eventService.saveEvent(event);

            ApiResponse<String> response = new ApiResponse<>();
            response.setResult("Images uploaded successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading images batch", e);
            ApiResponse<String> response = new ApiResponse<>();

            return ResponseEntity.badRequest().body(response);
        }
    }

    // Delete a single ticket type by id (direct hard delete)
    @DeleteMapping("/ticket/{ticketTypeId}")
    @ResponseBody
    public ApiResponse<Void> deleteTicket(@PathVariable("ticketTypeId") Long ticketTypeId) {
        try {
            ticketTypeRepo.deleteById(ticketTypeId);
            return ApiResponse.<Void>builder().message("Ticket deleted").build();
        } catch (Exception e) {
            log.error("Error deleting ticket {}", ticketTypeId, e);
            return ApiResponse.<Void>builder().message("Error: " + e.getMessage()).build();
        }
    }

}
