package com.group02.openevent.service.impl;

import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.service.EmailReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailReminderSericeImpl implements EmailReminderService {
    private final IEmailReminderRepo emailReminderRepository;

    /**
     * Lưu một yêu cầu nhắc nhở email mới hoặc cập nhật yêu cầu đã tồn tại.
     * Phương thức này được gọi bởi AgentEventService sau khi AI Agent xử lý.
     * * @param reminder Đối tượng EmailReminder cần lưu.
     * @return Đối tượng EmailReminder đã được lưu (có ID).
     */
    public EmailReminder save(EmailReminder reminder) {
        return emailReminderRepository.save(reminder);
    }

    /**
     * Lấy danh sách các nhắc nhở chưa gửi (dùng cho Service lập lịch gửi email).
     *
     * @return Danh sách các EmailReminder chưa gửi.
     */
    public List<EmailReminder> findPendingReminders() {
        return emailReminderRepository.findByIsSent(false);
    }


}

