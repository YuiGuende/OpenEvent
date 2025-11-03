package com.group02.openevent.security;

import jakarta.servlet.http.HttpSession;

/**
 * Utility class for session management
 */
public class SessionUtils {
    
    /**
     * Get user session from HTTP session
     * @param httpSession HTTP session
     * @return UserSession object
     * @throws IllegalArgumentException if user is not logged in
     */
    public static UserSession requireUser(HttpSession httpSession) {
        if (httpSession == null) {
            throw new IllegalArgumentException("Session không tồn tại");
        }
        
        // Try to get userSession first
        UserSession userSession = (UserSession) httpSession.getAttribute("userSession");
        if (userSession != null) {
            return userSession;
        }
        
        // Fallback: check for ACCOUNT_ID and ACCOUNT_ROLE (from login controller)
        Long accountId = (Long) httpSession.getAttribute("ACCOUNT_ID");
        String role = (String) httpSession.getAttribute("ACCOUNT_ROLE");
        
        if (accountId == null || role == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập");
        }
        
        // Create UserSession from account info
        UserSession newUserSession = new UserSession();
        newUserSession.setUserId(accountId);
        newUserSession.setRole(role);
        return newUserSession;
    }
    
    /**
     * Get user session from HTTP session (optional)
     * @param httpSession HTTP session
     * @return UserSession object or null
     */
    public static UserSession getUser(HttpSession httpSession) {
        if (httpSession == null) {
            return null;
        }
        
        return (UserSession) httpSession.getAttribute("userSession");
    }
    
    /**
     * Set user session in HTTP session
     * @param httpSession HTTP session
     * @param userSession User session to set
     */
    public static void setUser(HttpSession httpSession, UserSession userSession) {
        if (httpSession != null) {
            httpSession.setAttribute("userSession", userSession);
        }
    }
    
    /**
     * Clear user session from HTTP session
     * @param httpSession HTTP session
     */
    public static void clearUser(HttpSession httpSession) {
        if (httpSession != null) {
            httpSession.removeAttribute("userSession");
        }
    }
}
