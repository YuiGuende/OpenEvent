package com.group02.openevent.controller.volunteer;

import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.model.volunteer.VolunteerStatus;
import com.group02.openevent.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/manage")
@RequiredArgsConstructor
@Slf4j
public class VolunteerController {

    private final VolunteerService volunteerService;

    // Host: list all volunteer applications for an event
    @GetMapping("/event/{eventId}/volunteers/applications")
    public String listApplications(@PathVariable Long eventId, Model model) {
        List<VolunteerApplication> all = volunteerService.getVolunteerApplicationsByEventId(eventId);
        model.addAttribute("applications", all);
        model.addAttribute("eventId", eventId);
        return "host/volunteer-applications";
    }

    // Host: approve application
    @PostMapping("/volunteers/{applicationId}/approve")
    public String approve(@PathVariable Long applicationId,
                          @RequestParam Long eventId,
                          HttpSession session,
                          @RequestParam(required = false) String response) {
        Long reviewerUserId = (Long) session.getAttribute("USER_ID");
        if (reviewerUserId == null) {
            throw new IllegalStateException("User not logged in");
        }
        volunteerService.approveVolunteerApplication(applicationId, reviewerUserId, response);
        return "redirect:/manage/event/" + eventId + "/volunteers/applications?success=approved";
    }

    // Host: reject application
    @PostMapping("/volunteers/{applicationId}/reject")
    public String reject(@PathVariable Long applicationId,
                         @RequestParam Long eventId,
                         HttpSession session,
                         @RequestParam(required = false) String response) {
        Long reviewerUserId = (Long) session.getAttribute("USER_ID");
        if (reviewerUserId == null) {
            throw new IllegalStateException("User not logged in");
        }
        volunteerService.rejectVolunteerApplication(applicationId, reviewerUserId, response);
        return "redirect:/manage/event/" + eventId + "/volunteers/applications?success=rejected";
    }

    // Host: list approved volunteers (management)
    @GetMapping("/fragments/volunteers")
    public String listApproved(@RequestParam Long id, Model model) {
        List<VolunteerApplication> approved = volunteerService.getVolunteerApplicationsByEventIdAndStatus(id, VolunteerStatus.APPROVED);
        model.addAttribute("volunteers", approved);
        model.addAttribute("eventId", id);
        return "host/volunteers :: content";
    }

    // Host: "suspend" by marking as REJECTED with a flag in hostResponse
    @PostMapping("/volunteers/{applicationId}/suspend")
    public String suspend(@PathVariable Long applicationId, @RequestParam Long eventId, HttpSession session) {
        Long reviewerUserId = (Long) session.getAttribute("USER_ID");
        if (reviewerUserId == null) {
            throw new IllegalStateException("User not logged in");
        }
        volunteerService.rejectVolunteerApplication(applicationId, reviewerUserId, "[SUSPENDED] Suspended by host");
        return "redirect:/manage/event/" + eventId + "/volunteers?success=suspended";
    }

    // Host: "unsuspend" by marking as APPROVED again
    @PostMapping("/volunteers/{applicationId}/unsuspend")
    public String unsuspend(@PathVariable Long applicationId, @RequestParam Long eventId, HttpSession session) {
        Long reviewerUserId = (Long) session.getAttribute("USER_ID");
        if (reviewerUserId == null) {
            throw new IllegalStateException("User not logged in");
        }
        volunteerService.approveVolunteerApplication(applicationId, reviewerUserId, "Unsuspended by host");
        return "redirect:/manage/event/" + eventId + "/volunteers?success=unsuspended";
    }

    // Host: approve by customerId + eventId (used from form responses view)
    @PostMapping("/volunteers/approve-by-customer")
    public String approveByCustomer(@RequestParam Long eventId,
                                    @RequestParam Long customerId,
                                    @RequestParam(required = false) Long formId,
                                    HttpSession session) {
        Long reviewerUserId = (Long) session.getAttribute("USER_ID");
        if (reviewerUserId == null) {
            throw new IllegalStateException("User not logged in");
        }
        var opt = volunteerService.getVolunteerApplicationByCustomerAndEvent(customerId, eventId);
        opt.ifPresent(app -> volunteerService.approveVolunteerApplication(app.getVolunteerApplicationId(), reviewerUserId, "Approved from volunteer requests"));
        return "redirect:/manage/event/" + eventId + "/volunteer-requests?success=approved";
    }

    // Host: reject by customerId + eventId (used from form responses view)
    @PostMapping("/volunteers/reject-by-customer")
    public String rejectByCustomer(@RequestParam Long eventId,
                                   @RequestParam Long customerId,
                                   @RequestParam(required = false) Long formId,
                                   HttpSession session) {
        Long reviewerUserId = (Long) session.getAttribute("USER_ID");
        if (reviewerUserId == null) {
            throw new IllegalStateException("User not logged in");
        }
        var opt = volunteerService.getVolunteerApplicationByCustomerAndEvent(customerId, eventId);
        opt.ifPresent(app -> volunteerService.rejectVolunteerApplication(app.getVolunteerApplicationId(), reviewerUserId, "Rejected from volunteer requests"));
        return "redirect:/manage/event/" + eventId + "/volunteer-requests?success=rejected";
    }
}
