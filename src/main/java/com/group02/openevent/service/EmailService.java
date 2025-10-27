package com.group02.openevent.service;

import com.group02.openevent.model.event.Event;

/**
 * Service for sending emails
 */
public interface EmailService {
    
    /**
     * Send event reminder email to user
     * @param userEmail User's email address
     * @param event Event to remind about
     * @param minutesBefore Minutes before event starts
     */
    void sendEventReminderEmail(String userEmail, Event event, int minutesBefore);
    
    /**
     * Send test email
     * @param to Recipient email
     * @param subject Email subject
     * @param content Email content
     */
    void sendSimpleEmail(String to, String subject, String content);
}

