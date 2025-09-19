package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.*;
import com.group02.openevent.service.EventService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // POST - create event
    @PostMapping("/saveEvent")
    public Event createEvent(@RequestBody Map<String, Object> body) {
        Object typeObj = body.get("eventType");
        if (typeObj == null) {
            throw new IllegalArgumentException("Missing eventType in request body");
        }
        String type = String.valueOf(typeObj);
        switch (type) {
            case "MUSIC":
                return eventService.saveEvent(objectMapper.convertValue(body, MusicEvent.class));
            case "WORKSHOP":
                return eventService.saveEvent(objectMapper.convertValue(body, WorkshopEvent.class));
            case "FESTIVAL":
                return eventService.saveEvent(objectMapper.convertValue(body, FestivalEvent.class));
            case "COMPETITION":
                return eventService.saveEvent(objectMapper.convertValue(body, CompetitionEvent.class));
            default:
                throw new IllegalArgumentException("Unknown eventType: " + type + ". Valid: MUSIC, WORKSHOP, FESTIVAL, COMPETITION");
        }
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
}
