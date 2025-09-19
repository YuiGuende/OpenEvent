package com.group02.openevent.controller.teacher;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeacherHomeController {
	@GetMapping("/teacher")
	public String teacherHome() {
		return "redirect:/teacher.html";
	}
} 