package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.user.UserOrderDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private final IAccountRepo accountRepo;
    private final ICustomerRepo customerRepo;
    private final IEventRepo eventRepo;
    private final IOrderRepo orderRepo;
    private final IUserRepo userRepo;
    @Autowired
    private EventService eventService;
    @Autowired
    private OrderService orderService;

    public HomeController(IAccountRepo accountRepo, ICustomerRepo customerRepo, IEventRepo eventRepo, IOrderRepo orderRepo, IUserRepo userRepo) {
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.eventRepo = eventRepo;
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        try {

            // Get poster events for hero slider
            List<EventCardDTO> posterEvents = eventService.getPosterEvents();
            model.addAttribute("posterEvents", posterEvents != null ? posterEvents : List.of());

            // Get live events
            List<EventCardDTO> liveEvents = eventService.getLiveEvents(6);
            model.addAttribute("liveEvents", liveEvents != null ? liveEvents : List.of());

            // Get latest events (only 3 for homepage)
            List<EventCardDTO> latestEvents = eventService.getRecentEvents(3).stream()
                    .map(eventService::convertToDTO)
                    .collect(Collectors.toList());
            model.addAttribute("latestEvents", latestEvents);

            // Get your events from user's orders - fetch complete event data from DB
            List<EventCardDTO> myEvents = List.of();
            try {
                Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
                System.out.println("DEBUG: Account ID from session: " + accountId);
                
                if (accountId != null) {
                    Customer customer = customerRepo.findByUser_Account_AccountId(accountId).orElse(null);
                    System.out.println("DEBUG: Customer found: " + (customer != null ? customer.getCustomerId() : "null"));
                    
                    if (customer != null) {
                        // Get user's orders to find event IDs
                        List<UserOrderDTO> userOrders = orderService.getOrderDTOsByCustomer(customer, null);
                        System.out.println("DEBUG: User orders count: " + userOrders.size());
                        
                        // Extract unique event IDs from orders
                        List<Long> eventIds = userOrders.stream()
                                .filter(order -> order.getEventId() != null)
                                .map(UserOrderDTO::getEventId)
                                .distinct()
                                .collect(Collectors.toList());
                        
                        System.out.println("DEBUG: Unique event IDs: " + eventIds);
                        
                        // Fetch complete event data from database using event IDs
                        if (!eventIds.isEmpty()) {
                            List<Event> userEvents = eventRepo.findAllById(eventIds);
                            System.out.println("DEBUG: Fetched events count: " + userEvents.size());
                            
                            // Convert Event entities to EventCardDTO
                            myEvents = userEvents.stream()
                                    .map(eventService::convertToDTO)
                                    .collect(Collectors.toList());
                            System.out.println("DEBUG: Converted to EventCardDTO count: " + myEvents.size());
                        }
                    }
                } else {
                    System.out.println("DEBUG: User not logged in");
                }
            } catch (Exception e) {
                System.err.println("Error loading customer events: " + e.getMessage());
                e.printStackTrace();
            }
            model.addAttribute("myEvents", myEvents);

            // Get recommended events
            List<EventCardDTO> recommendedEvents = eventService.getRecommendedEvents(6);
            model.addAttribute("recommendedEvents", recommendedEvents != null ? recommendedEvents : List.of());

            return "index";
        } catch (Exception e) {
            System.err.println("Error in home controller: " + e.getMessage());
            e.printStackTrace();
            // Return simple home page with empty data
            model.addAttribute("posterEvents", List.of());
            model.addAttribute("liveEvents", List.of());
            model.addAttribute("myEvents", List.of());
            model.addAttribute("recommendedEvents", List.of());
            return "index";
        }
    }


    //use for dropdown user in header
    @GetMapping("/api/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Account account = accountRepo.findById(accountId).orElse(null);
        if (account == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        // Lấy User để xác định Role
        User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("accountId", account.getAccountId());
        userInfo.put("email", account.getEmail());
        userInfo.put("role", user.getRole().name());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/api/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }


}
