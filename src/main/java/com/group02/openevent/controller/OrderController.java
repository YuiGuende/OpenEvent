package com.group02.openevent.controller;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final IUserRepo userRepo;
    private final IEventRepo eventRepo;
    private final ITicketTypeRepo ticketTypeRepo;

    public OrderController(OrderService orderService, IUserRepo userRepo, IEventRepo eventRepo, ITicketTypeRepo ticketTypeRepo) {
        this.orderService = orderService;
        this.userRepo = userRepo;
        this.eventRepo = eventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody CreateOrderRequest request) {
        Order created = orderService.createOrder(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable("id") Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<Order> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.list(pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Tạo order mới với TicketType
     */
    @PostMapping("/create-with-ticket-types")
    public ResponseEntity<?> createWithTicketTypes(@Valid @RequestBody CreateOrderWithTicketTypeRequest request, HttpServletRequest httpRequest) {
        try {
            Long accountId = (Long) httpRequest.getAttribute("currentUserId");
            if (accountId == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
            }

            User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            }

            // Check if user already registered (paid) for this event
            if (orderService.hasUserRegisteredForEvent(user.getUserId(), request.getEventId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "You have already registered for this event"
                ));
            }

            // Check if user has pending (unpaid) order for this event
            Optional<Order> pendingOrder = orderService.getPendingOrderForEvent(user.getUserId(), request.getEventId());
            if (pendingOrder.isPresent()) {
                Order existingOrder = pendingOrder.get();
                
                // Cancel the old pending order
                orderService.cancelOrder(existingOrder.getOrderId());
                
                // Log the cancellation
                System.out.println("Cancelled old pending order: " + existingOrder.getOrderId() + " for user: " + user.getUserId());
            }

            Order order = orderService.createOrderWithTicketTypes(request, user);
            
            // Return simplified response to avoid JSON serialization issues
            Map<String, Object> response = Map.of(
                "success", true, 
                "orderId", order.getOrderId(),
                "totalAmount", order.getTotalAmount(),
                "status", order.getStatus().toString()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "Order creation failed: " + e.getClass().getSimpleName();
            }
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false, 
                "message", errorMessage
            ));
        }
    }

    /**
     * Lấy tất cả orders của user hiện tại
     */
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        List<Order> orders = orderService.getOrdersByUser(user);
        return ResponseEntity.ok(Map.of("success", true, "orders", orders));
    }


    /**
     * Kiểm tra xem user đã đăng ký event này chưa
     */
    @GetMapping("/check-registration/{eventId}")
    public ResponseEntity<?> checkRegistration(@PathVariable Long eventId, HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        boolean isRegistered = orderService.hasUserRegisteredForEvent(user.getUserId(), eventId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "isRegistered", isRegistered
        ));
    }

    /**
     * Hủy order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            orderService.cancelOrder(orderId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Order cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Xác nhận order (sau khi thanh toán thành công)
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            orderService.confirmOrder(orderId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Order confirmed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}


