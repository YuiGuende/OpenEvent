package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.MusicEventDetailDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.service.IMusicService;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MusicController {

    private final IMusicService musicService;
    private final EventService eventService;

    public MusicController(IMusicService musicService, EventService eventService) {
        this.musicService = musicService;
        this.eventService = eventService;
    }

    @GetMapping("/music/{id}")
    public String getMusicEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Kiểm tra event type trước
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
            
            // 2. Kiểm tra xem có phải Music Event không
            if (event.getEventType() != EventType.MUSIC) {
                // Redirect tới router chung, nó sẽ tự động forward tới đúng controller
                return "redirect:/events/" + id;
            }
            
            // 3. Lấy ra DTO duy nhất, đã chứa ĐẦY ĐỦ thông tin
            MusicEventDetailDTO eventDetail = musicService.getMusicEventById(id);
            // 4. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);
            model.addAttribute("error", null); // Clear any previous error

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết sự kiện ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            // Set eventDetail to null to avoid template errors
            model.addAttribute("eventDetail", null);
            model.addAttribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu. Sự kiện có thể không phải là Music Event hoặc không tồn tại.");
        }

        // 5. Trả về đúng file view
        return "music/musicHome";
    }
}
