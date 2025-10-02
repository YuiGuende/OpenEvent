package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.request.EventCreationRequest;
import com.group02.openevent.model.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/events")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/event")
    public String userRegistration(Model model) {
        //Empty Userform model object to store from data
        log.info("da vao ham nay");
        EventCreationRequest request = new EventCreationRequest();
        model.addAttribute("eventForm", request);
        List<String> listTypeEvent = Arrays.asList("MUSIC", "FESTIVAL", "WORKSHOP","COMPETITION","OTHERS");
        model.addAttribute("listTypeEvent", listTypeEvent);
        return "host/events";
    }

    // POST - create event
    @PostMapping("/saveEvent")
    public String createEvent(RedirectAttributes redirectAttributes,
                              @ModelAttribute("eventForm") EventCreationRequest request) {
        log.info("Controller Create User");
         EventResponse savedEvent =  eventService.saveEvent(request);
        return "redirect:/manage/" + savedEvent.getId();
    }

    // Màn hình quản lý / chỉnh sửa event
    @GetMapping("/manage/{id}")
    public String manageEvent(@PathVariable Long id, Model model) {
        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return "host/manage-event"; // -> host/manage-event.html
    }

    // GET - get event by id
    @GetMapping("/{id}")
    public Optional<Event> getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }
}
