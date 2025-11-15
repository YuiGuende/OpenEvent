package com.group02.openevent.event;

import com.group02.openevent.model.payment.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {
    private final Payment payment;
    private final Long actorId; // USER_ID from session (customer)

    public PaymentCompletedEvent(Object source, Payment payment, Long actorId) {
        super(source);
        this.payment = payment;
        this.actorId = actorId;
    }
}

