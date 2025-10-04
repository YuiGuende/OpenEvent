package com.group02.openevent.model.dto;

import com.group02.openevent.model.enums.Role;

public class AuthResponse {
	private Long accountId;
	private String email;
	private Role role;
	private String redirectPath;

	public AuthResponse() {}

	public AuthResponse(Long accountId, String email, Role role) {
		this.accountId = accountId;
		this.email = email;
		this.role = role;
	}

	public AuthResponse(Long accountId, String email, Role role, String redirectPath) {
		this.accountId = accountId;
		this.email = email;
		this.role = role;
		this.redirectPath = redirectPath;
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getRedirectPath() {
		return redirectPath;
	}

	public void setRedirectPath(String redirectPath) {
		this.redirectPath = redirectPath;
	}
}