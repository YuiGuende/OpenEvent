package com.group02.openevent.service.impl;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.ITicketRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final ITicketRepo ticketRepo;
    private final ITicketTypeRepo ticketTypeRepo;

    public TicketServiceImpl(ITicketRepo ticketRepo, ITicketTypeRepo ticketTypeRepo) {
        this.ticketRepo = ticketRepo;
        this.ticketTypeRepo = ticketTypeRepo;
    }

    @Override
    public Ticket createTicketFromOrder(Order order) {
        // Kiểm tra order đã thanh toán chưa
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Order must be paid to create ticket");
        }

        // Kiểm tra đã có ticket cho user và event này chưa
        if (ticketRepo.hasTicketForEvent(order.getUser().getUserId(), order.getEvent().getId())) {
            throw new IllegalArgumentException("User already has a ticket for this event");
        }

        // Tìm hoặc tạo ticket type cho event này
        TicketType ticketType = ticketTypeRepo.findByEvent_IdAndName(order.getEvent().getId(), "General Admission")
                .orElse(createTicketTypeForEvent(order.getEvent(), "General Admission", order.getAmount(), 1000));

        // Tạo ticket mới
        Ticket ticket = new Ticket();
        ticket.setTicketType(ticketType);
        ticket.setUser(order.getUser());

        return ticketRepo.save(ticket);
    }

    @Override
    public List<Ticket> getTicketsByUser(User user) {
        return ticketRepo.findByUser(user);
    }

    @Override
    public List<Ticket> getTicketsByUserAndEvent(User user, Event event) {
        return ticketRepo.findByUserAndEvent(user.getUserId(), event.getId());
    }

    @Override
    public boolean hasTicketForEvent(User user, Event event) {
        return ticketRepo.hasTicketForEvent(user.getUserId(), event.getId());
    }

    @Override
    public TicketType createTicketTypeForEvent(Event event, String name, BigDecimal price, Integer totalQuantity) {
        TicketType ticketType = new TicketType();
        ticketType.setName(name);
        ticketType.setPrice(price);
        ticketType.setTotalQuantity(totalQuantity);
        ticketType.setEvent(event);
        return ticketTypeRepo.save(ticketType);
    }

    @Override
    public List<TicketType> getTicketTypesByEvent(Event event) {
        return ticketTypeRepo.findByEvent(event);
    }

    @Override
    public Optional<Ticket> getTicketById(Long ticketId) {
        return ticketRepo.findById(ticketId);
    }
}