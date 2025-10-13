package com.group02.openevent.controller.event;

import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.ApiResponse;
import com.group02.openevent.model.event.*;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/events")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventController {


    private final EventService eventService;

    private final IImageService imageService;

    @Autowired
    public EventController(EventService eventService, IImageService imageService) {
        this.eventService = eventService;
        this.imageService = imageService;
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

    // POST - create event
//    public String createEvent(
//                                   @RequestBody EventCreationRequest request) {
//        log.info("startsAt = {}", request.getStartsAt());
//        log.info("endsAt = {}", request.getEndsAt());
//        ApiResponse<EventResponse> apiResponse = new ApiResponse<>();
//        apiResponse.setResult(eventService.saveEvent(request));
//        return apiResponse;
//    }
    @PostMapping("/saveEvent")
    public String createEvent(@ModelAttribute("eventForm") EventCreationRequest request,Model model) {
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
                                                     @ModelAttribute EventUpdateRequest request,Model model){
        log.info("Controller Update User");
        log.info(request.getSpeakers().getFirst().getName());
        EventResponse updated = eventService.updateEvent(id, request);
        model.addAttribute("updated", updated);
        model.addAttribute("message", "Cập nhật thành công!");


        return "fragments/getting-started :: content";
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
}
