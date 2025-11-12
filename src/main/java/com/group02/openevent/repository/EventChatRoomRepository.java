package com.group02.openevent.repository;

import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventChatRoomRepository extends JpaRepository<EventChatRoom, Long> {

    Optional<EventChatRoom> findByEventAndHostAndVolunteer(Event event, User host, User volunteer);

    @Query("SELECT r FROM EventChatRoom r " +
            "WHERE r.event.id = :eventId " +
            "AND (r.host.userId = :userId OR r.volunteer.userId = :userId)")
    List<EventChatRoom> findByEventIdAndParticipantId(@Param("eventId") Long eventId,
                                                      @Param("userId") Long userId);
}


