package com.group02.openevent.repository;

import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.ticket.TicketStatus;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITicketRepo extends JpaRepository<Ticket, Long> {
    
    // Tìm ticket theo ticket code
    Optional<Ticket> findByTicketCode(String ticketCode);
    
    // Tìm tickets theo user
    List<Ticket> findByUser(User user);
    
    // Tìm tickets theo user và status
    List<Ticket> findByUserAndStatus(User user, TicketStatus status);
    
    // Tìm tickets theo status
    List<Ticket> findByStatus(TicketStatus status);
    
    // Tìm tickets theo user ID
    @Query("SELECT t FROM Ticket t WHERE t.user.userId = :userId ORDER BY t.purchaseDate DESC")
    List<Ticket> findByUserId(@Param("userId") Long userId);
    
    // Tìm tickets theo user ID và status
    @Query("SELECT t FROM Ticket t WHERE t.user.userId = :userId AND t.status = :status ORDER BY t.purchaseDate DESC")
    List<Ticket> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TicketStatus status);
    
    // Đếm số tickets theo user và status
    long countByUserAndStatus(User user, TicketStatus status);
    
    // Tìm tickets đang pending (chưa thanh toán)
    @Query("SELECT t FROM Ticket t WHERE t.status = 'PENDING' AND t.purchaseDate < :expiredTime")
    List<Ticket> findExpiredPendingTickets(@Param("expiredTime") java.time.LocalDateTime expiredTime);
    
    // Tìm tickets theo event
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId ORDER BY t.purchaseDate DESC")
    List<Ticket> findByEventId(@Param("eventId") Long eventId);
    
    // Tìm tickets theo event và status
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.status = :status ORDER BY t.purchaseDate DESC")
    List<Ticket> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") TicketStatus status);
    
    // Kiểm tra user đã có ticket cho event chưa
    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.user.userId = :userId AND t.event.id = :eventId AND t.status = 'PAID'")
    boolean hasUserRegisteredEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);
}
