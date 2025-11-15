package com.group02.openevent.listener;

import com.group02.openevent.event.*;
import com.group02.openevent.model.auditLog.AuditLog;
import com.group02.openevent.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditLogEventListener {

    @Autowired
    private AuditLogService auditLogService;

    @EventListener
    @Async
    public void handleEventCreated(EventCreatedEvent event) {
        try {
            AuditLog auditLog = auditLogService.createAuditLog(
                    "EVENT_CREATED",
                    "EVENT",
                    event.getEvent().getId(),
                    event.getActorId(),
                    String.format("Event '%s' (ID: %d) was created", 
                            event.getEvent().getTitle(), 
                            event.getEvent().getId())
            );
            log.debug("Audit log created for EVENT_CREATED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for EVENT_CREATED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleEventUpdated(EventUpdatedEvent event) {
        try {
            AuditLog auditLog = auditLogService.createAuditLog(
                    "EVENT_UPDATED",
                    "EVENT",
                    event.getEvent().getId(),
                    event.getActorId(),
                    String.format("Event '%s' (ID: %d) was updated", 
                            event.getEvent().getTitle(), 
                            event.getEvent().getId())
            );
            log.debug("Audit log created for EVENT_UPDATED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for EVENT_UPDATED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleEventCancelled(EventCancelledEvent event) {
        try {
            AuditLog auditLog = auditLogService.createAuditLog(
                    "EVENT_CANCELLED",
                    "EVENT",
                    event.getEvent().getId(),
                    event.getActorId(),
                    String.format("Event '%s' (ID: %d) was cancelled", 
                            event.getEvent().getTitle(), 
                            event.getEvent().getId())
            );
            log.debug("Audit log created for EVENT_CANCELLED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for EVENT_CANCELLED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleEventApprovalRequested(EventApprovalRequestedEvent event) {
        try {
            String eventTitle = event.getRequest().getEvent() != null 
                    ? event.getRequest().getEvent().getTitle() 
                    : "Unknown";
            Long eventId = event.getRequest().getEvent() != null 
                    ? event.getRequest().getEvent().getId() 
                    : null;
            
            AuditLog auditLog = auditLogService.createAuditLog(
                    "EVENT_APPROVAL_REQUESTED",
                    "REQUEST",
                    event.getRequest().getRequestId(),
                    event.getActorId(),
                    String.format("Event approval requested for '%s' (Event ID: %d, Request ID: %d)", 
                            eventTitle, 
                            eventId,
                            event.getRequest().getRequestId())
            );
            log.debug("Audit log created for EVENT_APPROVAL_REQUESTED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for EVENT_APPROVAL_REQUESTED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleEventApproved(EventApprovedEvent event) {
        try {
            AuditLog auditLog = auditLogService.createAuditLog(
                    "EVENT_APPROVED",
                    "EVENT",
                    event.getEvent().getId(),
                    event.getActorId(),
                    String.format("Event '%s' (ID: %d) was approved", 
                            event.getEvent().getTitle(), 
                            event.getEvent().getId())
            );
            log.debug("Audit log created for EVENT_APPROVED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for EVENT_APPROVED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        try {
            String eventTitle = event.getNotification().getEvent() != null 
                    ? event.getNotification().getEvent().getTitle() 
                    : "N/A";
            Long eventId = event.getNotification().getEvent() != null 
                    ? event.getNotification().getEvent().getId() 
                    : null;
            
            AuditLog auditLog = auditLogService.createAuditLog(
                    "NOTIFICATION_CREATED",
                    "NOTIFICATION",
                    event.getNotification().getNotificationId(),
                    event.getActorId(),
                    String.format("Notification created (ID: %d) for event '%s' (Event ID: %d)", 
                            event.getNotification().getNotificationId(),
                            eventTitle,
                            eventId)
            );
            log.debug("Audit log created for NOTIFICATION_CREATED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for NOTIFICATION_CREATED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleUserCreated(UserCreatedEvent event) {
        try {
            String description = String.format("User created (ID: %d, Type: %s, Email: %s)", 
                    event.getUser().getUserId(),
                    event.getUserType(),
                    event.getUser().getAccount() != null 
                            ? event.getUser().getAccount().getEmail() 
                            : "N/A");
            
            // For self-registration, actorId might be null, use user's own ID
            Long actorId = event.getActorId() != null 
                    ? event.getActorId() 
                    : event.getUser().getUserId();
            
            AuditLog auditLog = auditLogService.createAuditLog(
                    "USER_CREATED",
                    "USER",
                    event.getUser().getUserId(),
                    actorId,
                    description
            );
            log.debug("Audit log created for USER_CREATED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for USER_CREATED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            String eventTitle = event.getOrder().getEvent() != null 
                    ? event.getOrder().getEvent().getTitle() 
                    : "Unknown";
            
            AuditLog auditLog = auditLogService.createAuditLog(
                    "ORDER_CREATED",
                    "ORDER",
                    event.getOrder().getOrderId(),
                    event.getActorId(),
                    String.format("Order created (ID: %d) for event '%s' (Event ID: %d), Amount: %s", 
                            event.getOrder().getOrderId(),
                            eventTitle,
                            event.getOrder().getEvent() != null 
                                    ? event.getOrder().getEvent().getId() 
                                    : null,
                            event.getOrder().getTotalAmount() != null 
                                    ? event.getOrder().getTotalAmount().toString() 
                                    : "N/A")
            );
            log.debug("Audit log created for ORDER_CREATED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for ORDER_CREATED: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            String eventTitle = event.getPayment().getOrder() != null 
                    && event.getPayment().getOrder().getEvent() != null
                    ? event.getPayment().getOrder().getEvent().getTitle() 
                    : "Unknown";
            
            AuditLog auditLog = auditLogService.createAuditLog(
                    "PAYMENT_COMPLETED",
                    "PAYMENT",
                    event.getPayment().getPaymentId(),
                    event.getActorId(),
                    String.format("Payment completed (ID: %d, Order ID: %d) for event '%s'", 
                            event.getPayment().getPaymentId(),
                            event.getPayment().getOrder() != null 
                                    ? event.getPayment().getOrder().getOrderId() 
                                    : null,
                            eventTitle)
            );
            log.debug("Audit log created for PAYMENT_COMPLETED: {}", auditLog.getAuditId());
        } catch (Exception e) {
            log.error("Error creating audit log for PAYMENT_COMPLETED: {}", e.getMessage(), e);
        }
    }
}

