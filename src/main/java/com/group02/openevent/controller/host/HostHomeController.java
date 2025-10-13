package com.group02.openevent.controller.host;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HostHomeController {

	@GetMapping("/host")
	public String hostHome() {
		return "redirect:/";
	}

    @GetMapping("/{eventId}/send-notification")
    public String showNotificationForm(@PathVariable Long eventId, Model model) {
        // Truyền eventId vào Model để Thymeleaf có thể lấy và đặt vào Form
        model.addAttribute("eventId", eventId);

        // Trả về tên file HTML template (ví dụ: send-notification-form.html)
        return "host/send-notification-form";
    }
}
