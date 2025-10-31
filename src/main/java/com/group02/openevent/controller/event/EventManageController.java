package com.group02.openevent.controller.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.*;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.TicketTypeService;
import com.group02.openevent.service.impl.CustomerServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import com.group02.openevent.service.IPlaceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventManageController {
    EventService eventService;
    IPlaceService placeService;
    ISpeakerRepo speakerRepo;
    IEventScheduleRepo scheduleRepo;
    IEventImageRepo imageRepo;
    ITicketTypeRepo ticketTypeRepo;
    EventMapper eventMapper;
    CustomerServiceImpl customerService;
    private Long getCustomerAccountId(HttpSession session) {
//        (Long) session.getAttribute("ACCOUNT_ID");
        Long accountId = Long.parseLong("2");
        if (accountId == null) {
            throw new RuntimeException("User not logged in");
        }
        return accountId;
    }

    @RequestMapping(value = "/manage/event/{eventId:\\d+}/{path:[^\\.]*}")
    public String showManagerPage(@PathVariable Long eventId, Model model) throws JsonProcessingException {

        Event event = eventService.getEventResponseById(eventId);

        model.addAttribute("event", event);

        model.addAttribute("content", "fragments/getting-started :: content");

        List<Speaker> speakersList = speakerRepo.findSpeakerByEventId(eventId);
        List<EventSchedule> schedulesList = scheduleRepo.findByEventId(eventId);
        List<EventImage> imagesList = imageRepo.findByEventId(eventId);
        List<TicketType> ticketTypesList = ticketTypeRepo.findByEventId(eventId);
        model.addAttribute("allPlaces", placeService.getAllByEventId(eventId));
        model.addAttribute("allTicketTypes", ticketTypesList);
        model.addAttribute("speakersData", speakersList);
        model.addAttribute("schedulesData", schedulesList);
        model.addAttribute("imagesData", imagesList);

        return "host/manager-event";
    }

    @GetMapping("/host/manager-event")
    public String showMainLayout(Model model) {

        model.addAttribute("content", "fragments/getting-started :: content");

        return "host/manager-event";
    }

    @GetMapping("/fragments/update-event")
    public String updateEvent(@RequestParam Long id, Model model) throws JsonProcessingException {
        log.info("🔍 Loading update-event fragment for event ID: {}", id);

        Event event = eventService.getEventResponseById(id);
        EventUpdateRequest request = eventMapper.toUpdateRequest(event);
        model.addAttribute("request", request);

        // Load places
        List<Place> allPlaces = placeService.getAllByEventId(id);
        log.info("📋 Places loaded for event {}: {}", id, allPlaces);
        log.info("📋 Places count: {}", allPlaces.size());
        model.addAttribute("allPlaces", allPlaces);

        // Load tickets
        List<TicketType> allTicketTypes = ticketTypeRepo.findByEventId(id);
        log.info("🎫 Tickets loaded for event {}: {}", id, allTicketTypes);
        log.info("🎫 Tickets count: {}", allTicketTypes.size());
        model.addAttribute("allTicketTypes", allTicketTypes);

        model.addAttribute("eventTypes", event.getClass().getSimpleName());

        // Add speakers, schedules, and images data for the fragment
        List<Speaker> speakersList = speakerRepo.findSpeakerByEventId(id);
        List<EventSchedule> schedulesList = scheduleRepo.findByEventId(id);
        List<EventImage> imagesList = imageRepo.findByEventId(id);
        model.addAttribute("speakersData", speakersList);
        model.addAttribute("schedulesData", schedulesList);
        model.addAttribute("imagesData", imagesList);

        log.info("✅ All data loaded for update-event fragment");
        return "fragments/update-event :: content";
    }

    @GetMapping("/fragments/getting-started")
    public String gettingStared(@RequestParam Long id, Model model) {
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);
        model.addAttribute("eventId", event.getId());
        return "fragments/getting-started :: content";
    }

    @GetMapping("/fragments/ticket")
    public String ticket(@RequestParam Long id, Model model) {
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);

        // Load tickets for the ticket fragment
        List<TicketType> allTicketTypes = ticketTypeRepo.findByEventId(id);
        model.addAttribute("allTicketTypes", allTicketTypes);

        return "fragments/update-ticket :: content";
    }

    //    @GetMapping("/fragments/dashboard")
//    public String dashboard(Model model) {
//        return "fragments/dashboard :: content";
//    }
//
//    @GetMapping("/fragments/reports")
//    public String reports(Model model) {
//        return "fragments/reports :: content";
//    }
//
//    @GetMapping("/fragments/attendees")
//    public String attendees(Model model) {
//        return "fragments/attendees :: content";
//    }
//
    @GetMapping("/fragments/orders")
    public String orders(@RequestParam Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Get orders for the specific event
        Page<OrderDTO> orders = customerService.getOrdersByEvent(id, status, pageable);

        model.addAttribute("orders", orders);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);

        return "fragments/orders :: content";
    }


    @GetMapping("/fragments/check-in")
    public String checkIn(@RequestParam Long id, Model model) {
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);
        return "fragments/check-in :: content";
    }

    @GetMapping("/fragments/dashboard-event")
    public String dashboard(@RequestParam Long id, Model model) {
        log.info("Loading dashboard fragment for event ID: {}", id);
        
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);

        // Load ticket types for dashboard
        List<TicketType> allTicketTypes = ticketTypeRepo.findByEventId(id);
        model.addAttribute("allTicketTypes", allTicketTypes);

        log.info("Dashboard fragment loaded for event {} with {} ticket types", id, allTicketTypes.size());
        return "fragments/dashboard-event :: content";
    }




}
