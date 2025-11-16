package com.group02.openevent.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VolunteerChatController {
    
    /**
     * Redirect to common event chat page
     * @deprecated Use /event-chat instead
     */
    @GetMapping("/volunteer/chat")
    public String volunteerChat(@RequestParam Long eventId) {
        return "redirect:/event-chat?eventId=" + eventId;
    }
}

