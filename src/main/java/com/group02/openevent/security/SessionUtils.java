package com.group02.openevent.security;

import com.group02.openevent.model.user.User;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility class for session management
 */
@Component
@RequiredArgsConstructor
public class SessionUtils {
    
    private final UserService userService;
    
    // Static instance for backward compatibility
    private static SessionUtils instance;
    
    // Post-construct để set static instance
    @jakarta.annotation.PostConstruct
    public void init() {
        instance = this;
    }
    
    /**
     * Get user session from HTTP session
     * @param httpSession HTTP session
     * @return UserSession object
     * @throws IllegalArgumentException if user is not logged in
     */
    public UserSession requireUser(HttpSession httpSession) {
        if (httpSession == null) {
            throw new IllegalArgumentException("Session không tồn tại");
        }
        
        // Try to get userSession first
        UserSession userSession = (UserSession) httpSession.getAttribute("userSession");
        if (userSession != null) {
            return userSession;
        }
        
        // Check for USER_ID first (preferred)
        Long userId = (Long) httpSession.getAttribute("USER_ID");
        if (userId != null) {
            UserSession newUserSession = new UserSession();
            newUserSession.setUserId(userId);
            String role = (String) httpSession.getAttribute("ACCOUNT_ROLE");
            if (role != null) {
                newUserSession.setRole(role);
            }
            return newUserSession;
        }
        
        // Fallback: check for ACCOUNT_ID and ACCOUNT_ROLE (from login controller)
        Long accountId = (Long) httpSession.getAttribute("USER_ID");
        String role = (String) httpSession.getAttribute("ACCOUNT_ROLE");
        
        if (accountId == null || role == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập");
        }
        
        // PATCH: Lấy userId thực sự từ accountId (không dùng accountId làm userId)
        Optional<User> userOpt = userService.getUserByAccountId(accountId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập");
        }
        
        User user = userOpt.get();
        Long actualUserId = user.getUserId();
        
        // Create UserSession from account info với userId đúng
        UserSession newUserSession = new UserSession();
        newUserSession.setUserId(actualUserId); // Sử dụng userId thực sự, không phải accountId
        newUserSession.setRole(role);
        
        // Cache vào session để lần sau không cần query lại
        httpSession.setAttribute("USER_ID", actualUserId);
        httpSession.setAttribute("userSession", newUserSession);
        
        return newUserSession;
    }
    
    /**
     * Static method for backward compatibility
     */
    public static UserSession requireUserStatic(HttpSession httpSession) {
        if (instance == null) {
            throw new IllegalStateException("SessionUtils instance not initialized. Make sure it's a Spring bean.");
        }
        return instance.requireUser(httpSession);
    }
    
    /**
     * Get user session from HTTP session (optional)
     * @param httpSession HTTP session
     * @return UserSession object or null
     */
    public UserSession getUser(HttpSession httpSession) {
        if (httpSession == null) {
            return null;
        }
        
        return (UserSession) httpSession.getAttribute("userSession");
    }
    
    /**
     * Static method for backward compatibility
     */
    public static UserSession getUserStatic(HttpSession httpSession) {
        if (instance == null) {
            return null;
        }
        return instance.getUser(httpSession);
    }
    
    /**
     * Set user session in HTTP session
     * @param httpSession HTTP session
     * @param userSession User session to set
     */
    public void setUser(HttpSession httpSession, UserSession userSession) {
        if (httpSession != null) {
            httpSession.setAttribute("userSession", userSession);
        }
    }
    
    /**
     * Static method for backward compatibility
     */
    public static void setUserStatic(HttpSession httpSession, UserSession userSession) {
        if (instance != null) {
            instance.setUser(httpSession, userSession);
        }
    }
    
    /**
     * Clear user session from HTTP session
     * @param httpSession HTTP session
     */
    public void clearUser(HttpSession httpSession) {
        if (httpSession != null) {
            httpSession.removeAttribute("userSession");
        }
    }
    
    /**
     * Static method for backward compatibility
     */
    public static void clearUserStatic(HttpSession httpSession) {
        if (instance != null) {
            instance.clearUser(httpSession);
        }
    }
}
