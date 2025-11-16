package com.group02.openevent.controller.chat;

import com.group02.openevent.service.EventChatService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EventChatWebSocketController {

    private final EventChatService chatService;
    private final UserService userService;

    // Expected payload: { "eventId": 123, "recipientUserId": 456, "content": "..." }
    @MessageMapping("/event-chat.send")
    public void sendMessage(@Payload Map<String, Object> payload, 
                           StompHeaderAccessor accessor,
                           Principal principal) {
        log.info("=== RECEIVED WEBSOCKET MESSAGE ===");
        log.info("Payload: {}", payload);
        log.info("Principal: {}", principal != null ? principal.getName() : "NULL");
        
        // Try to get HttpSession from WebSocket session attributes
        HttpSession httpSession = (HttpSession) accessor.getSessionAttributes().get("HTTP.SESSION");
        if (httpSession == null) {
            httpSession = (HttpSession) accessor.getSessionAttributes().get("HTTP_SESSION");
        }
        
        // If no HttpSession, try to get userId from Principal (set by UserIdHandshakeHandler)
        if (httpSession == null && principal != null) {
            try {
                Long userId = Long.parseLong(principal.getName());
                log.info("No HttpSession found, but Principal has userId: {}", userId);
                
                // Create a mock HttpSession wrapper or use userId directly
                // For now, we'll get user from userId and proceed
                Long eventId = toLong(payload.get("eventId"));
                Long recipientUserId = toLong(payload.get("recipientUserId"));
                String content = payload.get("content") != null ? payload.get("content").toString() : null;
                
                log.info("Parsed: eventId={}, recipientUserId={}, content={}", eventId, recipientUserId, content);
                log.info("Calling chatService.saveAndSendMessage with userId...");
                
                // Use a method that accepts userId instead of HttpSession
                chatService.saveAndSendMessage(eventId, recipientUserId, content, userId);
                log.info("✅ chatService.saveAndSendMessage completed successfully");
                return;
            } catch (NumberFormatException e) {
                log.error("❌ Principal name is not a valid userId: {}", principal.getName());
            }
        }
        
        if (httpSession == null) {
            log.error("❌ No HTTP session found in WebSocket attributes and Principal is null or invalid");
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        
        log.info("HTTP Session found: {}", httpSession.getId());

        Long eventId = toLong(payload.get("eventId"));
        Long recipientUserId = toLong(payload.get("recipientUserId"));
        String content = payload.get("content") != null ? payload.get("content").toString() : null;

        log.info("Parsed: eventId={}, recipientUserId={}, content={}", eventId, recipientUserId, content);
        log.info("Calling chatService.saveAndSendMessage...");

        try {
            chatService.saveAndSendMessage(eventId, recipientUserId, content, httpSession);
            log.info("✅ chatService.saveAndSendMessage completed successfully");
        } catch (Exception e) {
            log.error("❌ Error in chatService.saveAndSendMessage", e);
            throw e;
        }
        
        log.info("=== END WEBSOCKET MESSAGE HANDLING ===");
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }
}


