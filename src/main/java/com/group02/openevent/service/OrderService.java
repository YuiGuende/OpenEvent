package com.group02.openevent.service;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.model.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Optional<Order> getById(Long orderId);
    Page<Order> list(Pageable pageable);
    void delete(Long orderId);
}


