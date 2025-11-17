package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.ai.dto.EventItem;
import com.group02.openevent.ai.mapper.AIEventMapper;
import com.group02.openevent.model.email.EmailReminder;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IEmailReminderRepo;
import com.group02.openevent.service.*;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AgentEventService {
    @Autowired
    private EmailReminderService emailReminderService;
    @Autowired
    private EventService eventService;
    @Autowired
    private AIEventMapper AIEventMapper;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private HostService hostService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private IEventRepo  eventRepo;
    @Autowired
    private IEmailReminderRepo emailReminderRepo;
    @Autowired
    private UserService userService;

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

    /**
     * Tạo hoặc cập nhật lịch nhắc nhở email mặc định cho sự kiện.
     * @param eventId ID sự kiện cần nhắc nhở.
     * @param remindMinutes Số phút nhắc trước thời gian bắt đầu sự kiện.
     * @param userId ID của người dùng.
     */
    @Transactional
    public void createOrUpdateEmailReminder(Long eventId, int remindMinutes, Long userId) {
        try {
            // Kiểm tra xem đã có lịch nhắc nhở cho sự kiện này chưa
            Optional<EmailReminder> existingReminder = emailReminderRepo.findByEventIdAndUserId(eventId, userId);
            
            if (existingReminder.isPresent()) {
                // Cập nhật lịch nhắc nhở hiện có
                EmailReminder reminder = existingReminder.get();
                reminder.setRemindMinutes(remindMinutes);
                reminder.setCreatedAt(LocalDateTime.now());
                reminder.setSent(false); // Reset trạng thái gửi
                emailReminderRepo.save(reminder);
                log.info("🔄 Đã cập nhật lịch nhắc nhở cho sự kiện ID: {} với {} phút trước", eventId, remindMinutes);
            } else {
                // Tạo lịch nhắc nhở mới
                Event event = eventService.getEventByEventId(eventId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện có ID: " + eventId + " để tạo nhắc nhở."));

                EmailReminder newReminder = new EmailReminder();
                newReminder.setEvent(event);
                newReminder.setUserId(userId);
                newReminder.setRemindMinutes(remindMinutes);
                newReminder.setCreatedAt(LocalDateTime.now());
                newReminder.setSent(false);

                emailReminderRepo.save(newReminder);
                log.info("✅ Đã tạo lịch nhắc nhở mới cho sự kiện ID: {} với {} phút trước", eventId, remindMinutes);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo/cập nhật lịch nhắc nhở cho sự kiện ID: {} - {}", eventId, e.getMessage(), e);
            // Không throw exception để không ảnh hưởng đến việc xác nhận đơn hàng
        }
    }

    // ✅ Lưu sự kiện từ Action (ADD_EVENT) - New version with userId
    public void saveEventFromAction(Action action, Long userId) {
        try {
            Map<String, Object> args = action.getArgs();

            EventItem event = new EventItem();
            event.setTitle((String) args.get("title"));
            event.setDescription((String) args.getOrDefault("description", ""));
            event.setStartsAt(tryParseDateTime((String) args.get("start_time")));
            event.setEndsAt(tryParseDateTime((String) args.get("end_time")));
            event.setCreatedAt(LocalDateTime.now());
            event.setEventStatus(EventStatus.DRAFT); // mặc định khi tạo
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

            eventService.saveEvent(existing);
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
    public Event createEventByCustomer(Long userId, EventItem draft, @Nullable Long organizationId) {
        // 1) Load or create customer
        Event event;
        log.info("Saving event {}", draft.getEventType());
        log.info("Saving event from DTO type: {}", draft.getClass().getName());
        EventType draftType = draft.getEventType();
        if (draftType == null) {
            log.warn("Unknown or null EventType received. Defaulting to generic Event.");
            event = new Event();
        } else {
            switch (draftType) {
                case WORKSHOP:
                    event = new WorkshopEvent();
                    break;
                case MUSIC:
                    event = new MusicEvent();
                    break;
                case FESTIVAL:
                    event = new FestivalEvent();
                    break;
                case COMPETITION:
                    event = new CompetitionEvent();
                    break;
                default:
                    log.warn("Unknown EventType received: {}. Defaulting to generic Event.", draftType);
                    event = new Event();
                    break;
            }
        }
        log.info("Saving event {}", event.getClass().getName());
        AIEventMapper.createEventFromRequest(draft, event);
        final Event finalEvent = event;
        if (event.getSubEvents() != null) {
            event.getSubEvents().forEach(sub -> sub.setParentEvent(finalEvent));
        }
        // 2) Find or create Host (idempotent)
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        Host h = user.getHost();
        if (h == null) {
            // Auto-create host for user if they don't have one
            log.info("User {} does not have a host, auto-creating host", userId);
            h = new Host();
            h.setUser(user);
            h = hostService.save(h);
            // Note: user will be saved by cascade or you can explicitly save if needed
            log.info("Auto-created host {} for user {}", h.getId(), userId);
        }
        
        // 3) Required host
        finalEvent.setHost(h);
        
        // 4) Optional organization
        if (organizationId != null) {
            Organization org = organizationService.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization không tồn tại"));
            finalEvent.setOrganization(org);
        } else {
            finalEvent.setOrganization(null);
        }
        
        // 5) Safe defaults
        if (finalEvent.getStatus() == null) finalEvent.setStatus(EventStatus.DRAFT);
        if (finalEvent.getEventType() == null) finalEvent.setEventType(EventType.OTHERS);
        if (finalEvent.getCreatedAt() == null) finalEvent.setCreatedAt(LocalDateTime.now());

        // 6) Lưu event và tạo nhắc nhở mặc định cho host
        Event savedEvent = eventRepo.save(event);
        
        // Tạo lịch nhắc nhở mặc định cho host khi tạo event
        try {
            createOrUpdateEmailReminder(savedEvent.getId(), 5, userId);
            log.info("✅ Đã tạo lịch nhắc nhở mặc định cho host khi tạo event ID: {}", savedEvent.getId());
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo lịch nhắc nhở cho event ID: {} - {}", savedEvent.getId(), e.getMessage(), e);
            // Không throw exception để không ảnh hưởng đến việc tạo event
        }

        return savedEvent;
    }




}
