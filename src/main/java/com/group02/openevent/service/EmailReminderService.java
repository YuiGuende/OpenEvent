package com.group02.openevent.service;

import com.group02.openevent.model.email.EmailReminder;

import java.util.List;
import java.util.Optional;

public interface EmailReminderService {
    EmailReminder save(EmailReminder reminder);
    List<EmailReminder> findPendingReminders();
    Optional<EmailReminder> findByEventIdAndUserId(Long eventId, Long userId);
}
