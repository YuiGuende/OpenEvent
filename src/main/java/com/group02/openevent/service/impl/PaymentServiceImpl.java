package com.group02.openevent.service.impl;

import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.ticket.TicketStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.repository.IPaymentRepo;
import com.group02.openevent.repository.ITicketRepo;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.TicketService;
import com.group02.openevent.dto.payment.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final IPaymentRepo paymentRepo;
    private final ITicketRepo ticketRepo;
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

    public PaymentServiceImpl(IPaymentRepo paymentRepo, ITicketRepo ticketRepo, TicketService ticketService, RestTemplate restTemplate) {
        this.paymentRepo = paymentRepo;
        this.ticketRepo = ticketRepo;
        this.ticketService = ticketService;
        this.restTemplate = restTemplate;
    }

    @Override
    public Payment createPaymentLink(Ticket ticket, String returnUrl, String cancelUrl) {
        try {
            // Prepare PayOS API request
            // Generate positive order code from string
            int orderCode = Math.abs(ticket.getTicketCode().hashCode());
            int amount = ticket.getPrice().intValue();
            String description = ticket.getDescription();
            
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
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payosClientId);
            headers.set("x-api-key", payosApiKey);
            
            HttpEntity<PayOSPaymentRequest> entity = new HttpEntity<>(request, headers);
            
            System.out.println("Request body: " + request);
            
            ResponseEntity<PayOSPaymentResponse> response = restTemplate.postForEntity(url, entity, PayOSPaymentResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                PayOSPaymentResponse payOSResponse = response.getBody();
                
                // Create payment record
                Payment payment = new Payment();
                payment.setTicket(ticket);
                payment.setPayosPaymentId(Long.valueOf(payOSResponse.getData().getPaymentLinkId()));
                payment.setAmount(ticket.getPrice());
                payment.setDescription(description);
                payment.setCheckoutUrl(payOSResponse.getData().getCheckoutUrl());
                payment.setQrCode(payOSResponse.getData().getQrCode());
                payment.setStatus(PaymentStatus.PENDING);
                payment.setExpiredAt(expiredAt);
                payment.setCreatedAt(LocalDateTime.now());
                
                return paymentRepo.save(payment);
            } else {
                throw new RuntimeException("Failed to create payment link: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating payment link: " + e.getMessage());
        }
    }

    @Override
    public Optional<Payment> getPaymentByTicket(Ticket ticket) {
        return paymentRepo.findByTicket(ticket);
    }

    @Override
    public Optional<Payment> getPaymentByTicketId(Long ticketId) {
        return paymentRepo.findByTicket_TicketId(ticketId);
    }

    @Override
    public boolean verifyWebhook(PayOSWebhookData webhookData) {
        try {
            // Verify webhook signature
            String receivedSignature = webhookData.getSignature();
            String calculatedSignature = createWebhookSignature(webhookData);
            
            return receivedSignature != null && receivedSignature.equals(calculatedSignature);
        } catch (Exception e) {
            e.printStackTrace();
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
            Optional<Ticket> ticketOpt = ticketRepo.findByTicketCode(webhookData.getData().getOrderCode().toString());
            if (ticketOpt.isEmpty()) {
                return new PaymentResult(false, "Ticket not found");
            }

            Ticket ticket = ticketOpt.get();
            Optional<Payment> paymentOpt = getPaymentByTicket(ticket);
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
                
                ticket.setStatus(TicketStatus.PAID);
                ticketRepo.save(ticket);
                
                // User tham gia event sau khi thanh toán thành công
                ticketService.joinEventAfterPayment(ticket.getUser().getUserId(), ticket.getEvent().getId());
                
                return new PaymentResult(true, "Payment successful and ticket created", payment, "/payment/success?ticketCode=" + ticket.getTicketCode() + "&ticketId=" + ticket.getTicketId());
            } else {
                // Update payment status to CANCELLED
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancelledAt(LocalDateTime.now());
                paymentRepo.save(payment);
                
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepo.save(ticket);
                
                return new PaymentResult(true, "Payment cancelled", payment, "/payment/cancel?ticketCode=" + ticket.getTicketCode());
            }

        } catch (Exception e) {
            return new PaymentResult(false, "Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    public void updatePaymentStatus(Payment payment, PaymentStatus status, Long payosPaymentId) {
        payment.setStatus(status);
        if (payosPaymentId != null) {
            payment.setPayosPaymentId(payosPaymentId);
        }
        
        if (status == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        } else if (status == PaymentStatus.CANCELLED) {
            payment.setCancelledAt(LocalDateTime.now());
        }
        
        paymentRepo.save(payment);
    }

    @Override
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepo.findByTicket_User_UserId(userId);
    }

    @Override
    public List<Payment> getPaymentsByUserIdAndStatus(Long userId, PaymentStatus status) {
        return paymentRepo.findByTicket_User_UserIdAndStatus(userId, status);
    }

    @Override
    public boolean cancelPayment(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancelledAt(LocalDateTime.now());
            paymentRepo.save(payment);
            
            // Also cancel the ticket
            Ticket ticket = payment.getTicket();
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepo.save(ticket);
            
            return true;
        }
        return false;
    }

    @Override
    public void updateExpiredPayments() {
        // Payments expire after 15 minutes if not paid
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(15);
        List<Payment> expiredPayments = paymentRepo.findExpiredPendingPayments(expiredTime);
        
        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancelledAt(LocalDateTime.now());
            paymentRepo.save(payment);
            
            // Also expire the ticket
            Ticket ticket = payment.getTicket();
            ticket.setStatus(TicketStatus.EXPIRED);
            ticketRepo.save(ticket);
        }
    }

    @Override
    public PaymentResult handlePaymentWebhookFromPayOS(PayOSWebhookData webhookData) {
        try {
            // Find payment by order code
            Optional<Ticket> ticketOpt = ticketRepo.findByTicketCode(webhookData.getData().getOrderCode().toString());
            if (ticketOpt.isEmpty()) {
                return new PaymentResult(false, "Ticket not found");
            }

            Ticket ticket = ticketOpt.get();
            Optional<Payment> paymentOpt = getPaymentByTicket(ticket);
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
                
                ticket.setStatus(TicketStatus.PAID);
                ticketRepo.save(ticket);
                
                return new PaymentResult(true, "Payment successful and ticket created", payment, "/payment/success?ticketCode=" + ticket.getTicketCode() + "&ticketId=" + ticket.getTicketId());
            } else {
                // Payment failed or cancelled
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancelledAt(LocalDateTime.now());
                paymentRepo.save(payment);
                
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepo.save(ticket);
                
                return new PaymentResult(true, "Payment cancelled", payment, "/payment/cancel?ticketCode=" + ticket.getTicketCode());
            }

        } catch (Exception e) {
            return new PaymentResult(false, "Error processing webhook: " + e.getMessage());
        }
    }

    private String createPaymentSignatureFromRequest(PayOSPaymentRequest request) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderCode", request.getOrderCode());
            data.put("amount", request.getAmount());
            data.put("description", request.getDescription());
            data.put("items", request.getItems());
            data.put("returnUrl", request.getReturnUrl());
            data.put("cancelUrl", request.getCancelUrl());
            data.put("expiredAt", request.getExpiredAt());

            return createSignature(data);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String createWebhookSignature(PayOSWebhookData webhookData) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderCode", webhookData.getData().getOrderCode());
            data.put("amount", webhookData.getData().getAmount());
            data.put("description", webhookData.getData().getDescription());
            data.put("accountNumber", webhookData.getData().getAccountNumber());
            data.put("reference", webhookData.getData().getReference());
            data.put("transactionDateTime", webhookData.getData().getTransactionDateTime());
            data.put("currency", webhookData.getData().getCurrency());
            data.put("paymentLinkId", webhookData.getData().getPaymentLinkId());
            data.put("code", webhookData.getData().getCode());
            data.put("desc", webhookData.getData().getDesc());
            data.put("counterAccountBankId", webhookData.getData().getCounterAccountBankId());
            data.put("virtualAccountName", webhookData.getData().getVirtualAccountName());
            data.put("virtualAccountNumber", webhookData.getData().getVirtualAccountNumber());
            data.put("counterAccountBankName", webhookData.getData().getCounterAccountBankName());
            data.put("counterAccountName", webhookData.getData().getCounterAccountName());
            data.put("counterAccountNumber", webhookData.getData().getCounterAccountNumber());
            // data.put("subAccountId", webhookData.getData().getSubAccountId()); // Method not available
            data.put("transactionDateTime", webhookData.getData().getTransactionDateTime());

            return createSignature(data);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String createSignature(Map<String, Object> data) throws NoSuchAlgorithmException, InvalidKeyException {
        // Convert data to JSON string
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                jsonBuilder.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                jsonBuilder.append(value);
            } else if (value instanceof List) {
                jsonBuilder.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        jsonBuilder.append("{");
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        boolean itemFirst = true;
                        for (Map.Entry<?, ?> itemEntry : itemMap.entrySet()) {
                            if (!itemFirst) jsonBuilder.append(",");
                            jsonBuilder.append("\"").append(itemEntry.getKey()).append("\":");
                            Object itemValue = itemEntry.getValue();
                            if (itemValue instanceof String) {
                                jsonBuilder.append("\"").append(itemValue).append("\"");
                            } else if (itemValue instanceof Number) {
                                jsonBuilder.append(itemValue);
                            }
                            itemFirst = false;
                        }
                        jsonBuilder.append("}");
                    }
                }
                jsonBuilder.append("]");
            }
            first = false;
        }
        
        jsonBuilder.append("}");
        
        String jsonString = jsonBuilder.toString();
        System.out.println("JSON for signature: " + jsonString);
        
        // Create HMAC SHA256 signature
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(payosChecksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] signatureBytes = mac.doFinal(jsonString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(signatureBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}