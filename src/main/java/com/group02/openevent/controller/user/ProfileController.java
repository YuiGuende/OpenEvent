package com.group02.openevent.controller.user;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.IImageService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for customer profile management
 */
@Controller
@RequestMapping
@Slf4j
public class ProfileController {
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private IImageService imageService;
    
    @Autowired
    private IAccountRepo accountRepo;
    
    @Autowired
    private IUserRepo userRepo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * GET /profile - Display profile page
     */
    @GetMapping("/profile")
    public String showProfilePage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        log.info("=== ProfileController.showProfilePage called ===");
        
        // Debug session
        if (session != null) {
            log.info("ProfileController - Session ID: {}", session.getId());
            log.info("ProfileController - USER_ID: {}", session.getAttribute("USER_ID"));
            log.info("ProfileController - USER_ROLE: {}", session.getAttribute("USER_ROLE"));
            log.info("ProfileController - All session attributes:");
            java.util.Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                log.info("  - {}: {}", attrName, session.getAttribute(attrName));
            }
        } else {
            log.warn("ProfileController - Session is NULL!");
        }
        
        try {
            Customer customer = customerService.getCurrentCustomer(session);
            log.info("ProfileController - Customer found: {}", customer.getCustomerId());
            model.addAttribute("customer", customer);
            model.addAttribute("user", customer.getUser());
            log.info("ProfileController - Returning profile page");
            return "user/profile";
        } catch (RuntimeException e) {
            log.warn("ProfileController - Access denied to profile page: {}", e.getMessage());
            log.warn("ProfileController - Exception stack trace:", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để xem hồ sơ.");
            // Add currentUri to redirect so user can return after login
            log.info("ProfileController - Redirecting to /login?currentUri=/profile");
            return "redirect:/login?currentUri=/profile";
        }
    }
    
    /**
     * POST /api/profile/upload-avatar
     * Upload profile avatar image to Cloudinary and save URL to Customer.avatarUrl
     */
    @PostMapping("/api/profile/upload-avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File không được để trống"));
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File phải là ảnh"));
            }
            
            // Get current customer
            Customer customer = customerService.getCurrentCustomer(session);
            
            // Upload image to Cloudinary
            String imageUrl = imageService.saveImage(file);
            log.info("Avatar uploaded to Cloudinary: {} for customer {}", imageUrl, customer.getCustomerId());
            
            // Save URL to customer
            customer.setAvatarUrl(imageUrl);
            customerService.save(customer);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "Upload ảnh đại diện thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error uploading avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi hệ thống. Vui lòng thử lại."));
        }
    }
    
    /**
     * POST /api/profile/register-face
     * Register face for face check-in (set faceRegistered = true)
     * Requires customer to have avatarUrl first
     */
    @PostMapping("/api/profile/register-face")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerFace(HttpSession session) {
        try {
            // Get current customer
            Customer customer = customerService.getCurrentCustomer(session);
            
            // Validate customer has avatarUrl
            if (customer.getAvatarUrl() == null || customer.getAvatarUrl().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", 
                        "Bạn chưa có ảnh đại diện. Vui lòng upload ảnh đại diện trước khi đăng ký khuôn mặt."));
            }
            
            // Set faceRegistered = true
            customer.setFaceRegistered(true);
            customerService.save(customer);
            
            log.info("Face registered for customer {}", customer.getCustomerId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đăng ký khuôn mặt thành công. Bạn có thể sử dụng check-in bằng khuôn mặt.");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error registering face", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error registering face", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi hệ thống. Vui lòng thử lại."));
        }
    }
    
    /**
     * POST /api/profile/unregister-face
     * Unregister face for face check-in (set faceRegistered = false)
     */
    @PostMapping("/api/profile/unregister-face")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unregisterFace(HttpSession session) {
        try {
            // Get current customer
            Customer customer = customerService.getCurrentCustomer(session);
            
            // Set faceRegistered = false
            customer.setFaceRegistered(false);
            customerService.save(customer);
            
            log.info("Face unregistered for customer {}", customer.getCustomerId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Hủy đăng ký khuôn mặt thành công.");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error unregistering face", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error unregistering face", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi hệ thống. Vui lòng thử lại."));
        }
    }
    
    /**
     * PUT /api/profile/update
     * Update user profile information (name, phone number)
     * Email cannot be updated
     */
    @PutMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            HttpSession session) {
        
        try {
            // Get current customer
            Customer customer = customerService.getCurrentCustomer(session);
            User user = customer.getUser();
            
            // Update name if provided
            if (name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
            }
            
            // Update phone number if provided
            if (phoneNumber != null) {
                // Allow empty phone number
                user.setPhoneNumber(phoneNumber.trim().isEmpty() ? null : phoneNumber.trim());
            }
            
            // Save user
            userRepo.save(user);
            
            log.info("Profile updated for user {}", user.getUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật thông tin thành công");
            response.put("user", Map.of(
                "name", user.getName() != null ? user.getName() : "",
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error updating profile", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi hệ thống. Vui lòng thử lại."));
        }
    }
    
    /**
     * POST /api/profile/change-password
     * Change user password
     * Requires old password validation before setting new password
     */
    @PostMapping("/api/profile/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session) {
        
        try {
            // Validate inputs
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Vui lòng nhập mật khẩu cũ"));
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Vui lòng nhập mật khẩu mới"));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Mật khẩu mới phải có ít nhất 6 ký tự"));
            }
            
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Mật khẩu mới và xác nhận mật khẩu không khớp"));
            }
            
            // Get current customer and account
            Customer customer = customerService.getCurrentCustomer(session);
            User user = customer.getUser();
            Account account = user.getAccount();
            
            // Check if account has password (OAuth users might not have password)
            if (account.getPasswordHash() == null || account.getPasswordHash().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Tài khoản này không có mật khẩu. Vui lòng sử dụng phương thức đăng nhập khác."));
            }
            
            // Validate old password
            if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Mật khẩu cũ không đúng"));
            }
            
            // Check if new password is same as old password
            if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Mật khẩu mới phải khác mật khẩu cũ"));
            }
            
            // Update password
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            accountRepo.save(account);
            
            log.info("Password changed for account {}", account.getAccountId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error changing password", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Lỗi hệ thống. Vui lòng thử lại."));
        }
    }
}

