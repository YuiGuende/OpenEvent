package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.home.TopStudentDTO;
import com.group02.openevent.dto.user.UserOrderDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.TopStudentService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class HomeController {
    private final IAccountRepo accountRepo;
    private final ICustomerRepo customerRepo;
    private final IEventRepo eventRepo;
    private final IOrderRepo orderRepo;
    private final IUserRepo userRepo;
    private final UserService userService;
    private final HostService hostService;
    @Autowired
    private EventService eventService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TopStudentService topStudentService;

    public HomeController(IAccountRepo accountRepo, ICustomerRepo customerRepo, IEventRepo eventRepo, IOrderRepo orderRepo, IUserRepo userRepo, UserService userService, HostService hostService) {
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.eventRepo = eventRepo;
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.hostService = hostService;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        try {

            // Get poster events for hero slider
            List<EventCardDTO> posterEvents = eventService.getPosterEvents();
            model.addAttribute("posterEvents", posterEvents != null ? posterEvents : List.of());

            // Get live events (15 events, only ONGOING status)
            List<EventCardDTO> liveEvents = eventService.getLiveEvents(15);
            model.addAttribute("liveEvents", liveEvents != null ? liveEvents : List.of());

            // Get latest events (15 events, sorted by newest first)
            List<EventCardDTO> latestEvents = eventService.getRecentEvents(15).stream()
                    .map(eventService::convertToDTO)
                    .collect(Collectors.toList());
            model.addAttribute("latestEvents", latestEvents);

            // Get your events from user's orders - fetch complete event data from DB
            List<EventCardDTO> myEvents = List.of();
            try {
                // Kiểm tra user đã đăng nhập chưa
                Long userId = (Long) session.getAttribute("USER_ID");
                if (userId != null) {
                    User user = userService.getUserById(userId);
                    Customer customer = user.getCustomer();
                    
                    if (customer != null) {
                        // Get user's orders to find event IDs
                        List<UserOrderDTO> userOrders = orderService.getOrderDTOsByCustomer(customer, null);
                        
                        // Extract unique event IDs from orders
                        List<Long> eventIds = userOrders.stream()
                                .filter(order -> order.getEventId() != null)
                                .map(UserOrderDTO::getEventId)
                                .distinct()
                                .collect(Collectors.toList());
                        
                        // Fetch complete event data from database using event IDs
                        if (!eventIds.isEmpty()) {
                            List<Event> userEvents = eventRepo.findAllById(eventIds);
                            
                            // Convert Event entities to EventCardDTO
                            myEvents = userEvents.stream()
                                    .map(eventService::convertToDTO)
                                    .collect(Collectors.toList());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error loading customer events: {}", e.getMessage(), e);
            }
            model.addAttribute("myEvents", myEvents);

            // Get recommended events
            List<EventCardDTO> recommendedEvents = eventService.getRecommendedEvents(6);
            model.addAttribute("recommendedEvents", recommendedEvents != null ? recommendedEvents : List.of());

            // Get top students (top 3 by points) - Wrap in try-catch to prevent affecting other parts
            List<TopStudentDTO> topStudents = new ArrayList<>();
            try {
                topStudents = topStudentService.getTopStudents(3);

                // Đảm bảo luôn có ít nhất 3 students (service đã handle nhưng double check)
                if (topStudents == null) {
                    topStudents = new ArrayList<>();
                }
                if (topStudents.isEmpty()) {
                    for (int i = 1; i <= 3; i++) {
                        topStudents.add(TopStudentDTO.builder()
                                .customerId(null)
                                .name("Chưa có dữ liệu")
                                .email("")
                                .imageUrl("/img/sinhvien2.jpg")
                                .organization(null)
                                .points(0)
                                .rank(i)
                                .build());
                    }
                }
            } catch (Exception e) {
                log.error("ERROR loading top students (non-fatal): {}", e.getMessage(), e);
                // Tạo placeholder students để đảm bảo luôn có dữ liệu hiển thị
                topStudents = new ArrayList<>();
                for (int i = 1; i <= 3; i++) {
                    topStudents.add(TopStudentDTO.builder()
                            .customerId(null)
                            .name("Chưa có dữ liệu")
                            .email("")
                            .imageUrl("/img/sinhvien2.jpg")
                            .organization(null)
                            .points(0)
                            .rank(i)
                            .build());
                }
            }

            // Đảm bảo luôn có ít nhất 3 students trước khi add vào model
            while (topStudents.size() < 3) {
                topStudents.add(TopStudentDTO.builder()
                        .customerId(null)
                        .name("Chưa có dữ liệu")
                        .email("")
                        .imageUrl("/img/sinhvien2.jpg")
                        .organization(null)
                        .points(0)
                        .rank(topStudents.size() + 1)
                        .build());
            }

            model.addAttribute("topStudents", topStudents);

            return "index";
        } catch (Exception e) {
            log.error("Error in home controller: {}", e.getMessage(), e);
            // Return simple home page with empty data
            model.addAttribute("posterEvents", List.of());
            model.addAttribute("liveEvents", List.of());
            model.addAttribute("myEvents", List.of());
            model.addAttribute("recommendedEvents", List.of());

            // Đảm bảo luôn có topStudents trong model (ngay cả khi có exception)
            List<TopStudentDTO> placeholderStudents = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                placeholderStudents.add(TopStudentDTO.builder()
                        .customerId(null)
                        .name("Chưa có dữ liệu")
                        .email("")
                        .imageUrl("/img/sinhvien2.jpg")
                        .organization(null)
                        .points(0)
                        .rank(i)
                        .build());
            }
            model.addAttribute("topStudents", placeholderStudents);

            return "index";
        }
    }


    //use for dropdown user in header
    @GetMapping("/api/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        log.info("API: Getting current user");
        try {
            User user = userService.getCurrentUser(session);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("authenticated", true);
            userInfo.put("accountId", user.getAccount().getAccountId());
            userInfo.put("email", user.getAccount().getEmail());
            
            // Get name: if user is host, use Host.getHostName(), otherwise use User.getName() or email
            String name = null;
            if (user.hasHostRole() && user.getHost() != null) {
                try {
                    // Load host with user relationship to avoid LazyInitializationException
                    Host host = hostService.findHostByUserId(user.getUserId());
                    name = host.getHostName();
                } catch (Exception e) {
                    log.warn("Could not get host name, falling back to user name: {}", e.getMessage());
                    name = user.getName() != null ? user.getName() : user.getAccount().getEmail();
                }
            } else {
                name = user.getName() != null ? user.getName() : user.getAccount().getEmail();
            }
            userInfo.put("name", name);
            
            // Get avatar from User entity
            userInfo.put("avatar", user.getAvatar());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("authenticated", false);
            return ResponseEntity.ok(errorInfo);
        }
    }

    @PostMapping("/api/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }


}
