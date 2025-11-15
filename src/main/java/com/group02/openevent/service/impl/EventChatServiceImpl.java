package com.group02.openevent.service.impl;

import com.group02.openevent.dto.chat.ChatMessageDTO;
import com.group02.openevent.model.chat.ChatRoomType;
import com.group02.openevent.model.chat.EventChatMessage;
import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.model.chat.EventChatRoomParticipant;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.model.volunteer.VolunteerStatus;
import com.group02.openevent.repository.EventChatMessageRepository;
import com.group02.openevent.repository.EventChatRoomParticipantRepository;
import com.group02.openevent.repository.EventChatRoomRepository;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IDepartmentRepo;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventChatServiceImpl implements EventChatService {

    private final EventChatRoomRepository roomRepo;
    private final EventChatRoomParticipantRepository participantRepo;
    private final EventChatMessageRepository messageRepo;
    private final IEventRepo eventRepo;
    private final ICustomerRepo customerRepo;
    private final IDepartmentRepo departmentRepo;
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
        
        return saveAndSendMessageInternal(eventId, recipientUserId, content, currentUserId);
    }
    
    @Override
    @Transactional
    public ChatMessageDTO saveAndSendMessage(Long eventId, Long recipientUserId, String content, Long currentUserId) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        return saveAndSendMessageInternal(eventId, recipientUserId, content, currentUserId);
    }
    
    private ChatMessageDTO saveAndSendMessageInternal(Long eventId, Long recipientUserId, String content, Long currentUserId) {
        // Handle department chat (eventId = 0 or null)
        if (eventId == null || eventId == 0) {
            return saveDepartmentChatMessage(recipientUserId, content, currentUserId);
        }
        
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        // Identify host
        User hostUser = event.getHost() != null ? event.getHost().getUser() : null;
        if (hostUser == null) {
            throw new IllegalStateException("Event host not found for event: " + eventId);
        }

        // Check if this is a group chat (HOST_VOLUNTEERS) or 1-1 chat (HOST_DEPARTMENT)
        // First, check if HOST_VOLUNTEERS room exists for this event
        Optional<EventChatRoom> existingGroupRoom = roomRepo.findByEventAndHostAndRoomType(eventId, hostUser.getUserId());
        
        if (existingGroupRoom.isPresent() && existingGroupRoom.get().getRoomType() == ChatRoomType.HOST_VOLUNTEERS) {
            // GROUP CHAT: Message goes to all participants
            return saveGroupChatMessage(eventId, content, currentUserId, hostUser, existingGroupRoom.get());
        }
        
        // Check if this is HOST_DEPARTMENT room (1-1 chat)
        // IMPORTANT: Only use HOST_DEPARTMENT room if it has a valid department
        Optional<EventChatRoom> existingDeptRoom = roomRepo.findByHostAndRoomType(hostUser.getUserId());
        if (existingDeptRoom.isPresent() 
                && existingDeptRoom.get().getRoomType() == ChatRoomType.HOST_DEPARTMENT
                && existingDeptRoom.get().getDepartment() != null) {
            // 1-1 CHAT with Department
            return saveOneOnOneChatMessage(eventId, recipientUserId, content, currentUserId, hostUser, existingDeptRoom.get());
        }
        
        // Default: Create HOST_VOLUNTEERS room (group chat) if no room exists
        // This handles the case when sending first message
        EventChatRoom room = createHostVolunteerRoom(eventId, hostUser.getUserId());
        return saveGroupChatMessage(eventId, content, currentUserId, hostUser, room);
    }

    private ChatMessageDTO saveDepartmentChatMessage(Long recipientUserId, String content, Long currentUserId) {
        User currentUser = userService.getUserById(currentUserId);
        
        // For department chat, recipientUserId should be the host's userId
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient user ID is required for department chat");
        }
        
        // Find HOST_DEPARTMENT room where current user is department and recipient is host
        // Or where current user is host and recipient is department
        Optional<EventChatRoom> deptRoom = roomRepo.findByHostAndRoomType(recipientUserId);
        
        if (deptRoom.isEmpty()) {
            // Try the other way: current user is host, recipient is department
            deptRoom = roomRepo.findByHostAndRoomType(currentUserId);
            if (deptRoom.isPresent()) {
                EventChatRoom room = deptRoom.get();
                // Verify recipient is the department
                if (room.getDepartment() == null || !room.getDepartment().getUserId().equals(recipientUserId)) {
                    throw new IllegalArgumentException("Invalid recipient for department chat");
                }
            } else {
                throw new IllegalStateException("Department chat room not found. Please ensure host has registered.");
            }
        }
        
        EventChatRoom room = deptRoom.get();
        
        // Verify current user is either host or department
        boolean isHost = room.getHost().getUserId().equals(currentUserId);
        boolean isDepartment = room.getDepartment() != null && room.getDepartment().getUserId().equals(currentUserId);
        
        if (!isHost && !isDepartment) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not a participant of this department chat room");
        }
        
        // Persist message
        EventChatMessage message = new EventChatMessage();
        message.setChatRoom(room);
        message.setSender(currentUser);
        message.setBody(content.trim());
        EventChatMessage saved = messageRepo.save(message);

        String senderName = currentUser.getName() != null ? currentUser.getName() : 
                            (currentUser.getAccount() != null && currentUser.getAccount().getEmail() != null ? 
                             currentUser.getAccount().getEmail() : "Người dùng");
        
        ChatMessageDTO dto = new ChatMessageDTO(
                room.getId(),
                0L, // No event for department chat
                saved.getId(),
                currentUserId,
                senderName,
                recipientUserId,
                saved.getBody(),
                saved.getTimestamp()
        );

        // Broadcast to room-specific destination
        String destination = "/queue/event-chat/rooms/" + room.getId();
        try {
            log.info("=== BROADCASTING DEPARTMENT CHAT MESSAGE ===");
            log.info("Destination: {}", destination);
            log.info("DTO: roomId={}, messageId={}, senderUserId={}, recipientUserId={}, body={}, timestamp={}", 
                    dto.getRoomId(), dto.getMessageId(), dto.getSenderUserId(), 
                    dto.getRecipientUserId(), dto.getBody(), dto.getTimestamp());
            
            messagingTemplate.convertAndSend(destination, dto);
            
            log.info("✅ Successfully broadcasted message to destination: {}", destination);
            log.info("=== END BROADCAST ===");
        } catch (Exception e) {
            log.error("❌ ERROR broadcasting WebSocket message to destination: {}", destination, e);
            e.printStackTrace();
        }

        return dto;
    }

    private ChatMessageDTO saveGroupChatMessage(Long eventId, String content, Long currentUserId, 
                                               User hostUser, EventChatRoom room) {
        User currentUser = userService.getUserById(currentUserId);
        
        // Validate user is a participant (host or volunteer)
        boolean isHost = currentUserId.equals(hostUser.getUserId());
        boolean isParticipant = participantRepo.findByRoomIdAndUserId(room.getId(), currentUserId).isPresent();
        
        if (!isHost && !isParticipant) {
            // If not a participant yet, try to add them (if they're an approved volunteer)
            Optional<Customer> customerOpt = customerRepo.findByUser_UserId(currentUserId);
            if (customerOpt.isPresent()) {
                boolean approved = volunteerService.isCustomerApprovedVolunteer(
                        customerOpt.get().getCustomerId(), eventId);
                if (approved) {
                    addParticipant(room, currentUser);
                } else {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "You are not an approved volunteer for this event");
                }
            } else {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not a participant of this room");
            }
        }
        
        // Persist message
        EventChatMessage message = new EventChatMessage();
        message.setChatRoom(room);
        message.setSender(currentUser);
        message.setBody(content.trim());
        EventChatMessage saved = messageRepo.save(message);

        String senderName = currentUser.getName() != null ? currentUser.getName() : 
                            (currentUser.getAccount() != null && currentUser.getAccount().getEmail() != null ? 
                             currentUser.getAccount().getEmail() : "Người dùng");
        
        ChatMessageDTO dto = new ChatMessageDTO(
                room.getId(),
                eventId,
                saved.getId(),
                currentUserId,
                senderName,
                null, // No specific recipient in group chat
                saved.getBody(),
                saved.getTimestamp()
        );

        // Broadcast to room-specific destination
        String destination = "/queue/event-chat/rooms/" + room.getId();
        try {
            log.info("=== BROADCASTING GROUP CHAT MESSAGE ===");
            log.info("Destination: {}", destination);
            log.info("DTO: roomId={}, eventId={}, messageId={}, senderUserId={}, body={}, timestamp={}", 
                    dto.getRoomId(), dto.getEventId(), dto.getMessageId(), dto.getSenderUserId(), 
                    dto.getBody(), dto.getTimestamp());
            log.info("MessagingTemplate: {}", messagingTemplate != null ? "OK" : "NULL");
            
            messagingTemplate.convertAndSend(destination, dto);
            
            log.info("✅ Successfully broadcasted message to destination: {}", destination);
            log.info("=== END BROADCAST ===");
        } catch (Exception e) {
            log.error("❌ ERROR broadcasting WebSocket message to destination: {}", destination, e);
            e.printStackTrace();
        }

        return dto;
    }

    private ChatMessageDTO saveOneOnOneChatMessage(Long eventId, Long recipientUserId, String content, 
                                                  Long currentUserId, User hostUser, EventChatRoom room) {
        // Validate recipientUserId is provided
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient user ID is required for 1-1 chat");
        }
        
        // Validate not sending to yourself
        if (currentUserId.equals(recipientUserId)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        User currentUser = userService.getUserById(currentUserId);

        // For HOST_DEPARTMENT room, recipient should be department
        if (room.getDepartment() == null) {
            throw new IllegalStateException(
                "Cannot send message: Department chat room is not properly configured. " +
                "Please contact administrator to set up department for this host.");
        }
        
        if (!room.getDepartment().getUserId().equals(recipientUserId)) {
            throw new IllegalArgumentException(
                String.format("Invalid recipient for department chat. Expected department user ID: %d, Got: %d", 
                    room.getDepartment().getUserId(), recipientUserId));
        }

        // Persist message
        EventChatMessage message = new EventChatMessage();
        message.setChatRoom(room);
        message.setSender(currentUser);
        message.setBody(content.trim());
        EventChatMessage saved = messageRepo.save(message);

        String senderName = currentUser.getName() != null ? currentUser.getName() : 
                            (currentUser.getAccount() != null && currentUser.getAccount().getEmail() != null ? 
                             currentUser.getAccount().getEmail() : "Người dùng");
        
        ChatMessageDTO dto = new ChatMessageDTO(
                room.getId(),
                eventId,
                saved.getId(),
                currentUserId,
                senderName,
                recipientUserId,
                saved.getBody(),
                saved.getTimestamp()
        );

        // Broadcast to room-specific destination
        String destination = "/queue/event-chat/rooms/" + room.getId();
        try {
            log.info("=== BROADCASTING 1-1 CHAT MESSAGE ===");
            log.info("Destination: {}", destination);
            log.info("DTO: roomId={}, eventId={}, messageId={}, senderUserId={}, recipientUserId={}, body={}, timestamp={}", 
                    dto.getRoomId(), dto.getEventId(), dto.getMessageId(), dto.getSenderUserId(), 
                    dto.getRecipientUserId(), dto.getBody(), dto.getTimestamp());
            log.info("MessagingTemplate: {}", messagingTemplate != null ? "OK" : "NULL");
            
            messagingTemplate.convertAndSend(destination, dto);
            
            log.info("✅ Successfully broadcasted message to destination: {}", destination);
            log.info("=== END BROADCAST ===");
        } catch (Exception e) {
            log.error("❌ ERROR broadcasting WebSocket message to destination: {}", destination, e);
            e.printStackTrace();
        }

        return dto;
    }



    @Override
    public List<EventChatRoom> listRoomsForUser(Long eventId, Long currentUserId) {
        // Use new query that includes both HOST_DEPARTMENT and HOST_VOLUNTEERS rooms
        return roomRepo.findByEventIdAndParticipantIdIncludingDepartment(eventId, currentUserId);
    }

    @Override
    public List<EventChatRoom> getAllRoomsForUser(Long userId) {
        // 1. Lấy tất cả rooms mà user là host
        List<EventChatRoom> hostRooms = roomRepo.findByHostUserId(userId);
        
        // 2. Lấy tất cả rooms mà user là volunteer participant
        List<EventChatRoom> volunteerRooms = roomRepo.findByVolunteerParticipantId(userId);
        
        // 3. Lọc volunteer rooms: chỉ lấy rooms mà user đã được approved làm volunteer
        Optional<Customer> customerOpt = customerRepo.findByUser_UserId(userId);
        List<EventChatRoom> approvedVolunteerRooms = new ArrayList<>();
        
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            
            // Lấy tất cả approved volunteer applications của customer này
            List<VolunteerApplication> approvedApps = volunteerService
                .getVolunteerApplicationsByCustomerIdAndStatus(
                    customer.getCustomerId(), 
                    VolunteerStatus.APPROVED
                );
            
            // Tạo Set eventIds mà user đã được approved
            Set<Long> approvedEventIds = approvedApps.stream()
                .map(app -> app.getEvent().getId())
                .collect(Collectors.toSet());
            
            // Lọc volunteer rooms: chỉ lấy rooms của events mà user đã được approved
            approvedVolunteerRooms = volunteerRooms.stream()
                .filter(room -> {
                    if (room.getEvent() == null) {
                        return false;
                    }
                    return approvedEventIds.contains(room.getEvent().getId());
                })
                .collect(Collectors.toList());
        }
        
        // 4. Merge 2 danh sách và loại bỏ duplicate (dựa trên room.id)
        Set<Long> roomIds = new HashSet<>();
        List<EventChatRoom> allRooms = new ArrayList<>();
        
        // Thêm host rooms
        for (EventChatRoom room : hostRooms) {
            if (!roomIds.contains(room.getId())) {
                roomIds.add(room.getId());
                allRooms.add(room);
            }
        }
        
        // Thêm approved volunteer rooms (tránh duplicate)
        for (EventChatRoom room : approvedVolunteerRooms) {
            if (!roomIds.contains(room.getId())) {
                roomIds.add(room.getId());
                allRooms.add(room);
            }
        }
        
        return allRooms;
    }

    @Override
    public Page<EventChatMessage> getMessages(Long roomId, int page, int size, Long currentUserId) {
        // Authorization: ensure current user is a participant of the room
        EventChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        
        // Check if user is host, department, or participant
        boolean isHost = room.getHost().getUserId().equals(currentUserId);
        boolean isDepartment = room.getDepartment() != null && 
                               room.getDepartment().getUserId().equals(currentUserId);
        boolean isParticipant = participantRepo.findByRoomIdAndUserId(roomId, currentUserId).isPresent();
        
        if (!isHost && !isDepartment && !isParticipant) {
            throw new org.springframework.security.access.AccessDeniedException("FORBIDDEN");
        }
        
        return messageRepo.findByChatRoomIdOrderByTimestampAsc(roomId, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public EventChatRoom createHostDepartmentRoom(Long hostUserId) {
        User hostUser = userService.getUserById(hostUserId);
        
        // Kiểm tra đã có room chưa
        Optional<EventChatRoom> existing = roomRepo.findByHostAndRoomType(hostUserId);
        if (existing.isPresent()) {
            log.info("Host-Department room already exists for host: {}", hostUserId);
            return existing.get();
        }
        
        // Tìm department đầu tiên trong hệ thống (hoặc có thể là department mặc định)
        // Nếu không có department, có thể tạo room mà không có department
        Optional<Department> firstDepartment = departmentRepo.findAll().stream().findFirst();
        
        if (firstDepartment.isEmpty()) {
            log.warn("No department found in system. Creating room without department for host: {}", hostUserId);
            // Vẫn tạo room nhưng không có department
            EventChatRoom room = new EventChatRoom();
            room.setHost(hostUser);
            room.setDepartment(null);
            room.setEvent(null);
            room.setRoomType(ChatRoomType.HOST_DEPARTMENT);
            return roomRepo.save(room);
        }
        
        User departmentUser = firstDepartment.get().getUser();
        
        // Tạo room
        EventChatRoom room = new EventChatRoom();
        room.setHost(hostUser);
        room.setDepartment(departmentUser);
        room.setEvent(null); // Room Host-Department không gắn với event cụ thể
        room.setRoomType(ChatRoomType.HOST_DEPARTMENT);
        
        log.info("Created Host-Department room for host: {} and department: {}", 
                hostUserId, departmentUser.getUserId());
        
        return roomRepo.save(room);
    }

    @Override
    @Transactional
    public EventChatRoom createHostVolunteerRoom(Long eventId, Long hostUserId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        
        User hostUser = userService.getUserById(hostUserId);
        
        // Validate host là host của event này
        if (event.getHost() == null || !event.getHost().getUser().getUserId().equals(hostUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only event host can create volunteer room");
        }
        
        // Kiểm tra đã có room chưa
        Optional<EventChatRoom> existing = roomRepo.findByEventAndHostAndRoomType(eventId, hostUserId);
        if (existing.isPresent()) {
            log.info("Host-Volunteers room already exists for event: {} and host: {}", eventId, hostUserId);
            return existing.get();
        }
        
        // Tạo room
        EventChatRoom room = new EventChatRoom();
        room.setHost(hostUser);
        room.setDepartment(null);
        room.setEvent(event);
        room.setRoomType(ChatRoomType.HOST_VOLUNTEERS);
        
        room = roomRepo.save(room);
        
        // Thêm tất cả approved volunteers vào room
        List<VolunteerApplication> approvedVolunteers = 
                volunteerService.getVolunteerApplicationsByEventIdAndStatus(
                        eventId, VolunteerStatus.APPROVED
                );
        
        for (VolunteerApplication app : approvedVolunteers) {
            User volunteerUser = app.getCustomer().getUser();
            addParticipant(room, volunteerUser);
        }
        
        log.info("Created Host-Volunteers room for event: {} and host: {} with {} volunteers", 
                eventId, hostUserId, approvedVolunteers.size());
        
        return room;
    }

    private void addParticipant(EventChatRoom room, User volunteerUser) {
        // Kiểm tra đã tham gia chưa
        Optional<EventChatRoomParticipant> existing = participantRepo.findByRoomIdAndUserId(
                room.getId(), volunteerUser.getUserId());
        if (existing.isPresent()) {
            return; // Đã tham gia rồi
        }
        
        EventChatRoomParticipant participant = new EventChatRoomParticipant();
        participant.setRoom(room);
        participant.setUser(volunteerUser);
        participantRepo.save(participant);
    }
}


