package com.group02.openevent.controller.event;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("manage/event")
public class EventManageController {

    @GetMapping("/{eventId}/getting-stared")
    public String GettingStared(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("content", "fragments/getting-stared");
        return  "host/your-events";

    }

}
