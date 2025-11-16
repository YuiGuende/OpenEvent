package com.group02.openevent.controller.attendance;

import com.google.zxing.WriterException;
import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.dto.attendance.CheckInListDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.FaceCheckinService;
import com.group02.openevent.service.QRCodeService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

import java.time.Duration;
import java.util.List;


@Controller
@RequestMapping("/events")
@Slf4j
public class EventAttendanceController {
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private EventAttendanceService attendanceService;
    
    @Autowired
    private IEventAttendanceRepo attendanceRepo;

    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private FaceCheckinService faceCheckinService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private IOrderRepo orderRepo;


    /**
     * Trang hiển thị 2 QR codes (check-in & check-out)
     * URL: /events/{eventId}/attendance
     */
    @GetMapping("/{eventId}/attendance")
    public String showAttendancePage(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Get base URL from request or config
        String baseUrl = "http://localhost:8080"; // TODO: Get from config
        
        // Create URLs for QR codes - redirect to login first
        String checkinUrl = baseUrl + "/events/" + eventId + "/qr-checkin";
        String checkoutUrl = baseUrl + "/events/" + eventId + "/qr-checkout";
        
        model.addAttribute("event", event);
        model.addAttribute("checkinUrl", checkinUrl);
        model.addAttribute("checkoutUrl", checkoutUrl);
        
        return "event/event-checkin-page";
    }
    
