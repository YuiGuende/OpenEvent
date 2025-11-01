package com.group02.openevent.repository;

import com.group02.openevent.model.attendance.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IEventAttendanceRepo extends JpaRepository<EventAttendance, Long> {
    
    /**
     * Find attendance by event and email
     */
    Optional<EventAttendance> findByEventIdAndEmail(Long eventId, String email);
    
    /**
     * Find all attendances for an event
     */
    List<EventAttendance> findByEventId(Long eventId);
    
    /**
     * Find attendance by order and customer
     */
    Optional<EventAttendance> findByOrderOrderIdAndCustomerCustomerId(Long orderId, Long customerId);
    
    /**
     * Count attendances by event and status
     */
    @Query("SELECT COUNT(a) FROM EventAttendance a WHERE a.event.id = :eventId AND a.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") EventAttendance.AttendanceStatus status);
    
    /**
     * Get total check-ins for an event
     */
    @Query("SELECT COUNT(a) FROM EventAttendance a WHERE a.event.id = :eventId AND a.checkInTime IS NOT NULL")
    long countCheckedInByEventId(@Param("eventId") Long eventId);
    
    /**
     * Get total check-outs for an event
     */
    @Query("SELECT COUNT(a) FROM EventAttendance a WHERE a.event.id = :eventId AND a.checkOutTime IS NOT NULL")
    long countCheckedOutByEventId(@Param("eventId") Long eventId);
    
    /**
     * Get currently present attendees (checked-in but not checked-out)
     */
    @Query("SELECT COUNT(a) FROM EventAttendance a WHERE a.event.id = :eventId AND a.status = 'CHECKED_IN'")
    long countCurrentlyPresentByEventId(@Param("eventId") Long eventId);
    
    /**
     * Check if email already checked in for event
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM EventAttendance a " +
           "WHERE a.event.id = :eventId AND a.email = :email AND a.checkInTime IS NOT NULL")
    boolean existsByEventIdAndEmailAndCheckedIn(@Param("eventId") Long eventId, @Param("email") String email);
    
    /**
     * Count total attendances for an event
     */
    long countByEventId(Long eventId);
}

