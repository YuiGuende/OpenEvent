package com.group02.openevent.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for serving the full-screen chat web interface
 */
@Controller
public class ChatWebController {

    /**
     * Serve the chatweb.html page
     * This is the full-screen version of the chatbot
     * 
     * @param model Spring model for passing data to template
     * @param session HTTP session to get user information
     * @param sessionId Optional session ID from URL parameter
     * @param userId Optional user ID from URL parameter
     * @return Template name (chatweb.html will be resolved by Thymeleaf)
     */
    @GetMapping("/chatweb")
    public String chatWeb(
            Model model,
            HttpSession session,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long userId) {
        
        // Get user ID from session if not provided in URL
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (userId == null && accountId != null) {
            userId = accountId;
        }
        
        // Set default values if not provided
        if (userId == null) {
            userId = 2L; // Default user ID
        }
        
        // Pass data to template
        model.addAttribute("userId", userId);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("accountId", accountId);
        
        // Return template name (Thymeleaf will resolve to fragments/chatweb.html)
        // Note: Since chatweb.html is in fragments/, we need to specify the path
        return "fragments/chatweb";
    }
}



























