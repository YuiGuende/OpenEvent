package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.notification.Notification;
import com.group02.openevent.model.notification.NotificationReceiver;
import com.group02.openevent.model.notification.NotificationType;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.INotificationRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.INotificationService;
import com.group02.openevent.service.UserService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class INotificationServiceImpl implements INotificationService {
    private final UserService userService;
    private Logger logger = LoggerFactory.getLogger(INotificationServiceImpl.class);
    private IOrderRepo orderRepo;

    private INotificationRepo iNotificationRepo;

    private IEventRepo eventRepo;

    public INotificationServiceImpl(IOrderRepo orderRepo, INotificationRepo iNotificationRepo, IEventRepo eventRepo, UserService userService) {
        this.orderRepo = orderRepo;
        this.iNotificationRepo = iNotificationRepo;
        this.eventRepo = eventRepo;
        this.userService = userService;
    }

    @Override
    @Transactional
    public Notification sendNotificationToEventParticipants(Long eventId, Notification notification) {
        // 1. Lấy Event và gán vào notification
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        notification.setEvent(event);

        // 2. Lấy danh sách Account ID của người tham gia
        List<Long> receiverAccountIds = orderRepo.findDistinctCustomerAccountIdsByEventIdAndStatusPaid(eventId);

        if (receiverAccountIds.isEmpty()) {
            // Không có người tham gia nào để gửi thông báo
            logger.error("receiverAccountIds is empty");
            return null;
        }

        // 3. Lấy đối tượng Account dựa trên IDs
        List<User> receivers = userService.findAllById(receiverAccountIds);

        // 4. Tạo các NotificationReceiver cho từng người nhận
        List<NotificationReceiver> notificationReceivers = receivers.stream()
                .map(account -> {
                    NotificationReceiver receiver = new NotificationReceiver();
                    receiver.setNotification(notification); // Liên kết ngược
                    receiver.setReceiver(account);
                    receiver.setRead(false);
                    return receiver;
                })
                .collect(Collectors.toList());

        // 5. Liên kết danh sách receivers với notification
        notification.setReceivers(notificationReceivers);
        notification.setType(NotificationType.HOST_TO_CUSTOMER);

        // 6. Lưu Notification (Notification đã có sẵn các thông tin cơ bản: sender, message, type, event)
        return iNotificationRepo.save(notification);
    }
}
