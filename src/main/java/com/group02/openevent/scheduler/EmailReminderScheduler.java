package com.group02.openevent.scheduler;

import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduler to check and send email reminders for upcoming events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailReminderScheduler {
    
    private final IEmailReminderRepo emailReminderRepo;
    private final EmailService emailService;
    private final ICustomerRepo customerRepo;

    /**
     * Check and send pending email reminders every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    @Transactional
    public void checkAndSendReminders() {
        try {
            log.info("🔍 Checking for pending email reminders...");
            
            // Get all pending (unsent) reminders
            List<EmailReminder> pendingReminders = emailReminderRepo.findByIsSent(false);
            
            if (pendingReminders.isEmpty()) {
                log.debug("No pending reminders found");
                return;
            }
            
            log.info("Found {} pending reminders to process", pendingReminders.size());
            
            LocalDateTime now = LocalDateTime.now();
            int sentCount = 0;
            
            for (EmailReminder reminder : pendingReminders) {
                try {
                    Event event = reminder.getEvent();
                    
                    // Check if event still exists
                    if (event == null) {
                        log.warn("Event not found for reminder ID: {}, marking as sent", reminder.getId());
                        reminder.setSent(true);
                        emailReminderRepo.save(reminder);
                        continue;
                    }
                    
                    // Force load event data to avoid LazyInitializationException
                    // Access the event properties to trigger lazy loading
                    String eventTitle = event.getTitle(); // Force load title
                    LocalDateTime eventStartTime = event.getStartsAt();
                    
                    // Check if event has start time
                    if (eventStartTime == null) {
                        log.warn("Event {} ({}) has no start time, skipping reminder ID: {}", 
                            event.getId(), eventTitle, reminder.getId());
                        reminder.setSent(true);
                        emailReminderRepo.save(reminder);
                        continue;
                    }
                    
                    // Force load end time to avoid LazyInitializationException
                    LocalDateTime eventEndTime = event.getEndsAt(); // Force load end time
                    LocalDateTime reminderTime = eventStartTime.minusMinutes(reminder.getRemindMinutes());
                    
                    // Use eventEndTime in debug log to avoid unused variable warning
                    log.debug("Processing reminder for event {} (starts: {}, ends: {})", 
                        eventTitle, eventStartTime, eventEndTime);
                    
                    // Check if event has already started - don't send reminder
                    if (now.isAfter(eventStartTime) || now.isEqual(eventStartTime)) {
                        log.info("Event {} has already started, marking reminder ID: {} as sent", 
                            event.getId(), reminder.getId());
                        reminder.setSent(true);
                        emailReminderRepo.save(reminder);
                        continue;
                    }
                    
                    // Check if reminder time is too far in the past (> 1 day) - don't send
                    if (reminderTime.isBefore(now.minusDays(1))) {
                        log.info("Reminder time {} is too far in the past (> 1 day), marking reminder ID: {} as sent", 
                            reminderTime, reminder.getId());
                        reminder.setSent(true);
                        emailReminderRepo.save(reminder);
                        continue;
                    }
                    
                    // Check if it's time to send reminder (within 5 minute window)
                    if (now.isAfter(reminderTime) || now.isEqual(reminderTime)) {
                        // Get user email
                        Optional<Customer> customerOpt = customerRepo.findByUser_Account_AccountId(reminder.getUserId());
                        
                        if (customerOpt.isEmpty() || customerOpt.get().getUser() == null 
                            || customerOpt.get().getUser().getAccount() == null) {
                            log.warn("User not found for reminder ID: {}, marking as sent", reminder.getId());
                            reminder.setSent(true);
                            emailReminderRepo.save(reminder);
                            continue;
                        }
                        
                        String userEmail = customerOpt.get().getUser().getAccount().getEmail();
                        
                        // Check if user has email
                        if (userEmail == null || userEmail.trim().isEmpty()) {
                            log.warn("User {} has no email, skipping reminder ID: {}", 
                                reminder.getUserId(), reminder.getId());
                            reminder.setSent(true);
                            emailReminderRepo.save(reminder);
                            continue;
                        }
                        
                        // Validate email format
                        if (!isValidEmailFormat(userEmail)) {
                            log.warn("Invalid email format: {} for reminder ID: {}, marking as sent", 
                                userEmail, reminder.getId());
                            reminder.setSent(true);
                            emailReminderRepo.save(reminder);
                            continue;
                        }
                        
                        // Send email - only mark as sent if successful
                        try {
                            emailService.sendEventReminderEmail(userEmail, event, reminder.getRemindMinutes());
                            
                            // Mark as sent ONLY after successful email send
                            reminder.setSent(true);
                            emailReminderRepo.save(reminder);
                            
                            sentCount++;
                            log.info("✅ Sent reminder to {} for event: {}", userEmail, event.getTitle());
                            
                        } catch (Exception emailException) {
                            log.error("❌ Failed to send email to {} for reminder ID {}: {}", 
                                userEmail, reminder.getId(), emailException.getMessage());
                            // DON'T mark as sent - allow retry next time
                            // Could add retry counter to limit retries if needed
                        }
                        
                    } else {
                        log.debug("Reminder ID {} not yet due (send at: {})", 
                                reminder.getId(), reminderTime);
                    }
                    
                } catch (Exception e) {
                    log.error("❌ Failed to process reminder ID {}: {}", 
                            reminder.getId(), e.getMessage(), e);
                    // Continue processing other reminders
                }
            }
            
            if (sentCount > 0) {
                log.info("✅ Successfully sent {} email reminders", sentCount);
            }
            
        } catch (Exception e) {
            log.error("❌ Error in email reminder scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Basic email format validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
    
    /**
     * Cleanup old sent reminders (optional - run daily)
     * Removes reminders that were sent more than 30 days ago
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void cleanupOldReminders() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            List<EmailReminder> oldReminders = emailReminderRepo.findByIsSentAndCreatedAtBefore(true, cutoffDate);
            
            if (!oldReminders.isEmpty()) {
                emailReminderRepo.deleteAll(oldReminders);
                log.info("🗑️ Cleaned up {} old email reminders", oldReminders.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Error cleaning up old reminders: {}", e.getMessage(), e);
        }
    }
}

