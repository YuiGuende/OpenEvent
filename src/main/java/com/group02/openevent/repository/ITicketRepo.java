package com.group02.openevent.repository;

import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITicketRepo extends JpaRepository<Ticket, Long> {
    
    // Find tickets by user
    List<Ticket> findByUser(User user);
    
    // Find tickets by user ID
    List<Ticket> findByUser_UserId(Long userId);
    
    // Find tickets by ticket type
    List<Ticket> findByTicketType(TicketType ticketType);
    
    // Find tickets by ticket type ID
    List<Ticket> findByTicketType_TicketTypeId(Long ticketTypeId);
    
    // Find tickets by event (through ticket type)
    @Query("SELECT t FROM Ticket t WHERE t.ticketType.event.id = :eventId")
    List<Ticket> findByEventId(@Param("eventId") Long eventId);
    
    // Find tickets by user and event
    @Query("SELECT t FROM Ticket t WHERE t.user.userId = :userId AND t.ticketType.event.id = :eventId")
    List<Ticket> findByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);
    
    // Check if user has ticket for event
    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.user.userId = :userId AND t.ticketType.event.id = :eventId")
    boolean hasTicketForEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);
    
    // Get ticket count for event
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketType.event.id = :eventId")
    Long getTicketCountForEvent(@Param("eventId") Long eventId);
    
    // Get ticket count for ticket type
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketType.ticketTypeId = :ticketTypeId")
    Long getTicketCountForTicketType(@Param("ticketTypeId") Long ticketTypeId);
}