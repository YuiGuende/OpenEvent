//package com.group02.openevent.repository;
//
//
//import com.group02.openevent.model.notification.Notification;
//import com.group02.openevent.model.notification.NotificationType;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface INotificationRepo extends JpaRepository<Notification, Long> {
//
//    List<Notification> findByReceiverAccountId(Long receiverId);
//    List<Notification> findBySenderAccountId(Long senderId);
//    List<Notification> findByType(NotificationType type);
//    List<Notification> findByIsRead(Boolean isRead);
//
//    // Pageable listing
//    Page<Notification> findAll(Pageable pageable);
//    Page<Notification> findByReceiverAccountId(Long receiverId, Pageable pageable);
//    Page<Notification> findByType(NotificationType type, Pageable pageable);
//    Page<Notification> findByIsRead(Boolean isRead, Pageable pageable);
//}
