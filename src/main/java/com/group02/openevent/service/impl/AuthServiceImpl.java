package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.dto.response.AuthResponse;
import com.group02.openevent.dto.request.LoginRequest;
import com.group02.openevent.dto.request.RegisterRequest;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.session.Session;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.event.UserCreatedEvent;
import com.group02.openevent.service.AuthService;
import com.group02.openevent.service.SessionService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;



@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
	private final IAccountRepo accountRepo;
	private final ICustomerRepo customerRepo;
	private final IUserRepo userRepo;
	private final PasswordEncoder passwordEncoder;
	private final HttpSession httpSession;
	private final SessionService sessionService;
	private final UserService userService;
	private final ApplicationEventPublisher eventPublisher;

	public AuthServiceImpl(IAccountRepo accountRepo, ICustomerRepo customerRepo, IUserRepo userRepo,
			PasswordEncoder passwordEncoder, HttpSession httpSession, SessionService sessionService, UserService userService, ApplicationEventPublisher eventPublisher) {
		this.accountRepo = accountRepo;
		this.customerRepo = customerRepo;
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
		this.httpSession = httpSession;
		this.sessionService = sessionService;
		this.userService = userService;
		this.eventPublisher = eventPublisher;
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

		Account account = new Account();
		account.setEmail(request.getEmail().trim());
		account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		account = accountRepo.save(account);

		// Luôn tạo User record cho mọi account
		User user = userService.getOrCreateUser(account);
		if (user.getName() == null && request.getPhoneNumber() != null) {
			user.setName(account.getEmail()); // Default name
			user.setPhoneNumber(request.getPhoneNumber());
		}
		
		Customer customer = new Customer();
		customer.setUser(user);
		customer.setPoints(0);
		customerRepo.save(customer);

		// Publish UserCreatedEvent for audit log (self-registration, actorId = null)
		try {
			eventPublisher.publishEvent(new UserCreatedEvent(this, user, null, "CUSTOMER"));
		} catch (Exception e) {
			log.error("Error publishing UserCreatedEvent: {}", e.getMessage(), e);
		}

		// Lấy role từ User (sau khi đã tạo Customer)
		Role role = user.getRole();

		// Do NOT auto-login after registration; redirect to login page with success flag
		return new AuthResponse(account.getAccountId(), account.getEmail(), role, "/login?registered=1");
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

		// Lấy User để xác định Role
		User user = userRepo.findByAccount_AccountId(account.getAccountId())
				.orElseThrow(() -> new IllegalArgumentException("User not found for account"));
		Role role = user.getRole();

		// Set HTTP session attributes for backward compatibility
        log.info("set user id:{}", user.getUserId());
		httpSession.setAttribute("USER_ID", user.getUserId());
		httpSession.setAttribute("USER_ROLE", role.name());
		// Set CURRENT_USER_ID as String for WebSocket Principal mapping
		httpSession.setAttribute("CURRENT_USER_ID", String.valueOf(user.getUserId()));

		return new AuthResponse(account.getAccountId(), account.getEmail(), role, redirectFor(role));
	}

	/**
	 * Enhanced login method that creates a custom session
	 */
	public AuthResponse loginWithSession(LoginRequest request, HttpServletRequest httpRequest) {
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

		// Lấy User để xác định Role
		User user = userRepo.findByAccount_AccountId(account.getAccountId())
				.orElseThrow(() -> new IllegalArgumentException("User not found for account"));
		Role role = user.getRole();

		// Create custom session
		Session session = sessionService.createSession(account, httpRequest);

		// Set session token in HTTP session for backward compatibility
		httpSession.setAttribute("SESSION_TOKEN", session.getSessionToken());
		httpSession.setAttribute("USER_ID", account.getAccountId());
		httpSession.setAttribute("ACCOUNT_ROLE", role.name());
		// Set CURRENT_USER_ID as String for WebSocket Principal mapping
		httpSession.setAttribute("CURRENT_USER_ID", String.valueOf(user.getUserId()));

		return new AuthResponse(account.getAccountId(), account.getEmail(), role, redirectFor(role));
	}
} 