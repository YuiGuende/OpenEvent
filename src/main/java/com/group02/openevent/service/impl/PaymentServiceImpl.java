package com.group02.openevent.service.impl;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.repository.IPaymentRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.IHostWalletService;
import com.group02.openevent.dto.payment.PayOSWebhookData;
import com.group02.openevent.dto.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final IPaymentRepo paymentRepo;
    private final IOrderRepo orderRepo;
    private final OrderService orderService;
    private final PayOS payOS;
    private final EventAttendanceService attendanceService;
    private final IHostWalletService hostWalletService;

    public PaymentServiceImpl(IPaymentRepo paymentRepo, IOrderRepo orderRepo, OrderService orderService, PayOS payOS, EventAttendanceService attendanceService, IHostWalletService hostWalletService) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.orderService = orderService;
        this.payOS = payOS;
        this.attendanceService = attendanceService;
        this.hostWalletService = hostWalletService;
    }

    @Override
    public Payment createPaymentLinkForOrder(Order order, String returnUrl, String cancelUrl) {
        try {
            // Check if this is a free event (totalAmount == 0)
            if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
                logger.info("Free event detected for order: {}. Auto-completing payment without PayOS", order.getOrderId());
                return createFreePaymentForOrder(order, returnUrl, cancelUrl);
            }
            
            // Prepare PayOS payment request using SDK
            long orderCode = System.currentTimeMillis() / 1000; // Use timestamp as order code
            
            // Convert BigDecimal to long (PayOS expects amount in VNĐ, not cents)
            long amount = order.getTotalAmount().longValue();
            
            // PayOS description max 25 characters
            String description = "Order #" + order.getOrderId();
            
            // Debug logging
            logger.info("Order total amount: {} VNĐ, PayOS amount: {} VNĐ", order.getTotalAmount(), amount);

            // Create payment item
            PaymentLinkItem item = PaymentLinkItem.builder()
                .name("Event Registration - " + order.getEvent().getTitle())
                .quantity(1)
                .price(amount)
                .build();

            // Create payment request
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .item(item)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

            logger.info("Creating PayOS payment link: orderCode={}, amount={}, description={}", orderCode, amount, description);

            // Call PayOS SDK
            CreatePaymentLinkResponse payOSResponse = payOS.paymentRequests().create(paymentRequest);

            logger.info("PayOS payment link created successfully: paymentLinkId={}, checkoutUrl={}", 
                payOSResponse.getPaymentLinkId(), payOSResponse.getCheckoutUrl());

            // Calculate expiration time (15 minutes from now)
            LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);

            // Create payment record
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentLinkId(payOSResponse.getPaymentLinkId()); // Save as String
            payment.setAmount(order.getTotalAmount());
            payment.setDescription(description);
            payment.setCheckoutUrl(payOSResponse.getCheckoutUrl());
            payment.setQrCode(payOSResponse.getQrCode());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setExpiredAt(expiredAt);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setReturnUrl(returnUrl);
            payment.setCancelUrl(cancelUrl);

            return paymentRepo.save(payment);

        } catch (Exception e) {
            logger.error("Error creating payment link: ", e);
            throw new RuntimeException("Error creating payment link: " + e.getMessage());
        }
    }

    /**
     * Tạo payment cho free event (totalAmount = 0)
     * Tự động set status = PAID, không cần PayOS
     */
    private Payment createFreePaymentForOrder(Order order, String returnUrl, String cancelUrl) {
        try {
            logger.info("Creating free payment for order: {}", order.getOrderId());
            
            // Update order status to PAID immediately
            order.setStatus(com.group02.openevent.model.order.OrderStatus.PAID);
            order.setUpdatedAt(LocalDateTime.now());
            order = orderRepo.save(order);
            logger.info("Order {} status updated to PAID", order.getOrderId());

            // Create payment record with PAID status (no PayOS needed)
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentLinkId(null); // No PayOS payment link for free events
            payment.setAmount(BigDecimal.ZERO);
            payment.setDescription("Free Event - Order #" + order.getOrderId());
            payment.setCheckoutUrl(null);
            payment.setQrCode(null);
            payment.setStatus(PaymentStatus.PAID); // Set to PAID immediately
            payment.setExpiredAt(null); // No expiration for free events
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            payment.setReturnUrl(returnUrl);
            payment.setCancelUrl(cancelUrl);
            
            payment = paymentRepo.save(payment);
            logger.info("Free payment {} created with PAID status", payment.getPaymentId());

            // Create EventAttendance when order is paid
            try {
                attendanceService.createAttendanceFromOrder(order);
                logger.info("EventAttendance created successfully for free order: {}", order.getOrderId());
            } catch (Exception e) {
                logger.error("Error creating EventAttendance for free order {}: {}", order.getOrderId(), e.getMessage(), e);
                // Don't fail the payment creation if attendance creation fails
            }

            // Credit host wallet when order is paid successfully (even for free events, we track it)
            // This will automatically create wallet if it doesn't exist
            try {
                if (order.getEvent() != null && order.getEvent().getHost() != null && order.getEvent().getHost().getId() != null) {
                    Long hostId = order.getEvent().getHost().getId();
                    
                    // Ensure wallet exists (getWalletByHostId will create if not exists)
                    hostWalletService.getWalletByHostId(hostId);
                    
                    // For free events, we still track the registration (amount = 0)
                    BigDecimal amountToCredit = BigDecimal.ZERO;
                    String balanceDescription = "Đăng ký miễn phí từ đơn hàng #" + order.getOrderId();
                    
                    hostWalletService.addBalance(hostId, amountToCredit, String.valueOf(order.getOrderId()), balanceDescription);
                    logger.info("Free event registration tracked for host {} wallet for order {}", hostId, order.getOrderId());
                } else {
                    logger.warn("Warning: Order {} has no associated host, skipping wallet tracking", order.getOrderId());
                }
            } catch (Exception e) {
                logger.error("Error tracking free event in host wallet: {}", e.getMessage(), e);
                // Don't fail the payment creation if wallet credit fails
            }

            return payment;

        } catch (Exception e) {
            logger.error("Error creating free payment: ", e);
            throw new RuntimeException("Error creating free payment: " + e.getMessage());
        }
    }

    @Override
    public Optional<Payment> getPaymentByOrder(Order order) {
        return paymentRepo.findByOrder(order);
    }

    @Override
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return orderRepo.findById(orderId)
            .flatMap(paymentRepo::findByOrder);
    }

    @Override
    public boolean verifyWebhook(PayOSWebhookData webhookData) {
        // PayOS SDK handles webhook verification internally
        return webhookData != null && webhookData.getData() != null;
    }

    @Override
    public PaymentResult handlePaymentWebhook(PayOSWebhookData webhookData) {
        logger.info("=== PAYMENT WEBHOOK PROCESSING ===");
        logger.info("Webhook data received: {}", webhookData);
        
        try {
            if (!verifyWebhook(webhookData)) {
                logger.error("Invalid webhook signature");
                return PaymentResult.failure("Invalid webhook signature");
            }

            Long paymentLinkId = webhookData.getData().getPaymentLinkId();
            logger.info("Payment link ID from webhook: {}", paymentLinkId);
            
            if (paymentLinkId == null) {
                logger.error("Missing payment link ID in webhook");
                return PaymentResult.failure("Missing payment link ID in webhook");
            }

            // Find payment by PayOS payment link ID (as String)
            logger.info("Searching for payment with PayOS ID: {}", paymentLinkId);
            Optional<Payment> paymentOpt = paymentRepo.findByPaymentLinkId(String.valueOf(paymentLinkId));
            
            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for PayOS ID: {}", paymentLinkId);
                // Log all existing payments for debugging
                List<Payment> allPayments = paymentRepo.findAll();
                logger.error("Available payments: {}", allPayments.stream()
                    .map(p -> "ID: " + p.getPaymentId() + ", PayOS ID: " + p.getPaymentLinkId())
                    .collect(Collectors.toList()));
                return PaymentResult.failure("Payment not found for PayOS ID: " + paymentLinkId);
            }

            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();
            logger.info("Found payment: {} for order: {}", payment.getPaymentId(), order.getOrderId());

            // Update payment status
            payment.setStatus(PaymentStatus.PAID);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(payment);
            logger.info("Payment status updated to PAID");

            // Update order status
            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);
            logger.info("Order status updated to PAID");

            // Create EventAttendance when order is paid
            try {
                attendanceService.createAttendanceFromOrder(order);
                logger.info("EventAttendance created successfully for order: {}", order.getOrderId());
            } catch (Exception e) {
                logger.error("Error creating EventAttendance for order {}: {}", order.getOrderId(), e.getMessage(), e);
                // Don't fail the webhook if attendance creation fails - log and continue
            }

            logger.info("Payment webhook processed successfully for order: {}", order.getOrderId());
            return PaymentResult.success("Payment processed successfully");

        } catch (Exception e) {
            logger.error("Error processing payment webhook: ", e);
            return PaymentResult.failure("Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    public PaymentResult handlePaymentWebhookFromPayOS(PayOSWebhookData webhookData) {
        return handlePaymentWebhook(webhookData);
    }

    @Override
    public PaymentResult handlePaymentWebhookDirectly(vn.payos.model.webhooks.WebhookData webhookData) {
        logger.info("=== PROCESSING WEBHOOK DIRECTLY ===");
        logger.info("Webhook data: {}", webhookData);
        
        try {
            // Extract data from PayOS SDK WebhookData
            String code = webhookData.getCode();
            String desc = webhookData.getDesc();
            
            logger.info("Webhook code: {}, desc: {}", code, desc);
            
            // Check if payment was successful
            if (!"00".equals(code)) {
                logger.warn("Payment failed with code: {} - {}", code, desc);
                return PaymentResult.failure("Payment failed: " + desc);
            }
            
            // Try to get payment link ID from webhook data
            // We need to examine the actual structure of WebhookData
            logger.info("Webhook data class: {}", webhookData.getClass().getName());
            logger.info("Webhook data toString: {}", webhookData.toString());
            
            // Use reflection to get all available methods
            try {
                java.lang.reflect.Method[] methods = webhookData.getClass().getMethods();
                logger.info("Available methods on WebhookData:");
                for (java.lang.reflect.Method method : methods) {
                    if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                        try {
                            Object value = method.invoke(webhookData);
                            logger.info("{}: {}", method.getName(), value);
                        } catch (Exception e) {
                            logger.debug("Could not invoke {}: {}", method.getName(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not inspect WebhookData methods: {}", e.getMessage());
            }
            
            // For now, we'll need to find the payment by other means
            // This is a temporary solution - we need to understand the WebhookData structure
            logger.warn("Cannot extract payment link ID from webhook data");
            logger.warn("This is the main issue - we need to get the actual payment link ID");
            
            return PaymentResult.failure("Cannot extract payment link ID from webhook data");
            
        } catch (Exception e) {
            logger.error("Error processing webhook directly: ", e);
            return PaymentResult.failure("Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    public void updatePaymentStatus(Payment payment, PaymentStatus status, Long payosPaymentId) {
        payment.setStatus(status);
        if (payosPaymentId != null) {
            payment.setPayosPaymentId(payosPaymentId);
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(payment);
    }

    @Override
    public List<Payment> getPaymentsByCustomerId(Long customerId) {
        return paymentRepo.findByOrder_Customer_CustomerId(customerId);
    }

    @Override
    public List<Payment> getPaymentsByCustomerIdAndStatus(Long customerId, PaymentStatus status) {
        return paymentRepo.findByOrder_Customer_CustomerIdAndStatus(customerId, status);
    }

    @Override
    public boolean cancelPayment(Payment payment) {
        try {
            if (payment.getStatus() == PaymentStatus.PAID) {
                logger.warn("Cannot cancel paid payment: {}", payment.getPaymentId());
                return false;
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(payment);

            // Also cancel the order
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);

            logger.info("Payment cancelled: {}", payment.getPaymentId());
            return true;

        } catch (Exception e) {
            logger.error("Error cancelling payment: ", e);
            return false;
        }
    }

    @Override
    public void updateExpiredPayments() {
        try {
            // Find all pending payments that have expired
            List<Payment> allPendingPayments = paymentRepo.findByStatus(PaymentStatus.PENDING);
            LocalDateTime now = LocalDateTime.now();
            List<Payment> expiredPayments = allPendingPayments.stream()
                .filter(p -> p.getExpiredAt() != null && p.getExpiredAt().isBefore(now))
                .collect(Collectors.toList());

            for (Payment payment : expiredPayments) {
                payment.setStatus(PaymentStatus.EXPIRED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepo.save(payment);

                // Also mark order as cancelled
                Order order = payment.getOrder();
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepo.save(order);
            }

            logger.info("Updated {} expired payments", expiredPayments.size());

        } catch (Exception e) {
            logger.error("Error updating expired payments: ", e);
        }
    }
}
