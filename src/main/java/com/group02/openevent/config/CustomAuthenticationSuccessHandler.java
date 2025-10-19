package com.group02.openevent.config;


import com.group02.openevent.model.user.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
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
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        
        // Nếu có URL được lưu trữ (người dùng cố truy cập 1 trang được bảo vệ)
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            response.sendRedirect(targetUrl);
            return;
        }

        // Nếu không có URL được lưu, chuyển hướng về trang chủ
        response.sendRedirect("/");
    }
}
