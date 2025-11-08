package com.group02.openevent.config;


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

        // 1. Lấy thông tin người dùng từ Principal
        // Xử lý cả hai trường hợp: Form login (CustomUserDetails) và OAuth2 login (OAuth2User)
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            // Form login: sử dụng CustomUserDetails
           Optional<User> userOptional= userService.getUserByAccountId(userDetails.getAccountId());
            session.setAttribute("USER_ID", userOptional.get().getUserId());
            session.setAttribute("USER_ROLE", userDetails.getRole());
            log.info("Form login successful - Account ID: {}, Role: {}", userOptional.get().getUserId(), userDetails.getRole());
        } else if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
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
                session.setAttribute("USER_ID", userOptional.get().getUserId());
                session.setAttribute("USER_ROLE", role);
                log.info("OAuth2 login successful - Account ID: {}, Role: {}", accountId, role);
            } else {
                log.warn("OAuth2 user missing accountId or role in attributes. accountId: {}, role: {}", accountIdObj, role);
            }
        }

        // 2. Xử lý chuyển hướng sau khi đăng nhập thành công
        
        // PRIORITY 1: Kiểm tra custom redirectUrl từ form (dùng cho QR check-in)
        String customRedirectUrl = request.getParameter("redirectUrl");
        log.info("Login success - redirectUrl from form: {}", customRedirectUrl);
        
        if (customRedirectUrl != null && !customRedirectUrl.isEmpty()) {
            log.info("Redirecting to custom URL: {}", customRedirectUrl);
            response.sendRedirect(customRedirectUrl);
            return;
        }
        
        // PRIORITY 2: Kiểm tra SavedRequest (Spring Security tự động lưu URL trước đó)
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        
        // Nếu có URL được lưu trữ (người dùng cố truy cập 1 trang được bảo vệ)
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            log.info("Redirecting to saved request URL: {}", targetUrl);
            response.sendRedirect(targetUrl);
            return;
        }

        // PRIORITY 3: Nếu không có URL được lưu, chuyển hướng về trang chủ
        log.info("No redirect URL found, redirecting to home");
        response.sendRedirect("/");
    }
}
