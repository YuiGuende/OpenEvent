package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
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
@Transactional
public class AgentEventService {
    
    @Autowired
    private EventService eventService;

    // ✅ Lưu sự kiện từ Action (ADD_EVENT)
    public void saveEventFromAction(Action action) {
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

            eventService.saveEvent(event);
            System.out.println("✅ Đã lưu sự kiện: " + event.getTitle());

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
}
