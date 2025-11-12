package com.group02.openevent.service.impl;

import com.group02.openevent.dto.chat.ChatMessageDTO;
import com.group02.openevent.model.chat.EventChatMessage;
import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.EventChatMessageRepository;
import com.group02.openevent.repository.EventChatRoomRepository;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.EventChatService;
import com.group02.openevent.service.UserService;
import com.group02.openevent.service.VolunteerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventChatServiceImpl implements EventChatService {

    private final EventChatRoomRepository roomRepo;
    private final EventChatMessageRepository messageRepo;
    private final IEventRepo eventRepo;
    private final ICustomerRepo customerRepo;
    private final UserService userService;
    private final VolunteerService volunteerService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageDTO saveAndSendMessage(Long eventId, Long recipientUserId, String content, HttpSession session) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        User currentUser = userService.getCurrentUser(session);
        Long currentUserId = currentUser.getUserId();
        if (currentUserId.equals(recipientUserId)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        // Identify host and volunteer participants
        User hostUser = event.getHost() != null ? event.getHost().getUser() : null;
        if (hostUser == null) {
            throw new IllegalStateException("Event host not found for event: " + eventId);
        }
        User recipientUser = userService.getUserById(recipientUserId);

        // Determine roles of current and recipient
        boolean currentIsHost = currentUserId.equals(hostUser.getUserId());
        boolean recipientIsHost = recipientUser.getUserId().equals(hostUser.getUserId());

        // Resolve volunteer user (the one who is not the host)
        User volunteerUser = currentIsHost ? recipientUser : (recipientIsHost ? currentUser : recipientUser);
        if (volunteerUser.getUserId().equals(hostUser.getUserId())) {
            throw new IllegalArgumentException("Invalid participants for host/volunteer chat");
        }

        // Validate volunteer approval
        Optional<Customer> volunteerCustomerOpt = customerRepo.findByUser_UserId(volunteerUser.getUserId());
        if (volunteerCustomerOpt.isEmpty()) {
            throw new IllegalStateException("Volunteer must be a customer");
        }
        boolean approved = volunteerService.isCustomerApprovedVolunteer(volunteerCustomerOpt.get().getCustomerId(), eventId);
        if (!approved) {
            throw new org.springframework.security.access.AccessDeniedException("Volunteer is not approved for this event");
        }

        // Find or create room with (event, host, volunteer)
        User finalVolunteerUser = volunteerUser;
        EventChatRoom room = roomRepo.findByEventAndHostAndVolunteer(event, hostUser, finalVolunteerUser)
                .orElseGet(() -> {
                    EventChatRoom r = new EventChatRoom();
                    r.setEvent(event);
                    r.setHost(hostUser);
                    r.setVolunteer(finalVolunteerUser);
                    return roomRepo.save(r);
                });

        // Persist message
        EventChatMessage message = new EventChatMessage();
        message.setChatRoom(room);
        message.setSender(currentUser);
        message.setBody(content.trim());
        EventChatMessage saved = messageRepo.save(message);

        ChatMessageDTO dto = new ChatMessageDTO(
                room.getId(),
                event.getId(),
                saved.getId(),
                currentUserId,
                recipientUserId,
                saved.getBody(),
                saved.getTimestamp()
        );

        // Broadcast to both participants using userId-based queues
        String recipientDestination = "/queue/event-chat/" + recipientUserId;
        String senderDestination = "/queue/event-chat/" + currentUserId;
        messagingTemplate.convertAndSend(recipientDestination, dto);
        messagingTemplate.convertAndSend(senderDestination, dto);

        return dto;
    }

    @Override
    public List<EventChatRoom> listRoomsForUser(Long eventId, Long currentUserId) {
        return roomRepo.findByEventIdAndParticipantId(eventId, currentUserId);
    }

    @Override
    public Page<EventChatMessage> getMessages(Long roomId, int page, int size, Long currentUserId) {
        // Authorization: ensure current user is a participant of the room
        EventChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        if (!room.getHost().getUserId().equals(currentUserId) &&
                !room.getVolunteer().getUserId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("FORBIDDEN");
        }
        return messageRepo.findByChatRoomIdOrderByTimestampAsc(roomId, PageRequest.of(page, size));
    }
}


