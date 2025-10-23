package com.group02.openevent.repository;


import com.group02.openevent.model.notification.Notification;
import com.group02.openevent.model.notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface INotificationRepo extends JpaRepository<Notification, Long> {

}
