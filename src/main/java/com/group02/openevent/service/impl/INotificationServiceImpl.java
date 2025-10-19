package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.notification.Notification;
import com.group02.openevent.model.notification.NotificationReceiver;
import com.group02.openevent.model.notification.NotificationType;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.INotificationRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.INotificationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class INotificationServiceImpl implements INotificationService {
    private Logger logger = LoggerFactory.getLogger(INotificationServiceImpl.class);
    private IOrderRepo orderRepo;

    private IAccountRepo accountRepo;

    private INotificationRepo iNotificationRepo;

    public INotificationServiceImpl(IOrderRepo orderRepo, IAccountRepo accountRepo, INotificationRepo iNotificationRepo) {
        this.orderRepo = orderRepo;
        this.accountRepo = accountRepo;
        this.iNotificationRepo = iNotificationRepo;
    }

    @Override
    @Transactional
    public Notification sendNotificationToEventParticipants(Long eventId, Notification notification) {
        // 1. Lấy danh sách Account ID của người tham gia
        List<Long> receiverAccountIds = orderRepo.findDistinctCustomerAccountIdsByEventIdAndStatusPaid(eventId);

        if (receiverAccountIds.isEmpty()) {
            // Không có người tham gia nào để gửi thông báo
            logger.error("receiverAccountIds is empty");
            return null;
        }

        // 2. Lấy đối tượng Account dựa trên IDs
        List<Account> receivers = accountRepo.findAllById(receiverAccountIds);

        // 3. Tạo các NotificationReceiver cho từng người nhận
        List<NotificationReceiver> notificationReceivers = receivers.stream()
                .map(account -> {
                    NotificationReceiver receiver = new NotificationReceiver();
                    receiver.setNotification(notification); // Liên kết ngược
                    receiver.setReceiver(account);
                    receiver.setRead(false);
                    return receiver;
                })
                .collect(Collectors.toList());

        // 4. Liên kết danh sách receivers với notification
        notification.setReceivers(notificationReceivers);
        notification.setType(NotificationType.HOST_TO_CUSTOMER);

        // 5. Lưu Notification (Notification đã có sẵn các thông tin cơ bản: sender, message, type)
        return iNotificationRepo.save(notification);
    }
}
