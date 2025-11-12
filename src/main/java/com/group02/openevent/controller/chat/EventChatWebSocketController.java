package com.group02.openevent.controller.chat;

import com.group02.openevent.service.EventChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EventChatWebSocketController {

    private final EventChatService chatService;

    // Expected payload: { "eventId": 123, "recipientUserId": 456, "content": "..." }
    @MessageMapping("/event-chat.send")
    public void sendMessage(@Payload Map<String, Object> payload, StompHeaderAccessor accessor) {
        HttpSession httpSession = (HttpSession) accessor.getSessionAttributes().get("HTTP.SESSION");
        if (httpSession == null) {
            // Some Spring setups do not store HttpSession automatically; try fallback
            httpSession = (HttpSession) accessor.getSessionAttributes().get("HTTP_SESSION");
        }
        if (httpSession == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }

        Long eventId = toLong(payload.get("eventId"));
        Long recipientUserId = toLong(payload.get("recipientUserId"));
        String content = payload.get("content") != null ? payload.get("content").toString() : null;

        chatService.saveAndSendMessage(eventId, recipientUserId, content, httpSession);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }
}


