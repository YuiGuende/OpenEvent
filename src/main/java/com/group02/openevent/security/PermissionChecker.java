package com.group02.openevent.security;

import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.User;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class để kiểm tra permission và role của user
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionChecker {
    
    private final UserService userService;
    
    /**
     * Kiểm tra user có role cụ thể không
     * @param session HTTP session
     * @param role Role cần kiểm tra
     * @return true nếu user có role, false nếu không
     */
    public boolean hasRole(HttpSession session, Role role) {
        if (session == null) {
            log.debug("Session is null");
            return false;
        }
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            log.debug("USER_ID not found in session");
            return false;
        }
        
        try {
            User user = userService.getUserById(userId);
            return user.getRole() == role;
        } catch (Exception e) {
            log.error("Error checking role for user {}", userId, e);
            return false;
        }
    }
    
    /**
     * Kiểm tra user có một trong các roles không
     * @param session HTTP session
     * @param roles Mảng các roles
     * @return true nếu user có ít nhất 1 role trong danh sách
     */
    public boolean hasAnyRole(HttpSession session, Role... roles) {
        if (session == null) return false;
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) return false;
        
        try {
            User user = userService.getUserById(userId);
            Role userRole = user.getRole();
            
            for (Role role : roles) {
                if (userRole == role) return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking roles for user {}", userId, e);
            return false;
        }
    }
    
    /**
     * Kiểm tra user có tất cả roles không
     * @param session HTTP session
     * @param roles Mảng các roles
     * @return true nếu user có tất cả roles
     */
    public boolean hasAllRoles(HttpSession session, Role... roles) {
        if (session == null) return false;
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) return false;
        
        try {
            User user = userService.getUserById(userId);
            Role userRole = user.getRole();
            
            for (Role role : roles) {
                if (userRole != role) return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Error checking all roles for user {}", userId, e);
            return false;
        }
    }
    
    /**
     * Lấy current user từ session
     * @param session HTTP session
     * @return User object
     * @throws IllegalStateException nếu session null hoặc user chưa đăng nhập
     */
    public User getCurrentUser(HttpSession session) {
        if (session == null) {
            throw new IllegalStateException("Session is null");
        }
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userService.getUserById(userId);
    }
    
    /**
     * Kiểm tra user có phải là admin không
     */
    public boolean isAdmin(HttpSession session) {
        return hasRole(session, Role.ADMIN);
    }
    
    /**
     * Kiểm tra user có phải là host không
     */
    public boolean isHost(HttpSession session) {
        return hasRole(session, Role.HOST);
    }
    
    /**
     * Kiểm tra user có phải là department không
     */
    public boolean isDepartment(HttpSession session) {
        return hasRole(session, Role.DEPARTMENT);
    }
    
    /**
     * Kiểm tra user có phải là customer không
     */
    public boolean isCustomer(HttpSession session) {
        return hasRole(session, Role.CUSTOMER);
    }
}

