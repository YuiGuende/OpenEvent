package com.group02.openevent.repository;

import com.group02.openevent.model.chat.EventChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventChatRoomRepository extends JpaRepository<EventChatRoom, Long> {

    // Find Host-Department room
    @Query("SELECT r FROM EventChatRoom r " +
            "WHERE r.host.userId = :hostUserId " +
            "AND r.roomType = 'HOST_DEPARTMENT'")
    Optional<EventChatRoom> findByHostAndRoomType(@Param("hostUserId") Long hostUserId);

    // Find Host-Volunteers room for an event
    @Query("SELECT r FROM EventChatRoom r " +
            "WHERE r.event.id = :eventId " +
            "AND r.host.userId = :hostUserId " +
            "AND r.roomType = 'HOST_VOLUNTEERS'")
    Optional<EventChatRoom> findByEventAndHostAndRoomType(@Param("eventId") Long eventId,
                                                          @Param("hostUserId") Long hostUserId);

    // Find all rooms where user is a participant (host, department, or volunteer)
    @Query("SELECT DISTINCT r FROM EventChatRoom r " +
            "LEFT JOIN r.participants p " +
            "WHERE (r.host.userId = :userId " +
            "   OR r.department.userId = :userId " +
            "   OR p.user.userId = :userId)")
    List<EventChatRoom> findByParticipantId(@Param("userId") Long userId);

    // Find rooms for a specific event where user is a participant
    @Query("SELECT DISTINCT r FROM EventChatRoom r " +
            "LEFT JOIN r.participants p " +
            "WHERE r.event.id = :eventId " +
            "AND (r.host.userId = :userId " +
            "   OR r.department.userId = :userId " +
            "   OR p.user.userId = :userId)")
    List<EventChatRoom> findByEventIdAndParticipantId(@Param("eventId") Long eventId,
                                                      @Param("userId") Long userId);

    // Find rooms for user: includes HOST_DEPARTMENT (no event) and HOST_VOLUNTEERS (for event)
    @Query("SELECT DISTINCT r FROM EventChatRoom r " +
            "LEFT JOIN r.participants p " +
            "WHERE (" +
            "   (r.roomType = 'HOST_DEPARTMENT' " +
            "    AND (r.host.userId = :userId OR r.department.userId = :userId)) " +
            "   OR " +
            "   (r.roomType = 'HOST_VOLUNTEERS' " +
            "    AND r.event.id = :eventId " +
            "    AND (r.host.userId = :userId OR p.user.userId = :userId))" +
            ")")
    List<EventChatRoom> findByEventIdAndParticipantIdIncludingDepartment(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId);
}


