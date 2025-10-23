package com.group02.openevent.controller;

import com.group02.openevent.dto.request.LoginRequest;
import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.service.AuthService;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.model.user.Customer;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class loginController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private EventAttendanceService attendanceService;
    
    @Autowired
    private ICustomerRepo customerRepo;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String redirect, Model model) {
        if (redirect != null && !redirect.isEmpty()) {
            model.addAttribute("redirectUrl", redirect);
        }
        return "security/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email, 
                            @RequestParam String password,
                            @RequestParam(required = false) String redirectUrl,
                            @RequestParam(required = false) String checkinEventId,
                            @RequestParam(required = false) String checkinAction,
                            HttpSession session, 
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword(password);
            
            var response = authService.login(loginRequest);
            
            // Store in session
            session.setAttribute("ACCOUNT_ID", response.getAccountId());
            session.setAttribute("ACCOUNT_ROLE", response.getRole());
            
            // Handle automatic check-in/check-out if requested
            if (checkinEventId != null && !checkinEventId.isEmpty()) {
                try {
                    Long eventId = Long.parseLong(checkinEventId);
                    
                    // Get customer info
                    Customer customer = customerRepo.findByAccount_AccountId(response.getAccountId()).orElse(null);
                    if (customer != null) {
                        if ("checkin".equals(checkinAction)) {
                            // Auto check-in
                            AttendanceRequest attendanceRequest = new AttendanceRequest();
                            attendanceRequest.setFullName(customer.getAccount().getEmail()); // Use email as name fallback
                            attendanceRequest.setEmail(customer.getEmail());
                            attendanceRequest.setPhone(customer.getPhoneNumber());
                            attendanceRequest.setOrganization(customer.getOrganization() != null ? customer.getOrganization().getOrgName() : null);
                            
                            attendanceService.checkIn(eventId, attendanceRequest);
                            redirectAttributes.addFlashAttribute("successMessage", 
                                "✅ Đăng nhập và check-in thành công! Chào mừng " + customer.getEmail());
                        } else if ("checkout".equals(checkinAction)) {
                            // Auto check-out
                            attendanceService.checkOut(eventId, customer.getEmail());
                            redirectAttributes.addFlashAttribute("successMessage", 
                                "✅ Đăng nhập và check-out thành công! Cảm ơn bạn đã tham gia.");
                        }
                    }
                } catch (Exception e) {
                    // If check-in/out fails, still redirect but with error message
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "⚠️ Đăng nhập thành công nhưng check-in/out thất bại: " + e.getMessage());
                }
            }
            
            // Redirect to original URL if provided, otherwise go to home
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng nhập thất bại: " + e.getMessage());
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                model.addAttribute("redirectUrl", redirectUrl);
            }
            return "security/login";
        }
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String phone,
                               @RequestParam(required = false) String organization,
                               @RequestParam(required = false) String redirectUrl,
                               @RequestParam(required = false) String checkinEventId,
                               @RequestParam(required = false) String checkinAction,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
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
            
            // Handle automatic check-in/check-out if requested
            if (checkinEventId != null && !checkinEventId.isEmpty()) {
                try {
                    Long eventId = Long.parseLong(checkinEventId);
                    
                    // Get customer info
                    Customer customer = customerRepo.findByAccount_AccountId(response.getAccountId()).orElse(null);
                    if (customer != null) {
                        if ("checkin".equals(checkinAction)) {
                            // Auto check-in
                            AttendanceRequest attendanceRequest = new AttendanceRequest();
                            attendanceRequest.setFullName(customer.getAccount().getEmail()); // Use email as name fallback
                            attendanceRequest.setEmail(customer.getEmail());
                            attendanceRequest.setPhone(customer.getPhoneNumber());
                            attendanceRequest.setOrganization(organization != null ? organization : (customer.getOrganization() != null ? customer.getOrganization().getOrgName() : null));
                            
                            attendanceService.checkIn(eventId, attendanceRequest);
                            redirectAttributes.addFlashAttribute("successMessage", 
                                "✅ Đăng ký và check-in thành công! Chào mừng " + customer.getEmail());
                        } else if ("checkout".equals(checkinAction)) {
                            // Auto check-out
                            attendanceService.checkOut(eventId, customer.getEmail());
                            redirectAttributes.addFlashAttribute("successMessage", 
                                "✅ Đăng ký và check-out thành công! Cảm ơn bạn đã tham gia.");
                        }
                    }
                } catch (Exception e) {
                    // If check-in/out fails, still redirect but with error message
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "⚠️ Đăng ký thành công nhưng check-in/out thất bại: " + e.getMessage());
                }
            }
            
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
