package com.group02.openevent.service;

import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;

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
}



