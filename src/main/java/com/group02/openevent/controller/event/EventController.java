package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.*;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import com.group02.openevent.dto.request.EventCreationRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
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


    // GET - get event by type
    @GetMapping("/type/{type}")
    @ResponseBody
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
}
