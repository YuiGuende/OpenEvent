package com.group02.openevent.service;

import com.group02.openevent.dto.request.TicketUpdateRequest;
import com.group02.openevent.mapper.TicketMapper;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.impl.TicketTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketTypeServiceTest {
    @Mock
    private ITicketTypeRepo ticketTypeRepo;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private EventService eventService;
    @Mock
    private IOrderRepo orderRepo;

    @InjectMocks
    private TicketTypeServiceImpl ticketTypeService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }
    // ✅ TC01: Xóa vé khi isDeleted = true
    @Test
    void TC01_ShouldDeleteTicket_WhenIsDeletedTrue() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setTicketTypeId(5L);
        request.setIsDeleted(true);

        ticketTypeService.updateTickets(1L, List.of(request));

        verify(ticketTypeRepo).deleteById(5L);
        verify(ticketTypeRepo, never()).save(any());
    }

    // ✅ TC02: Tạo vé mới khi isNew = true
    @Test
    void TC02_ShouldCreateNewTicket_WhenIsNewTrue() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setIsNew(true);

        Event event = new Event();
        when(eventService.getEventById(1L)).thenReturn(Optional.of(event));

        TicketType mapped = new TicketType();
        when(ticketMapper.toTicketType(any())).thenReturn(mapped);

        ticketTypeService.updateTickets(1L, List.of(request));

        verify(ticketTypeRepo).save(mapped);
        assertEquals(event, mapped.getEvent());
    }

    // ✅ TC03: Cập nhật vé cũ khi isNew = false
    @Test
    void TC03_ShouldUpdateExistingTicket_WhenIsNewFalse() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setTicketTypeId(10L);
        request.setIsNew(false);

        TicketType existing = new TicketType();
        TicketType mapped = new TicketType();
        when(ticketTypeRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(ticketMapper.toTicketType(request)).thenReturn(mapped);

        ticketTypeService.updateTickets(1L, List.of(request));

        verify(ticketTypeRepo).save(existing);
    }

    // ✅ TC04: deleteTicketType() — Không xóa được vì có order liên quan
    @Test
    void TC04_ShouldThrowException_WhenOrderExists() {
        TicketType t = new TicketType();
        t.setTicketTypeId(7L);
        when(ticketTypeRepo.findById(7L)).thenReturn(Optional.of(t));
        when(orderRepo.existsByTicketType_TicketTypeId(7L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ticketTypeService.deleteTicketType(7L);
        });

        assertEquals("Không thể xóa loại vé vì đã có đơn hàng liên quan", ex.getMessage());
        verify(ticketTypeRepo, never()).delete(any());
    }

    // ✅ TC05: deleteTicketType() — Xóa thành công khi hợp lệ
    @Test
    void TC05_ShouldDeleteSuccessfully_WhenNoOrdersAndNotSold() {
        TicketType t = new TicketType();
        t.setTicketTypeId(9L);
        t.setSoldQuantity(0);

        when(ticketTypeRepo.findById(9L)).thenReturn(Optional.of(t));
        when(orderRepo.existsByTicketType_TicketTypeId(9L)).thenReturn(false);

        ticketTypeService.deleteTicketType(9L);

        verify(ticketTypeRepo).delete(t);
    }
}
