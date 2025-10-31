package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.service.ICompetitionService;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CompetitionController {

    private final ICompetitionService competitionService;
    private final EventService eventService;

    public CompetitionController(ICompetitionService competitionService, EventService eventService) {
        this.competitionService = competitionService;
        this.eventService = eventService;
    }

    @GetMapping("/competition/{id}")
    public String getCompetitionEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Kiểm tra event type trước
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
            
            // 2. Kiểm tra xem có phải Competition Event không
            if (event.getEventType() != EventType.COMPETITION) {
                // Redirect tới router chung, nó sẽ tự động forward tới đúng controller
                return "redirect:/events/" + id;
            }
            
            // 3. Lấy ra DTO duy nhất, đã chứa ĐẦY ĐỦ thông tin
            CompetitionEventDetailDTO eventDetail = competitionService.getCompetitionEventById(id);

            // 4. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);
            model.addAttribute("error", null);

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết cuộc thi ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("eventDetail", null);
            model.addAttribute("error", "Không thể tìm thấy cuộc thi bạn yêu cầu.");
        }

        // 5. Trả về đúng file view
        return "competition/competitionHome";
    }
}