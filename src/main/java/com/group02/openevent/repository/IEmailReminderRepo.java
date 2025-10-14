package com.group02.openevent.repository;

import com.group02.openevent.model.email.EmailReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IEmailReminderRepo extends JpaRepository<EmailReminder, Long> {
    
    /**
     * Find all unsent email reminders
     * @param isSent False for pending reminders, true for sent reminders
     * @return List of email reminders
     */
    List<EmailReminder> findByIsSent(boolean isSent);
    
    /**
     * Find sent reminders created before a certain date (for cleanup)
     * @param isSent Sent status
     * @param createdAt Cutoff date
     * @return List of old reminders
     */
    List<EmailReminder> findByIsSentAndCreatedAtBefore(boolean isSent, LocalDateTime createdAt);
}
