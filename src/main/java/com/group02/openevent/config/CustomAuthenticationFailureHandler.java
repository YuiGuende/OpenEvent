package com.group02.openevent.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        
        // Lấy thông báo lỗi từ exception
        String errorMessage = getErrorMessage(exception);
        
        // Lưu error message vào session để hiển thị trên trang login
        session.setAttribute("LOGIN_ERROR", errorMessage);
        
        log.warn("Login failed: {} - Session ID: {}", errorMessage, session.getId());
        log.info("Error message saved to session: {}", session.getAttribute("LOGIN_ERROR"));
        
        // Redirect về trang login với parameter error
        String redirectUrl = "/login?error";
        response.sendRedirect(redirectUrl);
    }
    
    private String getErrorMessage(AuthenticationException exception) {
        String exceptionMessage = exception.getMessage();
        
        // Xử lý các loại exception khác nhau
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("Bad credentials")) {
                return "Email hoặc mật khẩu không đúng";
            } else if (exceptionMessage.contains("User is disabled")) {
                return "Tài khoản đã bị vô hiệu hóa";
            } else if (exceptionMessage.contains("User account is locked")) {
                return "Tài khoản đã bị khóa";
            } else if (exceptionMessage.contains("User account has expired")) {
                return "Tài khoản đã hết hạn";
            } else if (exceptionMessage.contains("credentials")) {
                return "Email hoặc mật khẩu không đúng";
            }
        }
        
        // Default error message
        return "Email hoặc mật khẩu không đúng";
    }
}

