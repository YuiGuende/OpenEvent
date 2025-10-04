package com.group02.openevent.controller.event;

import com.group02.openevent.dto.request.EventCreationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;
//@RequestMapping("/manage/organizer")
@Controller
public class HostController {

    @GetMapping("/organizer")
    public String host(Model model) {
        model.addAttribute("content", "fragments/dashboard :: content");
        return "host/host";
    }
    // Khi người dùng gõ /dashboard, /events, /settings trực tiếp — ta vẫn trả về host layout
    @GetMapping({"/dashboard", "/events", "/settings"})
    public String directAccess(Model model) {
        // Mặc định load dashboard (JS sẽ tự fetch fragment đúng sau)
        model.addAttribute("content", "fragments/dashboard :: content");
        return "host/host";
    }
    @GetMapping("/fragment/dashboard")
    public String dashboard(Model model) {

        return "fragments/dashboard :: content";
    }

    @GetMapping("/fragment/events")
    public String events(Model model){
        EventCreationRequest request = new EventCreationRequest();
        model.addAttribute("eventForm", request);
        List<String> listTypeEvent = Arrays.asList("MUSIC", "FESTIVAL", "WORKSHOP","COMPETITION","OTHERS");
        model.addAttribute("listTypeEvent", listTypeEvent);

        return "fragments/events :: content";
    }
    @GetMapping("/fragment/settings")
    public String settings(Model model) {
        return "fragments/settings :: content";
    }
}
