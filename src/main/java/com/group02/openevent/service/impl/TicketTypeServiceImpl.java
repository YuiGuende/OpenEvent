package com.group02.openevent.service.impl;

import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.TicketTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TicketTypeServiceImpl implements TicketTypeService {

    @Autowired
    private ITicketTypeRepo ticketTypeRepo;

    @Override
    public TicketType createTicketType(TicketType ticketType) {
        if (ticketType.getSoldQuantity() == null) {
            ticketType.setSoldQuantity(0);
        }
        return ticketTypeRepo.save(ticketType);
    }

    @Override
    public Optional<TicketType> getTicketTypeById(Long id) {
        return ticketTypeRepo.findById(id);
    }

    @Override
    public List<TicketType> getAllTicketTypes() {
        return ticketTypeRepo.findAll();
    }

    @Override
    public TicketType updateTicketType(Long id, TicketType updatedTicketType) {
        return ticketTypeRepo.findById(id)
                .map(existing -> {
                    existing.setName(updatedTicketType.getName());
                    existing.setDescription(updatedTicketType.getDescription());
                    existing.setPrice(updatedTicketType.getPrice());
                    existing.setTotalQuantity(updatedTicketType.getTotalQuantity());
                    existing.setStartSaleDate(updatedTicketType.getStartSaleDate());
                    existing.setEndSaleDate(updatedTicketType.getEndSaleDate());
                    // Không update soldQuantity qua API này
                    return ticketTypeRepo.save(existing);
                })
                .orElse(null);
    }

    @Override
    public void deleteTicketType(Long id) {
        TicketType ticketType = ticketTypeRepo.findById(id)
                .orElse(null);
        // Kiểm tra đã bán vé chưa
        assert ticketType != null;
        if (ticketType.getSoldQuantity() > 0) {
            throw new IllegalStateException("Cannot delete ticket type with sold tickets");
        }

        ticketTypeRepo.delete(ticketType);
    }

    @Override
    public List<TicketType> getTicketTypesByEventId(Long eventId) {
        return ticketTypeRepo.findByEventId(eventId);
    }

    @Override
    public List<TicketType> getAvailableTicketTypesByEventId(Long eventId) {
        return ticketTypeRepo.findAvailableByEventId(eventId, LocalDateTime.now());
    }

    @Override
    public boolean canPurchaseTickets(Long ticketTypeId, Integer quantity) {
        Optional<TicketType> ticketTypeOpt = ticketTypeRepo.findById(ticketTypeId);
        if (ticketTypeOpt.isEmpty()) {
            return false;
        }
        return ticketTypeOpt.get().canPurchase(quantity);
    }

    @Override
    @Transactional
    public void reserveTickets(Long ticketTypeId, Integer quantity) {
        TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                .orElse(null);

        assert ticketType != null;
        if (!ticketType.canPurchase(quantity)) {
            throw new IllegalStateException("Cannot reserve " + quantity + " tickets for ticket type: " + ticketTypeId);
        }

        ticketType.increaseSoldQuantity(quantity);
        ticketTypeRepo.save(ticketType);
    }

    @Override
    @Transactional
    public void releaseTickets(Long ticketTypeId, Integer quantity) {
        TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                .orElse(null);

        assert ticketType != null;
        ticketType.decreaseSoldQuantity(quantity);
        ticketTypeRepo.save(ticketType);
    }

    @Override
    @Transactional
    public void confirmPurchase(Long ticketTypeId, Integer quantity) {
        // Trong trường hợp này, việc reserve đã tăng soldQuantity
        // Nên confirmPurchase chỉ cần log hoặc update trạng thái khác
        TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                .orElse(null);

        // Purchase confirmed
    }

    @Override
    public Page<TicketType> getTicketTypesPageable(Pageable pageable) {
        return ticketTypeRepo.findAll(pageable);
    }

    @Override
    public List<TicketType> getTicketTypesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return ticketTypeRepo.findByPriceRange(minPrice, maxPrice);
    }

    @Override
    public boolean isTicketTypeAvailable(Long ticketTypeId) {
        return ticketTypeRepo.isAvailableNow(ticketTypeId, LocalDateTime.now());
    }

    @Override
    public Integer getTotalSoldByEventId(Long eventId) {
        Integer result = ticketTypeRepo.getTotalSoldByEventId(eventId);
        return result != null ? result : 0;
    }

    @Override
    public Integer getTotalAvailableByEventId(Long eventId) {
        Integer result = ticketTypeRepo.getTotalAvailableByEventId(eventId);
        return result != null ? result : 0;
    }
}