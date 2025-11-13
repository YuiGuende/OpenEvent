package com.group02.openevent.controller.form;

import com.group02.openevent.dto.form.*;
import com.group02.openevent.service.EventFormService;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.AuditLogService;
import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
@Slf4j
public class EventFormController {

    private final EventFormService eventFormService;
    private final EventAttendanceService attendanceService;
    private final ICustomerRepo customerRepo;
    private final IAccountRepo accountRepo;
    private final UserService userService;
    private final AuditLogService auditLogService;

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
                return "redirect:/manage/event/0/create-forms?error=missing_event_id";
            }
            
            eventFormService.createForm(request);
            log.info("Form created successfully for event ID: {}", request.getEventId());
            return "redirect:/manage/event/" + request.getEventId() + "/create-forms?success=form_created";
        } catch (Exception e) {
            log.error("Error creating form for event ID: {}", request.getEventId(), e);
            Long eventId = request.getEventId() != null ? request.getEventId() : 0L;
            String encodedMessage = UriUtils.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/manage/event/" + eventId + "/create-forms?error=form_creation_failed&message=" + encodedMessage;
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
                                 @RequestParam(required = false) Long eventId,
                                 HttpServletRequest httpRequest) {
        log.info("=== FORM SUBMIT REQUEST RECEIVED ===");
        log.info("Form ID from request: {}", request.getFormId());
        log.info("Event ID from param: {}", eventId);
        log.info("Responses count: {}", request.getResponses() != null ? request.getResponses().size() : 0);
        
        try {
            // Validate before submitting
            if (request.getFormId() == null) {
                log.error("Form ID is null!");
                throw new RuntimeException("Form ID is required");
            }
            
            // Lấy customerId từ session thay vì từ form
            Long accountIdFromAttr = (Long) httpRequest.getAttribute("currentUserId");
            Long accountId = accountIdFromAttr;
            
            if (accountId == null) {
                HttpSession session = httpRequest.getSession(false);
                if (session != null) {
                    accountId = (Long) session.getAttribute("USER_ID");
                }
            }
            
            if (accountId == null) {
                throw new RuntimeException("User not logged in");
            }
            
            final Long finalAccountId = accountId;
            
            // Tìm Customer từ accountId
            Customer customer = customerRepo.findByUser_Account_AccountId(finalAccountId).orElse(null);
            if (customer == null) {
                // Tự động tạo Customer nếu chưa có (giống như OrderController)
                Account account = accountRepo.findById(finalAccountId)
                        .orElseThrow(() -> new RuntimeException("Account not found for ID: " + finalAccountId));
                
                // Get or create User
                com.group02.openevent.model.user.User user = userService.getOrCreateUser(account);
                
                customer = new Customer();
                customer.setUser(user);
                customer.setPoints(0);
                customer = customerRepo.save(customer);
            }
            
            // Set customerId vào request
            request.setCustomerId(customer.getCustomerId());
            
            // Validate responses
            if (request.getResponses() == null || request.getResponses().isEmpty()) {
                throw new RuntimeException("No responses provided");
            }
            
            // Log để debug
            log.info("Submitting form {} for customer {}", request.getFormId(), customer.getCustomerId());
            log.info("Number of responses: {}", request.getResponses().size());
            for (int i = 0; i < request.getResponses().size(); i++) {
                SubmitResponseRequest.ResponseItem item = request.getResponses().get(i);
                log.info("Response {}: questionId={}, responseValue={}", i, item.getQuestionId(), item.getResponseValue());
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
            
            log.info("Form submitted successfully. Form type: {}, Event ID: {}", formType, eventId);

            // Resolve current user email (for attendance)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = (auth != null && auth.getName() != null) ? auth.getName().trim().toLowerCase() : null;

            // Branch by form type
            if (formType == com.group02.openevent.model.form.EventForm.FormType.CHECKIN) {
                // Check if already checked in before attempting check-in
                boolean alreadyCheckedIn = attendanceService.isAlreadyCheckedIn(eventId, currentEmail);
                
                if (!alreadyCheckedIn) {
                    // Only check-in if not already checked in
                    AttendanceRequest ar = new AttendanceRequest();
                    ar.setEmail(currentEmail);
                    // Optional: derive name/phone/org from first few responses if needed
                    if (request.getResponses() != null && !request.getResponses().isEmpty()) {
                        // Use first response as fullName if non-empty
                        String first = request.getResponses().get(0).getResponseValue();
                        ar.setFullName(first != null ? first : "");
                    }
                    try {
                        attendanceService.checkIn(eventId, ar);
                        log.info("Check-in successful for event {} with email {}", eventId, currentEmail);
                        // Redirect with check-in success message
                        String redirectUrl = "/events/" + eventId + "?success=checkin_success";
                        log.info("Redirecting to: {}", redirectUrl);
                        return "redirect:" + redirectUrl;
                    } catch (Exception e) {
                        log.warn("Check-in failed (may already be checked in): {}", e.getMessage());
                        // Continue anyway - form response is already saved
                        // Redirect with warning message
                        String redirectUrl = "/events/" + eventId + "?warning=checkin_failed&message=" + UriUtils.encode(e.getMessage(), StandardCharsets.UTF_8);
                        log.info("Redirecting to: {}", redirectUrl);
                        return "redirect:" + redirectUrl;
                    }
                } else {
                    log.info("User {} already checked in for event {}, skipping check-in but form response saved", currentEmail, eventId);
                    // Redirect with message that already checked in
                    String redirectUrl = "/events/" + eventId + "?info=already_checked_in&message=" + UriUtils.encode("Bạn đã check-in rồi. Form response đã được lưu.", StandardCharsets.UTF_8);
                    log.info("Redirecting to: {}", redirectUrl);
                    return "redirect:" + redirectUrl;
                }
            }

            if (formType == com.group02.openevent.model.form.EventForm.FormType.FEEDBACK) {
                if (currentEmail != null) {
                    attendanceService.checkOut(eventId, currentEmail);
                }
                
                // Create audit log for feedback submission
                try {
                    Long userId = customer.getUser() != null ? customer.getUser().getUserId() : null;
                    auditLogService.createAuditLog(
                        "FEEDBACK_SUBMITTED",
                        "FEEDBACK",
                        request.getFormId(),
                        userId,
                        String.format("Feedback submitted for event (Event ID: %d, Form ID: %d) by customer (ID: %d)",
                            eventId != null ? eventId : 0L,
                            request.getFormId(),
                            customer.getCustomerId())
                    );
                    log.debug("Audit log created for FEEDBACK_SUBMITTED: Form ID {}", request.getFormId());
                } catch (Exception e) {
                    log.error("Error creating audit log for FEEDBACK_SUBMITTED: {}", e.getMessage(), e);
                }
                
                // Redirect to event detail page after successful feedback submission
                String redirectUrl = "/events/" + eventId + "?success=feedback_submitted";
                log.info("Redirecting to: {}", redirectUrl);
                return "redirect:" + redirectUrl;
            }

            // After submit, redirect về trang event detail (router chung sẽ tự động route đúng event type)
            if (eventId != null) {
                String redirectUrl = "/events/" + eventId + "?success=form_submitted";
                log.info("Redirecting to: {}", redirectUrl);
                return "redirect:" + redirectUrl;
            }
            log.info("Redirecting to home page");
            return "redirect:/?success=form_submitted";
        } catch (Exception e) {
            log.error("=== ERROR SUBMITTING FORM ===");
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace: ", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            String encodedMessage = UriUtils.encode(errorMsg, StandardCharsets.UTF_8);
            
            // Try to get eventId from request if not in param
            Long finalEventId = eventId;
            if (finalEventId == null && request.getFormId() != null) {
                try {
                    EventFormDTO formDto = eventFormService.getFormById(request.getFormId());
                    finalEventId = formDto.getEventId();
                } catch (Exception ex) {
                    log.error("Could not get eventId from form: {}", ex.getMessage());
                }
            }
            
            if (finalEventId != null) {
                log.info("Redirecting to error page: /events/{}", finalEventId);
                return "redirect:/events/" + finalEventId + "?error=submission_failed&message=" + encodedMessage;
            }
            log.info("Redirecting to home page with error");
            return "redirect:/?error=submission_failed&message=" + encodedMessage;
        }
    }

    // Host: View responses by eventId (all form types)
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
            
            // Group responses by customerId (group all responses of same customer together)
            Map<Long, List<FormResponseDTO>> groupedRegisterResponses = registerResponses.stream()
                .collect(Collectors.groupingBy(
                    FormResponseDTO::getCustomerId,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            Map<Long, List<FormResponseDTO>> groupedCheckinResponses = checkinResponses.stream()
                .collect(Collectors.groupingBy(
                    FormResponseDTO::getCustomerId,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            Map<Long, List<FormResponseDTO>> groupedFeedbackResponses = feedbackResponses.stream()
                .collect(Collectors.groupingBy(
                    FormResponseDTO::getCustomerId,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            
            model.addAttribute("groupedRegisterResponses", groupedRegisterResponses);
            model.addAttribute("groupedCheckinResponses", groupedCheckinResponses);
            model.addAttribute("groupedFeedbackResponses", groupedFeedbackResponses);
            // Keep old format for backward compatibility (but won't be used in template)
            model.addAttribute("registerResponses", new ArrayList<FormResponseDTO>());
            model.addAttribute("checkinResponses", new ArrayList<FormResponseDTO>());
            model.addAttribute("feedbackResponses", new ArrayList<FormResponseDTO>());
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
    
    // Host: View responses by formId (single form)
    @GetMapping("/form/{formId}/responses")
    public String viewFormResponses(@PathVariable Long formId, @RequestParam(required = false) Long eventId, Model model) {
        try {
            // Get form details
            EventFormDTO form = eventFormService.getFormById(formId);
            if (form == null) {
                throw new RuntimeException("Form not found");
            }
            
            // Use eventId from form if not provided
            Long resolvedEventId = eventId != null ? eventId : form.getEventId();
            
            // Get responses for this specific form
            List<FormResponseDTO> responses = eventFormService.getResponsesByFormId(formId);
            if (responses == null) responses = new ArrayList<>();
            
            // Group responses by customerId (group all responses of same customer together)
            Map<Long, List<FormResponseDTO>> groupedRegisterResponses = new LinkedHashMap<>();
            Map<Long, List<FormResponseDTO>> groupedCheckinResponses = new LinkedHashMap<>();
            Map<Long, List<FormResponseDTO>> groupedFeedbackResponses = new LinkedHashMap<>();
            
            Map<Long, List<FormResponseDTO>> groupedResponses = responses.stream()
                .collect(Collectors.groupingBy(
                    FormResponseDTO::getCustomerId,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            
            switch (form.getFormType()) {
                case REGISTER -> groupedRegisterResponses = groupedResponses;
                case CHECKIN -> groupedCheckinResponses = groupedResponses;
                case FEEDBACK -> groupedFeedbackResponses = groupedResponses;
            }
            
            model.addAttribute("form", form);
            model.addAttribute("groupedRegisterResponses", groupedRegisterResponses);
            model.addAttribute("groupedCheckinResponses", groupedCheckinResponses);
            model.addAttribute("groupedFeedbackResponses", groupedFeedbackResponses);
            // Keep old format for backward compatibility (but won't be used in template)
            model.addAttribute("registerResponses", new ArrayList<FormResponseDTO>());
            model.addAttribute("checkinResponses", new ArrayList<FormResponseDTO>());
            model.addAttribute("feedbackResponses", new ArrayList<FormResponseDTO>());
            model.addAttribute("eventId", resolvedEventId);
            model.addAttribute("formId", formId);
            return "host/view-responses";
        } catch (Exception e) {
            log.error("Error loading responses for form ID: {}", formId, e);
            String encodedMessage = UriUtils.encode(
                    e.getMessage() != null ? e.getMessage() : "Unknown error", 
                    StandardCharsets.UTF_8);
            if (eventId != null) {
                return "redirect:/manage/event/" + eventId + "/create-forms?error=responses_not_found&message=" + encodedMessage;
            }
            return "redirect:/?error=responses_not_found&message=" + encodedMessage;
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
    
    // Host: View form details
    @GetMapping("/view/{formId}")
    public String viewForm(@PathVariable Long formId, Model model) {
        try {
            EventFormDTO form = eventFormService.getFormById(formId);
            model.addAttribute("form", form);
            model.addAttribute("eventId", form.getEventId());
            return "host/view-form";
        } catch (Exception e) {
            log.error("Error loading form ID: {}", formId, e);
            return "redirect:/?error=form_not_found";
        }
    }
    
    // API: Get form statistics
    @GetMapping("/{formId}/stats")
    @ResponseBody
    public ResponseEntity<com.group02.openevent.dto.form.FormStatsDTO> getFormStats(@PathVariable Long formId) {
        try {
            com.group02.openevent.dto.form.FormStatsDTO stats = eventFormService.getFormStatistics(formId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting form statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Host: View form statistics page (full page)
    @GetMapping("/stats/{formId}")
    public String viewFormStats(@PathVariable Long formId, Model model) {
        try {
            com.group02.openevent.dto.form.EventFormDTO form = eventFormService.getFormById(formId);
            model.addAttribute("formId", formId);
            model.addAttribute("formTitle", form.getFormTitle());
            model.addAttribute("formType", form.getFormType());
            model.addAttribute("eventId", form.getEventId());
            model.addAttribute("content", "fragments/statis-form :: content");
            return "host/manager-event";
        } catch (Exception e) {
            log.error("Error loading form statistics page: {}", e.getMessage(), e);
            return "redirect:/?error=form_not_found";
        }
    }
    
    // Host: Delete form
    @PostMapping("/{formId}/delete")
    public String deleteForm(@PathVariable Long formId, @RequestParam(required = false) Long eventId) {
        try {
            // Get eventId from form before deleting
            if (eventId == null) {
                EventFormDTO form = eventFormService.getFormById(formId);
                eventId = form.getEventId();
            }
            eventFormService.deleteForm(formId);
            log.info("Form {} deleted successfully", formId);
            // Redirect back to create-forms page
            return "redirect:/manage/event/" + eventId + "/create-forms?success=form_deleted";
        } catch (Exception e) {
            log.error("Error deleting form ID: {}", formId, e);
            if (eventId != null) {
                return "redirect:/manage/event/" + eventId + "/create-forms?error=delete_failed";
            }
            return "redirect:/?error=delete_failed";
        }
    }
}
