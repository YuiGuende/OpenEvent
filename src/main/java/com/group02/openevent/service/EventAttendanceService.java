package com.group02.openevent.service;

import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EventAttendanceService {
    
    /**
     * Process check-in for an event
     */
    EventAttendance checkIn(Long eventId, AttendanceRequest request);
    
    /**
     * Process check-out for an event
     */
    EventAttendance checkOut(Long eventId, String email);
    
    /**
     * Get all attendances for an event
     */
    List<EventAttendance> getAttendancesByEventId(Long eventId);
    
    /**
     * Get attendance by event and email
     */
    Optional<EventAttendance> getAttendanceByEventAndEmail(Long eventId, String email);
    
    /**
     * Get attendance statistics for an event
     */
    AttendanceStatsDTO getAttendanceStats(Long eventId);
    
    /**
     * Check if email already checked in
     */
    boolean isAlreadyCheckedIn(Long eventId, String email);
    public EventAttendance listCheckIn(Long id,Long attendanceId);
    public EventAttendance checkOut(Long id,Long attendanceId);
    public EventAttendance addAttendee(Long eventId, String name, String email,
                                       String phone, Long ticketTypeId, String organization);
    public EventAttendance updateAttendee(Long id,Long attendanceId, String name, String email,
                                          String phone, String organization);
    public void deleteAttendee(Long id, Long attendanceId);
    public Page<EventAttendance> filterAttendees(
            Long eventId,
            Long ticketTypeFilter,
            String paymentStatusFilter,
            String checkinStatusFilter,
            Pageable pageable);
    public List<EventAttendance> filterAttendees(
            Long eventId,
            Long ticketTypeFilter,
            String paymentStatusFilter,
            String checkinStatusFilter);
    
    /**
     * Tạo EventAttendance từ Order khi order được thanh toán thành công
     * @param order Order đã được thanh toán (status = PAID)
     * @return EventAttendance đã được tạo
     */
    EventAttendance createAttendanceFromOrder(Order order);
}



