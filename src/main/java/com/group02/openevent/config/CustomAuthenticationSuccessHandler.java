package com.group02.openevent.config;


import com.group02.openevent.model.user.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // Component này dùng để lưu trữ các request bị chặn trước đó (ví dụ: người dùng cố truy cập /event/10)
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Authentication authentication) throws IOException, ServletException {
        
        HttpSession session = request.getSession();

        // 1. Lấy thông tin người dùng từ Principal (Phải đảm bảo UserDetailsService trả về CustomUserDetails)
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            // LƯU Ý: CustomUserDetails phải có phương thức getAccountId() và getRole()
            session.setAttribute("ACCOUNT_ID", userDetails.getAccountId()); 
            session.setAttribute("ACCOUNT_ROLE", userDetails.getRole());
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
