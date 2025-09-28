package com.group02.openevent.repository;

import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITicketTypeRepo extends JpaRepository<TicketType, Long> {
    
    // Find ticket types by event
    List<TicketType> findByEvent(Event event);
    
    // Find ticket types by event ID
    List<TicketType> findByEvent_Id(Long eventId);
    
    // Find ticket type by event and name
    Optional<TicketType> findByEventAndName(Event event, String name);
    
    // Find ticket type by event ID and name
    Optional<TicketType> findByEvent_IdAndName(Long eventId, String name);
    
    // Check if ticket type exists for event
    @Query("SELECT COUNT(t) > 0 FROM TicketType t WHERE t.event.id = :eventId")
    boolean existsByEventId(@Param("eventId") Long eventId);
    
    // Get ticket type count for event
    @Query("SELECT COUNT(t) FROM TicketType t WHERE t.event.id = :eventId")
    Long getTicketTypeCountForEvent(@Param("eventId") Long eventId);
}
