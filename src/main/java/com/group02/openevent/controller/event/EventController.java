package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // POST - create event
    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return eventService.saveEvent(event);
    }

    // GET - get event by id
    @GetMapping("/{id}")
    public Optional<Event> getEvent(@PathVariable Integer id) {
        return eventService.getEventById(id);
    }
}
