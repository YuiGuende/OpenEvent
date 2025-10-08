package com.group02.openevent.controller.department;

import com.group02.openevent.dto.*;
import com.group02.openevent.dto.department.ArticleDTO;
import com.group02.openevent.dto.department.DepartmentStatsDTO;
import com.group02.openevent.dto.department.FeaturedEventDTO;
import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.department.ArticleStatus;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private EventService eventService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DepartmentService departmentService;

    private Long getDepartmentAccountId(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            throw new RuntimeException("User not logged in");
        }
        return accountId;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, HttpServletRequest request) {
        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        DepartmentStatsDTO stats = departmentService.getDepartmentStats(department.getAccountId());

        model.addAttribute("department", department);
        model.addAttribute("stats", stats);
        model.addAttribute("totalEvents", stats.getTotalEvents());
        model.addAttribute("pendingRequests", stats.getPendingRequests());
        model.addAttribute("ongoingEvents", stats.getOngoingEvents());
        model.addAttribute("totalParticipants", stats.getTotalParticipants());
        model.addAttribute("currentUri", request.getRequestURI());//ko hieu lam nhe
        return "department/dashboard";
    }

    @GetMapping("/events")
    public String listEvents(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) EventStatus status,
            Model model) {

        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Event> events = eventService.getEventsByDepartment(department.getAccountId(), eventType, status, pageable);

        model.addAttribute("events", events);
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("eventStatuses", EventStatus.values());
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "department/events";
    }

    @PostMapping("/events/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateEventStatus(@PathVariable Long id, @RequestParam EventStatus status) {
        try {
            Event updatedEvent = eventService.updateEventStatus(id, status);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Event status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public String requests(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RequestStatus status,
            Model model) {

        Long accountId = getDepartmentAccountId(session);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Request> requests = requestService.getRequestsByReceiver(accountId, status, pageable);

        model.addAttribute("requests", requests);
        model.addAttribute("requestStatuses", RequestStatus.values());
        model.addAttribute("selectedStatus", status);

        return "department/requests";
    }

    @PostMapping("/requests/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveRequest(
            HttpSession session,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            Long accountId = getDepartmentAccountId(session);
            String responseMessage = payload.get("responseMessage");

            Request approvedRequest = requestService.approveRequest(id, responseMessage);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Request approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/requests/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectRequest(
            HttpSession session,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            Long accountId = getDepartmentAccountId(session);
            String responseMessage = payload.get("responseMessage");

            Request rejectedRequest = requestService.rejectRequest(id, responseMessage);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Request rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/articles")
    public String articles(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ArticleStatus status,
            Model model) {

        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ArticleDTO> articles = articleService.getArticlesByDepartment(department.getAccountId(), status, pageable);

        model.addAttribute("articles", articles);
        model.addAttribute("articleStatuses", ArticleStatus.values());
        model.addAttribute("selectedStatus", status);

        return "department/articles";
    }

    @GetMapping("/articles/create")
    public String createArticleForm(Model model) {
        model.addAttribute("article", new ArticleDTO());
        return "department/article-form";
    }

    @GetMapping("/articles/{id}/edit")
    public String editArticleForm(@PathVariable Long id, Model model) {
        ArticleDTO article = articleService.getArticleById(id);
        model.addAttribute("article", article);
        return "department/article-form";
    }

    @PostMapping("/articles/save")
    public String saveArticle(
            HttpSession session,
            @ModelAttribute ArticleDTO articleDTO,
            @RequestParam(required = false) MultipartFile imageFile) {
        try {
            Long accountId = getDepartmentAccountId(session);
            Department department = departmentService.getDepartmentByAccountId(accountId);

            articleDTO.setDepartmentId(department.getAccountId());

            if (imageFile != null && !imageFile.isEmpty()) {
                articleService.saveArticleWithImage(articleDTO, imageFile);
            } else {
                articleService.saveArticle(articleDTO);
            }

            return "redirect:/department/articles";
        } catch (Exception e) {
            return "redirect:/department/articles?error=" + e.getMessage();
        }
    }

    @PostMapping("/articles/{id}/delete")
    public String deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return "redirect:/department/articles";
    }

    @PostMapping("/articles/{id}/publish")
    @ResponseBody
    public ResponseEntity<?> publishArticle(@PathVariable Long id) {
        try {
            articleService.publishArticle(id);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Article published successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/stats/events-by-month")
    @ResponseBody
    public ResponseEntity<?> getEventsByMonth(HttpSession session) {
        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Map<String, Object> data = departmentService.getEventsByMonth(department.getAccountId());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/api/stats/events-by-type")
    @ResponseBody
    public ResponseEntity<?> getEventsByType(HttpSession session) {
        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Map<String, Object> data = departmentService.getEventsByType(department.getAccountId());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/api/stats/participants-trend")
    @ResponseBody
    public ResponseEntity<?> getParticipantsTrend(HttpSession session) {
        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Map<String, Object> data = departmentService.getParticipantsTrend(department.getAccountId());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/orders")
    public String orders(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            Model model) {

        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderDTO> orders = departmentService.getOrdersByDepartment(department.getAccountId(), status, pageable);

        model.addAttribute("orders", orders);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);

        return "department/orders";
    }

    @GetMapping("/api/stats/revenue-trend")
    @ResponseBody
    public ResponseEntity<?> getRevenueTrend(HttpSession session) {
        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        Map<String, Object> data = departmentService.getRevenueTrend(department.getAccountId());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/api/stats/featured-events")
    @ResponseBody
    public ResponseEntity<?> getFeaturedEvents(HttpSession session) {
        Long accountId = getDepartmentAccountId(session);
        Department department = departmentService.getDepartmentByAccountId(accountId);

        List<FeaturedEventDTO> featuredEvents = departmentService.getFeaturedEvents(department.getAccountId(), 5);
        return ResponseEntity.ok(featuredEvents);
    }
}
