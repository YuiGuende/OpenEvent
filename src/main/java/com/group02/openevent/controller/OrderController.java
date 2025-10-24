package com.group02.openevent.controller;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.VoucherService;
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
    private final ICustomerRepo customerRepo;
    private final VoucherService voucherService;

    public OrderController(OrderService orderService, ICustomerRepo customerRepo, VoucherService voucherService) {
        this.orderService = orderService;
        this.customerRepo = customerRepo;
        this.voucherService = voucherService;
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

            Customer customer = customerRepo.findByAccount_AccountId(accountId).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Customer not found"));
            }

            // Check if customer already registered (paid) for this event
            if (orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), request.getEventId())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "You have already registered for this event"
                ));
            }

            // Check if customer has pending (unpaid) order for this event
            Optional<Order> pendingOrder = orderService.getPendingOrderForEvent(customer.getCustomerId(), request.getEventId());
            if (pendingOrder.isPresent()) {
                Order existingOrder = pendingOrder.get();

                // Cancel the old pending order
                orderService.cancelOrder(existingOrder.getOrderId());

                // Log the cancellation
            }

            Order order = orderService.createOrderWithTicketTypes(request, customer);

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

        Customer customer = customerRepo.findByAccount_AccountId(accountId).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Customer not found"));
        }

        List<Order> orders = orderService.getOrdersByCustomer(customer);
        return ResponseEntity.ok(Map.of("success", true, "orders", orders));
    }


    /**
     * Kiểm tra xem user đã đăng ký event này chưa
     */
    @GetMapping("/check-registration/{eventId}")
    public ResponseEntity<?> checkRegistration(@PathVariable Long eventId, HttpServletRequest httpRequest) {
        try {
            Long accountId = (Long) httpRequest.getAttribute("currentUserId");
            if (accountId == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
            }

            Customer customer = customerRepo.findByAccount_AccountId(accountId).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Customer not found"));
            }

            boolean isRegistered = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), eventId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isRegistered", isRegistered
            ));
        } catch (Exception e) { // <-- THÊM CATCH Ở ĐÂY
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() // Sẽ trả về "DB error" như test mong đợi
            ));
        }
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

    /**
     * Áp dụng voucher vào order
     */
    @PostMapping("/{orderId}/apply-voucher")
    public ResponseEntity<?> applyVoucher(@PathVariable Long orderId, @RequestParam String voucherCode, HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            Optional<Order> orderOpt = orderService.getById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Order not found"));
            }

            Order order = orderOpt.get();

            // Kiểm tra quyền sở hữu order
            if (!order.getCustomer().getAccount().getAccountId().equals(accountId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }

            // Áp dụng voucher
            voucherService.applyVoucherToOrder(voucherCode, order);
            orderService.save(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Voucher applied successfully",
                    "discountAmount", order.getVoucherDiscountAmount(),
                    "newTotalAmount", order.getTotalAmount()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách voucher khả dụng
     */
    @GetMapping("/available-vouchers")
    public ResponseEntity<?> getAvailableVouchers(HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            var vouchers = voucherService.getAvailableVouchers();
            return ResponseEntity.ok(Map.of("success", true, "vouchers", vouchers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Kiểm tra voucher có khả dụng không
     */
    @GetMapping("/check-voucher")
    public ResponseEntity<?> checkVoucher(@RequestParam String voucherCode, HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            boolean isAvailable = voucherService.isVoucherAvailable(voucherCode);
            if (isAvailable) {
                var voucher = voucherService.getVoucherByCode(voucherCode);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "available", true,
                        "discountAmount", voucher.get().getDiscountAmount(),
                        "description", voucher.get().getDescription()
                ));
            } else {
                return ResponseEntity.ok(Map.of("success", true, "available", false));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}


