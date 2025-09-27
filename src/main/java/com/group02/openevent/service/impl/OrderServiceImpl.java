package com.group02.openevent.service.impl;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final IOrderRepo orderRepo;
    private final IEventRepo eventRepo;

    public OrderServiceImpl(IOrderRepo orderRepo, IEventRepo eventRepo) {
        this.orderRepo = orderRepo;
        this.eventRepo = eventRepo;
    }

    @Override
    public Order createOrder(CreateOrderRequest request, User user) {
        // Validate event exists
        Optional<Event> eventOpt = eventRepo.findById(request.getEventId());
        if (eventOpt.isEmpty()) {
            throw new IllegalArgumentException("Event not found");
        }

        Event event = eventOpt.get();

        // Check if user already registered for this event
        if (hasUserRegisteredEvent(user.getUserId(), request.getEventId())) {
            throw new IllegalArgumentException("User has already registered for this event");
        }

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setEvent(event);
        order.setOrderCode(generateOrderCode());
        order.setAmount(request.getAmount());
        order.setCurrency("VND");
        order.setStatus(OrderStatus.PENDING);
        // PayOS yêu cầu description tối đa 25 ký tự
        String description = "Event Registration";
        order.setDescription(description);
        order.setParticipantName(request.getParticipantName());
        order.setParticipantEmail(request.getParticipantEmail());
        order.setParticipantPhone(request.getParticipantPhone());
        order.setParticipantOrganization(request.getParticipantOrganization());
        order.setNotes(request.getNotes());

        return orderRepo.save(order);
    }

    @Override
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepo.findById(orderId);
    }

    @Override
    public Optional<Order> getOrderByCode(String orderCode) {
        return orderRepo.findByOrderCode(orderCode);
    }

    @Override
    public List<Order> getOrdersByUser(User user) {
        return orderRepo.findByUser(user);
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepo.findByUserId(userId);
    }

    @Override
    public List<Order> getOrdersByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderRepo.findByUserIdAndStatus(userId, status);
    }

    @Override
    public void updateOrderStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        orderRepo.save(order);
    }

    @Override
    public boolean cancelOrder(Order order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepo.save(order);
            return true;
        }
        return false;
    }

    @Override
    public String generateOrderCode() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 1000));
        return "ORD" + timestamp.substring(timestamp.length() - 8) + random;
    }

    @Override
    public void updateExpiredOrders() {
        // Orders expire after 15 minutes if not paid
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(15);
        List<Order> expiredOrders = orderRepo.findExpiredPendingOrders(expiredTime);
        
        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepo.save(order);
        }
    }

    @Override
    public OrderResponse getOrderStatistics(Long userId) {
        List<Order> allOrders = getOrdersByUserId(userId);
        
        // Group orders by status
        Map<String, Long> statistics = allOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().name(),
                        Collectors.counting()
                ));

        // Add total count
        statistics.put("TOTAL", (long) allOrders.size());

        OrderResponse response = new OrderResponse();
        response.setStatistics(statistics);
        
        return response;
    }

    @Override
    public boolean hasUserRegisteredEvent(Long userId, Long eventId) {
        List<Order> userOrders = getOrdersByUserId(userId);
        return userOrders.stream()
                .anyMatch(order -> order.getEvent().getId().equals(eventId) && 
                                 order.getStatus() == OrderStatus.PAID);
    }
}
