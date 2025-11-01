package com.group02.openevent.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserHomeController {
	@GetMapping("/user")
	public String userHome() {
		return "redirect:/";
	}
} 