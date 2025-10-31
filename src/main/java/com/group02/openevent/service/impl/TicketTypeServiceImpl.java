package com.group02.openevent.service.impl;

import com.group02.openevent.dto.request.TicketUpdateRequest;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.mapper.TicketMapper;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.TicketTypeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketTypeServiceImpl implements TicketTypeService {

    @Autowired
    private ITicketTypeRepo ticketTypeRepo;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    @Lazy
    private EventService eventService;
    @Autowired
    private IOrderRepo orderRepo;


    @Override
    public Optional<TicketType> getTicketTypeById(Long id) {
        return ticketTypeRepo.findById(id);
    }

    @Override
    public List<TicketType> getAllTicketTypes() {
        return ticketTypeRepo.findAll();
    }

    @Override
    public void updateTickets(Long eventId, List<TicketUpdateRequest> tickets) {
        for (TicketUpdateRequest request : tickets) {
            if (Boolean.TRUE.equals(request.getIsDeleted())) {
                if (request.getTicketTypeId() != null) {
                    ticketTypeRepo.deleteById(request.getTicketTypeId());
                }
                continue;
            }

            TicketType ticket = ticketMapper.toTicketType(request);

            if (Boolean.TRUE.equals(request.getIsNew())) {
                Event event = eventService.getEventById(eventId)
                        .orElseThrow(() -> new EntityNotFoundException("Event not found"));
                ticket.setEvent(event);
            } else {
                TicketType existing = ticketTypeRepo.findById(request.getTicketTypeId())
                        .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
                BeanUtils.copyProperties(ticket, existing, "id", "event");
                ticket = existing;
            }

            ticketTypeRepo.save(ticket);
        }
    }

    @Override
    public void deleteTicketType(Long id) {
        TicketType ticketType = ticketTypeRepo.findById(id)
                .orElse(null);
        // Kiểm tra đã bán vé chưa
        assert ticketType != null;

        // Nếu có đơn hàng tham chiếu đến ticket type này thì không cho xóa
        if (orderRepo.existsByTicketType_TicketTypeId(id)) {
            throw new IllegalStateException("Không thể xóa loại vé vì đã có đơn hàng liên quan");
        }

        // Dự phòng: nếu soldQuantity > 0 cũng không cho xóa
        if (ticketType.getSoldQuantity() != null && ticketType.getSoldQuantity() > 0) {
            throw new IllegalStateException("Không thể xóa loại vé đã bán");
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
        return ticketTypeOpt.get().canPurchase();
    }

    @Override
    @Transactional
    public void reserveTickets(Long ticketTypeId) {
        TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                .orElse(null);

        assert ticketType != null;
        if (!ticketType.canPurchase()) {
            throw new IllegalStateException("Cannot reserve tickets for ticket type: " + ticketTypeId);
        }

        ticketType.increaseSoldQuantity();
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

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeDTO> getTicketTypeDTOsByEventId(Long eventId) {
        List<TicketType> ticketTypeList = ticketTypeRepo.findByEventId(eventId);
        return ticketTypeRepo.findByEventId(eventId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeDTO> getAvailableTicketTypeDTOsByEventId(Long eventId) {
        return ticketTypeRepo.findAvailableByEventIdIgnoreTime(eventId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketTypeDTO> getTicketTypeDTOById(Long ticketTypeId) {
        return ticketTypeRepo.findById(ticketTypeId)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional
    public TicketType saveTicketType(TicketType ticketType) {
        return ticketTypeRepo.save(ticketType);
    }

//    @Override
//    @Transactional
//    public void deleteTicketType(Long ticketTypeId) {
//        ticketTypeRepo.deleteById(ticketTypeId);
//    }

    @Override
    public TicketTypeDTO convertToDTO(TicketType ticketType) {
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isAvailable = ticketType.getAvailableQuantity() > 0
                    && (ticketType.getStartSaleDate() == null || now.isAfter(ticketType.getStartSaleDate()))
                    && (ticketType.getEndSaleDate() == null || now.isBefore(ticketType.getEndSaleDate()));

            boolean isSaleActive = ticketType.isSalePeriodActive();
            boolean isSoldOut = ticketType.getAvailableQuantity() <= 0;
            boolean saleNotStarted = ticketType.getStartSaleDate() != null && now.isBefore(ticketType.getStartSaleDate());
            boolean saleOverdue = ticketType.getEndSaleDate() != null && now.isAfter(ticketType.getEndSaleDate());

            String saleStartCountdownText = null;
            if (saleNotStarted) {
                LocalDateTime start = ticketType.getStartSaleDate();
                long days = java.time.Duration.between(now, start).toDays();
                if (days < 0) days = 0;
                saleStartCountdownText = days + (days == 1 ? " day" : " days");
            }

            TicketTypeDTO ticketTypeDTO = TicketTypeDTO.builder()
                    .ticketTypeId(ticketType.getTicketTypeId())
                    .eventId(ticketType.getEvent().getId())
                    .eventTitle(ticketType.getEvent().getTitle())
                    .eventImageUrl(ticketType.getEvent().getImageUrl())
                    .name(ticketType.getName())
                    .description(ticketType.getDescription())
                    .price(ticketType.getPrice())
                    .sale(ticketType.getSale() != null ? ticketType.getSale() : BigDecimal.ZERO)
                    .finalPrice(ticketType.getFinalPrice())
                    .totalQuantity(ticketType.getTotalQuantity())
                    .soldQuantity(ticketType.getSoldQuantity())
                    .availableQuantity(ticketType.getAvailableQuantity())
                    .startSaleDate(ticketType.getStartSaleDate())
                    .endSaleDate(ticketType.getEndSaleDate())
                    .isAvailable(isAvailable)
                    .isSaleActive(isSaleActive)
                    .isSoldOut(isSoldOut)
                    .saleNotStarted(saleNotStarted)
                    .saleStartCountdownText(saleStartCountdownText)
                    .saleOverdue(saleOverdue)
                    .build();
            return ticketTypeDTO;
        } catch (Exception e) {
            System.err.println("Error converting TicketType to DTO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSoldTickets(Long eventId) {
        Integer total = ticketTypeRepo.getTotalSoldByEventId(eventId);
        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTicketCapacity(Long eventId) {
        Integer total = ticketTypeRepo.getTotalTicketCapacityByEventId(eventId);
        return total != null ? total : 0;
    }
}