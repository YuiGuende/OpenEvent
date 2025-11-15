package com.group02.openevent.config;


import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.CustomUserDetails;
import com.group02.openevent.model.user.User;
import com.group02.openevent.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // Component này dùng để lưu trữ các request bị chặn trước đó (ví dụ: người dùng cố truy cập /event/10)
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final UserService userService;

    public CustomAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Authentication authentication) throws IOException, ServletException {
        
        HttpSession session = request.getSession();
        String userRole = null; // Lưu role để dùng cho redirect
        
        log.info("=== onAuthenticationSuccess called ===");
        log.info("Authentication principal type: {}", authentication.getPrincipal().getClass().getName());

        // 1. Lấy thông tin người dùng từ Principal
        // Xử lý cả hai trường hợp: Form login (CustomUserDetails) và OAuth2 login (OAuth2User)
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            // Form login: sử dụng CustomUserDetails
            log.info("Processing CustomUserDetails (Form login)");
           Optional<User> userOptional= userService.getUserByAccountId(userDetails.getAccountId());
            if (userOptional.isPresent()) {
                session.setAttribute("USER_ID", userOptional.get().getUserId());
                userRole = userDetails.getRole();
                session.setAttribute("USER_ROLE", userRole);
                log.info("Form login successful - Account ID: {}, User ID: {}, Role: {}", 
                        userDetails.getAccountId(), userOptional.get().getUserId(), userRole);
                log.info("Granted Authorities: {}", authentication.getAuthorities());
            } else {
                log.error("User not found for accountId: {}", userDetails.getAccountId());
            }
        } else if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            log.info("Processing OAuth2User (OAuth2 login)");
            // OAuth2 login: lấy thông tin từ OAuth2User attributes
            Object accountIdObj = oauth2User.getAttributes().get("accountId");
            String role = (String) oauth2User.getAttributes().get("role");
            
            // Xử lý accountId có thể là Long hoặc Integer
            Long accountId = null;
            if (accountIdObj instanceof Long) {
                accountId = (Long) accountIdObj;
            } else if (accountIdObj instanceof Integer) {
                accountId = ((Integer) accountIdObj).longValue();
            } else if (accountIdObj instanceof Number) {
                accountId = ((Number) accountIdObj).longValue();
            }
            
            if (accountId != null && role != null) {
                Optional<User> userOptional= userService.getUserByAccountId(accountId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    session.setAttribute("USER_ID", user.getUserId());
                    userRole = role;
                    session.setAttribute("USER_ROLE", userRole);
                    log.info("OAuth2 login successful - Account ID: {}, User ID: {}, Role: {}", accountId, user.getUserId(), userRole);
                } else {
                    log.warn("OAuth2 user not found for accountId: {}", accountId);
                }
            } else {
                log.warn("OAuth2 user missing accountId or role in attributes. accountId: {}, role: {}", accountIdObj, role);
            }
        } else {
            log.warn("Unknown authentication principal type: {}", authentication.getPrincipal().getClass().getName());
        }
        
        log.info("Final userRole after authentication processing: {}", userRole);

        // 2. Xử lý chuyển hướng sau khi đăng nhập thành công
        
        // PRIORITY 1: Kiểm tra redirect parameter từ query string (từ URL như /login?redirect=/forms/feedback/1)
        String redirectParam = request.getParameter("redirect");
        String redirectUrlParam = request.getParameter("redirectUrl");
        String customRedirectUrl = redirectUrlParam != null ? redirectUrlParam : redirectParam;
        log.info("Login success - redirect from query: {}, redirectUrl from query: {}, final: {}", 
                 redirectParam, redirectUrlParam, customRedirectUrl);
        
        if (customRedirectUrl != null && !customRedirectUrl.isEmpty()) {
            log.info("Redirecting to custom URL from query parameter: {}", customRedirectUrl);
            response.sendRedirect(customRedirectUrl);
            return;
        }
        
        // PRIORITY 2: Kiểm tra SavedRequest (Spring Security tự động lưu URL trước đó)
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        log.info("SavedRequest check - savedRequest is null: {}", savedRequest == null);
        
        // Nếu có URL được lưu trữ (người dùng cố truy cập 1 trang được bảo vệ)
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            log.info("SavedRequest URL: {}", targetUrl);
            
            // Nếu SavedRequest là "/" (trang chủ), bỏ qua và chuyển sang role-based redirect
            if (targetUrl != null && (targetUrl.equals("/") || targetUrl.equals(""))) {
                log.info("SavedRequest is home page (/), ignoring and using role-based redirect instead");
            } else {
                log.info("Redirecting to saved request URL: {}", targetUrl);
                response.sendRedirect(targetUrl);
                return;
            }
        }
        
        log.info("No valid SavedRequest found, proceeding to role-based redirect");

        // PRIORITY 3: Chuyển hướng dựa trên role của user
        log.info("=== PRIORITY 3: Role-based redirect ===");
        log.info("User role from session: {}", userRole);
        log.info("Authentication principal type: {}", authentication.getPrincipal().getClass().getName());
        
        String defaultRedirectUrl = getDefaultRedirectUrlByRole(userRole);
        log.info("No redirect URL found, redirecting to default URL for role {}: {}", userRole, defaultRedirectUrl);
        response.sendRedirect(defaultRedirectUrl);
    }
    
    /**
     * Lấy URL mặc định để redirect dựa trên role của user
     * @param roleString Role của user (String)
     * @return URL để redirect
     */
    private String getDefaultRedirectUrlByRole(String roleString) {
        log.info("=== getDefaultRedirectUrlByRole called ===");
        log.info("Input roleString: {}", roleString);
        
        if (roleString == null || roleString.isEmpty()) {
            log.warn("Role is null or empty, redirecting to home");
            return "/";
        }
        
        log.info("Role string before conversion: {}", roleString);
        try {
            Role role = Role.valueOf(roleString.toUpperCase());
            log.info("Role enum value: {}", role);
            
            String redirectUrl;
            switch (role) {
                case DEPARTMENT:
                    redirectUrl = "/department/dashboard";
                    break;
                case ADMIN:
                    redirectUrl = "/admin/dashboard";
                    break;
                case HOST:
                    redirectUrl = "/events";
                    break;
                case CUSTOMER:
                case VOLUNTEER:
                default:
                    redirectUrl = "/";
                    break;
            }
            log.info("Selected redirect URL: {}", redirectUrl);
            return redirectUrl;
        } catch (IllegalArgumentException e) {
            log.error("Error converting role string '{}' to Role enum: {}", roleString, e.getMessage(), e);
            log.warn("Unknown role: {}, redirecting to home", roleString);
            return "/";
        }
    }
}
