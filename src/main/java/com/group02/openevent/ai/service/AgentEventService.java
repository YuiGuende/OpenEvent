package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service để xử lý các hành động event từ AI Agent
 * @author Admin
 */
@Service
public class AgentEventService {
    @Autowired
    private EmailReminderService emailReminderService;
    @Autowired
    private EventService eventService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private HostService hostService;
    
    @Autowired
    private OrganizationService organizationService;

    /**
     * Lưu yêu cầu nhắc nhở email vào cơ sở dữ liệu.
     * @param eventId ID sự kiện cần nhắc nhở.
     * @param remindMinutes Số phút nhắc trước thời gian bắt đầu sự kiện.
     */
    public void saveEmailReminder(Long eventId, int remindMinutes, Long userId) {
        // 1. Tạo đối tượng EmailReminder
        Event event = eventService.getEventByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện có ID: " + eventId + " để tạo nhắc nhở."));

        EmailReminder reminder = new EmailReminder();
        // Gán Khóa ngoại Event object (Thay thế cho việc set eventId thô)
        reminder.setEvent(event);
        reminder.setUserId(userId);
        reminder.setRemindMinutes(remindMinutes);
        reminder.setCreatedAt(LocalDateTime.now());
        reminder.setSent(false);

        // 3. Lưu vào Database
        emailReminderService.save(reminder);
        System.out.println("lưu được rồi e");
    }

    // ✅ Lưu sự kiện từ Action (ADD_EVENT) - New version with userId
    public void saveEventFromAction(Action action, Long userId) {
        try {
            Map<String, Object> args = action.getArgs();

            Event event = new Event();
            event.setTitle((String) args.get("title"));
            event.setDescription((String) args.getOrDefault("description", ""));
            event.setStartsAt(tryParseDateTime((String) args.get("start_time")));
            event.setEndsAt(tryParseDateTime((String) args.get("end_time")));
            event.setCreatedAt(LocalDateTime.now());
            event.setStatus(EventStatus.DRAFT); // mặc định khi tạo
            event.setEventType(EventType.OTHERS);

            // Get organization_id if provided
            Long organizationId = null;
            if (args.containsKey("organization_id") && args.get("organization_id") != null) {
                organizationId = Long.valueOf(args.get("organization_id").toString());
            }

            // Use createEventByCustomer to handle host creation and organization assignment
            Event savedEvent = createEventByCustomer(userId, event, organizationId);
            System.out.println("✅ Đã lưu sự kiện: " + savedEvent.getTitle());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Lỗi khi lưu sự kiện: " + e.getMessage());
        }
    }

    public void saveEvent(Event event) {
        try {
            eventService.saveEvent(event);
            System.out.println("✅ Đã lưu sự kiện: " + event.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Lỗi khi lưu sự kiện: " + e.getMessage());
        }
    }

    // ✅ Cập nhật sự kiện từ Action (UPDATE_EVENT)
    public void updateEventFromAction(Action action) {
        try {
            Map<String, Object> args = action.getArgs();
            Event existing = null;

            if (args.containsKey("event_id")) {
                Long id = (Long) args.get("event_id");
                Optional<Event> eventOpt = eventService.getEventByEventId(id);
                if (eventOpt.isPresent()) {
                    existing = eventOpt.get();
                }
            } else if (args.containsKey("original_title")) {
                String oriTitle = (String) args.get("original_title");
                Optional<Event> eventOpt = eventService.getFirstEventByTitle(oriTitle);
                if (eventOpt.isPresent()) {
                    existing = eventOpt.get();
                }
            }

            if (existing == null) {
                System.out.println("❌ Không tìm thấy sự kiện để cập nhật.");
                return;
            }

            if (args.containsKey("title")) existing.setTitle((String) args.get("title"));
            if (args.containsKey("description")) existing.setDescription((String) args.get("description"));
            if (args.containsKey("start_time")) existing.setStartsAt(tryParseDateTime((String) args.get("start_time")));
            if (args.containsKey("end_time")) existing.setEndsAt(tryParseDateTime((String) args.get("end_time")));

            existing.setCreatedAt(LocalDateTime.now());

            eventService.saveEventAgent(existing);
            System.out.println("🔄 Đã cập nhật sự kiện: " + existing.getTitle());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Lỗi khi cập nhật sự kiện: " + e.getMessage());
        }
    }

    // ✅ Xóa sự kiện từ Action (DELETE_EVENT)
    public void deleteEventFromAction(Action action) {
        try {
            Map<String, Object> args = action.getArgs();
            boolean deleted = false;

            if (args.containsKey("event_id")) {
                Long id = (Long) args.get("event_id");
                deleted = eventService.removeEvent(id);
            } else if (args.containsKey("title")) {
                String title = (String) args.get("title");
                deleted = eventService.deleteByTitle(title);
            }

            if (deleted) {
                System.out.println("🗑️ Đã xoá sự kiện thành công");
            } else {
                System.out.println("⚠️ Không tìm thấy sự kiện để xoá.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Lỗi khi xóa sự kiện: " + e.getMessage());
        }
    }

    // ✅ Parse datetime
    private static LocalDateTime tryParseDateTime(String input) {
        List<String> patterns = List.of(
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd HH:mm",
                "dd/MM/yyyy HH:mm",
                "dd-MM-yyyy HH:mm"
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(input, formatter);
            } catch (Exception ignored) {}
        }

        throw new IllegalArgumentException("❌ Không thể parse ngày giờ: " + input);
    }

    /**
     * Tạo sự kiện bởi Customer với auto-promote Customer thành Host nếu cần
     * @param userId ID của user (accountId)
     * @param draft Event draft cần tạo
     * @param organizationId ID của organization (optional)
     * @return Event đã được lưu
     */
    public Event createEventByCustomer(Long userId, Event draft, @Nullable Long organizationId) {
        // 1) Load or create customer
        Customer c = customerService.getOrCreateByUserId(userId);
        
        // 2) Find or create Host (idempotent)
        Host h = c.getHost();
        if (h == null) {
            h = hostService.findByCustomerId(c.getCustomerId()).orElseGet(() -> {
                Host nh = new Host();
                nh.setCustomer(c);
                // Set default host name from account email if available
                if (c.getAccount() != null && c.getAccount().getEmail() != null) {
                    nh.setHostName(c.getAccount().getEmail().split("@")[0]);
                } else {
                    nh.setHostName("Host_" + c.getCustomerId());
                }
                return hostService.save(nh);
            });
        }
        
        // 3) Required host
        draft.setHost(h);
        
        // 4) Optional organization
        if (organizationId != null) {
            Organization org = organizationService.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization không tồn tại"));
            draft.setOrganization(org);
        } else {
            draft.setOrganization(null);
        }
        
        // 5) Safe defaults
        if (draft.getStatus() == null) draft.setStatus(EventStatus.DRAFT);
        if (draft.getEventType() == null) draft.setEventType(EventType.OTHERS);
        if (draft.getCreatedAt() == null) draft.setCreatedAt(LocalDateTime.now());

        return eventService.saveEventAgent(draft);
    }
}
