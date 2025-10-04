package com.group02.openevent.service.impl;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.TicketTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final IOrderRepo orderRepo;
    private final IEventRepo eventRepo;
    private final ITicketTypeRepo ticketTypeRepo;
    private final TicketTypeService ticketTypeService;

    public OrderServiceImpl(IOrderRepo orderRepo, IEventRepo eventRepo, 
                           ITicketTypeRepo ticketTypeRepo, TicketTypeService ticketTypeService) {
        this.orderRepo = orderRepo;
        this.eventRepo = eventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
        this.ticketTypeService = ticketTypeService;
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
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderWithTicketTypes(CreateOrderWithTicketTypeRequest request, Customer customer) {
        try {
            // Validate event exists
            Event event = eventRepo.findById(request.getEventId())
                    .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.getEventId()));

            // For simplicity, we'll take the first ticket type from the request
            // In a real scenario, you might want to handle multiple ticket types differently
            if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
                throw new IllegalArgumentException("At least one ticket type must be specified");
            }

            CreateOrderWithTicketTypeRequest.OrderItemRequest firstItem = request.getOrderItems().get(0);
            
            // Validate ticket type exists and is available
            TicketType ticketType = ticketTypeRepo.findById(firstItem.getTicketTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Ticket type not found: " + firstItem.getTicketTypeId()));

            // Check if ticket type can be purchased (always quantity 1 for simplified order)
            if (!ticketTypeService.canPurchaseTickets(firstItem.getTicketTypeId(), 1)) {
                throw new IllegalStateException("Cannot purchase ticket of type: " + ticketType.getName() + 
                    " (Available: " + ticketType.getAvailableQuantity() + ")");
            }

            // Reserve tickets (always quantity 1 for simplified order)
            ticketTypeService.reserveTickets(firstItem.getTicketTypeId(), 1);

            // Create order
            Order order = new Order();
            order.setUser(customer);
            order.setEvent(event);
            order.setTicketType(ticketType);
            order.setParticipantName(request.getParticipantName());
            order.setParticipantEmail(request.getParticipantEmail());
            order.setParticipantPhone(request.getParticipantPhone());
            order.setParticipantOrganization(request.getParticipantOrganization());
            order.setNotes(request.getNotes());
            order.setStatus(OrderStatus.PENDING);

            // Calculate total amount
            order.calculateTotalAmount();

            // Save order
            Order savedOrder = orderRepo.save(order);
            
            return savedOrder;
            
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public List<Order> getOrdersByUser(Customer customer) {
        return orderRepo.findByCustomer(customer);
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepo.findByUserId(userId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }

        // Release reserved tickets
        if (order.getTicketType() != null) {
            ticketTypeService.releaseTickets(order.getTicketType().getTicketTypeId(), 1);
        }

        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(order);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm order with status: " + order.getStatus());
        }

        // Confirm purchase for the ticket type
        if (order.getTicketType() != null) {
            ticketTypeService.confirmPurchase(order.getTicketType().getTicketTypeId(), 1);
        }

        // Update order status
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepo.save(order);
    }

    @Override
    public void delete(Long orderId) {
        orderRepo.deleteById(orderId);
    }

    @Override
    public boolean hasUserRegisteredForEvent(Long userId, Long eventId) {
        List<Order> orders = orderRepo.findByUserId(userId);
        // Only count as registered if order is CONFIRMED or PAID (payment successful)
        // PENDING orders (unpaid) are not counted as registered
        return orders.stream()
                .anyMatch(order -> order.getEvent().getId().equals(eventId) && 
                         (order.getStatus() == OrderStatus.CONFIRMED || 
                          order.getStatus() == OrderStatus.PAID));
    }

    @Override
    public Optional<Order> getPendingOrderForEvent(Long userId, Long eventId) {
        List<Order> orders = orderRepo.findByUserId(userId);
        return orders.stream()
                .filter(order -> order.getEvent().getId().equals(eventId) && 
                               order.getStatus() == OrderStatus.PENDING)
                .findFirst();
    }

    @Override
    public Integer countUniqueParticipantsByEventId(Long eventId) {
        return orderRepo.countConfirmedParticipantsByEventId(eventId);
    }

    @Override
    public List<Event> findConfirmedEventsByCustomerId(Long customerId) {
        return orderRepo.findEventsByCustomerId(customerId);
    }
}


