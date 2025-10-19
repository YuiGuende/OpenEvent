package com.group02.openevent.controller.auth;

import com.group02.openevent.dto.request.LoginRequest;
import com.group02.openevent.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String redirect, Model model) {
        if (redirect != null && !redirect.isEmpty()) {
            model.addAttribute("redirectUrl", redirect);
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

    @PostMapping("/register")
    public String handleRegister(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String phone,
                               @RequestParam(required = false) String redirectUrl,
                               HttpSession session,
                               Model model) {
        try {
            // Create register request
            var registerRequest = new com.group02.openevent.dto.request.RegisterRequest();
            registerRequest.setEmail(email);
            registerRequest.setPassword(password);
            registerRequest.setPhoneNumber(phone);
            registerRequest.setRole(com.group02.openevent.model.enums.Role.CUSTOMER); // Default role
            
            var response = authService.register(registerRequest);
            
            // Store in session
            session.setAttribute("ACCOUNT_ID", response.getAccountId());
            session.setAttribute("ACCOUNT_ROLE", response.getRole());
            
            // Redirect to original URL if provided, otherwise go to home
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng ký thất bại: " + e.getMessage());
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                model.addAttribute("redirectUrl", redirectUrl);
            }
            return "security/login";
        }
    }

    @GetMapping("/ticket")
    public String showTicketDemo() {
        return "event/view-ticket";
    }
}
