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
                    String eventTitle = event.getTitle();
                    LocalDateTime eventStartTime = event.getStartsAt();
                    LocalDateTime eventEndTime = event.getEndsAt();
                    LocalDateTime reminderTime = eventStartTime.minusMinutes(reminder.getRemindMinutes());
                    
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
                        
                        // Send email
                        emailService.sendEventReminderEmail(userEmail, event, reminder.getRemindMinutes());
                        
                        // Mark as sent
                        reminder.setSent(true);
                        emailReminderRepo.save(reminder);
                        
                        sentCount++;
                        log.info("✅ Sent reminder to {} for event: {}", userEmail, event.getTitle());
                        
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

