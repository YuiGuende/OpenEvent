package com.group02.openevent.repository;

import com.group02.openevent.model.chat.EventChatRoomParticipant;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventChatRoomParticipantRepository extends JpaRepository<EventChatRoomParticipant, Long> {

    List<EventChatRoomParticipant> findByRoom_Id(Long roomId);

    Optional<EventChatRoomParticipant> findByRoom_IdAndUser_UserId(Long roomId, Long userId);

    @Query("SELECT p FROM EventChatRoomParticipant p " +
            "WHERE p.room.id = :roomId " +
            "AND p.user.userId = :userId")
    Optional<EventChatRoomParticipant> findByRoomIdAndUserId(@Param("roomId") Long roomId,
                                                              @Param("userId") Long userId);

    @Query("SELECT p.user.userId FROM EventChatRoomParticipant p " +
            "WHERE p.room.id = :roomId")
    List<Long> findUserIdsByRoomId(@Param("roomId") Long roomId);
}

