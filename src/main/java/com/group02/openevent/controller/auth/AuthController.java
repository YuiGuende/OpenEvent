package com.group02.openevent.controller.auth;

import com.group02.openevent.dto.response.AuthResponse;
import com.group02.openevent.dto.request.LoginRequest;
import com.group02.openevent.dto.request.RegisterRequest;
import com.group02.openevent.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
		try {
			AuthResponse response = authService.register(request);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.badRequest().body(error);
		} catch (Exception e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", "Đã xảy ra lỗi trong quá trình đăng ký. Vui lòng thử lại.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		try {
			AuthResponse response = authService.login(request);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
		} catch (Exception e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", "Đã xảy ra lỗi trong quá trình đăng nhập. Vui lòng thử lại.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}
} 