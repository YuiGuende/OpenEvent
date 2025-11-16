package com.group02.openevent.service;

import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.user.Customer;

/**
 * Service for face-based check-in functionality
 */
public interface FaceCheckinService {
    
    /**
     * Process face-based check-in for an event
     * 
     * @param eventId The event ID
     * @param capturedImage The image captured from camera (byte array)
     * @param currentCustomer The currently authenticated customer
     * @return EventAttendance after successful check-in
     * @throws RuntimeException if validation fails or face doesn't match
     */
    EventAttendance faceCheckIn(Long eventId, byte[] capturedImage, Customer currentCustomer);
}

