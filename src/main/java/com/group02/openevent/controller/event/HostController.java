package com.group02.openevent.controller.event;

import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.requestApproveEvent.RequestDTO;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.service.*;
import com.group02.openevent.service.DepartmentService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IImageService;
import com.group02.openevent.service.RequestService;
import com.group02.openevent.service.impl.HostServiceImpl;
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
    private final UserService userService;
    private final RequestService requestService;

    @Autowired
    public HostController(EventService eventService, IImageService imageService, HostService hostService, CustomerService customerService, UserService userService, RequestService requestService) {
        this.eventService = eventService;
        this.imageService = imageService;
        this.hostService = hostService;
        this.customerService = customerService;
        this.userService = userService;
        this.requestService = requestService;
    }

    @GetMapping("/organizer")
    public String host(Model model) {
        model.addAttribute("content", "fragments/dashboard :: content");
        return "host/host";
    }

    // Khi người dùng gõ /dashboard, /events, /requests, /settings trực tiếp — ta vẫn trả về host layout
    @GetMapping({"/dashboard", "/events", "/requests", "/settings"})
    public String directAccess(Model model) {
        // Mặc định load dashboard (JS sẽ tự fetch fragment đúng sau)
        model.addAttribute("content", "fragments/dashboard :: content");
        return "host/host";
    }

    @GetMapping("/fragment/dashboard")
    public String dashboard(Model model, HttpSession session) {
        try {
            Long hostId = hostService.getHostFromSession(session).getId();
            List<Event> eventResponses = eventService.getEventByHostId(hostId);
            model.addAttribute("events", eventResponses);
            log.info("Events: " + eventResponses);
            java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();
            model.addAttribute("currentTime", currentTime);
            return "fragments/dashboard :: content";
        } catch (RuntimeException e) {
            log.error("Error loading dashboard fragment: {}", e.getMessage(), e);
            model.addAttribute("error", "Không thể tải nội dung: " + e.getMessage());
            return "fragments/dashboard :: content";
        }
    }

    @GetMapping("/fragment/events")
    public String events(
            Model model,
            HttpSession session,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortFilter
    ) {
        Long hostId = hostService.getHostFromSession(session).getId();
        log.info("hostId={}, search={}, sortFilter={}", hostId, search, sortFilter);

        EventCreationRequest request = new EventCreationRequest();
        model.addAttribute("eventForm", request);

        // Get all events for host
        List<Event> allEvents = eventService.getEventByHostId(hostId);
        java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();

        // Filter events based on search and time-based filter
        List<Event> filteredEvents = allEvents.stream()
                .filter(event -> {
                    // Search filter: filter by title (case-insensitive)
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.trim().toLowerCase();
                        String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
                        if (!title.contains(searchLower)) {
                            return false;
                        }
                    }

                    // Time-based filter: Compare with current time (real-time status)
                    if (sortFilter != null && !sortFilter.trim().isEmpty() && !"OLDEST_FIRST".equals(sortFilter.toUpperCase())) {
                        // CANCEL status: only show if filter is not time-based
                        if (event.getStatus() == EventStatus.CANCEL) {
                            // CANCEL events don't match time-based filters
                            return false;
                        }

                        // FINISH status: only show in FINISHED filter
                        if (event.getStatus() == EventStatus.FINISH) {
                            return "FINISHED".equals(sortFilter.toUpperCase());
                        }

                        // For other statuses (DRAFT, PUBLIC, ONGOING): check time-based status
                        if (event.getStartsAt() != null && event.getEndsAt() != null) {
                            switch (sortFilter.toUpperCase()) {
                                case "ONGOING":
                                    // Event is ongoing: started but not ended
                                    boolean isOngoing = (event.getStartsAt().isBefore(currentTime) || event.getStartsAt().isEqual(currentTime))
                                                     && (event.getEndsAt().isAfter(currentTime) || event.getEndsAt().isEqual(currentTime));
                                    return isOngoing;
                                case "UPCOMING":
                                    // Event is upcoming: starts in the future
                                    return event.getStartsAt().isAfter(currentTime);
                                case "FINISHED":
                                    // Event is finished: ended in the past
                                    return event.getEndsAt().isBefore(currentTime);
                            }
                        } else {
                            // If no time information, don't match time-based filters
                            return false;
                        }
                    }

                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // Sort events
        if (sortFilter != null && "OLDEST_FIRST".equals(sortFilter.toUpperCase())) {
            filteredEvents.sort((e1, e2) -> {
                if (e1.getCreatedAt() == null && e2.getCreatedAt() == null) return 0;
                if (e1.getCreatedAt() == null) return 1;
                if (e2.getCreatedAt() == null) return -1;
                return e1.getCreatedAt().compareTo(e2.getCreatedAt());
            });
        } else {
            // Default: newest first
            filteredEvents.sort((e1, e2) -> {
                if (e1.getCreatedAt() == null && e2.getCreatedAt() == null) return 0;
                if (e1.getCreatedAt() == null) return 1;
                if (e2.getCreatedAt() == null) return -1;
                return e2.getCreatedAt().compareTo(e1.getCreatedAt());
            });
        }

        List<EventType> listTypeEvent = Arrays.asList(EventType.MUSIC, EventType.FESTIVAL, EventType.WORKSHOP, EventType.COMPETITION, EventType.OTHERS);
        model.addAttribute("listTypeEvent", listTypeEvent);
        model.addAttribute("events", filteredEvents);

        // Add current time for status calculation
        model.addAttribute("currentTime", currentTime);

        // Preserve filter values in model for UI
        model.addAttribute("searchValue", search != null ? search : "");
        model.addAttribute("sortFilterValue", sortFilter != null ? sortFilter : "");

        return "fragments/events :: content";
    }

    @GetMapping("/fragment/settings")
    public String settings(Model model) {
        return "fragments/settings :: content";
    }

    @GetMapping("/fragment/wallet")
    public String wallet(Model model) {
        return "fragments/wallet :: content";
    }

    @GetMapping("/wallet")
    public String walletDirect(Model model) {
        model.addAttribute("content", "fragments/wallet :: content");
        return "host/host";
    }

    @GetMapping("/fragment/sent-requests")
    public String sentRequests(Model model, HttpSession session) {
        try {
            Long hostId = hostService.getHostFromSession(session).getId();
            log.info("Loading sent requests for hostId={}", hostId);

            // Get requests by hostId
            List<RequestDTO> sentRequests = requestService.getRequestsByHostId(hostId);

            model.addAttribute("sentRequests", sentRequests);
            log.info("Loaded {} sent requests for host {}", sentRequests.size(), hostId);
        } catch (Exception e) {
            log.error("Error loading sent requests: {}", e.getMessage(), e);
            model.addAttribute("sentRequests", new java.util.ArrayList<>());
        }

        return "fragments/sent-requests :: content";
    }

    @GetMapping("/requests")
    public String requestsDirect(Model model) {
        model.addAttribute("content", "fragments/sent-requests :: content");
        return "host/host";
    }


//    @GetMapping("/event/manage/{id}")
//    public String manageEvent(@PathVariable Long id, Model model) {
//        Event event = eventService.getEventById(id)
//                .orElseThrow(() -> new RuntimeException("Event not found"));
//        model.addAttribute("event", event);
//        return "manager-event"; // -> host/manage-event.html
//    }

}
