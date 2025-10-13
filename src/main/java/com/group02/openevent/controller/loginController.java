package com.group02.openevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class loginController {

    @GetMapping("/login")
    public String showLoginPage() {
        return "security/login";
    }
/// demo
    @GetMapping("/update-event")
    public String showTicketDemo() {
        return "fragments/update-event";
    }
    @GetMapping("/host/ticket")
    public String showYourEvent(){
        return "fragments/update-ticket";
    }
    @GetMapping("/host/getting-stared")
    public String showEvent(){
        return "host/manager-event";
    }
    @GetMapping("/host/organization")
    public String showOrganization(){
        return "host/host";
    }
}
