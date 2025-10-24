package com.group02.openevent.service.impl;

import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.EventAttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventAttendanceServiceImpl implements EventAttendanceService {
    
    @Autowired
    private IEventAttendanceRepo attendanceRepo;
    
    @Autowired
    private IEventRepo eventRepo;
    
    @Override
    @Transactional
    public EventAttendance checkIn(Long eventId, AttendanceRequest request) {
        log.info("Processing check-in for event {} with email {}", eventId, request.getEmail());
        
        // Validate event exists
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        
        // Check if already checked in
        Optional<EventAttendance> existingOpt = attendanceRepo.findByEventIdAndEmail(eventId, request.getEmail());
        
        if (existingOpt.isPresent()) {
            EventAttendance existing = existingOpt.get();
            
            if (existing.getStatus() == EventAttendance.AttendanceStatus.CHECKED_IN 
                || existing.getStatus() == EventAttendance.AttendanceStatus.CHECKED_OUT) {
                throw new RuntimeException("Email này đã check-in lúc " + existing.getCheckInTime());
            }
            
            // Update existing pending attendance
            existing.setCheckInTime(LocalDateTime.now());
            existing.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
            existing.setFullName(request.getFullName());
            existing.setPhone(request.getPhone());
            existing.setOrganization(request.getOrganization());
            
            return attendanceRepo.save(existing);
        }
        
        // Create new attendance record
        EventAttendance attendance = new EventAttendance();
        attendance.setEvent(event);
        attendance.setFullName(request.getFullName());
        attendance.setEmail(request.getEmail());
        attendance.setPhone(request.getPhone());
        attendance.setOrganization(request.getOrganization());
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
        
        return attendanceRepo.save(attendance);
    }
    
    @Override
    @Transactional
    public EventAttendance checkOut(Long eventId, String email) {
        log.info("Processing check-out for event {} with email {}", eventId, email);
        
        // Find attendance record
        EventAttendance attendance = attendanceRepo.findByEventIdAndEmail(eventId, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin check-in với email: " + email));
        
        // Validate status
        if (attendance.getStatus() != EventAttendance.AttendanceStatus.CHECKED_IN) {
            throw new RuntimeException("Bạn chưa check-in hoặc đã check-out rồi");
        }
        
        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Bạn đã check-out lúc " + attendance.getCheckOutTime());
        }
        
        // Update check-out
        attendance.setCheckOutTime(LocalDateTime.now());
        attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_OUT);
        
        return attendanceRepo.save(attendance);
    }
    
    @Override
    public List<EventAttendance> getAttendancesByEventId(Long eventId) {
        return attendanceRepo.findByEventId(eventId);
    }
    
    @Override
    public Optional<EventAttendance> getAttendanceByEventAndEmail(Long eventId, String email) {
        return attendanceRepo.findByEventIdAndEmail(eventId, email);
    }
    
    @Override
    public AttendanceStatsDTO getAttendanceStats(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        
        long totalCheckedIn = attendanceRepo.countCheckedInByEventId(eventId);
        long totalCheckedOut = attendanceRepo.countCheckedOutByEventId(eventId);
        long currentlyPresent = attendanceRepo.countCurrentlyPresentByEventId(eventId);
        long totalAttendees = attendanceRepo.countByEventId(eventId);
        
        return new AttendanceStatsDTO(
                eventId,
                event.getTitle(),
                totalCheckedIn,
                totalCheckedOut,
                currentlyPresent,
                totalAttendees
        );
    }
    
    @Override
    public boolean isAlreadyCheckedIn(Long eventId, String email) {
        return attendanceRepo.existsByEventIdAndEmailAndCheckedIn(eventId, email);
    }
}



