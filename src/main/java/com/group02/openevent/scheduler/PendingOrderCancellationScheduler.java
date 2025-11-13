package com.group02.openevent.scheduler;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IPaymentRepo;
import com.group02.openevent.service.AuditLogService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler để tự động hủy các đơn hàng và payment có trạng thái PENDING sau một khoảng thời gian
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderCancellationScheduler {
    
    private final IOrderRepo orderRepo;
    private final IPaymentRepo paymentRepo;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AuditLogService auditLogService;
    
    @Value("${order.pending-timeout-minutes:30}")
    private int pendingTimeoutMinutes;
    
    @Value("${order.auto-cancel-enabled:true}")
    private boolean autoCancelEnabled;
    
    /**
     * Tự động hủy các đơn hàng và payment PENDING quá lâu
     * Chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    @Transactional
    public void cancelExpiredPendingOrders() {
        if (!autoCancelEnabled) {
            log.debug("Auto-cancel for pending orders is disabled");
            return;
        }
        
        try {
            log.info("🔍 Checking for expired pending orders and payments...");
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
            log.debug("Cutoff time: {} ({} minutes ago)", cutoffTime, pendingTimeoutMinutes);
            
            // Hủy các payment PENDING quá lâu
            int cancelledPayments = cancelExpiredPendingPayments(cutoffTime);
            
            // Hủy các order PENDING quá lâu (không có payment hoặc payment đã bị hủy)
            int cancelledOrders = cancelExpiredPendingOrders(cutoffTime);
            
            if (cancelledPayments > 0 || cancelledOrders > 0) {
                log.info("✅ Cancelled {} expired payments and {} expired orders", 
                        cancelledPayments, cancelledOrders);
            } else {
                log.debug("No expired pending orders or payments found");
            }
            
        } catch (Exception e) {
            log.error("❌ Error in pending order cancellation scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Hủy các payment PENDING quá lâu
     */
    private int cancelExpiredPendingPayments(LocalDateTime cutoffTime) {
        List<Payment> expiredPayments = paymentRepo.findPendingPaymentsCreatedBefore(cutoffTime);
        
        if (expiredPayments.isEmpty()) {
            return 0;
        }
        
        log.info("Found {} expired pending payments to cancel", expiredPayments.size());
        
        int cancelledCount = 0;
        for (Payment payment : expiredPayments) {
            try {
                // Kiểm tra lại status để tránh race condition
                if (payment.getStatus() != PaymentStatus.PENDING) {
                    log.debug("Payment {} is no longer PENDING, skipping", payment.getPaymentId());
                    continue;
                }
                
                // Hủy payment (sẽ tự động hủy order liên quan trong PaymentService)
                boolean cancelled = paymentService.cancelPayment(payment);
                if (cancelled) {
                    cancelledCount++;
                    log.debug("Cancelled expired payment: {}", payment.getPaymentId());
                    
                    // Create audit log for auto-cancelled payment
                    try {
                        String eventTitle = payment.getOrder() != null 
                                && payment.getOrder().getEvent() != null
                                ? payment.getOrder().getEvent().getTitle()
                                : "Unknown Event";
                        
                        auditLogService.createAuditLog(
                                "PAYMENT_AUTO_CANCELLED",
                                "PAYMENT",
                                payment.getPaymentId(),
                                null, // System action, no actor
                                String.format("Payment (ID: %d, Order ID: %d) for event '%s' was automatically cancelled due to timeout (%d minutes)",
                                        payment.getPaymentId(),
                                        payment.getOrder() != null ? payment.getOrder().getOrderId() : null,
                                        eventTitle,
                                        pendingTimeoutMinutes)
                        );
                    } catch (Exception e) {
                        log.warn("Failed to create audit log for auto-cancelled payment {}: {}", 
                                payment.getPaymentId(), e.getMessage());
                    }
                } else {
                    log.warn("Failed to cancel payment: {}", payment.getPaymentId());
                }
                
            } catch (Exception e) {
                log.error("Error cancelling payment {}: {}", payment.getPaymentId(), e.getMessage(), e);
            }
        }
        
        return cancelledCount;
    }
    
    /**
     * Hủy các order PENDING quá lâu (không có payment hoặc payment đã bị hủy)
     */
    private int cancelExpiredPendingOrders(LocalDateTime cutoffTime) {
        List<Order> expiredOrders = orderRepo.findPendingOrdersCreatedBefore(cutoffTime);
        
        if (expiredOrders.isEmpty()) {
            return 0;
        }
        
        log.info("Found {} expired pending orders to check", expiredOrders.size());
        
        int cancelledCount = 0;
        for (Order order : expiredOrders) {
            try {
                // Kiểm tra lại status để tránh race condition
                if (order.getStatus() != OrderStatus.PENDING) {
                    log.debug("Order {} is no longer PENDING, skipping", order.getOrderId());
                    continue;
                }
                
                // Kiểm tra xem order có payment PENDING không
                // Nếu có payment PENDING, bỏ qua (sẽ được xử lý bởi cancelExpiredPendingPayments)
                boolean hasPendingPayment = paymentRepo.findByOrder_OrderId(order.getOrderId())
                        .map(p -> p.getStatus() == PaymentStatus.PENDING)
                        .orElse(false);
                
                if (hasPendingPayment) {
                    log.debug("Order {} has pending payment, will be handled by payment cancellation", 
                            order.getOrderId());
                    continue;
                }
                
                // Hủy order
                orderService.cancelOrder(order.getOrderId());
                cancelledCount++;
                log.debug("Cancelled expired order: {}", order.getOrderId());
                
                // Create audit log for auto-cancelled order
                try {
                    String eventTitle = order.getEvent() != null 
                            ? order.getEvent().getTitle()
                            : "Unknown Event";
                    
                    auditLogService.createAuditLog(
                            "ORDER_AUTO_CANCELLED",
                            "ORDER",
                            order.getOrderId(),
                            null, // System action, no actor
                            String.format("Order (ID: %d) for event '%s' was automatically cancelled due to timeout (%d minutes)",
                                    order.getOrderId(),
                                    eventTitle,
                                    pendingTimeoutMinutes)
                    );
                } catch (Exception e) {
                    log.warn("Failed to create audit log for auto-cancelled order {}: {}", 
                            order.getOrderId(), e.getMessage());
                }
                
            } catch (Exception e) {
                log.error("Error cancelling order {}: {}", order.getOrderId(), e.getMessage(), e);
            }
        }
        
        return cancelledCount;
    }
}

