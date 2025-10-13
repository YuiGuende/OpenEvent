package com.group02.openevent.service.impl;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.repository.IPaymentRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.OrderService;
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

    public PaymentServiceImpl(IPaymentRepo paymentRepo, IOrderRepo orderRepo, OrderService orderService, PayOS payOS) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.orderService = orderService;
        this.payOS = payOS;
    }

    @Override
    public Payment createPaymentLinkForOrder(Order order, String returnUrl, String cancelUrl) {
        try {
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
        try {
            if (!verifyWebhook(webhookData)) {
                return PaymentResult.failure("Invalid webhook signature");
            }

            Long paymentLinkId = webhookData.getData().getPaymentLinkId();
            if (paymentLinkId == null) {
                return PaymentResult.failure("Missing payment link ID in webhook");
            }

            // Find payment by PayOS payment link ID (as String)
            Optional<Payment> paymentOpt = paymentRepo.findByPaymentLinkId(String.valueOf(paymentLinkId));
            if (paymentOpt.isEmpty()) {
                return PaymentResult.failure("Payment not found for PayOS ID: " + paymentLinkId);
            }

            Payment payment = paymentOpt.get();
            Order order = payment.getOrder();

            // Update payment status
            payment.setStatus(PaymentStatus.PAID);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(payment);

            // Update order status
            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);

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
