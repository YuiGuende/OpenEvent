package com.group02.openevent.controller.auth;

import com.group02.openevent.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class LoginController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String redirect,
            @RequestParam(required = false) String redirectUrl,
            @RequestParam(required = false) String currentUri,
            @RequestParam(required = false) String error,
            jakarta.servlet.http.HttpServletRequest request,
            HttpSession session,
            Model model) {
        
        // Log referer to see where the request is coming from
        String referer = request.getHeader("Referer");
        log.info("Login page request - Referer: {}, Request URI: {}", referer, request.getRequestURI());
        // Support both 'redirect' and 'redirectUrl' parameter names, and 'currentUri'
        String targetUrl = redirectUrl != null ? redirectUrl : (redirect != null ? redirect : currentUri);
        log.info("Login page - redirect param: {}, redirectUrl param: {}, currentUri param: {}, final targetUrl: {}", 
                 redirect, redirectUrl, currentUri, targetUrl);
        if (targetUrl != null && !targetUrl.isEmpty()) {
            model.addAttribute("redirectUrl", targetUrl);
        }
        
        // Lấy error message từ session (được set bởi CustomAuthenticationFailureHandler)
        // Kiểm tra cả parameter error và session để đảm bảo không bỏ sót
        log.info("Login page loaded - error parameter: {}, session ID: {}", error, session != null ? session.getId() : "null");
        
        // Debug: Check if user is already logged in
        if (session != null) {
            Long userId = (Long) session.getAttribute("USER_ID");
            log.info("Login page - USER_ID in session: {}", userId);
            if (userId != null) {
                // User is already logged in, redirect to target URL or home
                // Avoid redirect loop: if targetUrl is /login, redirect to home instead
                if (targetUrl != null && !targetUrl.isEmpty() && !targetUrl.equals("/login") && !targetUrl.startsWith("/login?")) {
                    log.info("User already logged in (USER_ID: {}), redirecting to target URL: {}", userId, targetUrl);
                    return "redirect:" + targetUrl;
                }
                log.info("User already logged in (USER_ID: {}), redirecting to home (targetUrl was: {})", userId, targetUrl);
                return "redirect:/";
            }
        }
        
        String errorMessage = null;
        if (session != null) {
            errorMessage = (String) session.getAttribute("LOGIN_ERROR");
            log.info("Error message from session: {}", errorMessage);
        }
        
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            // Xóa error message khỏi session sau khi đã lấy
            session.removeAttribute("LOGIN_ERROR");
            log.info("Login error message added to model: {}", errorMessage);
        } else if (error != null) {
            // Fallback nếu có parameter error nhưng không có message trong session
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không đúng");
            log.info("Login error parameter found but no message in session - using fallback");
        }
        
        return "security/login";
    }

//    @PostMapping("/login")
//    public String handleLogin(@RequestParam String email,
//                            @RequestParam String password,
//                            @RequestParam(required = false) String redirectUrl,
//                            HttpSession session,
//                            Model model) {
//        try {
//            LoginRequest loginRequest = new LoginRequest();
//            loginRequest.setEmail(email);
//            loginRequest.setPassword(password);
//
//            var response = authService.login(loginRequest);
//
//            // Store in session
//            session.setAttribute("ACCOUNT_ID", response.getAccountId());
//            session.setAttribute("ACCOUNT_ROLE", response.getRole());
//
//            // Redirect to original URL if provided, otherwise go to home
//            if (redirectUrl != null && !redirectUrl.isEmpty()) {
//                return "redirect:" + redirectUrl;
//            }
//            return "redirect:/";
//        } catch (Exception e) {
//            model.addAttribute("error", "Đăng nhập thất bại: " + e.getMessage());
//            if (redirectUrl != null && !redirectUrl.isEmpty()) {
//                model.addAttribute("redirectUrl", redirectUrl);
//            }
//            return "security/login";
//        }
//    }

//    @PostMapping("/register")
//    public String handleRegister(@RequestParam String email,
//                               @RequestParam String password,
//                               @RequestParam String phone,
//                               @RequestParam(required = false) String redirectUrl,
//                               HttpSession session,
//                               Model model) {
//        System.out.println("register is called");
//        try {
//            // Create register request
//            var registerRequest = new com.group02.openevent.dto.request.RegisterRequest();
//            registerRequest.setEmail(email);
//            registerRequest.setPassword(password);
//            registerRequest.setPhoneNumber(phone);
//            registerRequest.setRole(com.group02.openevent.model.enums.Role.CUSTOMER); // Default role
//
//            var response = authService.register(registerRequest);
//
//            // Store in session
//            session.setAttribute("ACCOUNT_ID", response.getAccountId());
//            session.setAttribute("ACCOUNT_ROLE", response.getRole());
//
//            // Redirect to original URL if provided, otherwise go to home
//            if (redirectUrl != null && !redirectUrl.isEmpty()) {
//                return "redirect:" + redirectUrl;
//            }
//            return "redirect:/";
//        } catch (Exception e) {
//            model.addAttribute("error", "Đăng ký thất bại: " + e.getMessage());
//            if (redirectUrl != null && !redirectUrl.isEmpty()) {
//                model.addAttribute("redirectUrl", redirectUrl);
//            }
//            return "security/login";
//        }
//    }

    @GetMapping("/ticket")
    public String showTicketDemo() {
        return "event/view-ticket";
    }
}
