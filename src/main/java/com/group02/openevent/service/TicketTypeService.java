package com.group02.openevent.service;

import com.group02.openevent.model.ticket.TicketType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TicketTypeService {
    TicketType createTicketType(TicketType ticketType);
    Optional<TicketType> getTicketTypeById(Long id);
    List<TicketType> getAllTicketTypes();
    TicketType updateTicketType(Long id, TicketType updatedTicketType);
    void deleteTicketType(Long id);
    List<TicketType> getTicketTypesByEventId(Long eventId);
    List<TicketType> getAvailableTicketTypesByEventId(Long eventId);
    boolean canPurchaseTickets(Long ticketTypeId, Integer quantity);
    void reserveTickets(Long ticketTypeId, Integer quantity);
    void releaseTickets(Long ticketTypeId, Integer quantity);
    void confirmPurchase(Long ticketTypeId, Integer quantity);
    Page<TicketType> getTicketTypesPageable(Pageable pageable);
    List<TicketType> getTicketTypesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    boolean isTicketTypeAvailable(Long ticketTypeId);
    Integer getTotalSoldByEventId(Long eventId);
    Integer getTotalAvailableByEventId(Long eventId);
}
