package com.group02.openevent.service;

import com.group02.openevent.model.dto.AuthResponse;
import com.group02.openevent.model.dto.LoginRequest;
import com.group02.openevent.model.dto.RegisterRequest;

public interface AuthService {
	AuthResponse register(RegisterRequest request);
	AuthResponse login(LoginRequest request);
} 