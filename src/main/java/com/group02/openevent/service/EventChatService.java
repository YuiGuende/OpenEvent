package com.group02.openevent.service;

import com.group02.openevent.dto.chat.ChatMessageDTO;
import com.group02.openevent.model.chat.EventChatRoom;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface EventChatService {

    ChatMessageDTO saveAndSendMessage(Long eventId, Long recipientUserId, String content, HttpSession session);
    
    ChatMessageDTO saveAndSendMessage(Long eventId, Long recipientUserId, String content, Long currentUserId);

    List<EventChatRoom> listRoomsForUser(Long eventId, Long currentUserId);

    org.springframework.data.domain.Page<com.group02.openevent.model.chat.EventChatMessage> getMessages(Long roomId, int page, int size, Long currentUserId);

    /**
     * Tạo room Host-Department tự động khi host register
     * @param hostUserId ID của host user
     * @return EventChatRoom đã được tạo
     */
    EventChatRoom createHostDepartmentRoom(Long hostUserId);

    /**
     * Tạo room Host-Volunteers thủ công khi host muốn
     * @param eventId ID của event
     * @param hostUserId ID của host user
     * @return EventChatRoom đã được tạo
     */
    EventChatRoom createHostVolunteerRoom(Long eventId, Long hostUserId);

    /**
     * Lấy tất cả rooms mà user là participant (host, department, hoặc volunteer)
     * Không phụ thuộc vào event - trả về tất cả rooms mà user có quyền truy cập
     * @param userId ID của user
     * @return Danh sách tất cả rooms mà user là participant
     */
    List<EventChatRoom> getAllRoomsForUser(Long userId);
}


