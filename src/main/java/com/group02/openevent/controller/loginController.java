package com.group02.openevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class loginController {
    @GetMapping("login")
    public String showLoginPage() {
        return "security/login";

    }
    @GetMapping("host")
    public String showHostPage() {
        return "host/host";
    }

    @GetMapping("events")
    public String showEventsPage() {
        return "host/events";
    }
    @GetMapping("create")
    public String showCreatePage() {
        return "host/create-event";
    }
}
