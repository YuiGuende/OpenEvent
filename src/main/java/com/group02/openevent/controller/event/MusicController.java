package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.MusicEventDetailDTO;
import com.group02.openevent.service.IMusicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MusicController {

    private final IMusicService musicService;

    // Chỉ cần inject IMusicService là đủ
    public MusicController(IMusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/music/{id}")
    public String getMusicEventDetail(@PathVariable("id") Long id, Model model) {
        try {

            MusicEventDetailDTO eventDetail = musicService.getMusicEventById(id);
            // 2. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết sự kiện ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu.");
        }

        // 3. Trả về đúng file view
        return "music/musicHome";
    }
}
