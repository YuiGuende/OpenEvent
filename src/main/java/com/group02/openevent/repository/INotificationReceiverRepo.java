package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.notification.NotificationReceiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface INotificationReceiverRepo extends JpaRepository<NotificationReceiver, Long> {
    
    List<NotificationReceiver> findByReceiverOrderByNotification_CreatedAtDesc(Account receiver);
    
    @Query("SELECT nr FROM NotificationReceiver nr WHERE nr.receiver.accountId = :accountId ORDER BY nr.notification.createdAt DESC")
    List<NotificationReceiver> findByReceiverAccountIdOrderByCreatedAtDesc(@Param("accountId") Long accountId);
    
    @Query("SELECT COUNT(nr) FROM NotificationReceiver nr WHERE nr.receiver.accountId = :accountId AND nr.read = false")
    long countUnreadByReceiverAccountId(@Param("accountId") Long accountId);
    
    @Query("SELECT nr FROM NotificationReceiver nr WHERE nr.receiver.accountId = :accountId AND nr.read = false ORDER BY nr.notification.createdAt DESC")
    List<NotificationReceiver> findUnreadByReceiverAccountId(@Param("accountId") Long accountId);
    
    Optional<NotificationReceiver> findByReceiver_AccountIdAndNotification_NotificationId(Long accountId, Long notificationId);
}

