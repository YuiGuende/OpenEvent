package com.group02.openevent.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CommonController {
	
	/**
	 * DEPRECATED: Role switching is no longer supported because roles are determined
	 * from User entities (customer/host/admin/department), not from Account.
	 * To switch roles, you need to create/remove the corresponding role entities.
	 */
	@PostMapping("/api/switch-role")
	public ResponseEntity<String> switchRole(HttpSession session) {
		return ResponseEntity.badRequest().body("Role switching is no longer supported. Roles are determined from User entities.");
	}

}
