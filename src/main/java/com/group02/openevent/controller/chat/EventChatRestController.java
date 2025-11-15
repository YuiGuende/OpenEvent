package com.group02.openevent.controller.chat;

import com.group02.openevent.dto.chat.ChatRoomDTO;
import com.group02.openevent.dto.chat.UserSummaryDTO;
import com.group02.openevent.model.chat.EventChatMessage;
import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.model.chat.ChatRoomType;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.EventChatRoomParticipantRepository;
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
    private final EventChatRoomParticipantRepository participantRepo;

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
                    } else if (room.getRoomType() == ChatRoomType.HOST_VOLUNTEERS) {
                        if (room.getEvent() != null) {
                            roomEventId = room.getEvent().getId();
                            roomEventTitle = room.getEvent().getTitle();
                        }
                    }
                    
                    // Đếm số participants cho HOST_VOLUNTEERS room
                    Integer participantCount = null;
                    if (room.getRoomType() == ChatRoomType.HOST_VOLUNTEERS) {
                        // Đếm participants: host (1) + participants trong bảng
                        int count = 1; // Host luôn có
                        List<com.group02.openevent.model.chat.EventChatRoomParticipant> participants = 
                            participantRepo.findByRoom_Id(room.getId());
                        if (participants != null) {
                            count += participants.size();
                        }
                        participantCount = count;
                    }
                    
                    return new ChatRoomDTO(
                            room.getId(),
                            room.getCreatedAt().toString(),
                            toDto(room.getHost()),
                            toDto(counterpart),
                            room.getRoomType().name(),
                            roomEventId,
                            roomEventTitle,
                            participantCount
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
        
        // Đếm participants cho room mới tạo
        Integer participantCount = null;
        if (room.getRoomType() == ChatRoomType.HOST_VOLUNTEERS) {
            int count = 1; // Host luôn có
            List<com.group02.openevent.model.chat.EventChatRoomParticipant> participants = 
                participantRepo.findByRoom_Id(room.getId());
            if (participants != null) {
                count += participants.size();
            }
            participantCount = count;
        }
        
        return ResponseEntity.ok(new ChatRoomDTO(
                room.getId(),
                room.getCreatedAt().toString(),
                toDto(room.getHost()),
                null, // Group chat, no single counterpart
                room.getRoomType().name(),
                room.getEvent() != null ? room.getEvent().getId() : null,
                room.getEvent() != null ? room.getEvent().getTitle() : null,
                participantCount
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


