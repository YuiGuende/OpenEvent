package com.group02.openevent.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.account.Account;
import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(optional = false)
	@JoinColumn(name = "account_id", nullable = false, unique = true,
		foreignKey = @ForeignKey(name = "fk_user_account"))
	@JsonIgnoreProperties({"passwordHash"})
	private Account account;

	@Column(name = "phone_number", length = 20)
	private String phoneNumber;

	@Column(name = "organization", length = 150)
	private String organization;

	@Column(name = "email", length = 100)
	private String email;

	@Column(name = "points", nullable = false)
	private Integer points = 0;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}
} 