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
