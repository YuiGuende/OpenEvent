package com.group02.openevent.controller.request;

import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/my/requests")
@RequiredArgsConstructor
@Slf4j
public class MyRequestsController {

    private final ICustomerRepo customerRepo;
    private final VolunteerService volunteerService;

    @GetMapping
    public String myRequests(Model model, HttpSession session) {
        // Chỉ sử dụng USER_ID
        Long userId = (Long) session.getAttribute("USER_ID");
        
        if (userId == null) {
            log.warn("No USER_ID found in session, redirecting to login");
            return "redirect:/login";
        }
        
        // Tìm Customer bằng userId
        var customer = customerRepo.findByUser_UserId(userId).orElse(null);
        if (customer == null) {
            log.info("No customer found for userId {}, returning empty requests list", userId);
            model.addAttribute("sentRequests", List.of());
            return "user/my-requests";
        }
        
        List<VolunteerApplication> apps = volunteerService.getVolunteerApplicationsByCustomerId(customer.getCustomerId());
        model.addAttribute("sentRequests", apps);
        return "user/my-requests";
    }
}


