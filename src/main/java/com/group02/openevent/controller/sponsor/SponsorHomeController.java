package com.group02.openevent.controller.sponsor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SponsorHomeController {
	@GetMapping("/sponsor")
	public String sponsorHome() {
		return "redirect:/sponsor.html";
	}
} 