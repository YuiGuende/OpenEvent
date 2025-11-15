package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.IFestivalService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.UserService;
import com.group02.openevent.service.VolunteerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FestivalController {

    private final IFestivalService festivalService;
    private final EventService eventService;
    private final OrderService orderService;
    private final UserService userService;
    private final VolunteerService volunteerService;

    public FestivalController(IFestivalService festivalService, EventService eventService, OrderService orderService, UserService userService, VolunteerService volunteerService) {
        this.festivalService = festivalService;
        this.eventService = eventService;
        this.orderService = orderService;
        this.userService = userService;
        this.volunteerService = volunteerService;
    }

    @GetMapping("/festival/{id}")
    public String getFestivalEventDetail(@PathVariable("id") Long id, Model model, HttpSession session) {
        try {
            // 1. Kiểm tra event type trước
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
            
            // 2. Kiểm tra xem có phải Festival Event không
            if (event.getEventType() != EventType.FESTIVAL) {
                // Redirect tới router chung, nó sẽ tự động forward tới đúng controller
                return "redirect:/events/" + id;
            }
            
            // Check if user has already purchased a ticket for this event
            boolean hasPurchasedTicket = false;
            Long customerId = null;
            Boolean isVolunteerApproved = null;
            
            try {
                Customer customer = userService.getCurrentUser(session).getCustomer();
                if (customer != null) {
                    customerId = customer.getCustomerId();
                    hasPurchasedTicket = orderService.hasCustomerRegisteredForEvent(customerId, id);
                    // Check if user is an approved volunteer
                    try {
                        isVolunteerApproved = volunteerService.isCustomerApprovedVolunteer(customerId, id);
                    } catch (Exception ve) {
                        // If volunteer check fails, assume not approved
                        isVolunteerApproved = false;
                    }
                }
            } catch (Exception e) {
                // User not logged in or not a customer - allow access
                hasPurchasedTicket = false;
                isVolunteerApproved = false;
            }
            
            // 3. Lấy ra DTO
            FestivalEventDetailDTO eventDetail = festivalService.getFestivalEventById(id);
            model.addAttribute("eventDetail", eventDetail);
            model.addAttribute("error", null);
            model.addAttribute("hasPurchasedTicket", hasPurchasedTicket);
            model.addAttribute("eventId", id);
            model.addAttribute("customerId", customerId);
            model.addAttribute("isVolunteerApproved", isVolunteerApproved);
        } catch (Exception e) {
            System.err.println("Lỗi khi tải chi tiết lễ hội ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("eventDetail", null);
            model.addAttribute("error", "Không thể tìm thấy lễ hội bạn yêu cầu.");
            // Ensure variables are set even on error
            model.addAttribute("eventId", id);
            model.addAttribute("customerId", null);
            model.addAttribute("isVolunteerApproved", false);
        }

        return "festival/festivalHome";
    }
}