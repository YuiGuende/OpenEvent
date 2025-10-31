package com.group02.openevent.controller.attendance;

import com.google.zxing.WriterException;
import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.QRCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private QRCodeService qrCodeService;
    
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
    public ResponseEntity<List<EventAttendance>> getAttendances(@PathVariable Long eventId) {
        List<EventAttendance> attendances = attendanceService.getAttendancesByEventId(eventId);
        return ResponseEntity.ok(attendances);
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
}




