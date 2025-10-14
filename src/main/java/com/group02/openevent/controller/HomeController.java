package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.service.EventService;
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

@Controller
public class HomeController {
    private final IAccountRepo accountRepo;
    @Autowired
    private EventService eventService;

    public HomeController(IAccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @GetMapping("/")
    public String home(Model model) {
        try {

            // Get poster events for hero slider
            List<EventCardDTO> posterEvents = eventService.getPosterEvents();
            model.addAttribute("posterEvents", posterEvents != null ? posterEvents : List.of());

            // Get live events
            List<EventCardDTO> liveEvents = eventService.getLiveEvents(6);
            model.addAttribute("liveEvents", liveEvents != null ? liveEvents : List.of());

            // Get your events (for now, using recommended events - TODO: implement customer-specific events)
            try {
                List<EventCardDTO> myEvents = eventService.getCustomerEvents((long) 2);//id ví dụ
                model.addAttribute("myEvents", myEvents != null ? myEvents : List.of());
            } catch (Exception e) {
                System.err.println("Error loading customer events: " + e.getMessage());
                model.addAttribute("myEvents", List.of());
            }

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

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("accountId", account.getAccountId());
        userInfo.put("email", account.getEmail());
        userInfo.put("role", account.getRole().name());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/api/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

}
