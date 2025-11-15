package com.group02.openevent.controller.ai;

import com.group02.openevent.dto.ai.*;
import com.group02.openevent.models.ai.ChatMessage;
import com.group02.openevent.services.ai.ChatSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/ai/sessions")
public class ChatApiController {

    private final ChatSessionService service;

    public ChatApiController(ChatSessionService service) {
        this.service = service;
    }

    private Long getCurrentUserId(HttpSession session) {
        Long accountId = (Long) session.getAttribute("USER_ID");
        if (accountId == null) {
            throw new org.springframework.security.access.AccessDeniedException("NOT_AUTHENTICATED");
        }
        return accountId;
    }

    private void assertOwnership(Long userId, HttpSession session) {
        Long currentUserId = getCurrentUserId(session);
        if (!userId.equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("FORBIDDEN");
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SessionItem>> sessions(@PathVariable Long userId, HttpSession session) {
        assertOwnership(userId, session);
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping
    public ResponseEntity<NewSessionRes> create(@RequestBody NewSessionReq req, HttpSession session) {
        assertOwnership(req.userId(), session);
        return ResponseEntity.ok(service.create(req.userId(), req.title()));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<ChatMessage>> history(@PathVariable Long userId,
                                                     @RequestParam String sessionId,
                                                     HttpSession session) {
        assertOwnership(userId, session);
        return ResponseEntity.ok(service.history(userId, sessionId));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatReply> chat(@RequestBody ChatRequest req, HttpSession session) {
        assertOwnership(req.userId(), session);
        return ResponseEntity.ok(service.chat(req));
    }
}
