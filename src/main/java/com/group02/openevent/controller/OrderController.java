package com.group02.openevent.controller;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.UserService;
import com.group02.openevent.service.VoucherService;
import com.group02.openevent.model.order.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ICustomerRepo customerRepo;
    private final IAccountRepo accountRepo;
    private final VoucherService voucherService;
    private final UserService userService;
    private final EventService eventService;
    private final EventAttendanceService attendanceService;

    public OrderController(OrderService orderService, ICustomerRepo customerRepo, IAccountRepo accountRepo, 
                          VoucherService voucherService, UserService userService, EventService eventService,
                          EventAttendanceService attendanceService) {
        this.orderService = orderService;
        this.customerRepo = customerRepo;
        this.accountRepo = accountRepo;
        this.voucherService = voucherService;
        this.userService = userService;
        this.eventService = eventService;
        this.attendanceService = attendanceService;
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
    public ResponseEntity<?> createWithTicketTypes(@Valid @RequestBody CreateOrderWithTicketTypeRequest request, HttpServletRequest httpRequest, HttpSession httpSession) {
        try {
            Customer customer = userService.getCurrentUser(httpSession).getCustomer();
            
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
            }

            // Kiểm tra event có miễn phí không
            boolean isFree = eventService.isFreeEvent(request.getEventId());
            
            Order order = orderService.createOrderWithTicketTypes(request, customer);
            
            if (isFree) {
                // EVENT MIỄN PHÍ: Set status = PAID ngay và tạo EventAttendance
                log.info("Event {} is free, setting order {} to PAID immediately", request.getEventId(), order.getOrderId());
                order.setStatus(OrderStatus.PAID);
                order = orderService.save(order);
                
                // Tự động tạo EventAttendance
                try {
                    attendanceService.createAttendanceFromOrder(order);
                    log.info("EventAttendance created successfully for free event order {}", order.getOrderId());
                } catch (Exception e) {
                    log.error("Error creating EventAttendance for free event order {}: {}", order.getOrderId(), e.getMessage(), e);
                    // Không fail request nếu tạo attendance thất bại
                }
                
                // Return response cho free event
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "orderId", order.getOrderId(),
                        "totalAmount", 0,
                        "status", "PAID",
                        "isFree", true,
                        "message", "Đăng ký thành công! Event này miễn phí. Bạn có thể check-in khi event diễn ra."
                ));
            } else {
                // EVENT CÓ PHÍ: Flow bình thường (tạo payment link)
                Map<String, Object> response = Map.of(
                        "success", true,
                        "orderId", order.getOrderId(),
                        "totalAmount", order.getTotalAmount(),
                        "status", order.getStatus().toString(),
                        "isFree", false
                );
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "Order creation failed: " + e.getClass().getSimpleName();
            }
            
            log.error("Error creating order: {}", errorMessage, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", errorMessage
            ));
        }
    }


    /**
     * Kiểm tra xem user đã đăng ký event này chưa
     */
    @GetMapping("/check-registration/{eventId}")
    public ResponseEntity<?> checkRegistration(@PathVariable Long eventId, HttpServletRequest httpRequest,HttpSession httpSession) {
        try {
            Customer customer = userService.getCurrentUser(httpSession).getCustomer();

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
            if (!order.getCustomer().getUser().getAccount().getAccountId().equals(accountId)) {
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


