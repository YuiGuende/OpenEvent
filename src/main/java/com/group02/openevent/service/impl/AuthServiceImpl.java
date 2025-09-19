package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.admin.Admin;
import com.group02.openevent.model.dto.AuthResponse;
import com.group02.openevent.model.dto.LoginRequest;
import com.group02.openevent.model.dto.RegisterRequest;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IAdminRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

import jakarta.servlet.http.HttpSession;

@Service
public class AuthServiceImpl implements AuthService {
	private final IAccountRepo accountRepo;
	private final IAdminRepo adminRepo;
	private final IUserRepo userRepo;
	private final PasswordEncoder passwordEncoder;
	private final HttpSession httpSession;

	public AuthServiceImpl(IAccountRepo accountRepo, IAdminRepo adminRepo, IUserRepo userRepo,
			PasswordEncoder passwordEncoder, HttpSession httpSession) {
		this.accountRepo = accountRepo;
		this.adminRepo = adminRepo;
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
		this.httpSession = httpSession;
	}

	private String redirectFor(Role role) {
		switch (role) {
			case ADMIN: return "/admin";
			case SPONSOR: return "/sponsor";
			case TEACHER: return "/teacher";
			default: return "/user";
		}
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (accountRepo.existsByEmail(request.getEmail())) {
			throw new IllegalArgumentException("Email already registered");
		}

		Role incoming = Objects.requireNonNullElse(request.getRole(), Role.USER);
		if (incoming == Role.ADMIN) {
			throw new IllegalArgumentException("ADMIN role cannot be registered");
		}
		Role role = incoming;

		Account account = new Account();
		account.setEmail(request.getEmail());
		account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		account.setRole(role);
		account = accountRepo.save(account);

		User user = new User();
		user.setAccount(account);
		user.setPhoneNumber(request.getPhoneNumber());
		user.setOrganization(request.getOrganization());
		user.setEmail(request.getEmail());
		user.setPoints(0);
		userRepo.save(user);

		// Do NOT auto-login after registration; redirect to login page with success flag
		return new AuthResponse(account.getAccountId(), account.getEmail(), account.getRole(), "/security/login.html?registered=1");
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		Account account = accountRepo.findByEmail(request.getEmail())
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		httpSession.setAttribute("ACCOUNT_ID", account.getAccountId());
		httpSession.setAttribute("ACCOUNT_ROLE", account.getRole().name());

		return new AuthResponse(account.getAccountId(), account.getEmail(), account.getRole(), redirectFor(account.getRole()));
	}
} 