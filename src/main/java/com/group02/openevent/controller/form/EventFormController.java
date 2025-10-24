package com.group02.openevent.controller.form;

import com.group02.openevent.dto.form.*;
import com.group02.openevent.service.EventFormService;
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

    // Host: Create form for event
    @GetMapping("/create/{eventId}")
    public String showCreateForm(@PathVariable Long eventId, Model model) {
        CreateFormRequest request = new CreateFormRequest();
        request.setEventId(eventId);
        model.addAttribute("eventId", eventId);
        model.addAttribute("createFormRequest", request);
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

    // User: View and submit feedback form
    @GetMapping("/feedback/{eventId}")
    public String showFeedbackForm(@PathVariable Long eventId, Model model) {
        try {
            EventFormDTO form = eventFormService.getFormByEventId(eventId);
            model.addAttribute("form", form);
            model.addAttribute("eventId", eventId);
            model.addAttribute("submitResponseRequest", new SubmitResponseRequest());
            return "user/feedback-form";
        } catch (Exception e) {
            log.error("Error loading feedback form for event ID: {}", eventId, e);
            // Instead of redirecting, show empty form page
            model.addAttribute("eventId", eventId);
            model.addAttribute("noFormMessage", "Chưa có form feedback nào cho sự kiện này. Vui lòng liên hệ host để tạo form.");
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
            
            eventFormService.submitResponse(request);
            
            // Use the eventId from the request parameter if available
            if (eventId != null) {
                return "redirect:/music/" + eventId + "?success=feedback_submitted";
            }
            return "redirect:/?success=feedback_submitted";
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
            List<FormResponseDTO> responses = eventFormService.getResponsesByEventId(eventId);
            
            // Handle empty list gracefully
            if (responses == null) {
                responses = new ArrayList<>();
            }
            
            model.addAttribute("responses", responses);
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
