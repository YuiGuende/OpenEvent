package com.group02.openevent.controller.admin;


import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private EventService eventService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Add dashboard statistics
        model.addAttribute("totalEvents", eventService.countTotalEvents());
        model.addAttribute("draftEvents", eventService.countEventsByStatus(EventStatus.DRAFT));
        model.addAttribute("publicEvents", eventService.countEventsByStatus(EventStatus.PUBLIC));
        model.addAttribute("ongoingEvents", eventService.countEventsByStatus(EventStatus.ONGOING));
        return "admin/dashboard";
    }

    @GetMapping("/events")
    public String listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) EventStatus status,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Event> events = eventService.listEvents(eventType, status, pageable);

        model.addAttribute("events", events);
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("eventStatuses", EventStatus.values());
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "admin/events";
    }

    @PostMapping("/events/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveEvent(@PathVariable Long id) {
        try {
            Event approvedEvent = eventService.approveEvent(id);
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Event approved successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/events/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateEventStatus(@PathVariable Long id, @RequestParam EventStatus status) {
        try {
            Event updatedEvent = eventService.updateEventStatus(id, status);
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Event status updated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "admin/notifications";
    }

    @GetMapping("/orders")
    public String orders() {
        return "admin/orders";
    }

    @GetMapping("/reports")
    public String reports() {
        return "admin/reports";
    }

    @GetMapping("/articles")
    public String articles() {
        return "admin/articles";
    }

    @GetMapping("/requests")
    public String requests() {
        return "admin/requests";
    }
}
