package com.group02.openevent.repository;

import com.group02.openevent.model.attendance.EventAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Find attendance by order ID
     */
    Optional<EventAttendance> findByOrder_OrderId(Long orderId);
    
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

    Page<EventAttendance>findByEvent_Id(Long eventId, Pageable pageable);

    @Query("SELECT ea FROM EventAttendance ea " +
            "WHERE ea.order.event.id = :eventId " +
            "AND (ea.order.participantName LIKE %:search% " +
            "OR ea.order.participantEmail LIKE %:search% " +
            "OR ea.order.participantPhone LIKE %:search%)")
    Page<EventAttendance> searchAttendees(@Param("eventId") Long eventId,
                                          @Param("search") String search,
                                          Pageable pageable);
    // Câu query chính cho Pageable
    @Query(value = "SELECT ea FROM EventAttendance ea " +
            "LEFT JOIN FETCH ea.order o " +
            "LEFT JOIN FETCH o.ticketType tt " +
            "WHERE ea.event.id = :eventId " +
            "AND (:ticketTypeId IS NULL OR tt.ticketTypeId = :ticketTypeId) " +
            "AND (:paymentStatus IS NULL OR CAST(o.status AS STRING) = :paymentStatus) " +
            "AND (:checkinStatus IS NULL OR " +
            "  (:checkinStatus = 'CHECKED_IN' AND ea.checkInTime IS NOT NULL) OR " +
            "  (:checkinStatus = 'NOT_CHECKED_IN' AND ea.checkInTime IS NULL))",
            countQuery = "SELECT COUNT(ea) FROM EventAttendance ea " + // Câu query COUNT riêng
                    "WHERE ea.event.id = :eventId " +
                    "AND (:ticketTypeId IS NULL OR ea.order.ticketType.ticketTypeId = :ticketTypeId) " +
                    "AND (:paymentStatus IS NULL OR CAST(ea.order.status AS STRING) = :paymentStatus) " +
                    "AND (:checkinStatus IS NULL OR " +
                    "  (:checkinStatus = 'CHECKED_IN' AND ea.checkInTime IS NOT NULL) OR " +
                    "  (:checkinStatus = 'NOT_CHECKED_IN' AND ea.checkInTime IS NULL))")
    Page<EventAttendance> filterAttendees(@Param("eventId") Long eventId,
                                          @Param("ticketTypeId") Long ticketTypeId,
                                          @Param("paymentStatus") String paymentStatus,
                                          @Param("checkinStatus") String checkinStatus,
                                          Pageable pageable);
    @Query("SELECT DISTINCT ea FROM EventAttendance ea " +
            "LEFT JOIN FETCH ea.order o " +
            "LEFT JOIN FETCH o.ticketType tt " +
            "WHERE ea.event.id = :eventId " +
            "AND (:ticketTypeId IS NULL OR tt.ticketTypeId = :ticketTypeId) " +
            "AND (:paymentStatus IS NULL OR CAST(o.status AS STRING) = :paymentStatus) " +
            "AND (:checkinStatus IS NULL OR " +
            "  (:checkinStatus = 'CHECKED_IN' AND ea.checkInTime IS NOT NULL) OR " +
            "  (:checkinStatus = 'NOT_CHECKED_IN' AND ea.checkInTime IS NULL))")
    List<EventAttendance> filterAttendees(
            @Param("eventId") Long eventId,
            @Param("ticketTypeId") Long ticketTypeId,
            @Param("paymentStatus") String paymentStatus,
            @Param("checkinStatus") String checkinStatus);
    Optional<EventAttendance> findByEvent_IdAndAttendanceId(Long eventId, Long attendanceId);
}

