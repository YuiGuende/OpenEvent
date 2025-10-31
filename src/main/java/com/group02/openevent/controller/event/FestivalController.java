package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.service.IFestivalService;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FestivalController {

    private final IFestivalService festivalService;
    private final EventService eventService;

    public FestivalController(IFestivalService festivalService, EventService eventService) {
        this.festivalService = festivalService;
        this.eventService = eventService;
    }

    @GetMapping("/festival/{id}")
    public String getFestivalEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Kiểm tra event type trước
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
            
            // 2. Kiểm tra xem có phải Festival Event không
            if (event.getEventType() != EventType.FESTIVAL) {
                // Redirect tới router chung, nó sẽ tự động forward tới đúng controller
                return "redirect:/events/" + id;
            }
            
            // 3. Lấy ra DTO
            FestivalEventDetailDTO eventDetail = festivalService.getFestivalEventById(id);
            model.addAttribute("eventDetail", eventDetail);
            model.addAttribute("error", null);
        } catch (Exception e) {
            System.err.println("Lỗi khi tải chi tiết lễ hội ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("eventDetail", null);
            model.addAttribute("error", "Không thể tìm thấy lễ hội bạn yêu cầu.");
        }

        return "festival/festivalHome";
    }
}