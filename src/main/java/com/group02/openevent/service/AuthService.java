package com.group02.openevent.service;

import com.group02.openevent.model.dto.response.AuthResponse;
import com.group02.openevent.model.dto.request.LoginRequest;
import com.group02.openevent.model.dto.request.RegisterRequest;

public interface AuthService {
	AuthResponse register(RegisterRequest request);
	AuthResponse login(LoginRequest request);
} 