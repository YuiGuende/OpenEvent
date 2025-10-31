package com.group02.openevent.controller.form;

import com.group02.openevent.dto.form.*;
import com.group02.openevent.service.EventFormService;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.dto.attendance.AttendanceRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
@Slf4j
public class EventFormController {

    private final EventFormService eventFormService;
     private final EventAttendanceService attendanceService;

    // Host: Create form for event
    @GetMapping("/create/{eventId}")
    public String showCreateForm(@PathVariable Long eventId, @RequestParam(required = false) String type, Model model) {
        CreateFormRequest request = new CreateFormRequest();
        request.setEventId(eventId);
        if (type != null) {
            try {
                request.setFormType(com.group02.openevent.model.form.EventForm.FormType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                request.setFormType(com.group02.openevent.model.form.EventForm.FormType.FEEDBACK);
            }
        }
        // Compute page title to avoid complex Thymeleaf expressions
        String pageTitle;
        if (request.getFormType() == null) {
            pageTitle = "Create Feedback Form";
        } else {
            switch (request.getFormType()) {
                case REGISTER -> pageTitle = "Create Register Form";
                case CHECKIN -> pageTitle = "Create Check-in Form";
                default -> pageTitle = "Create Feedback Form";
            }
        }
        model.addAttribute("eventId", eventId);
        model.addAttribute("createFormRequest", request);
        model.addAttribute("pageTitle", pageTitle);
        return "host/create-feedback-form";
    }

    @PostMapping("/create")
    public String createForm(@ModelAttribute CreateFormRequest request) {
        try {
            if (request.getEventId() == null) {
                log.error("Event ID is null in form creation request");
                return "redirect:/music/null?error=missing_event_id";
            }
            
            eventFormService.createForm(request);
            return "redirect:/music/" + request.getEventId() + "?success=form_created";
        } catch (Exception e) {
            log.error("Error creating form for event ID: {}", request.getEventId(), e);
            Long eventId = request.getEventId() != null ? request.getEventId() : 0L;
            String encodedMessage = UriUtils.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/music/" + eventId + "?error=form_creation_failed&message=" + encodedMessage;
        }
    }

    // User: Select form type (Register, Check-in, Feedback)
    @GetMapping("/{eventId}")
    public String showSelectFormType(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "user/select-form-type";
    }

    // User: View and submit feedback form
    @GetMapping("/feedback/{eventId}")
    public String showFeedbackForm(@PathVariable Long eventId, Model model) {
        try {
            EventFormDTO form = eventFormService.getActiveFormByEventIdAndType(eventId, com.group02.openevent.model.form.EventForm.FormType.FEEDBACK);
            model.addAttribute("form", form);
            model.addAttribute("eventId", eventId);
            model.addAttribute("submitResponseRequest", new SubmitResponseRequest());
            return "user/feedback-form";
        } catch (Exception e) {
            log.error("Error loading feedback form for event ID: {}", eventId, e);
            // Instead of redirecting, show empty form page
            model.addAttribute("eventId", eventId);
            model.addAttribute("noFormMessage", "Chưa có form feedback nào cho sự kiện này. Vui lòng liên hệ host để tạo form.");
            model.addAttribute("form", null);
            return "user/feedback-form";
        }
    }

    // User: View register form after payment
    @GetMapping("/register/{eventId}")
    public String showRegisterForm(@PathVariable Long eventId, Model model) {
        try {
            EventFormDTO form = eventFormService.getActiveFormByEventIdAndType(eventId, com.group02.openevent.model.form.EventForm.FormType.REGISTER);
            model.addAttribute("form", form);
            model.addAttribute("eventId", eventId);
            model.addAttribute("submitResponseRequest", new SubmitResponseRequest());
            return "user/feedback-form"; // reuse generic form template
        } catch (Exception e) {
            model.addAttribute("eventId", eventId);
            model.addAttribute("noFormMessage", "Chưa có form đăng ký nào cho sự kiện này.");
            model.addAttribute("form", null);
            return "user/feedback-form";
        }
    }

    // User: View check-in form (after login and QR)
    @GetMapping("/checkin/{eventId}")
    public String showCheckinForm(@PathVariable Long eventId, Model model) {
        try {
            EventFormDTO form = eventFormService.getActiveFormByEventIdAndType(eventId, com.group02.openevent.model.form.EventForm.FormType.CHECKIN);
            model.addAttribute("form", form);
            model.addAttribute("eventId", eventId);
            model.addAttribute("submitResponseRequest", new SubmitResponseRequest());
            return "user/feedback-form"; // reuse generic form template
        } catch (Exception e) {
            model.addAttribute("eventId", eventId);
            model.addAttribute("noFormMessage", "Chưa có form check-in nào cho sự kiện này.");
            model.addAttribute("form", null);
            return "user/feedback-form";
        }
    }

    @PostMapping("/feedback/submit")
    public String submitFeedback(@ModelAttribute SubmitResponseRequest request,
                                 @RequestParam(required = false) Long eventId) {
        try {
            // Validate before submitting
            if (request.getFormId() == null) {
                throw new RuntimeException("Form ID is required");
            }
            if (request.getCustomerId() == null) {
                throw new RuntimeException("Customer ID is required");
            }
            
            // Persist responses
            eventFormService.submitResponse(request);

            // Determine form type and event
            EventFormDTO formDto = eventFormService.getFormById(request.getFormId());
            com.group02.openevent.model.form.EventForm.FormType formType = formDto.getFormType();
            Long resolvedEventId = formDto.getEventId();
            if (resolvedEventId != null) {
                eventId = resolvedEventId;
            }

            // Resolve current user email (for attendance)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = (auth != null && auth.getName() != null) ? auth.getName().trim().toLowerCase() : null;

            // Branch by form type
            if (formType == com.group02.openevent.model.form.EventForm.FormType.CHECKIN) {
                AttendanceRequest ar = new AttendanceRequest();
                ar.setEmail(currentEmail);
                // Optional: derive name/phone/org from first few responses if needed
                if (request.getResponses() != null && !request.getResponses().isEmpty()) {
                    // Use first response as fullName if non-empty
                    String first = request.getResponses().get(0).getResponseValue();
                    ar.setFullName(first != null ? first : "");
                }
                attendanceService.checkIn(eventId, ar);
                return "redirect:/events/" + eventId + "/checkin-form?success=checkin_success";
            }

            if (formType == com.group02.openevent.model.form.EventForm.FormType.FEEDBACK) {
                if (currentEmail != null) {
                    attendanceService.checkOut(eventId, currentEmail);
                }
                return "redirect:/events/" + eventId + "/checkout-form?success=checkout_success";
            }

            // After submit, redirect by type
            if (eventId != null) {
                return "redirect:/music/" + eventId + "?success=form_submitted";
            }
            return "redirect:/?success=form_submitted";
        } catch (Exception e) {
            log.error("Error submitting feedback: {}", e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            String encodedMessage = UriUtils.encode(errorMsg, StandardCharsets.UTF_8);
            if (eventId != null) {
                return "redirect:/music/" + eventId + "?error=submission_failed&message=" + encodedMessage;
            }
            return "redirect:/?error=submission_failed&message=" + encodedMessage;
        }
    }

    // Host: View responses
    @GetMapping("/responses/{eventId}")
    public String viewResponses(@PathVariable Long eventId, Model model) {
        try {
            // Get responses for each form type
            List<FormResponseDTO> registerResponses = eventFormService.getResponsesByEventIdAndFormType(
                eventId, com.group02.openevent.model.form.EventForm.FormType.REGISTER);
            List<FormResponseDTO> checkinResponses = eventFormService.getResponsesByEventIdAndFormType(
                eventId, com.group02.openevent.model.form.EventForm.FormType.CHECKIN);
            List<FormResponseDTO> feedbackResponses = eventFormService.getResponsesByEventIdAndFormType(
                eventId, com.group02.openevent.model.form.EventForm.FormType.FEEDBACK);
            
            // Handle empty lists gracefully
            if (registerResponses == null) registerResponses = new ArrayList<>();
            if (checkinResponses == null) checkinResponses = new ArrayList<>();
            if (feedbackResponses == null) feedbackResponses = new ArrayList<>();
            
            model.addAttribute("registerResponses", registerResponses);
            model.addAttribute("checkinResponses", checkinResponses);
            model.addAttribute("feedbackResponses", feedbackResponses);
            model.addAttribute("eventId", eventId);
            return "host/view-responses";
        } catch (Exception e) {
            log.error("Error loading responses for event ID: {}", eventId, e);
            String encodedMessage = UriUtils.encode(
                    e.getMessage() != null ? e.getMessage() : "Unknown error", 
                    StandardCharsets.UTF_8);
            return "redirect:/music/" + eventId + "?error=no_responses_found&message=" + encodedMessage;
        }
    }

    // API endpoints for AJAX
    @GetMapping("/api/event/{eventId}")
    @ResponseBody
    public ResponseEntity<EventFormDTO> getFormByEventId(@PathVariable Long eventId) {
        try {
            EventFormDTO form = eventFormService.getFormByEventId(eventId);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/submit")
    @ResponseBody
    public ResponseEntity<String> submitResponseApi(@RequestBody SubmitResponseRequest request) {
        try {
            eventFormService.submitResponse(request);
            return ResponseEntity.ok("Response submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit response: " + e.getMessage());
        }
    }
}
