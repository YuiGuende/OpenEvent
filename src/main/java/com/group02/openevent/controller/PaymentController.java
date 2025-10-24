package com.group02.openevent.controller;

import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
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
     * Webhook endpoint cho PayOS - Xử lý trực tiếp không qua SDK
     */
    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody(required = false) Map<String, Object> webhookBody) {
        System.out.println("=== WEBHOOK RECEIVED ===");
        System.out.println("Webhook body: " + webhookBody);
        
        // Handle PayOS test webhook (empty or minimal body)
        if (webhookBody == null || webhookBody.isEmpty()) {
            System.out.println("Empty webhook body - PayOS test request");
            Map<String, Object> response = new HashMap<>();
            response.put("error", 0);
            response.put("message", "ok");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }
        
        try {
            // Extract data from webhook body directly
            String code = (String) webhookBody.get("code");
            String desc = (String) webhookBody.get("desc");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) webhookBody.get("data");
            
            if (dataMap == null) {
                System.out.println("No data in webhook");
                Map<String, Object> response = new HashMap<>();
                response.put("error", 0);
                response.put("message", "ok");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }
            
            System.out.println("Webhook code: " + code);
            System.out.println("Webhook desc: " + desc);
            System.out.println("Payment data: " + dataMap);
            
            // Extract payment information
            String paymentLinkId = (String) dataMap.get("paymentLinkId");
            Object orderCodeObj = dataMap.get("orderCode");
            Long orderCode = orderCodeObj instanceof Integer ? ((Integer) orderCodeObj).longValue() : (Long) orderCodeObj;
            Object amountObj = dataMap.get("amount");
            Integer amount = amountObj instanceof Integer ? (Integer) amountObj : ((Number) amountObj).intValue();
            String description = (String) dataMap.get("description");
            
            System.out.println("Payment Link ID: " + paymentLinkId);
            System.out.println("Order Code: " + orderCode);
            System.out.println("Amount: " + amount);
            System.out.println("Description: " + description);
            
            // Find payment directly by orderId from description or by orderCode
            Long orderId = extractOrderIdFromDescription(description);
            Optional<Payment> paymentOpt = Optional.empty();
            
            if (orderId != null) {
                System.out.println("Extracted order ID from description: " + orderId);
                paymentOpt = paymentService.getPaymentByOrderId(orderId);
            }
            
            // If not found by orderId, try finding by orderCode
            if (paymentOpt.isEmpty() && orderCode != null) {
                System.out.println("Searching for payment by orderCode: " + orderCode);
                Optional<Order> orderOpt = orderService.getById(orderCode);
                if (orderOpt.isPresent()) {
                    paymentOpt = paymentService.getPaymentByOrder(orderOpt.get());
                }
            }
            
            if (paymentOpt.isEmpty()) {
                System.out.println("Payment not found!");
                Map<String, Object> response = new HashMap<>();
                response.put("error", 0);
                response.put("message", "ok");
                response.put("data", Map.of(
                    "success", false,
                    "message", "Payment not found"
                ));
                return ResponseEntity.ok(response);
            }
            
            // Update payment and order status directly
            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();
            
            System.out.println("Current payment status: " + payment.getStatus());
            System.out.println("Current order status: " + order.getStatus());
            
            // Allow updating even if payment was CANCELLED or EXPIRED
            // PayOS webhook confirms user has paid, so we should update
            if (payment.getStatus() == PaymentStatus.PAID) {
                System.out.println("Payment already marked as PAID, skipping update");
            } else {
                payment.setStatus(PaymentStatus.PAID);
                payment.setUpdatedAt(java.time.LocalDateTime.now());
                paymentService.updatePaymentStatus(payment, PaymentStatus.PAID, null);
                
                order.setStatus(OrderStatus.PAID);
                order.setUpdatedAt(java.time.LocalDateTime.now());
                orderService.save(order);
                
                System.out.println("Payment and Order updated to PAID");
            }
            
            PaymentResult result = PaymentResult.success("Payment processed successfully");
            
            System.out.println("Webhook processing result: " + result.isSuccess());
            System.out.println("Result message: " + result.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", 0);
            response.put("message", "ok");
            response.put("data", Map.of(
                "success", result.isSuccess(),
                "message", result.getMessage()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Webhook processing error: " + e.getMessage());
            e.printStackTrace();
            
            // PayOS expects success response even for test webhooks
            Map<String, Object> response = new HashMap<>();
            response.put("error", 0);
            response.put("message", "ok");
            response.put("data", Map.of(
                "success", false,
                "error", e.getMessage()
            ));
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Test endpoint để kiểm tra webhook có hoạt động không
     */
    @GetMapping("/webhook/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testWebhook() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Webhook endpoint is accessible");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Test webhook với data giả để kiểm tra logic
     */
    @PostMapping("/webhook/test-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testWebhookWithData(@RequestBody Map<String, Object> testData) {
        System.out.println("=== TEST WEBHOOK WITH DATA ===");
        System.out.println("Test data: " + testData);
        
        try {
            // Tạo PayOSWebhookData giả để test
            PayOSWebhookData webhookData = new PayOSWebhookData();
            webhookData.setCode(0);
            webhookData.setDesc("Test webhook");
            
            PayOSWebhookData.Data data = new PayOSWebhookData.Data();
            data.setOrderCode(12345L);
            data.setAmount(100000);
            data.setDescription("Test payment");
            data.setPaymentLinkId(999L);
            data.setCode("00");
            data.setDesc("Test webhook");
            
            webhookData.setData(data);
            
            // Test webhook processing
            PaymentResult result = paymentService.handlePaymentWebhookFromPayOS(webhookData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test webhook processed");
            response.put("result", result.isSuccess());
            response.put("resultMessage", result.getMessage());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Test webhook error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Test webhook failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Helper method to extract order ID from description
     * Description format: "CSUO5KESD48 Order 17"
     */
    private Long extractOrderIdFromDescription(String description) {
        try {
            if (description != null && description.contains("Order ")) {
                String[] parts = description.split("Order ");
                if (parts.length > 1) {
                    return Long.parseLong(parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting order ID from description: " + e.getMessage());
        }
        return null;
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

            var payments = paymentService.getPaymentsByCustomerId(accountId);
            
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
        System.out.println("=== CONVERTING WEBHOOK DATA ===");
        System.out.println("WebhookData: " + webhookData);
        System.out.println("Code: " + webhookData.getCode());
        System.out.println("Desc: " + webhookData.getDesc());
        
        PayOSWebhookData payOSWebhookData = new PayOSWebhookData();
        payOSWebhookData.setCode(Integer.valueOf(webhookData.getCode()));
        payOSWebhookData.setDesc(webhookData.getDesc());
        
        // Create data structure with actual webhook data
        PayOSWebhookData.Data data = new PayOSWebhookData.Data();
        
        // Try to extract actual data from webhook
        try {
            // Use reflection to get data from webhook
            java.lang.reflect.Method[] methods = webhookData.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                    try {
                        Object value = method.invoke(webhookData);
                        System.out.println("Method " + method.getName() + ": " + value);
                        
                        // Map specific fields
                        if (method.getName().equals("getOrderCode") && value != null) {
                            data.setOrderCode(Long.valueOf(value.toString()));
                        } else if (method.getName().equals("getAmount") && value != null) {
                            data.setAmount(Integer.valueOf(value.toString()));
                        } else if (method.getName().equals("getDescription") && value != null) {
                            data.setDescription(value.toString());
                        } else if (method.getName().equals("getPaymentLinkId") && value != null) {
                            data.setPaymentLinkId(Long.valueOf(value.toString()));
                        }
                    } catch (Exception e) {
                        System.out.println("Could not invoke " + method.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting webhook data: " + e.getMessage());
        }
        
        // Set default values if not found
        if (data.getOrderCode() == null) data.setOrderCode(0L);
        if (data.getAmount() == null) data.setAmount(0);
        if (data.getDescription() == null) data.setDescription("Webhook from PayOS SDK");
        if (data.getPaymentLinkId() == null) data.setPaymentLinkId(0L);
        
        data.setCode(webhookData.getCode());
        data.setDesc(webhookData.getDesc());
        
        payOSWebhookData.setData(data);
        
        System.out.println("Converted PayOSWebhookData: " + payOSWebhookData);
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

            // Check if order belongs to current customer
            if (!order.getCustomer().getAccount().getAccountId().equals(accountId)) {
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
}
