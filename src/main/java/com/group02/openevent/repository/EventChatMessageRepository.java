package com.group02.openevent.repository;

import com.group02.openevent.model.chat.EventChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventChatMessageRepository extends JpaRepository<EventChatMessage, Long> {

    @Query("SELECT m FROM EventChatMessage m " +
            "WHERE m.chatRoom.id = :chatRoomId " +
            "ORDER BY m.timestamp ASC")
    Page<EventChatMessage> findByChatRoomIdOrderByTimestampAsc(@Param("chatRoomId") Long chatRoomId,
                                                               Pageable pageable);
}


