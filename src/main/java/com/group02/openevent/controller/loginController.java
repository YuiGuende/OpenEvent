package com.group02.openevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class loginController {
    @GetMapping("/login")
    public String showLoginPage() {
        return "security/login";

    }
}
