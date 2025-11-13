package com.group02.openevent.controller.chat;

import com.group02.openevent.dto.chat.ChatRoomDTO;
import com.group02.openevent.dto.chat.UserSummaryDTO;
import com.group02.openevent.model.chat.EventChatMessage;
import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.model.user.User;
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
    public List<ChatRoomDTO> getRooms(@PathVariable Long eventId, HttpSession session) {
        User current = userService.getCurrentUser(session);
        List<EventChatRoom> rooms = chatService.listRoomsForUser(eventId, current.getUserId());

        return rooms.stream()
                .map(room -> {
                    // For HOST_DEPARTMENT: department is counterpart
                    // For HOST_VOLUNTEERS: show event info
                    User counterpart = null;
                    Long roomEventId = null;
                    String roomEventTitle = null;
                    
                    if (room.getRoomType() == com.group02.openevent.model.chat.ChatRoomType.HOST_DEPARTMENT) {
                        counterpart = room.getDepartment();
                        // Fallback: Nếu department null, dùng default department với userId = 2
                        if (counterpart == null) {
                            try {
                                counterpart = userService.getUserById(2L);
                            } catch (Exception e) {
                                // Nếu không tìm thấy userId = 2, để null (frontend sẽ xử lý)
                            }
                        }
                    } else if (room.getRoomType() == com.group02.openevent.model.chat.ChatRoomType.HOST_VOLUNTEERS) {
                        if (room.getEvent() != null) {
                            roomEventId = room.getEvent().getId();
                            roomEventTitle = room.getEvent().getTitle();
                        }
                    }
                    
                    return new ChatRoomDTO(
                            room.getId(),
                            room.getCreatedAt().toString(),
                            toDto(room.getHost()),
                            toDto(counterpart),
                            room.getRoomType().name(),
                            roomEventId,
                            roomEventTitle
                    );
                })
                .toList();
    }

    @PostMapping("/rooms/create-volunteer-room")
    public ResponseEntity<ChatRoomDTO> createVolunteerRoom(
            @RequestParam Long eventId,
            HttpSession session) {
        User current = userService.getCurrentUser(session);
        EventChatRoom room = chatService.createHostVolunteerRoom(eventId, current.getUserId());
        
        return ResponseEntity.ok(new ChatRoomDTO(
                room.getId(),
                room.getCreatedAt().toString(),
                toDto(room.getHost()),
                null, // Group chat, no single counterpart
                room.getRoomType().name(),
                room.getEvent() != null ? room.getEvent().getId() : null,
                room.getEvent() != null ? room.getEvent().getTitle() : null
        ));
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

    private UserSummaryDTO toDto(User user) {
        if (user == null) {
            return null;
        }
        String email = user.getAccount() != null ? user.getAccount().getEmail() : null;
        return new UserSummaryDTO(user.getUserId(), user.getName(), email);
    }
}


