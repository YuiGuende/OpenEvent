package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.repository.IAccountRepo;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CommonController {
	private final IAccountRepo accountRepo;

	public CommonController(IAccountRepo accountRepo) {
		this.accountRepo = accountRepo;
	}

	@PostMapping("/api/switch-role")
	public ResponseEntity<String> switchRole(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
		if (accountId == null) {
			return ResponseEntity.badRequest().body("Not logged in");
		}

		Account account = accountRepo.findById(accountId).orElse(null);
		if (account == null) {
			return ResponseEntity.badRequest().body("Account not found");
		}

		// Chỉ cho phép chuyển đổi giữa CUSTOMER và HOST
		if (account.getRole() == Role.CUSTOMER) {
			account.setRole(Role.HOST);
		} else if (account.getRole() == Role.HOST) {
			account.setRole(Role.CUSTOMER);
		} else {
			return ResponseEntity.badRequest().body("Cannot switch role for this account type");
		}
		accountRepo.save(account);
		session.setAttribute("ACCOUNT_ROLE", account.getRole().name());
		return ResponseEntity.ok("Role switched successfully");
	}

}
