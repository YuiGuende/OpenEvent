package com.group02.openevent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class SessionTestController {

    @GetMapping("/session-status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // Check HTTP session
        if (request.getSession(false) != null) {
            Long accountId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
            String sessionToken = (String) request.getSession(false).getAttribute("SESSION_TOKEN");
            
            response.put("hasHttpSession", true);
            response.put("accountId", accountId);
            response.put("sessionToken", sessionToken);
            
            if (accountId != null) {
                response.put("authenticated", true);
                response.put("message", "User is logged in via HTTP session");
            } else {
                response.put("authenticated", false);
                response.put("message", "No account ID in HTTP session");
            }
        } else {
            response.put("hasHttpSession", false);
            response.put("authenticated", false);
            response.put("message", "No HTTP session found");
        }
        
        return ResponseEntity.ok(response);
    }
}