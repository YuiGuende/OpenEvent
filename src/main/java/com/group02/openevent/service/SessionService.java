package com.group02.openevent.service;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.session.Session;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface SessionService {
    
    /**
     * Create a new session for the given account
     */
    Session createSession(Account account, HttpServletRequest request);
    
    /**
     * Validate session token and return session if valid
     */
    Optional<Session> validateSession(String sessionToken);
    
    /**
     * Get active sessions for a user
     */
    List<Session> getActiveSessions(Long accountId);
    
    /**
     * Destroy a specific session
     */
    boolean destroySession(String sessionToken);
    
    /**
     * Destroy all sessions for a user
     */
    void destroyAllUserSessions(Long accountId);
    
    /**
     * Update session last accessed time
     */
    void updateSessionActivity(String sessionToken);
    
    /**
     * Extend session expiration time
     */
    boolean extendSession(String sessionToken, int hours);
    
    /**
     * Check if user has reached maximum concurrent sessions
     */
    boolean canCreateNewSession(Long accountId);
    
    /**
     * Clean up expired sessions
     */
    void cleanupExpiredSessions();
    
    /**
     * Get session count for a user
     */
    long getSessionCount(Long accountId);
    
    /**
     * Check if session is valid and active
     */
    boolean isSessionValid(String sessionToken);
}
