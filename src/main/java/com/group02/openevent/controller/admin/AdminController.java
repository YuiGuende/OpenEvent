package com.group02.openevent.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {
	@GetMapping("/admin")
	public String adminHome() {
		return "redirect:/admin/admin.html";
	}
} 