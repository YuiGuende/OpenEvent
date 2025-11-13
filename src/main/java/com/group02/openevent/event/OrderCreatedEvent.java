package com.group02.openevent.event;

import com.group02.openevent.model.order.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderCreatedEvent extends ApplicationEvent {
    private final Order order;
    private final Long actorId; // USER_ID from session (customer)

    public OrderCreatedEvent(Object source, Order order, Long actorId) {
        super(source);
        this.order = order;
        this.actorId = actorId;
    }
}

