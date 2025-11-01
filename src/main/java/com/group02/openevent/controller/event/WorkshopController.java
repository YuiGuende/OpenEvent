package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.service.IWorkshopService;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WorkshopController {

    private final IWorkshopService workshopService;
    private final EventService eventService;

    public WorkshopController(IWorkshopService workshopService, EventService eventService) {
        this.workshopService = workshopService;
        this.eventService = eventService;
    }

    @GetMapping("/workshop/{id}")
    public String getWorkshopEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Kiểm tra event type trước
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
            
            // 2. Kiểm tra xem có phải Workshop Event không
            if (event.getEventType() != EventType.WORKSHOP) {
                // Redirect tới router chung, nó sẽ tự động forward tới đúng controller
                return "redirect:/events/" + id;
            }
            
            // 3. Lấy ra DTO duy nhất, đã chứa ĐẦY ĐỦ thông tin
            WorkshopEventDetailDTO eventDetail = workshopService.getWorkshopEventById(id);

            // 4. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);
            model.addAttribute("error", null);

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết workshop ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("eventDetail", null);
            model.addAttribute("error", "Không thể tìm thấy workshop bạn yêu cầu.");
        }

        // 5. Trả về đúng file view
        return "workshop/workshopHome";
    }
}