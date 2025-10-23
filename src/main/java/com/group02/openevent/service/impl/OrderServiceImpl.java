package com.group02.openevent.service.impl;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.dto.user.UserOrderDTO;
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
import com.group02.openevent.service.VoucherService;
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
    private final VoucherService voucherService;

    public OrderServiceImpl(IOrderRepo orderRepo, IEventRepo eventRepo, 
                           ITicketTypeRepo ticketTypeRepo, TicketTypeService ticketTypeService,
                           VoucherService voucherService) {
        this.orderRepo = orderRepo;
        this.eventRepo = eventRepo;
        this.ticketTypeRepo = ticketTypeRepo;
        this.ticketTypeService = ticketTypeService;
        this.voucherService = voucherService;
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
            if (request.getTicketTypeId() == null) {
                throw new IllegalArgumentException("At least one ticket type must be specified");
            }

            // Validate ticket type exists and is available
            TicketType ticketType = ticketTypeRepo.findById(request.getTicketTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Ticket type not found: " + request.getTicketTypeId()));

            // Check if ticket type can be purchased (always quantity 1 for simplified order)
            if (!ticketTypeService.canPurchaseTickets(request.getTicketTypeId(), 1)) {
                throw new IllegalStateException("Cannot purchase ticket of type: " + ticketType.getName() + 
                    " (Available: " + ticketType.getAvailableQuantity() + ")");
            }

            // Reserve tickets (always quantity 1 for simplified order)
            ticketTypeService.reserveTickets(request.getTicketTypeId());

            // Create order
            Order order = new Order();
            order.setCustomer(customer);
            order.setEvent(event);
            order.setTicketType(ticketType);
            order.setParticipantName(request.getParticipantName());
            order.setParticipantEmail(request.getParticipantEmail());
            order.setParticipantPhone(request.getParticipantPhone());
            order.setParticipantOrganization(request.getParticipantOrganization());
            order.setNotes(request.getNotes());
            order.setStatus(OrderStatus.PENDING);

            // Set host discount from event
            if (event.getHost() != null && event.getHost().getHostDiscountPercent() != null) {
                order.setHostDiscountPercent(event.getHost().getHostDiscountPercent());
            }

            // Calculate total amount first
            order.calculateTotalAmount();

            // Save order first to get ID
            Order savedOrder = orderRepo.save(order);

            // Process voucher code if provided (after order is saved)
            if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                try {
                    // Apply voucher to saved order
                    voucherService.applyVoucherToOrder(request.getVoucherCode(), savedOrder);
                    
                    // Recalculate total amount after voucher
                    savedOrder.calculateTotalAmount();
                    savedOrder = orderRepo.save(savedOrder);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continue without voucher if it fails
                }
            }
            
            return savedOrder;
            
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public List<Order> getOrdersByCustomer(Customer customer) {
        return orderRepo.findByCustomer(customer);
    }

    @Override
    public List<Order> getOrdersByCustomerId(Long customerId) {
        return orderRepo.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrderDTO> getOrderDTOsByCustomerId(Long customerId, OrderStatus status) {
        List<Order> orders = orderRepo.findByCustomerId(customerId);
        if (status != null) {
            orders = orders.stream()
                    .filter(o -> o.getStatus() == status)
                    .toList();
        }
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return orders.stream().map(this::mapToUserOrderDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrderDTO> getOrderDTOsByCustomer(Customer customer, OrderStatus status) {
        return getOrderDTOsByCustomerId(customer.getCustomerId(), status);
    }

    private UserOrderDTO mapToUserOrderDTO(Order order) {
        return UserOrderDTO.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .eventId(order.getEvent() != null ? order.getEvent().getId() : null)
                .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : null)
                .eventImageUrl(order.getEvent() != null ? order.getEvent().getImageUrl() : null)
                .eventStartsAt(order.getEvent() != null ? order.getEvent().getStartsAt() : null)
                .ticketTypeId(order.getTicketType() != null ? order.getTicketType().getTicketTypeId() : null)
                .ticketTypeName(order.getTicketType() != null ? order.getTicketType().getName() : null)
                .totalAmount(order.getTotalAmount())
                .build();
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
        order.setStatus(OrderStatus.PAID);
        orderRepo.save(order);
    }

    @Override
    public void delete(Long orderId) {
        orderRepo.deleteById(orderId);
    }

    @Override
    public Order save(Order order) {
        return orderRepo.save(order);
    }

    @Override
    public boolean hasCustomerRegisteredForEvent(Long customerId, Long eventId) {
        List<Order> orders = orderRepo.findByCustomerId(customerId);
        // Only count as registered if order is CONFIRMED or PAID (payment successful)
        // PENDING orders (unpaid) are not counted as registered
        return orders.stream()
                .anyMatch(order -> order.getEvent().getId().equals(eventId) && 
                         (order.getStatus() == OrderStatus.PAID));
    }

    @Override
    public Optional<Order> getPendingOrderForEvent(Long customerId, Long eventId) {
        List<Order> orders = orderRepo.findByCustomerId(customerId);
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


