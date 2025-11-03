package com.group02.openevent.controller.event;

import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

//@RequestMapping("/manage/organizer")
@Controller
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HostController {

    private final EventService eventService;
    private final IImageService imageService;
    private final HostService hostService;
    private final CustomerService customerService;

    @Autowired
    public HostController(EventService eventService, IImageService imageService, HostService hostService, CustomerService customerService) {
        this.eventService = eventService;
        this.imageService = imageService;
        this.hostService = hostService;
        this.customerService = customerService;
    }

    private Long getHostAccountId(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            throw new RuntimeException("User not logged in");
        }
        Customer customer = customerService.getCustomerByAccountId(accountId);
        Long hostId = customer.getHost().getId();
        if (hostId == null) {
            throw new RuntimeException("Host not found");
        }
        return hostId;
    }

    @GetMapping("/organizer")
    public String host(Model model) {
        model.addAttribute("content", "fragments/dashboard :: content");
        return "host/host";
    }

    // Khi người dùng gõ /dashboard, /events, /settings trực tiếp — ta vẫn trả về host layout
    @GetMapping({"/dashboard", "/events", "/settings"})
    public String directAccess(Model model) {
        // Mặc định load dashboard (JS sẽ tự fetch fragment đúng sau)
        model.addAttribute("content", "fragments/dashboard :: content");
        return "host/host";
    }

    @GetMapping("/fragment/dashboard")
    public String dashboard(Model model, HttpSession session) {
        List<Event> eventResponses = eventService.getEventByHostId(getHostAccountId(session));
        model.addAttribute("events", eventResponses);
        log.info("Events: " + eventResponses);
        return "fragments/dashboard :: content";
    }

    @GetMapping("/fragment/events")
    public String events(Model model, HttpSession session) {
        EventCreationRequest request = new EventCreationRequest();
        model.addAttribute("eventForm", request);
        List<Event> eventResponses = eventService.getEventByHostId(getHostAccountId(session));
        List<EventType> listTypeEvent = Arrays.asList(EventType.MUSIC, EventType.FESTIVAL, EventType.WORKSHOP, EventType.COMPETITION, EventType.OTHERS);
        model.addAttribute("listTypeEvent", listTypeEvent);
        model.addAttribute("events", eventResponses);
        return "fragments/events :: content";
    }

    @GetMapping("/fragment/settings")
    public String settings(Model model) {
        return "fragments/settings :: content";
    }


//    @GetMapping("/event/manage/{id}")
//    public String manageEvent(@PathVariable Long id, Model model) {
//        Event event = eventService.getEventById(id)
//                .orElseThrow(() -> new RuntimeException("Event not found"));
//        model.addAttribute("event", event);
//        return "manager-event"; // -> host/manage-event.html
//    }

}
