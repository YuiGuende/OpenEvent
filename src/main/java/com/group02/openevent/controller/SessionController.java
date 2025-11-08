package com.group02.openevent.controller;

import com.group02.openevent.model.session.Session;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    
    private final SessionService sessionService;
    private final IUserRepo userRepo;
    
    public SessionController(SessionService sessionService, IUserRepo userRepo) {
        this.sessionService = sessionService;
        this.userRepo = userRepo;
    }
    
    /**
     * Get current session information
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        
        Optional<Session> sessionOpt = sessionService.validateSession(sessionToken);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        
        Session session = sessionOpt.get();
        // Lấy User để xác định Role
        User user = userRepo.findByAccount_AccountId(session.getAccount().getAccountId())
                .orElse(null);
        
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("authenticated", true);
        sessionInfo.put("sessionId", session.getSessionId());
        sessionInfo.put("accountId", session.getAccount().getAccountId());
        sessionInfo.put("email", session.getAccount().getEmail());
        sessionInfo.put("role", user != null ? user.getRole().name() : "CUSTOMER");
        sessionInfo.put("createdAt", session.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        sessionInfo.put("lastAccessedAt", session.getLastAccessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        sessionInfo.put("expiresAt", session.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        sessionInfo.put("ipAddress", session.getIpAddress());
        sessionInfo.put("deviceInfo", session.getDeviceInfo());
        
        return ResponseEntity.ok(sessionInfo);
    }
    
    /**
     * Get all active sessions for current user
     */
    @GetMapping("/my-sessions")
    public ResponseEntity<List<Map<String, Object>>> getMySessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<Session> currentSessionOpt = sessionService.validateSession(sessionToken);
        if (currentSessionOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Long accountId = currentSessionOpt.get().getAccount().getAccountId();
        List<Session> sessions = sessionService.getActiveSessions(accountId);
        
        List<Map<String, Object>> sessionList = sessions.stream()
            .map(session -> {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("sessionId", session.getSessionId());
                sessionMap.put("createdAt", session.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                sessionMap.put("lastAccessedAt", session.getLastAccessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                sessionMap.put("expiresAt", session.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                sessionMap.put("ipAddress", session.getIpAddress());
                sessionMap.put("deviceInfo", session.getDeviceInfo());
                sessionMap.put("isCurrent", session.getSessionToken().equals(sessionToken));
                return sessionMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(sessionList);
    }
    
    /**
     * Terminate a specific session
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> terminateSession(
            @PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        }
        
        Optional<Session> currentSessionOpt = sessionService.validateSession(sessionToken);
        if (currentSessionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid session"));
        }
        
        // Find the session to terminate
        List<Session> userSessions = sessionService.getActiveSessions(currentSessionOpt.get().getAccount().getAccountId());
        Optional<Session> targetSessionOpt = userSessions.stream()
            .filter(session -> session.getSessionId().equals(sessionId))
            .findFirst();
        
        if (targetSessionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session not found"));
        }
        
        boolean success = sessionService.destroySession(targetSessionOpt.get().getSessionToken());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Session terminated successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to terminate session"));
        }
    }
    
    /**
     * Terminate all other sessions (keep current one)
     */
    @DeleteMapping("/terminate-others")
    public ResponseEntity<Map<String, String>> terminateOtherSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        }
        
        Optional<Session> currentSessionOpt = sessionService.validateSession(sessionToken);
        if (currentSessionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid session"));
        }
        
        Long accountId = currentSessionOpt.get().getAccount().getAccountId();
        List<Session> sessions = sessionService.getActiveSessions(accountId);
        
        // Terminate all sessions except current one
        int terminatedCount = 0;
        for (Session session : sessions) {
            if (!session.getSessionToken().equals(sessionToken)) {
                if (sessionService.destroySession(session.getSessionToken())) {
                    terminatedCount++;
                }
            }
        }
        
        return ResponseEntity.ok(Map.of("message", "Terminated " + terminatedCount + " other sessions"));
    }
    
    /**
     * Extend current session
     */
    @PostMapping("/extend")
    public ResponseEntity<Map<String, String>> extendSession(
            @RequestParam(defaultValue = "24") int hours,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        }
        
        boolean success = sessionService.extendSession(sessionToken, hours);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Session extended successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to extend session"));
        }
    }
    
    /**
     * Get session statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSessionStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<Session> currentSessionOpt = sessionService.validateSession(sessionToken);
        if (currentSessionOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Long accountId = currentSessionOpt.get().getAccount().getAccountId();
        long sessionCount = sessionService.getSessionCount(accountId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", sessionCount);
        stats.put("canCreateNew", sessionService.canCreateNewSession(accountId));
        stats.put("currentSessionValid", true);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Logout (terminate current session)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpSession httpSession) {
        
        String sessionToken = extractSessionToken(authHeader, request);
        if (sessionToken != null) {
            sessionService.destroySession(sessionToken);
        }
        
        // Also invalidate HTTP session
        httpSession.invalidate();
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    // Helper method to extract session token
    private String extractSessionToken(String authHeader, HttpServletRequest request) {
        // Try to get from Authorization header first
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try to get from request parameter
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        
        // Try to get from session attribute
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (String) session.getAttribute("SESSION_TOKEN");
        }
        
        return null;
    }
}
