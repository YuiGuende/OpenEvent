package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.service.IWorkshopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WorkshopController {

    private final IWorkshopService workshopService;

    public WorkshopController(IWorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @GetMapping("/workshop/{id}")
    public String getWorkshopEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Lấy ra DTO duy nhất, đã chứa ĐẦY ĐỦ thông tin
            WorkshopEventDetailDTO eventDetail = workshopService.getWorkshopEventById(id);

            // 2. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết workshop ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tìm thấy workshop bạn yêu cầu.");
        }

        // 3. Trả về đúng file view
        return "workshop/workshopHome";
    }
}