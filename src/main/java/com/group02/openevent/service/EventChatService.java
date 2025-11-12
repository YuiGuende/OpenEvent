package com.group02.openevent.service;

import com.group02.openevent.dto.chat.ChatMessageDTO;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface EventChatService {

    ChatMessageDTO saveAndSendMessage(Long eventId, Long recipientUserId, String content, HttpSession session);

    List<com.group02.openevent.model.chat.EventChatRoom> listRoomsForUser(Long eventId, Long currentUserId);

    org.springframework.data.domain.Page<com.group02.openevent.model.chat.EventChatMessage> getMessages(Long roomId, int page, int size, Long currentUserId);
}


