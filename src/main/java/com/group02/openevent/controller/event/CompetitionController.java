package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CompetitionController {

    private final ICompetitionService competitionService;

    public CompetitionController(ICompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping("/competition/{id}")
    public String getCompetitionEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            // 1. Lấy ra DTO duy nhất, đã chứa ĐẦY ĐỦ thông tin
            CompetitionEventDetailDTO eventDetail = competitionService.getCompetitionEventById(id);

            // 2. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết cuộc thi ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tìm thấy cuộc thi bạn yêu cầu.");
        }

        // 3. Trả về đúng file view
        return "competition/competitionHome";
    }
}