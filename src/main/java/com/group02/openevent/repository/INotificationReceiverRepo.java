package com.group02.openevent.repository;

import com.group02.openevent.model.notification.NotificationReceiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface INotificationReceiverRepo extends JpaRepository<NotificationReceiver, Long> {

    
    @Query("SELECT nr FROM NotificationReceiver nr WHERE nr.receiver.userId = :userId ORDER BY nr.notification.createdAt DESC")
    List<NotificationReceiver> findByReceiverUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(nr) FROM NotificationReceiver nr WHERE nr.receiver.userId = :userId AND nr.read = false")
    long countUnreadByReceiverUserId(@Param("userId") Long userId);
    
    @Query("SELECT nr FROM NotificationReceiver nr WHERE nr.receiver.userId = :userId AND nr.read = false ORDER BY nr.notification.createdAt DESC")
    List<NotificationReceiver> findUnreadByReceiverUserId(@Param("userId") Long userId);
    
    Optional<NotificationReceiver> findByReceiver_UserIdAndNotification_NotificationId(Long userId, Long notificationId);
}

