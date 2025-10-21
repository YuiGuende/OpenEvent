package com.group02.openevent.service;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.dto.user.UserOrderDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Order createOrderWithTicketTypes(CreateOrderWithTicketTypeRequest request, Customer customer);
    Optional<Order> getById(Long orderId);
    List<Order> getOrdersByCustomer(Customer customer);
    List<Order> getOrdersByCustomerId(Long customerId);
    Page<Order> list(Pageable pageable);
    void delete(Long orderId);
    void cancelOrder(Long orderId);
    void confirmOrder(Long orderId);
    Order save(Order order);
    boolean hasCustomerRegisteredForEvent(Long customerId, Long eventId);
    Optional<Order> getPendingOrderForEvent(Long customerId, Long eventId);
    Integer countUniqueParticipantsByEventId( Long eventId);
    List<Event> findConfirmedEventsByCustomerId(Long customerId);
    // New: DTO-based retrieval for customer order listing
    List<UserOrderDTO> getOrderDTOsByCustomerId(Long customerId, OrderStatus status);
    List<UserOrderDTO> getOrderDTOsByCustomer(Customer customer, OrderStatus status);
}