    /**
     * Generate QR code image on-the-fly
     * URL: /qr-code/generate?url=...
     */
    @GetMapping("/qr-code/generate")
    public ResponseEntity<byte[]> generateQRCode(@RequestParam String url) {
        try {
            byte[] qrImage = qrCodeService.generateQRCodeImage(url, 350, 350);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            
            return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Form check-in
     * URL: /events/{eventId}/checkin-form
     * Note: Spring Security tự động yêu cầu authentication cho endpoint này
     */
    @GetMapping("/{eventId}/checkin-form")
    public String showCheckinForm(@PathVariable Long eventId, Model model) {
        // Spring Security đã tự động kiểm tra authentication
        // Nếu chưa đăng nhập, user sẽ được redirect tới /login và sau đó quay lại đây
        
        Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        model.addAttribute("event", event);
        model.addAttribute("attendanceRequest", new AttendanceRequest());
        
        return "event/checkin-form";
    }
    
    /**
     * QR Code Check-in redirect to login
     * URL: /events/{eventId}/qr-checkin
     * Redirects to check-in form (EventForm with type CHECKIN)
     */
    @GetMapping("/{eventId}/qr-checkin")
    public String qrCheckinRedirect(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Redirect to check-in form (EventForm)
        String checkinFormUrl = "/forms/checkin/" + eventId;
        String redirectUrl = "redirect:/login?checkin=true&eventId=" + eventId + "&eventTitle=" + 
               java.net.URLEncoder.encode(event.getTitle(), java.nio.charset.StandardCharsets.UTF_8) +
               "&action=checkin&redirectUrl=" + java.net.URLEncoder.encode(checkinFormUrl, java.nio.charset.StandardCharsets.UTF_8);
        
        log.info("QR Check-in for event {} - Redirecting to: {}", eventId, redirectUrl);
        log.info("Target checkin form URL: {}", checkinFormUrl);
        
        return redirectUrl;
    }
    
    /**
     * QR Code Check-out redirect to login
     * URL: /events/{eventId}/qr-checkout
     * Redirects to feedback form (EventForm with type FEEDBACK)
     */
    @GetMapping("/{eventId}/qr-checkout")
    public String qrCheckoutRedirect(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Redirect to feedback form (EventForm)
        String feedbackFormUrl = "/forms/feedback/" + eventId;
        String redirectUrl = "redirect:/login?checkin=true&eventId=" + eventId + "&eventTitle=" + 
               java.net.URLEncoder.encode(event.getTitle(), java.nio.charset.StandardCharsets.UTF_8) +
               "&action=checkout&redirectUrl=" + java.net.URLEncoder.encode(feedbackFormUrl, java.nio.charset.StandardCharsets.UTF_8);
        
        log.info("QR Check-out for event {} - Redirecting to: {}", eventId, redirectUrl);
        log.info("Target feedback form URL: {}", feedbackFormUrl);
        
        return redirectUrl;
    }
    
    /**
     * Xử lý check-in
     * POST: /events/{eventId}/checkin
     */
    @PostMapping("/{eventId}/checkin")
    public String processCheckin(
            @PathVariable Long eventId,
            @ModelAttribute AttendanceRequest request,
            RedirectAttributes redirectAttributes) {
        
        try {
            EventAttendance attendance = attendanceService.checkIn(eventId, request);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                " Check-in thành công! Chào mừng " + attendance.getFullName() + " đến với sự kiện!");
            redirectAttributes.addFlashAttribute("checkInTime", attendance.getCheckInTime());
            
            return "redirect:/events/" + eventId + "/checkin-form";
            
        } catch (Exception e) {
            log.error("Error during check-in for event {}", eventId, e);
            redirectAttributes.addFlashAttribute("errorMessage", " " + e.getMessage());
            return "redirect:/events/" + eventId + "/checkin-form";
        }
    }
    
    /**
     * Form check-out
     * URL: /events/{eventId}/checkout-form
     */
    @GetMapping("/{eventId}/checkout-form")
    public String showCheckoutForm(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        model.addAttribute("event", event);
        
        return "event/checkout-form";
    }
    
    /**
     * Xử lý check-out
     * POST: /events/{eventId}/checkout
     */
    @PostMapping("/{eventId}/checkout")
    public String processCheckout(
            @PathVariable Long eventId,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        
        try {
            EventAttendance attendance = attendanceService.checkOut(eventId, email);
            
            // Calculate duration
            Duration duration = Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            
            redirectAttributes.addFlashAttribute("successMessage", 
                " Check-out thành công! Cảm ơn bạn đã tham gia. Thời gian: " + hours + "h " + minutes + "m");
            
            return "redirect:/events/" + eventId + "/checkout-form";
            
        } catch (Exception e) {
            log.error("Error during check-out for event {}", eventId, e);
            redirectAttributes.addFlashAttribute("errorMessage", " " + e.getMessage());
            return "redirect:/events/" + eventId + "/checkout-form";
        }
    }
    
    /**
     * API: Get attendance list for an event
     * GET: /events/{eventId}/attendances
     */
    @GetMapping("/{eventId}/attendances")
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<CheckInListDTO>> getAttendances(@PathVariable Long eventId) {
        log.info("=== Fetching attendances for event: {} ===", eventId);

        try {
            // Verify event exists first
            Event event = eventService.getEventById(eventId)
                    .orElse(null);
            if (event == null) {
                log.error("❌ Event not found with ID: {}", eventId);
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(new java.util.ArrayList<>());
            }
            log.info("✅ Event found: {} (ID: {})", event.getTitle(), eventId);

            // Use service method that eagerly loads order to avoid lazy loading issues
            List<EventAttendance> attendances = attendanceService.getAttendancesByEventId(eventId);
            log.info("Found {} attendances for event {}", attendances.size(), eventId);

            if (attendances.isEmpty()) {
                log.warn("⚠️ No attendances found for event {}. This could mean:", eventId);
                log.warn("  1. No orders have been paid for this event");
                log.warn("  2. EventAttendance records were not created when orders were paid");
                log.warn("  3. Query is not finding records correctly");

                // Try to get count directly from repo to verify
                long totalCount = attendanceRepo.countByEventId(eventId);
                log.info("📊 Total EventAttendance count in DB for event {}: {}", eventId, totalCount);

                if (totalCount > 0) {
                    log.error("❌ CRITICAL: Database has {} records but query returned 0! Query issue detected!", totalCount);
                }
            } else {
                log.info("Sample attendance: ID={}, Name={}, Email={}, CheckInTime={}, Status={}, OrderId={}",
                    attendances.get(0).getAttendanceId(),
                    attendances.get(0).getFullName(),
                    attendances.get(0).getEmail(),
                    attendances.get(0).getCheckInTime(),
                    attendances.get(0).getStatus(),
                    attendances.get(0).getOrder() != null ? attendances.get(0).getOrder().getOrderId() : "null");
            }

            // Convert to DTO to avoid circular reference and nested depth issues
            // Process within transaction to allow lazy loading of orderId if needed
            List<CheckInListDTO> dtos = attendances.stream()
                .map(attendance -> {
                    CheckInListDTO dto = new CheckInListDTO();
                    dto.setAttendanceId(attendance.getAttendanceId());

                    // Get orderId safely - Hibernate proxy allows accessing ID without full load
                    // But we need to be in a transaction context
                    Long orderId = null;
                    try {
                        // Check if order proxy is initialized
                        if (attendance.getOrder() != null) {
                            // Use Hibernate's getIdentifier() if available, or just getOrderId()
                            // This should work within transaction context
                            orderId = attendance.getOrder().getOrderId();
                            log.debug("Got orderId {} for attendance {}", orderId, attendance.getAttendanceId());
                        } else {
                            log.debug("Attendance {} has no associated order (manually added)", attendance.getAttendanceId());
                        }
                    } catch (org.hibernate.LazyInitializationException e) {
                        // Should not happen if we're in transaction, but handle gracefully
                        log.warn("Could not get orderId for attendance {} (lazy load failed): {}",
                            attendance.getAttendanceId(), e.getMessage());
                        orderId = null;
                    } catch (Exception e) {
                        log.warn("Could not get orderId for attendance {}: {}",
                            attendance.getAttendanceId(), e.getMessage());
                        orderId = null;
                    }

                    dto.setOrderId(orderId);
                    dto.setFullName(attendance.getFullName());
                    dto.setEmail(attendance.getEmail());
                    dto.setPhone(attendance.getPhone());
                    dto.setOrganization(attendance.getOrganization());
                    dto.setCheckInTime(attendance.getCheckInTime());
                    dto.setCheckOutTime(attendance.getCheckOutTime());
                    dto.setStatus(attendance.getStatus() != null ? attendance.getStatus().name() : null);
                    dto.setCreatedAt(attendance.getCreatedAt());

                    log.debug("Created DTO: attendanceId={}, orderId={}, name={}, email={}, status={}, checkInTime={}",
                        attendance.getAttendanceId(), orderId, dto.getFullName(), dto.getEmail(),
                        dto.getStatus(), dto.getCheckInTime());

                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

            log.info("=== Returning {} attendance DTOs for event {} ===", dtos.size(), eventId);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("❌ Error fetching attendances for event {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new java.util.ArrayList<>());
        }
    }
    
    /**
     * API: Get attendance statistics
     * GET: /events/{eventId}/attendance-stats
     */
    @GetMapping("/{eventId}/attendance-stats")
    @ResponseBody
    public ResponseEntity<AttendanceStatsDTO> getAttendanceStats(@PathVariable Long eventId) {
        AttendanceStatsDTO stats = attendanceService.getAttendanceStats(eventId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Host dashboard: View all attendances
     * GET: /events/{eventId}/manage-attendance
     */
    @GetMapping("/{eventId}/manage-attendance")
    public String manageAttendance(@PathVariable Long eventId, Model model) {
        Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        List<EventAttendance> attendances = attendanceService.getAttendancesByEventId(eventId);
        AttendanceStatsDTO stats = attendanceService.getAttendanceStats(eventId);
        
        model.addAttribute("event", event);
        model.addAttribute("attendances", attendances);
        model.addAttribute("stats", stats);
        
        return "host/manage-attendance";
    }
    
    /**
     * Face check-in page
     * GET: /events/{eventId}/face-checkin
     * Requires login and customer must have avatarUrl and faceRegistered = true
     */
    @GetMapping("/{eventId}/face-checkin")
    public String showFaceCheckinPage(
            @PathVariable Long eventId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Check if user is logged in and get customer
            Customer customer = customerService.getCurrentCustomer(session);
            
            // Check if customer has avatarUrl
            if (customer.getAvatarUrl() == null || customer.getAvatarUrl().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Bạn chưa có ảnh đại diện. Vui lòng cập nhật ảnh đại diện trong hồ sơ.");
                return "redirect:/profile";
            }
            
            // Check if face is registered
            if (customer.getFaceRegistered() == null || !customer.getFaceRegistered()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Bạn chưa đăng ký khuôn mặt. Vui lòng đăng ký khuôn mặt trong hồ sơ.");
                return "redirect:/profile";
            }
            
            Event event = eventService.getEventById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            
            model.addAttribute("event", event);
            model.addAttribute("attendanceRequest", new AttendanceRequest());
            
            return "event/face-checkin";
            
        } catch (RuntimeException e) {
            // User not logged in or not a customer
            log.warn("Access denied to face check-in page: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Vui lòng đăng nhập và đảm bảo bạn đã có ảnh đại diện và đã đăng ký khuôn mặt.");
            return "redirect:/profile";
        }
    }
    
    /**
     * Face check-in API endpoint
     * POST: /api/events/{eventId}/face-checkin
     * Accepts JSON: { "imageBase64": "..." }
     */
    @PostMapping("/api/{eventId}/face-checkin")
    @ResponseBody
    public ResponseEntity<?> processFaceCheckin(
            @PathVariable Long eventId,
            @RequestBody java.util.Map<String, String> requestBody,
            HttpSession session) {
        
        try {
            // Get current customer
            Customer currentCustomer = customerService.getCurrentCustomer(session);
            
            // Decode base64 image
            String imageBase64 = requestBody.get("imageBase64");
            if (imageBase64 == null || imageBase64.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "error", "Không tìm thấy ảnh"));
            }
            
            // Remove data URL prefix if present (data:image/jpeg;base64,...)
            if (imageBase64.contains(",")) {
                imageBase64 = imageBase64.substring(imageBase64.indexOf(",") + 1);
            }
            
            byte[] imageBytes;
            try {
                imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
            } catch (IllegalArgumentException e) {
                log.error("Invalid base64 image data", e);
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "error", "Ảnh không hợp lệ"));
            }
            
            // Process face check-in
            EventAttendance attendance = faceCheckinService.faceCheckIn(eventId, imageBytes, currentCustomer);
            
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Check-in thành công! Chào mừng " + attendance.getFullName() + " đến với sự kiện!",
                "checkInTime", attendance.getCheckInTime().toString()
            ));
            
        } catch (RuntimeException e) {
            log.error("Error during face check-in for event {}", eventId, e);
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during face check-in", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("success", false, "error", "Lỗi hệ thống. Vui lòng thử lại."));
        }
    }

    /**
     * API endpoint to check customer's check-in status for an event
     * GET: /api/events/{eventId}/checkin-status
     */
    @GetMapping("/api/{eventId}/checkin-status")
    @ResponseBody
    public ResponseEntity<?> getCheckinStatus(
            @PathVariable Long eventId,
            HttpSession session) {
        try {
            Customer customer = customerService.getCurrentCustomer(session);

            // ✅ Tìm check-in bằng customerId và orderId (giống logic face check-in)
            // 1. Tìm paid order của customer cho event này
            List<Order> customerOrders = orderRepo.findByCustomerId(customer.getCustomerId());
            List<Order> paidOrdersForEvent = customerOrders.stream()
                .filter(order -> order.getEvent() != null &&
                               order.getEvent().getId().equals(eventId) &&
                               order.getStatus() == OrderStatus.PAID)
                .collect(java.util.stream.Collectors.toList());

            java.util.Optional<EventAttendance> attendanceOpt = java.util.Optional.empty();

            // 2. Nếu có paid order, tìm EventAttendance bằng orderId
            if (!paidOrdersForEvent.isEmpty()) {
                Order paidOrder = paidOrdersForEvent.get(0);
                attendanceOpt = attendanceRepo.findByOrder_OrderId(paidOrder.getOrderId());
            }

            // 3. Fallback: nếu không tìm thấy bằng orderId, thử tìm bằng email
            if (attendanceOpt.isEmpty()) {
                User user = customer.getUser();
                if (user != null && user.getAccount() != null) {
                    String email = user.getAccount().getEmail();
                    if (email != null && !email.trim().isEmpty()) {
                        attendanceOpt = attendanceService.getAttendanceByEventAndEmail(eventId, email);
                    }
                }
            }
            
            boolean checkedIn = false;
            String checkInTime = null;
            
            if (attendanceOpt.isPresent()) {
                EventAttendance attendance = attendanceOpt.get();
                checkedIn = attendance.getCheckInTime() != null;
                if (checkedIn) {
                    checkInTime = attendance.getCheckInTime().toString();
                }
            }
            
            return ResponseEntity.ok(java.util.Map.of(
                "checkedIn", checkedIn,
                "checkInTime", checkInTime != null ? checkInTime : "",
                "canCheckIn", !checkedIn // Can check in if not already checked in
            ));
            
        } catch (RuntimeException e) {
            // Customer not found or not logged in
            return ResponseEntity.ok(java.util.Map.of(
                "checkedIn", false,
                "canCheckIn", false,
                "message", "Please login to check-in"
            ));
        } catch (Exception e) {
            log.error("Error checking check-in status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of("error", "Error checking check-in status"));
        }
    }


}




