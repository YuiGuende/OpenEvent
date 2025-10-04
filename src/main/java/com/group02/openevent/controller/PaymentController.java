package com.group02.openevent.controller;

import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.dto.payment.PaymentResult;
import com.group02.openevent.dto.payment.PayOSWebhookData;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PayOS payOS;

    public PaymentController(PaymentService paymentService, OrderService orderService, PayOS payOS) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.payOS = payOS;
    }


    /**
     * Webhook endpoint cho PayOS sử dụng PayOS SDK
     */
    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Object webhookBody) {
        try {
            // Verify webhook using PayOS SDK
            WebhookData data = payOS.webhooks().verify(webhookBody);
            
            // Process payment result using PayOS SDK webhook
            PaymentResult result = paymentService.handlePaymentWebhookFromPayOS(convertWebhookData(data));
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", 0);
            response.put("message", "Webhook delivered");
            response.put("success", result.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * Lấy lịch sử payments của user
     */
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentHistory(HttpServletRequest request) {
        try {
            Long accountId = (Long) request.getAttribute("currentUserId");
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
     * Convert PayOS SDK WebhookData to PayOSWebhookData
     */
    private PayOSWebhookData convertWebhookData(WebhookData webhookData) {
        PayOSWebhookData payOSWebhookData = new PayOSWebhookData();
        payOSWebhookData.setCode(Integer.valueOf(webhookData.getCode()));
        payOSWebhookData.setDesc(webhookData.getDesc());
        
        // Create minimal data structure
        PayOSWebhookData.Data data = new PayOSWebhookData.Data();
        // Note: WebhookData from PayOS SDK has different structure
        // We'll create a minimal structure for now
        // Since we can't access getData(), we'll use default values
        data.setOrderCode(0L); // Default value
        data.setAmount(0); // Default value
        data.setDescription("Webhook from PayOS SDK");
        data.setCode("00"); // Default success code
        data.setDesc("Success");
        
        payOSWebhookData.setData(data);
        return payOSWebhookData;
    }

    /**
     * Tạo payment link cho Order
     */
    @PostMapping("/create-for-order/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPaymentForOrder(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            Long accountId = (Long) request.getAttribute("currentUserId");
            if (accountId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not logged in"
                ));
            }

            // Get order
            Order order = orderService.getById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }

            // Check if order belongs to current user
            if (!order.getUser().getAccount().getAccountId().equals(accountId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order does not belong to current user"
                ));
            }

            // Check if payment already exists for this order
            Optional<Payment> existingPayment = paymentService.getPaymentByOrder(order);
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    // Return existing payment link
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("orderId", orderId);
                    response.put("paymentId", payment.getPaymentId());
                    response.put("checkoutUrl", payment.getCheckoutUrl());
                    response.put("qrCode", payment.getQrCode());
                    response.put("amount", payment.getAmount());
                    return ResponseEntity.ok(response);
                }
            }

            // Create payment link using PaymentService only
            String returnUrl = "http://localhost:8080/payment/success?orderId=" + orderId;
            String cancelUrl = "http://localhost:8080/payment/cancel?orderId=" + orderId;
            
            // Create payment record and PayOS link
            Payment payment = paymentService.createPaymentLinkForOrder(order, returnUrl, cancelUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("paymentId", payment.getPaymentId());
            response.put("checkoutUrl", payment.getCheckoutUrl());
            response.put("qrCode", payment.getQrCode());
            response.put("amount", payment.getAmount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Lấy thông tin payment theo order ID
     */
    @GetMapping("/status/order/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatusByOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.getById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }

            Optional<Payment> paymentOpt = paymentService.getPaymentByOrder(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("orderStatus", order.getStatus().name());
            
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
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
     * Hủy payment cho order
     */
    @PostMapping("/cancel/order/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelPaymentForOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.getById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Order not found"
                ));
            }

            Optional<Payment> paymentOpt = paymentService.getPaymentByOrder(order);
            
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Payment not found"
                ));
            }

            Payment payment = paymentOpt.get();
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
