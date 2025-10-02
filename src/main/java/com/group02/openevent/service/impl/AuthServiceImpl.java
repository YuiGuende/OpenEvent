package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.dto.AuthResponse;
import com.group02.openevent.model.dto.LoginRequest;
import com.group02.openevent.model.dto.RegisterRequest;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;


import jakarta.servlet.http.HttpSession;

@Service
public class AuthServiceImpl implements AuthService {
	private final IAccountRepo accountRepo;
	private final IUserRepo userRepo;
	private final PasswordEncoder passwordEncoder;
	private final HttpSession httpSession;

	public AuthServiceImpl(IAccountRepo accountRepo, IUserRepo userRepo,
			PasswordEncoder passwordEncoder, HttpSession httpSession) {
		this.accountRepo = accountRepo;
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
		this.httpSession = httpSession;
	}

	private String redirectFor(Role role) {
		// All users redirect to home page
		return "/";
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		// Validate input
		if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email không được để trống");
		}
		if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
			throw new IllegalArgumentException("Mật khẩu không được để trống");
		}
		if (request.getPassword().length() < 6) {
			throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
		}

		if (accountRepo.existsByEmail(request.getEmail().trim())) {
			throw new IllegalArgumentException("Email này đã được đăng ký");
		}

		// Luôn set role là USER khi đăng ký, bỏ qua lựa chọn từ frontend
		Role role = Role.USER;

		Account account = new Account();
		account.setEmail(request.getEmail().trim());
		account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		account.setRole(role);
		account = accountRepo.save(account);

		// Luôn tạo User record cho mọi account
		User user = new User();
		user.setAccount(account);
		user.setPhoneNumber(request.getPhoneNumber());
		user.setOrganization(request.getOrganization());
		user.setEmail(request.getEmail().trim());
		user.setPoints(0);
		userRepo.save(user);

		// Do NOT auto-login after registration; redirect to login page with success flag
		return new AuthResponse(account.getAccountId(), account.getEmail(), account.getRole(), "/login?registered=1");
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		// Validate input
		if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email không được để trống");
		}
		if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
			throw new IllegalArgumentException("Mật khẩu không được để trống");
		}

		Account account = accountRepo.findByEmail(request.getEmail().trim())
				.orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

		if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
			throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
		}

		httpSession.setAttribute("ACCOUNT_ID", account.getAccountId());
		httpSession.setAttribute("ACCOUNT_ROLE", account.getRole().name());

		return new AuthResponse(account.getAccountId(), account.getEmail(), account.getRole(), redirectFor(account.getRole()));
	}
} 