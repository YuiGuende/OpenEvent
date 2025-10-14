package com.group02.openevent.ai.controller;

import com.group02.openevent.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/send-email")
    public String testEmail() {
        emailService.sendSimpleEmail(
                "ntlequyen2911@gmail.com",
                "Test Email",
                "This is a test email from OpenEvent"
        );
        return "Email sent!";
    }
}
