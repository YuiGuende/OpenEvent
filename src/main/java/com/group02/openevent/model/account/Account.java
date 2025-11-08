package com.group02.openevent.model.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = true, length = 255)
    private String passwordHash;

    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;  // "GOOGLE", "FACEBOOK", etc.

    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;  // Google user ID

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"account", "customer", "host", "admin", "department"})
    private User user;

    public Account() {
    }

    public Account(Long accountId, String email, String passwordHash) {
        this.accountId = accountId;
        this.email = email;
        this.passwordHash = passwordHash;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public String getOauthProviderId() {
        return oauthProviderId;
    }

    public void setOauthProviderId(String oauthProviderId) {
        this.oauthProviderId = oauthProviderId;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", email='" + email + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", oauthProvider='" + oauthProvider + '\'' +
                ", oauthProviderId='" + oauthProviderId + '\'' +
                '}';
    }
}