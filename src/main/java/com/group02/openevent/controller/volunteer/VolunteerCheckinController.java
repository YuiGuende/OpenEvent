package com.group02.openevent.controller.volunteer;

import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.volunteer.VolunteerStatus;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.UserService;
import com.group02.openevent.service.VolunteerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/my-volunteer")
@RequiredArgsConstructor
@Slf4j
public class VolunteerCheckinController {
    
    private final VolunteerService volunteerService;
    private final EventAttendanceService attendanceService;
    private final EventService eventService;
    private final UserService userService;
    private final ICustomerRepo customerRepo;
    private final IEventAttendanceRepo attendanceRepo;
    private final IOrderRepo orderRepo;
    
    /**
     * Hiển thị danh sách các event mà user đang làm volunteer
     */
    @GetMapping
    public String myVolunteerEvents(HttpSession session, Model model) {
        try {
            User currentUser = userService.getCurrentUser(session);
            Long userId = currentUser.getUserId();
            
            // Lấy customer của user
            Optional<Customer> customerOpt = customerRepo.findByUser_UserId(userId);
            if (customerOpt.isEmpty()) {
                model.addAttribute("error", "Customer not found");
                return "error/500";
            }
            
            Customer customer = customerOpt.get();
            
            // Lấy danh sách volunteer applications đã được approve
            List<com.group02.openevent.model.volunteer.VolunteerApplication> approvedApplications = 
                volunteerService.getVolunteerApplicationsByCustomerIdAndStatus(
                    customer.getCustomerId(), VolunteerStatus.APPROVED);
            
            // Lấy danh sách events từ applications
            List<Event> volunteerEvents = approvedApplications.stream()
                .map(com.group02.openevent.model.volunteer.VolunteerApplication::getEvent)
                .collect(Collectors.toList());
            
            model.addAttribute("events", volunteerEvents);
            model.addAttribute("userId", userId);
            
            return "volunteer/my-volunteer";
        } catch (Exception e) {
            log.error("Error loading volunteer events", e);
            model.addAttribute("error", "Error loading volunteer events: " + e.getMessage());
            return "error/500";
        }
    }
    
    /**
     * Hiển thị danh sách attendees của một event để volunteer check-in
     */
    @GetMapping("/{eventId}/attendees")
    public String showAttendees(@PathVariable Long eventId, HttpSession session, Model model) {
        try {
            User currentUser = userService.getCurrentUser(session);
            Long userId = currentUser.getUserId();
            
            // Kiểm tra user có phải là approved volunteer của event này không
            Optional<Customer> customerOpt = customerRepo.findByUser_UserId(userId);
            if (customerOpt.isEmpty()) {
                model.addAttribute("error", "Customer not found");
                return "error/500";
            }
            
            Customer customer = customerOpt.get();
            boolean isVolunteer = volunteerService.isCustomerApprovedVolunteer(customer.getCustomerId(), eventId);
            
            if (!isVolunteer) {
                model.addAttribute("error", "You are not an approved volunteer for this event");
                return "error/403";
            }
            
            // Lấy thông tin event
            Event event = eventService.getEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
            
            // Lấy danh sách attendees của event
            List<EventAttendance> attendees = attendanceService.getAttendancesByEventId(eventId);
            
            // Kiểm tra và tạo EventAttendance cho các orders đã thanh toán nhưng chưa có EventAttendance
            List<Order> paidOrders = orderRepo.findByEventId(eventId).stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .collect(java.util.stream.Collectors.toList());
            
            log.info("Found {} paid orders for event {}", paidOrders.size(), eventId);
            
            // Tạo EventAttendance cho các orders chưa có
            int createdCount = 0;
            for (Order order : paidOrders) {
                try {
                    // Kiểm tra xem đã có EventAttendance chưa
                    Optional<EventAttendance> existing = attendanceRepo.findByOrder_OrderId(order.getOrderId());
                    if (existing.isEmpty()) {
                        // Tạo EventAttendance từ order
                        attendanceService.createAttendanceFromOrder(order);
                        createdCount++;
                        log.info("Created EventAttendance for order {}", order.getOrderId());
                    }
                } catch (Exception e) {
                    log.warn("Error creating EventAttendance for order {}: {}", order.getOrderId(), e.getMessage());
                }
            }
            
            if (createdCount > 0) {
                log.info("Created {} new EventAttendance records for event {}", createdCount, eventId);
                // Lấy lại danh sách attendees sau khi tạo
                attendees = attendanceService.getAttendancesByEventId(eventId);
            }
            
            log.info("Total attendees for event {}: {}", eventId, attendees.size());
            
            model.addAttribute("eventId", eventId);
            model.addAttribute("attendees", attendees);
            model.addAttribute("event", event);
            
            return "volunteer/volunteer-attendees";
        } catch (Exception e) {
            log.error("Error loading attendees for event {}", eventId, e);
            model.addAttribute("error", "Error loading attendees: " + e.getMessage());
            return "error/500";
        }
    }
    
    /**
     * Volunteer check-in một attendee
     */
    @PostMapping("/{eventId}/checkin/{attendanceId}")
    public String checkinAttendee(
            @PathVariable Long eventId,
            @PathVariable Long attendanceId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(session);
            Long userId = currentUser.getUserId();
            
            // Kiểm tra user có phải là approved volunteer của event này không
            Optional<Customer> customerOpt = customerRepo.findByUser_UserId(userId);
            if (customerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Customer not found");
                return "redirect:/my-volunteer";
            }
            
            Customer customer = customerOpt.get();
            boolean isVolunteer = volunteerService.isCustomerApprovedVolunteer(customer.getCustomerId(), eventId);
            
            if (!isVolunteer) {
                redirectAttributes.addFlashAttribute("error", "You are not an approved volunteer for this event");
                return "redirect:/my-volunteer";
            }
            
            // Lấy attendance record
            Optional<EventAttendance> attendanceOpt = attendanceRepo.findByEvent_IdAndAttendanceId(eventId, attendanceId);
            if (attendanceOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Attendance record not found");
                return "redirect:/my-volunteer/" + eventId + "/attendees";
            }
            
            EventAttendance attendance = attendanceOpt.get();
            
            // Kiểm tra đã check-in chưa
            if (attendance.getCheckInTime() != null) {
                redirectAttributes.addFlashAttribute("info", "Attendee already checked in at " + attendance.getCheckInTime());
                return "redirect:/my-volunteer/" + eventId + "/attendees";
            }
            
            // Check-in attendee using service method
            attendance = attendanceService.listCheckIn(eventId, attendanceId);
            
            log.info("Volunteer {} checked in attendee {} for event {}", userId, attendanceId, eventId);
            redirectAttributes.addFlashAttribute("success", "Successfully checked in " + attendance.getFullName());
            
            return "redirect:/my-volunteer/" + eventId + "/attendees";
        } catch (Exception e) {
            log.error("Error checking in attendee {} for event {}", attendanceId, eventId, e);
            redirectAttributes.addFlashAttribute("error", "Error checking in attendee: " + e.getMessage());
            return "redirect:/my-volunteer/" + eventId + "/attendees";
        }
    }
}

