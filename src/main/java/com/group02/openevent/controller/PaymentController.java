package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.payment.PaymentResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final IAccountRepo accountRepo;
    private final IUserRepo userRepo;
    private final PayOS payOS;

    public PaymentController(PaymentService paymentService, OrderService orderService,
                           IAccountRepo accountRepo, IUserRepo userRepo, PayOS payOS) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
        this.payOS = payOS;
    }

    /**
     * Tạo order và payment link sử dụng PayOS SDK
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody CreateOrderRequest request,
                                                           HttpSession session) {
        try {
            Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
            if (accountId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not logged in"
                ));
            }

            // Get user
            Account account = accountRepo.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Account not found"
                ));
            }

            User user = userRepo.findByAccount(account).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            // Create order
            var order = orderService.createOrder(request, user);

            // Create payment link using PayOS SDK
            String returnUrl = "http://localhost:8080/payment/success?orderCode=" + order.getOrderCode();
            String cancelUrl = "http://localhost:8080/payment/cancel?orderCode=" + order.getOrderCode();
            
            // Sử dụng PayOS SDK để tạo payment link
            PaymentData paymentData = PaymentData.builder()
                .orderCode((long) Math.abs(order.getOrderCode().hashCode()))
                .amount(order.getAmount().intValue())
                .description(order.getDescription())
                .items(null)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();
            
            var paymentLink = payOS.createPaymentLink(paymentData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderCode", order.getOrderCode());
            response.put("paymentUrl", paymentLink.getCheckoutUrl());
            response.put("checkoutUrl", paymentLink.getCheckoutUrl());
            response.put("qrCode", paymentLink.getQrCode());
            response.put("amount", paymentLink.getAmount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Webhook endpoint cho PayOS sử dụng PayOS SDK
     */
    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Webhook webhookData) {
        try {
            // Verify webhook using PayOS SDK
            WebhookData data = payOS.verifyPaymentWebhookData(webhookData);
            System.out.println("Webhook verified: " + data);
            
            // Process payment result
            PaymentResult result = paymentService.handlePaymentWebhookFromPayOS(data);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", 0);
            response.put("message", "Webhook delivered");
            response.put("success", result.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thông tin payment theo order code
     */
    @GetMapping("/status/{orderCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String orderCode) {
        try {
            var orderOpt = orderService.getOrderByCode(orderCode);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }

            var order = orderOpt.get();
            var paymentOpt = paymentService.getPaymentByOrder(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderCode", order.getOrderCode());
            response.put("orderStatus", order.getStatus().name());
            
            if (paymentOpt.isPresent()) {
                var payment = paymentOpt.get();
                response.put("paymentStatus", payment.getStatus().name());
                response.put("checkoutUrl", payment.getCheckoutUrl());
                response.put("qrCode", payment.getQrCode());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Lấy lịch sử payments của user
     */
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentHistory(HttpSession session) {
        try {
            Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
            if (accountId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not logged in"
                ));
            }

            var payments = paymentService.getPaymentsByUserId(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("payments", payments);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Hủy payment
     */
    @PostMapping("/cancel/{orderCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelPayment(@PathVariable String orderCode) {
        try {
            var orderOpt = orderService.getOrderByCode(orderCode);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }

            var order = orderOpt.get();
            var paymentOpt = paymentService.getPaymentByOrder(order);
            
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Payment not found"
                ));
            }

            var payment = paymentOpt.get();
            boolean cancelled = paymentService.cancelPayment(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", cancelled);
            response.put("message", cancelled ? "Payment cancelled successfully" : "Cannot cancel payment");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
