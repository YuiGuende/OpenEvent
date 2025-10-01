package com.group02.openevent.service.impl;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final IOrderRepo orderRepo;
    private final IEventRepo eventRepo;

    public OrderServiceImpl(IOrderRepo orderRepo, IEventRepo eventRepo) {
        this.orderRepo = orderRepo;
        this.eventRepo = eventRepo;
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Event event = eventRepo.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.getEventId()));

        Order order = new Order();
        order.setEvent(event);
        order.setParticipantName(request.getParticipantName());
        order.setParticipantEmail(request.getParticipantEmail());
        order.setParticipantPhone(request.getParticipantPhone());
        order.setParticipantOrganization(request.getParticipantOrganization());
        order.setNotes(request.getNotes());
        return orderRepo.save(order);
    }

    @Override
    public Optional<Order> getById(Long orderId) {
        return orderRepo.findById(orderId);
    }

    @Override
    public Page<Order> list(Pageable pageable) {
        return orderRepo.findAll(pageable);
    }

    @Override
    public void delete(Long orderId) {
        orderRepo.deleteById(orderId);
    }
}


