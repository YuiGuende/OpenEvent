package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.service.IFestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FestivalController {

    private final IFestivalService festivalService;

    public FestivalController(IFestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping("/festival/{id}")
    public String getFestivalEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            FestivalEventDetailDTO eventDetail = festivalService.getFestivalEventById(id);
            model.addAttribute("eventDetail", eventDetail);
        } catch (Exception e) {
            System.err.println("Lỗi khi tải chi tiết lễ hội ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tìm thấy lễ hội bạn yêu cầu.");
        }

        return "festival/festivalHome";
    }
}