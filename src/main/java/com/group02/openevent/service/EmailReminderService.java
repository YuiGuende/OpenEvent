package com.group02.openevent.service;

import com.group02.openevent.model.email.EmailReminder;

import java.util.List;

public interface EmailReminderService {
    EmailReminder save(EmailReminder reminder);
    List<EmailReminder> findPendingReminders();
}
