package com.group02.openevent.controller.event;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HostController {
    @GetMapping("/dashboard")
    public String dashboard(){
        return "fragement/host :: content";
    }
    @GetMapping("/events")
    public String events(){
        return "fragement/events :: content";
    }
    @GetMapping("/settings")
    public String settings(){
        return "fragement/settings :: content";
    }
}
