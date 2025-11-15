package com.group02.openevent.aspect;

import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.User;
import com.group02.openevent.security.annotation.RequireRole;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP Aspect để kiểm tra role của user trước khi cho phép truy cập method
 * Sử dụng annotation @RequireRole
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthorizationAspect {
    
    private final UserService userService;
    
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        log.info("Role authorization check for method: {}", joinPoint.getSignature().getName());
        
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.warn("Cannot access HTTP request for authorization check");
            throw new IllegalStateException("Cannot access HTTP request");
        }
        
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("No session found for authorization check");
            throw new AccessDeniedException("User not authenticated");
        }
        
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            log.warn("USER_ID not found in session");
            throw new AccessDeniedException("User not authenticated");
        }
        
        User user;
        try {
            user = userService.getUserById(userId);
        } catch (Exception e) {
            log.error("Error getting user by id: {}", userId, e);
            throw new AccessDeniedException("User not found");
        }
        
        Role userRole = user.getRole();
        Role[] requiredRoles = requireRole.value();
        boolean requireAll = requireRole.requireAll();
        
        log.info("User {} has role {}, required roles: {}", userId, userRole, java.util.Arrays.toString(requiredRoles));
        
        if (requireAll) {
            // Cần tất cả roles
            for (Role role : requiredRoles) {
                if (userRole != role) {
                    log.warn("User {} with role {} does not have required role {}", 
                            userId, userRole, role);
                    throw new AccessDeniedException("Access denied: Required role " + role);
                }
            }
        } else {
            // Chỉ cần 1 trong số roles
            boolean hasRole = false;
            for (Role role : requiredRoles) {
                if (userRole == role) {
                    hasRole = true;
                    break;
                }
            }
            if (!hasRole) {
                log.warn("User {} with role {} does not have any of required roles {}", 
                        userId, userRole, java.util.Arrays.toString(requiredRoles));
                throw new AccessDeniedException("Access denied: Required one of roles " + 
                        java.util.Arrays.toString(requiredRoles));
            }
        }
        
        log.info("Role authorization passed for user {} with role {}", userId, userRole);
    }
    
    /**
     * Lấy HttpServletRequest từ RequestContextHolder
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.error("Error getting HttpServletRequest", e);
            return null;
        }
    }
}

