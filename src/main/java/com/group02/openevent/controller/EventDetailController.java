package com.group02.openevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class EventDetailController {

	@GetMapping("/event/{eventId}")
	public String eventDetail(@PathVariable Integer eventId) {
		System.out.println("EventDetailController: eventDetail called with eventId: " + eventId);
		return "event/event-detail";
	}

	@GetMapping("/event/{eventId}/register")
	public String eventRegister(@PathVariable Integer eventId) {
		return "event/event-register";
	}

}
