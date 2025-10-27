package com.group02.openevent.security;

import java.time.LocalDateTime;

/**
 * User session information
 */
public class UserSession {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    
    public UserSession() {
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }
    
    public UserSession(Long userId, String username, String email, String role) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    /**
     * Update last activity time
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Check if session is expired
     * @param timeoutMinutes timeout in minutes
     * @return true if expired
     */
    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }
    
    @Override
    public String toString() {
        return "UserSession{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", loginTime=" + loginTime +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
