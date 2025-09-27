package com.group02.openevent.service.impl;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.repository.IPaymentRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.TicketService;
import com.group02.openevent.dto.payment.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.payos.type.WebhookData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final IPaymentRepo paymentRepo;
    private final IOrderRepo orderRepo;
    private final TicketService ticketService;
    private final RestTemplate restTemplate;

    @Value("${payos.client.id:}")
    private String payosClientId;

    @Value("${payos.api.key:}")
    private String payosApiKey;

    @Value("${payos.checksum.key:}")
    private String payosChecksumKey;

    @Value("${payos.base.url:https://api-merchant.payos.vn}")
    private String payosBaseUrl;

    public PaymentServiceImpl(IPaymentRepo paymentRepo, IOrderRepo orderRepo, TicketService ticketService, RestTemplate restTemplate) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.ticketService = ticketService;
        this.restTemplate = restTemplate;
    }

    @Override
    public Payment createPaymentLink(Order order, String returnUrl, String cancelUrl) {
        try {
            // Prepare PayOS API request
            // Generate positive order code from string
            int orderCode = Math.abs(order.getOrderCode().hashCode());
            int amount = order.getAmount().intValue();
            String description = order.getDescription();
            
            // Set expired time (15 minutes from now)
            LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);
            long expiredAtSeconds = expiredAt.toEpochSecond(java.time.ZoneOffset.UTC);
            
            // Create items list
            List<PayOSItem> items = Arrays.asList(
                new PayOSItem("Event Registration", 1, amount)
            );
            
            // Create PayOS payment request WITHOUT signature first
            PayOSPaymentRequest request = new PayOSPaymentRequest();
            request.setOrderCode(orderCode);
            request.setAmount(amount);
            request.setDescription(description);
            request.setItems(items);
            request.setReturnUrl(returnUrl);
            request.setCancelUrl(cancelUrl);
            request.setExpiredAt(expiredAtSeconds);
            
            // Calculate signature from the request object
            String signature = createPaymentSignatureFromRequest(request);
            request.setSignature(signature);
            
            System.out.println("Expired at (seconds): " + expiredAtSeconds);
            System.out.println("Payment signature: " + signature);
            System.out.println("Attempting PayOS API call WITH signature field");
            
            // Call PayOS API - thử v1 thay vì v2
            String url = payosBaseUrl + "/v1/payment-requests";
            System.out.println("PayOS API URL: " + url);
            
            // Thử URL khác nếu cần
            // String url = "https://api-merchant.payos.vn/v2/payment-requests";
            // String url = "https://api.payos.vn/v2/payment-requests";
            
            // Validate required fields
            if (orderCode <= 0) {
                throw new RuntimeException("Order code must be positive: " + orderCode);
            }
            if (amount <= 0) {
                throw new RuntimeException("Amount must be positive: " + amount);
            }
            if (description == null || description.trim().isEmpty()) {
                throw new RuntimeException("Description is required");
            }
            if (items == null || items.isEmpty()) {
                throw new RuntimeException("Items are required");
            }
            
            System.out.println("PayOS API Request URL: " + url);
            System.out.println("PayOS API Request: " + request);
            System.out.println("PayOS Client ID: " + payosClientId);
            System.out.println("PayOS API Key: " + (payosApiKey != null ? "***" + payosApiKey.substring(Math.max(0, payosApiKey.length() - 4)) : "null"));
            System.out.println("Order Code: " + orderCode);
            System.out.println("Amount: " + amount);
            System.out.println("Description: " + description);
            System.out.println("Items count: " + items.size());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payosClientId);
            headers.set("x-api-key", payosApiKey);
            
            // Make API call with signature
            HttpEntity<PayOSPaymentRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<PayOSPaymentResponse> response = restTemplate.postForEntity(
                url, entity, PayOSPaymentResponse.class
            );
            
            System.out.println("PayOS API Response Status: " + response.getStatusCode());
            System.out.println("PayOS API Response Body: " + response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                PayOSPaymentResponse responseBody = response.getBody();
                
                // Check if response indicates success
                if (responseBody.getCode() != null && responseBody.getCode() != 0) {
                    throw new RuntimeException("PayOS API error: " + responseBody.getDesc() + " (Code: " + responseBody.getCode() + ")");
                }
                
                // Check if data is null
                if (responseBody.getData() == null) {
                    throw new RuntimeException("PayOS API returned null data. Response: " + responseBody);
                }
                
                // Create Payment record
                Payment payment = new Payment();
                payment.setOrder(order);
                // Convert String paymentLinkId to Long for payosPaymentId
                try {
                    payment.setPayosPaymentId(Long.parseLong(responseBody.getData().getPaymentLinkId()));
                } catch (NumberFormatException e) {
                    // If paymentLinkId is not a valid number, use order ID as fallback
                    payment.setPayosPaymentId(order.getOrderId());
                }
                payment.setPaymentLinkId(responseBody.getData().getPaymentLinkId());
                payment.setCheckoutUrl(responseBody.getData().getCheckoutUrl());
                payment.setQrCode(responseBody.getData().getQrCode());
                payment.setAmount(order.getAmount());
                payment.setCurrency("VND");
                payment.setStatus(PaymentStatus.PENDING);
                payment.setDescription(order.getDescription());
                payment.setReturnUrl(returnUrl);
                payment.setCancelUrl(cancelUrl);
                payment.setExpiredAt(expiredAt);
                payment.setPayosSignature("NO_SIGNATURE_REQUIRED");

                return paymentRepo.save(payment);
            } else {
                throw new RuntimeException("Failed to create PayOS payment link. Status: " + response.getStatusCode() + ", Body: " + response.getBody());
            }
            
        } catch (Exception e) {
            System.err.println("PayOS API Error: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Create mock payment for development
            if (payosClientId.isEmpty() || payosApiKey.isEmpty()) {
                System.out.println("PayOS credentials not configured, creating mock payment...");
                return createMockPayment(order, returnUrl, cancelUrl);
            }
            
            throw new RuntimeException("Error creating payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Payment> getPaymentByOrder(Order order) {
        return paymentRepo.findByOrder(order);
    }

    @Override
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepo.findByOrderOrderId(orderId);
    }

    @Override
    public boolean verifyWebhook(PayOSWebhookData webhookData) {
        try {
            String dataString = webhookData.getData().getOrderCode() + "|" + 
                               webhookData.getData().getAmount() + "|" + 
                               webhookData.getData().getDescription();
            
            String signature = calculateHMAC(dataString, payosChecksumKey);
            return signature.equals(webhookData.getSignature());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PaymentResult handlePaymentWebhook(PayOSWebhookData webhookData) {
        try {
            // Verify webhook
            if (!verifyWebhook(webhookData)) {
                return new PaymentResult(false, "Invalid webhook signature");
            }

            // Find payment by order code
            Optional<Order> orderOpt = orderRepo.findByOrderCode(webhookData.getData().getOrderCode().toString());
            if (orderOpt.isEmpty()) {
                return new PaymentResult(false, "Order not found");
            }

            Order order = orderOpt.get();
            Optional<Payment> paymentOpt = getPaymentByOrder(order);
            if (paymentOpt.isEmpty()) {
                return new PaymentResult(false, "Payment not found");
            }

            Payment payment = paymentOpt.get();

            // Update payment status
            if ("PAID".equals(webhookData.getData().getCode())) {
                // Update payment status to PAID
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepo.save(payment);
                
                order.setStatus(OrderStatus.PAID);
                orderRepo.save(order);
                
                // Tạo ticket sau khi thanh toán thành công
                try {
                    Ticket ticket = ticketService.createTicketFromOrder(order);
                    return new PaymentResult(true, "Payment successful and ticket created", payment, "/payment/success?orderCode=" + order.getOrderCode() + "&ticketId=" + ticket.getTicketId());
                } catch (Exception e) {
                    // Log error nhưng vẫn trả về success vì payment đã thành công
                    return new PaymentResult(true, "Payment successful but ticket creation failed: " + e.getMessage(), payment, "/payment/success?orderCode=" + order.getOrderCode());
                }
            } else {
                // Update payment status to CANCELLED
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancelledAt(LocalDateTime.now());
                paymentRepo.save(payment);
                
                order.setStatus(OrderStatus.CANCELLED);
                orderRepo.save(order);
                
                return new PaymentResult(true, "Payment cancelled", payment, "/payment/cancel?orderCode=" + order.getOrderCode());
            }

        } catch (Exception e) {
            return new PaymentResult(false, "Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    public void updatePaymentStatus(Payment payment, PaymentStatus status, Long payosPaymentId) {
        payment.setStatus(status);
        payment.setPayosPaymentId(payosPaymentId);
        
        if (status == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        } else if (status == PaymentStatus.CANCELLED) {
            payment.setCancelledAt(LocalDateTime.now());
        }
        
        paymentRepo.save(payment);
    }

    @Override
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepo.findByUserId(userId);
    }

    @Override
    public List<Payment> getPaymentsByUserIdAndStatus(Long userId, PaymentStatus status) {
        return paymentRepo.findByUserIdAndStatus(userId, status);
    }

    @Override
    public boolean cancelPayment(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancelledAt(LocalDateTime.now());
            paymentRepo.save(payment);
            
            // Update order status
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepo.save(order);
            
            return true;
        }
        return false;
    }

    @Override
    public void updateExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> expiredPayments = paymentRepo.findExpiredPendingPayments(now);
        
        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepo.save(payment);
            
            // Update order status
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.EXPIRED);
            orderRepo.save(order);
        }
    }

    @Override
    public PaymentResult handlePaymentWebhookFromPayOS(WebhookData webhookData) {
        try {
            // Find payment by order code
            Optional<Order> orderOpt = orderRepo.findByOrderCode(webhookData.getOrderCode().toString());
            if (orderOpt.isEmpty()) {
                return new PaymentResult(false, "Order not found");
            }

            Order order = orderOpt.get();
            Optional<Payment> paymentOpt = getPaymentByOrder(order);
            if (paymentOpt.isEmpty()) {
                return new PaymentResult(false, "Payment not found");
            }

            Payment payment = paymentOpt.get();

            // Update payment status based on PayOS response
            if ("00".equals(webhookData.getCode())) {
                // Payment successful
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepo.save(payment);
                
                order.setStatus(OrderStatus.PAID);
                orderRepo.save(order);
                
                // Create ticket after successful payment
                try {
                    Ticket ticket = ticketService.createTicketFromOrder(order);
                    return new PaymentResult(true, "Payment successful and ticket created", payment, "/payment/success?orderCode=" + order.getOrderCode() + "&ticketId=" + ticket.getTicketId());
                } catch (Exception e) {
                    // Log error but still return success since payment was successful
                    return new PaymentResult(true, "Payment successful but ticket creation failed: " + e.getMessage(), payment, "/payment/success?orderCode=" + order.getOrderCode());
                }
            } else {
                // Payment failed or cancelled
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancelledAt(LocalDateTime.now());
                paymentRepo.save(payment);
                
                order.setStatus(OrderStatus.CANCELLED);
                orderRepo.save(order);
                
                return new PaymentResult(true, "Payment cancelled", payment, "/payment/cancel?orderCode=" + order.getOrderCode());
            }

        } catch (Exception e) {
            return new PaymentResult(false, "Error processing webhook: " + e.getMessage());
        }
    }



    private Payment createMockPayment(Order order, String returnUrl, String cancelUrl) {
        try {
            // Set expired time (15 minutes from now)
            LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);
            
            // Create mock Payment record
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPayosPaymentId(order.getOrderId()); // Use order ID as fallback
            payment.setPaymentLinkId("MOCK_PAYMENT_LINK_" + order.getOrderId());
            
            // Mock checkout URL - for development only
            String mockCheckoutUrl = "http://localhost:8080/payment/mock-success?orderCode=" + order.getOrderCode();
            payment.setCheckoutUrl(mockCheckoutUrl);
            
            payment.setQrCode("https://img.vietqr.io/image/970422-1234567890-qr_only.png");
            payment.setAmount(order.getAmount());
            payment.setCurrency("VND");
            payment.setStatus(PaymentStatus.PENDING);
            payment.setDescription(order.getDescription());
            payment.setReturnUrl(returnUrl);
            payment.setCancelUrl(cancelUrl);
            payment.setExpiredAt(expiredAt);
            payment.setPayosSignature("MOCK_SIGNATURE");

            System.out.println("Created mock payment with checkout URL: " + mockCheckoutUrl);
            return paymentRepo.save(payment);
            
        } catch (Exception e) {
            throw new RuntimeException("Error creating mock payment: " + e.getMessage(), e);
        }
    }

    private String calculateHMAC(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacData);
    }

    /**
     * Tạo signature cho PayOS payment request theo đúng tài liệu PayOS
     * Format: key1=value1&key2=value2&... (sắp xếp theo thứ tự bảng chữ cái của key)
     * Chỉ sử dụng các trường cơ bản: amount, cancelUrl, description, orderCode, returnUrl
     */
    private String createPaymentSignatureFromRequest(PayOSPaymentRequest request) {
        try {
            // Chỉ sử dụng các trường cơ bản theo tài liệu PayOS
            Map<String, Object> dataMap = new TreeMap<>();
            dataMap.put("amount", request.getAmount());
            dataMap.put("cancelUrl", request.getCancelUrl());
            dataMap.put("description", request.getDescription());
            dataMap.put("orderCode", request.getOrderCode());
            dataMap.put("returnUrl", request.getReturnUrl());
            
            // Tạo string theo format key=value&key=value
            StringBuilder dataStringBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                if (dataStringBuilder.length() > 0) {
                    dataStringBuilder.append("&");
                }
                
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // Xử lý giá trị null hoặc undefined
                if (value == null || "undefined".equals(value) || "null".equals(value)) {
                    value = "";
                }
                
                dataStringBuilder.append(key).append("=").append(value);
            }
            
            String dataString = dataStringBuilder.toString();
            System.out.println("PayOS signature data string: " + dataString);
            
            // Sử dụng checksum_key để tạo signature
            String signature = calculateHMAC(dataString, payosChecksumKey);
            System.out.println("Using checksum key for signature: " + (payosChecksumKey != null ? "***" + payosChecksumKey.substring(Math.max(0, payosChecksumKey.length() - 4)) : "null"));
            System.out.println("Generated signature: " + signature);
            
            return signature;
        } catch (Exception e) {
            throw new RuntimeException("Error creating payment signature: " + e.getMessage(), e);
        }
    }
}
