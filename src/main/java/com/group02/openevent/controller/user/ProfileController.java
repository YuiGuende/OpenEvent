package com.group02.openevent.controller.user;

import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.IImageService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    
    /**
     * GET /profile - Display profile page
     */
    @GetMapping("/profile")
    public String showProfilePage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = customerService.getCurrentCustomer(session);
            model.addAttribute("customer", customer);
            model.addAttribute("user", customer.getUser());
            return "user/profile";
        } catch (RuntimeException e) {
            log.warn("Access denied to profile page: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để xem hồ sơ.");
            return "redirect:/login";
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
}

