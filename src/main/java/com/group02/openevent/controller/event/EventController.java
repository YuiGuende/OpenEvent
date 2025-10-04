package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.*;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IImageService;
import com.group02.openevent.service.impl.CloudinaryServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    private final IImageService imageService;

    @Autowired
    public EventController(EventService eventService, IImageService imageService) {
        this.eventService = eventService;
        this.imageService = imageService;
    }

    @Autowired
    private ObjectMapper objectMapper;


    // POST - create event
    @PostMapping("save/music")
    public MusicEvent saveMusic(@RequestBody MusicEvent event) {
        return eventService.saveMusicEvent(event);
    }

    @PostMapping("save/festival")
    public FestivalEvent saveFestival(@RequestBody FestivalEvent event) {
        return eventService.saveFestivalEvent(event);
    }

    @PostMapping("save/competition")
    public CompetitionEvent saveCompetition(@RequestBody CompetitionEvent event) {
        return eventService.saveCompetitionEvent(event);
    }



    @PostMapping("save/workshop")
    public WorkshopEvent saveWorkshop(@RequestBody WorkshopEvent event) {
        return eventService.saveWorkshopEvent(event);
    }


    // GET - get event by id
    @GetMapping("/{id}")
    public Optional<Event> getEvent(@PathVariable Integer id) {
        return eventService.getEventById(id);
    }

    // GET - get event by type
    @GetMapping("/type/{type}")
    public List<Event> getEventsByType(@PathVariable String type) {
        Class<? extends Event> eventType = getEventTypeClass(type);
        return eventService.getEventsByType(eventType);
    }

    @GetMapping("/getAll")
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
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
            case "CONFERENCE":
                return Event.class;
            case "OTHERS":
                return Event.class;
            default:
                throw new IllegalArgumentException("Unknown event type: " + type);
        }
    }

    @PostMapping("/upload/image")
    public ResponseEntity<Event> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("index") int index,
            @RequestParam("mainPoster") boolean mainPoster,
            @RequestParam("eventId") Integer eventId) throws IOException {
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
}
