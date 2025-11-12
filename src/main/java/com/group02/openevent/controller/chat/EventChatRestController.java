package com.group02.openevent.controller.chat;

import com.group02.openevent.model.chat.EventChatMessage;
import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.service.EventChatService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-chat")
@RequiredArgsConstructor
public class EventChatRestController {

    private final EventChatService chatService;
    private final UserService userService;

    @GetMapping("/rooms/{eventId}")
    public ResponseEntity<List<EventChatRoom>> myRooms(@PathVariable Long eventId, HttpSession session) {
        Long currentUserId = userService.getCurrentUser(session).getUserId();
        List<EventChatRoom> rooms = chatService.listRoomsForUser(eventId, currentUserId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<EventChatMessage>> history(@PathVariable Long roomId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          HttpSession session) {
        Long currentUserId = userService.getCurrentUser(session).getUserId();
        Page<EventChatMessage> result = chatService.getMessages(roomId, page, size, currentUserId);
        return ResponseEntity.ok(result);
    }
}


