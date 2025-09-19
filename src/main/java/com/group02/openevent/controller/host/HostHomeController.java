package com.group02.openevent.controller.host;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HostHomeController {

	@GetMapping("/host")
	public String hostHome() {
		return "redirect:/host/host.html";
	}
}
