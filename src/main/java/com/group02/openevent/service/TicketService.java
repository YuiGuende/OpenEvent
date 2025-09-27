package com.group02.openevent.service;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TicketService {
    Ticket createTicketFromOrder(Order order);
    List<Ticket> getTicketsByUser(User user);
    List<Ticket> getTicketsByUserAndEvent(User user, Event event);
    boolean hasTicketForEvent(User user, Event event);
    TicketType createTicketTypeForEvent(Event event, String name, BigDecimal price, Integer totalQuantity);
    List<TicketType> getTicketTypesByEvent(Event event);
    Optional<Ticket> getTicketById(Long ticketId);
}