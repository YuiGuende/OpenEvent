package com.group02.openevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class loginController {
    @GetMapping("getting-started")
    public String showLoginPage() {
        return "host/getting-started";

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
    @GetMapping("view-ticket")
    public String showViewTicketPage() {
        return "host/view-ticket";
    }
}
