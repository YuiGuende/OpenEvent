package com.group02.openevent.controller.chat;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EventChatPageController {
    
    private final UserService userService;
    private final IEventRepo eventRepo;
    
    public EventChatPageController(UserService userService, IEventRepo eventRepo) {
        this.userService = userService;
        this.eventRepo = eventRepo;
    }
    
    @GetMapping("/event-chat")
    public String eventChat(
            @RequestParam Long eventId,
            HttpSession session,
            Model model) {
        
        try {
            User currentUser = userService.getCurrentUser(session);
            Event event = eventRepo.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
            
            // Check if user is host of this event
            boolean isHost = event.getHost() != null && 
                           event.getHost().getUser() != null &&
                           event.getHost().getUser().getUserId().equals(currentUser.getUserId());
            
            model.addAttribute("eventId", eventId);
            model.addAttribute("uid", currentUser.getUserId());
            model.addAttribute("eventTitle", event.getTitle());
            model.addAttribute("isHost", isHost);
            
            return "event/event-chat";
        } catch (Exception e) {
            // User not logged in or event not found, redirect to login
            return "redirect:/login?redirectUrl=/event-chat?eventId=" + eventId;
        }
    }
}




