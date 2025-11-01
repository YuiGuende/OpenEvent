package com.group02.openevent.service;


import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.notification.Notification;
import org.springframework.stereotype.Service;

public interface INotificationService {


    Notification sendNotificationToEventParticipants(Long eventId, Notification notification);
}