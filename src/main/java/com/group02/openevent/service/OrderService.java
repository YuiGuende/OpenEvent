package com.group02.openevent.service;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Order createOrderWithTicketTypes(CreateOrderWithTicketTypeRequest request, Customer customer);
    Optional<Order> getById(Long orderId);
    List<Order> getOrdersByUser(Customer customer);
    List<Order> getOrdersByUserId(Long userId);
    Page<Order> list(Pageable pageable);
    void delete(Long orderId);
    void cancelOrder(Long orderId);
    void confirmOrder(Long orderId);
    boolean hasUserRegisteredForEvent(Long userId, Long eventId);
    Optional<Order> getPendingOrderForEvent(Long userId, Long eventId);
    Integer countUniqueParticipantsByEventId( Long eventId);
    List<Event> findConfirmedEventsByCustomerId(Long customerId);
}


