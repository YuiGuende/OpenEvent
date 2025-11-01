package com.group02.openevent.controller.request;

import com.group02.openevent.annotation.RequireEventHost;
import com.group02.openevent.annotation.RequireRequestReceiver; // THÊM MỚI
import com.group02.openevent.controller.payout.PayoutController;
import com.group02.openevent.dto.notification.RequestFormDTO;
import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.CreateRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.RequestDTO;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import com.group02.openevent.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // THÊM MỚI
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.NoSuchElementException; // THÊM MỚI (hoặc khác tùy theo logic aspect)

@Controller
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {
    private static final Logger logger = LoggerFactory.getLogger(RequestController.class);
    private final RequestService requestService;

    /**
     * Create a new request (e.g., event approval request)
     */
    @PostMapping(consumes = {"application/json"})
    @ResponseBody
    public ResponseEntity<RequestDTO> createRequestJson(@RequestBody CreateRequestDTO createRequestDTO) {
        try {
            RequestDTO createdRequest = requestService.createRequest(createRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/form")
    public String showRequestForm(
            @RequestParam Long eventId,
            Model model) {
        RequestFormDTO formData = new RequestFormDTO();
        try{
            formData =requestService.getRequestFormData(eventId);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        model.addAttribute("formData", formData);
        return "fragments/request-form";
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @ResponseBody
    @RequireEventHost(eventIdParamName = "eventId", userIdParamName = "senderId")
    public ResponseEntity<RequestDTO> createRequestWithFile(
            @SessionAttribute("ACCOUNT_ID") Long senderId,
            // @RequestParam("senderId") Long senderId,
            @RequestParam("receiverId") Long receiverId,
            @RequestParam("type") RequestType type,
            @RequestParam(value = "eventId", required = false) Long eventId,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            CreateRequestDTO createRequestDTO = CreateRequestDTO.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .type(type)
                    .eventId(eventId)
                    .orderId(orderId)
                    .message(message)
                    .targetUrl(targetUrl)
                    .build();
            System.out.println("received request"+createRequestDTO);
            RequestDTO createdRequest = requestService.createRequestWithFile(createRequestDTO, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Approve a request
     */
    @PutMapping("/{requestId}/approve")
    @ResponseBody
    @RequireRequestReceiver // <-- THÊM MỚI
    public ResponseEntity<RequestDTO> approveRequest(
            @PathVariable Long requestId,
            @RequestBody ApproveRequestDTO approveRequestDTO,
            @SessionAttribute("ACCOUNT_ID") Long currentUserId) { // <-- THÊM MỚI
        try {
            RequestDTO approvedRequest = requestService.approveRequest(requestId, approveRequestDTO);
            return ResponseEntity.ok(approvedRequest);
        } catch (AccessDeniedException e) { // <-- THÊM MỚI
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Reject a request
     */
    @PutMapping("/{requestId}/reject")
    @ResponseBody
    @RequireRequestReceiver // <-- THÊM MỚI
    public ResponseEntity<RequestDTO> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody ApproveRequestDTO approveRequestDTO,
            @SessionAttribute("ACCOUNT_ID") Long currentUserId) { // <-- THÊM MỚI
        try {
            RequestDTO rejectedRequest = requestService.rejectRequest(requestId, approveRequestDTO);
            return ResponseEntity.ok(rejectedRequest);
        } catch (AccessDeniedException e) { // <-- THÊM MỚI
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get request by ID
     */
    @GetMapping("/{requestId}")
    @ResponseBody
    public ResponseEntity<RequestDTO> getRequestById(@PathVariable Long requestId) {
        return requestService.getRequestById(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all requests with optional filters
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<RequestDTO>> getAllRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType type) {
        List<RequestDTO> requests;
        if (status != null) {
            requests = requestService.getRequestsByStatus(status);
        } else if (type != null) {
            requests = requestService.getRequestsByType(type);
        } else {
            requests = requestService.getAllRequests();
        }
        return ResponseEntity.ok(requests);
    }

    /**
     * Get requests with pagination
     */
    @GetMapping("/paginated")
    @ResponseBody
    public ResponseEntity<Page<RequestDTO>> getRequestsPaginated(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RequestDTO> requests = requestService.listRequests(status, type, pageable);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get requests by sender ID
     */
    @GetMapping("/sender/{senderId}")
    @ResponseBody
    public ResponseEntity<List<RequestDTO>> getRequestsBySender(@PathVariable Long senderId) {
        List<RequestDTO> requests = requestService.getRequestsBySenderId(senderId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get requests by receiver ID
     */
    @GetMapping("/receiver/{receiverId}")
    @ResponseBody
    public ResponseEntity<List<RequestDTO>> getRequestsByReceiver(@PathVariable Long receiverId) {
        List<RequestDTO> requests = requestService.getRequestsByReceiverId(receiverId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get requests by event ID
     */
    @GetMapping("/event/{eventId}")
    @ResponseBody
    public ResponseEntity<List<RequestDTO>> getRequestsByEvent(@PathVariable Long eventId) {
        List<RequestDTO> requests = requestService.getRequestsByEventId(eventId);
        return ResponseEntity.ok(requests);
    }
}