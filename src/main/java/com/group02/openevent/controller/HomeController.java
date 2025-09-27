package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.repository.IAccountRepo;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {
	private final IAccountRepo accountRepo;

	public HomeController(IAccountRepo accountRepo) {
		this.accountRepo = accountRepo;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/index.html";
	}

	@GetMapping("/api/current-user")
	public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
		if (accountId == null) {
			return ResponseEntity.ok(Map.of("authenticated", false));
		}

		Account account = accountRepo.findById(accountId).orElse(null);
		if (account == null) {
			return ResponseEntity.ok(Map.of("authenticated", false));
		}

		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("authenticated", true);
		userInfo.put("accountId", account.getAccountId());
		userInfo.put("email", account.getEmail());
		userInfo.put("role", account.getRole().name());

		return ResponseEntity.ok(userInfo);
	}

	@PostMapping("/api/logout")
	public ResponseEntity<String> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.ok("Logged out successfully");
	}
}
