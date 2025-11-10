package com.group02.openevent.controller.api;

import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.dto.response.DashboardStatsResponse;
import com.group02.openevent.dto.response.HostDashboardStatsResponse;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.service.DashboardService;
import com.group02.openevent.service.HostService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {
    
    private final DashboardService dashboardService;
    private final HostService hostService;
    
    @GetMapping("/event/{eventId}/stats")
    public ResponseEntity<DashboardStatsResponse> getEventDashboardStats(@PathVariable Long eventId) {
        log.info("API: Getting dashboard stats for event ID: {}", eventId);
        
        try {
            DashboardStatsResponse stats = dashboardService.getEventDashboardStats(eventId);
            log.info("API: Successfully retrieved dashboard stats for event {}", eventId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("API: Error getting dashboard stats for event {}: {}", eventId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/host/{hostId}/stats")
    public ResponseEntity<HostDashboardStatsResponse> getHostDashboardStats(@PathVariable Long hostId) {
        log.info("API: Getting dashboard stats for host ID: {}", hostId);
        
        try {
            HostDashboardStatsResponse stats = dashboardService.getHostDashboardStats(hostId);
            log.info("API: Successfully retrieved dashboard stats for host {}", hostId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("API: Error getting dashboard stats for host {}: {}", hostId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get host dashboard stats for current logged-in host (uses session)
     */
    @GetMapping("/host/stats")
    public ResponseEntity<HostDashboardStatsResponse> getCurrentHostDashboardStats(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long hostId = hostService.findHostIdByAccountId(accountId);
            HostDashboardStatsResponse stats = dashboardService.getHostDashboardStats(hostId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("API: Error getting dashboard stats for current host: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get recent PAID orders for current logged-in host (uses session)
     * Only returns orders with PAID status
     */
    @GetMapping("/host/orders")
    public ResponseEntity<?> getCurrentHostOrders(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.badRequest().body("User not authenticated");
        }
        
        try {
            Long hostId = hostService.findHostIdByAccountId(accountId);
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            // Only get PAID orders for recent orders display
            Page<Order> ordersPage = dashboardService.getHostPaidOrders(hostId, pageable);
            
            log.info("Found {} PAID orders for host {} (page {}, size {})", 
                    ordersPage.getTotalElements(), hostId, page, size);
            
            // Convert to DTO
            List<OrderDTO> orderDTOs = ordersPage.getContent().stream()
                    .map(this::convertToOrderDTO)
                    .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("orders", orderDTOs);
            response.put("totalElements", ordersPage.getTotalElements());
            response.put("totalPages", ordersPage.getTotalPages());
            response.put("currentPage", ordersPage.getNumber());
            response.put("pageSize", ordersPage.getSize());
            response.put("hasNext", ordersPage.hasNext());
            response.put("hasPrevious", ordersPage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("API: Error getting orders for current host: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    private com.group02.openevent.dto.department.OrderDTO convertToOrderDTO(com.group02.openevent.model.order.Order order) {
        String customerName = null;
        String customerEmail = null;
        if (order.getCustomer() != null) {
            customerName = order.getCustomer().getName();
            customerEmail = order.getCustomer().getEmail();
            if (customerEmail == null && order.getCustomer().getAccount() != null) {
                customerEmail = order.getCustomer().getAccount().getEmail();
            }
        }
        
        return com.group02.openevent.dto.department.OrderDTO.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getCustomerId() : null)
                .eventId(order.getEvent() != null ? order.getEvent().getId() : null)
                .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : null)
                .eventImageUrl(order.getEvent() != null ? order.getEvent().getImageUrl() : null)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .participantName(order.getParticipantName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .ticketTypeName(order.getTicketType() != null ? order.getTicketType().getName() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
