package com.group02.openevent.service;

import com.group02.openevent.dto.request.TicketUpdateRequest;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.mapper.TicketMapper;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.impl.TicketTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    private Event mockEvent;
    private TicketType mockTicketType;
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockEvent = new Event();
        mockEvent.setId(1L);
        mockEvent.setTitle("Test Event");
        mockEvent.setImageUrl("test.jpg");

        mockTicketType = new TicketType();
        mockTicketType.setTicketTypeId(100L);
        mockTicketType.setName("Standard Ticket");
        mockTicketType.setEvent(mockEvent);
        mockTicketType.setPrice(new BigDecimal("50.00"));
        mockTicketType.setTotalQuantity(100);
        mockTicketType.setSoldQuantity(20);
        // Bán vé trong 1 tháng tới
        mockTicketType.setStartSaleDate(LocalDateTime.now().minusDays(1));
        mockTicketType.setEndSaleDate(LocalDateTime.now().plusMonths(1));
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
    @Test
    @DisplayName("Test getTicketTypeById - Found")
    void testGetTicketTypeById_Found() {
        // Given
        when(ticketTypeRepo.findById(100L)).thenReturn(Optional.of(mockTicketType));

        // When
        Optional<TicketType> result = ticketTypeService.getTicketTypeById(100L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Standard Ticket", result.get().getName());
    }

    @Test
    @DisplayName("Test getTicketTypeById - Not Found")
    void testGetTicketTypeById_NotFound() {
        // Given
        when(ticketTypeRepo.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<TicketType> result = ticketTypeService.getTicketTypeById(999L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test canPurchaseTickets - Can Purchase")
    void testCanPurchaseTickets_CanPurchase() {
        // Given (Total: 100, Sold: 20)
        when(ticketTypeRepo.findById(100L)).thenReturn(Optional.of(mockTicketType));

        // When (Muốn mua 10 vé)
        boolean canPurchase = ticketTypeService.canPurchaseTickets(100L, 10);

        // Then
        assertTrue(canPurchase);
    }


    @Test
    @DisplayName("Test canPurchaseTickets - Not Found")
    void testCanPurchaseTickets_NotFound() {
        // Given
        when(ticketTypeRepo.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean canPurchase = ticketTypeService.canPurchaseTickets(999L, 1);

        // Then
        assertFalse(canPurchase);
    }

    @Test
    @DisplayName("Test releaseTickets - Success")
    void testReleaseTickets_Success() {
        // Given (Sold: 20)
        when(ticketTypeRepo.findById(100L)).thenReturn(Optional.of(mockTicketType));

        // When
        ticketTypeService.releaseTickets(100L, 5);

        // Then
        // 20 - 5 = 15
        assertEquals(15, mockTicketType.getSoldQuantity());
        verify(ticketTypeRepo, times(1)).save(mockTicketType);
    }

    @Test
    @DisplayName("Test getTotalSoldByEventId - Returns value")
    void testGetTotalSoldByEventId_ReturnsValue() {
        // Given
        when(ticketTypeRepo.getTotalSoldByEventId(1L)).thenReturn(150);

        // When
        Integer total = ticketTypeService.getTotalSoldByEventId(1L);

        // Then
        assertEquals(150, total);
    }

    @Test
    @DisplayName("Test getTotalSoldByEventId - Returns 0 when null")
    void testGetTotalSoldByEventId_ReturnsZeroWhenNull() {
        // Given
        when(ticketTypeRepo.getTotalSoldByEventId(1L)).thenReturn(null);

        // When
        Integer total = ticketTypeService.getTotalSoldByEventId(1L);

        // Then
        assertEquals(0, total);
    }

    @Test
    @DisplayName("Test saveTicketType - Success")
    void testSaveTicketType() {
        // Given
        when(ticketTypeRepo.save(any(TicketType.class))).thenReturn(mockTicketType);

        // When
        TicketType saved = ticketTypeService.saveTicketType(mockTicketType);

        // Then
        assertNotNull(saved);
        assertEquals(100L, saved.getTicketTypeId());
        verify(ticketTypeRepo, times(1)).save(mockTicketType);
    }


    @Test
    @DisplayName("Test convertToDTO - Is Available and No Sale")
    void testConvertToDTO_IsAvailable_NoSale() {
        // Given (Sử dụng mockTicketType từ setup)

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        assertTrue(dto.isAvailable());
        assertEquals(new BigDecimal("50.00"), dto.getPrice());
        assertEquals(new BigDecimal("0"), dto.getSale());
        assertEquals(new BigDecimal("50.00"), dto.getFinalPrice());
        assertEquals(80, dto.getAvailableQuantity());
        assertEquals(1L, dto.getEventId());
        assertEquals("Test Event", dto.getEventTitle());
    }

    @Test
    @DisplayName("Test convertToDTO - Is Available and With Sale")
    void testConvertToDTO_IsAvailable_WithSale() {
        // Given
        mockTicketType.setSale(new BigDecimal("10.00")); // Giảm 10

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        assertTrue(dto.isAvailable());
        assertEquals(new BigDecimal("50.00"), dto.getPrice());
        assertEquals(new BigDecimal("10.00"), dto.getSale());
        assertEquals(new BigDecimal("45.00"), dto.getFinalPrice()); // 50 - 10
    }

    @Test
    @DisplayName("Test convertToDTO - Not Available (Sold Out)")
    void testConvertToDTO_NotAvailable_SoldOut() {
        // Given
        mockTicketType.setSoldQuantity(100); // Hết vé

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        assertFalse(dto.isAvailable());
        assertEquals(0, dto.getAvailableQuantity());
    }

    @Test
    @DisplayName("Test convertToDTO - Not Available (Sale Ended)")
    void testConvertToDTO_NotAvailable_SaleEnded() {
        // Given
        mockTicketType.setEndSaleDate(LocalDateTime.now().minusMinutes(1)); // Hết hạn bán

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        assertFalse(dto.isAvailable());
    }

    @Test
    @DisplayName("Test getTicketTypeDTOsByEventId - Success")
    void testGetTicketTypeDTOsByEventId() {
        // Given
        when(ticketTypeRepo.findByEventId(1L)).thenReturn(List.of(mockTicketType));
        // Phương thức này sẽ gọi convertToDTO nội bộ,
        // vì vậy chúng ta đang test cả hai trong một lần

        // When
        List<TicketTypeDTO> dtoList = ticketTypeService.getTicketTypeDTOsByEventId(1L);

        // Then
        assertEquals(1, dtoList.size());
        TicketTypeDTO dto = dtoList.get(0);

        assertEquals(100L, dto.getTicketTypeId());
        assertEquals("Standard Ticket", dto.getName());
        assertTrue(dto.isAvailable());
    }
    @Test
    @DisplayName("Test convertToDTO - Branch: 'saleNotStarted' is true")
    void testConvertToDTO_Branch_SaleNotStarted() {
        // Given
        // Cài đặt ngày bán vé trong tương lai
        mockTicketType.setStartSaleDate(LocalDateTime.now().plusDays(5));

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        // Xác minh nhánh 'if (saleNotStarted)' đã chạy
        assertTrue(dto.isSaleNotStarted());
        assertNotNull(dto.getSaleStartCountdownText());
        assertFalse(dto.isAvailable()); // Chưa có sẵn vì chưa tới ngày bán

        // Xác minh nội dung countdown text (ví dụ: "5 days")
        assertTrue(dto.getSaleStartCountdownText().contains("5 days") || dto.getSaleStartCountdownText().contains("4 days"));
    }

    @Test
    @DisplayName("Test convertToDTO - Branch: 'getSale()' is null")
    void testConvertToDTO_Branch_SaleIsNull() {
        // Given
        mockTicketType.setSale(null); // Set sale là null
        mockTicketType.setPrice(new BigDecimal("100.00"));

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        // Xác minh logic (ticketType.getSale() != null ? ticketType.getSale() : BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, dto.getSale());
        // Giá cuối cùng bằng giá gốc vì không giảm giá
        assertEquals(new BigDecimal("100.00"), dto.getFinalPrice());
    }

    @Test
    @DisplayName("Test convertToDTO - Branch: 'catch (Exception e)'")
    void testConvertToDTO_Branch_CatchesException() {
        // Given
        // Gây ra NullPointerException bằng cách set event là null
        mockTicketType.setEvent(null);

        // When & Then
        // Xác minh rằng phương thức đã ném ra lỗi (thay vì trả về null)
        assertThrows(Exception.class, () -> {
            ticketTypeService.convertToDTO(mockTicketType);
        });

        // (Trong console, bạn cũng sẽ thấy "Error converting TicketType to DTO...")
    }

    @Test
    @DisplayName("Test convertToDTO - Branch: 'isSoldOut' is true")
    void testConvertToDTO_Branch_IsSoldOut() {
        // Given
        mockTicketType.setTotalQuantity(100);
        mockTicketType.setSoldQuantity(100); // Đã bán hết

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        // Xác minh các boolean liên quan
        assertTrue(dto.isSoldOut());
        assertFalse(dto.isAvailable());
        assertEquals(0, dto.getAvailableQuantity());
    }

    @Test
    @DisplayName("Test convertToDTO - Branch: 'saleOverdue' is true")
    void testConvertToDTO_Branch_SaleOverdue() {
        // Given
        mockTicketType.setEndSaleDate(LocalDateTime.now().minusDays(1L)); // Đã hết hạn bán

        // When
        TicketTypeDTO dto = ticketTypeService.convertToDTO(mockTicketType);

        // Then
        assertTrue(dto.isSaleOverdue());
        assertFalse(dto.isAvailable());
    }
    @Test
    @DisplayName("Test reserveTickets - Success (Logic ngầm 1 vé)")
    void testReserveTickets_Success() {
        // Given
        // 1. Tạo một MOCK TicketType, KHÔNG dùng "mockTicketType" từ setUp
        TicketType ticketToReturn = mock(TicketType.class);

        // 2. Giả lập repository trả về MOCK này
        when(ticketTypeRepo.findById(100L)).thenReturn(Optional.of(ticketToReturn));

        // 3. Giờ bạn có thể dùng when() trên MOCK này (SỬA LỖI)
        when(ticketToReturn.canPurchase()).thenReturn(true);

        // When
        ticketTypeService.reserveTickets(100L);

        // Then
        // Xác minh rằng các phương thức trên MOCK đã được gọi
        verify(ticketToReturn, times(1)).increaseSoldQuantity();
        verify(ticketTypeRepo, times(1)).save(ticketToReturn);
    }

    @Test
    @DisplayName("Test reserveTickets - Throws IllegalStateException when cannot purchase")
    void testReserveTickets_ThrowsException() {
        // Given
        // 1. Tạo một MOCK TicketType, KHÔNG dùng "mockTicketType" từ setUp
        TicketType ticketToReturn = mock(TicketType.class);

        // 2. Giả lập repository trả về MOCK này
        when(ticketTypeRepo.findById(100L)).thenReturn(Optional.of(ticketToReturn));

        // 3. Giờ bạn có thể dùng when() trên MOCK này (SỬA LỖI)
        when(ticketToReturn.canPurchase()).thenReturn(false); // canPurchase() trả về false

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            ticketTypeService.reserveTickets(100L);
        });

        // Đảm bảo nó không lưu lại gì cả
        verify(ticketTypeRepo, never()).save(any());
    }

    @Test
    @DisplayName("Test getTicketTypesPageable - (Sửa lỗi import)")
    void testGetTicketTypesPageable() {
        // Given
        // Dùng import đúng của Spring
        Pageable pageable =  PageRequest.of(0, 5);
        Page<TicketType> mockPage = new PageImpl<>(List.of(mockTicketType), pageable, 1);

        when(ticketTypeRepo.findAll( pageable)).thenReturn(mockPage);

        // When
        Page<TicketType> result = ticketTypeService.getTicketTypesPageable(pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals("Standard Ticket", result.getContent().get(0).getName());
        verify(ticketTypeRepo, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Test getAllTicketTypes - (Hàm bị thiếu)")
    void testGetAllTicketTypes() {
        // Given
        when(ticketTypeRepo.findAll()).thenReturn(List.of(mockTicketType));

        // When
        List<TicketType> result = ticketTypeService.getAllTicketTypes();

        // Then
        assertEquals(1, result.size());
        verify(ticketTypeRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("Test getAvailableTicketTypeDTOsByEventId - (Hàm bị thiếu)")
    void testGetAvailableTicketTypeDTOsByEventId() {
        // Given
        when(ticketTypeRepo.findAvailableByEventIdIgnoreTime(1L)).thenReturn(List.of(mockTicketType));

        // When
        List<TicketTypeDTO> result = ticketTypeService.getAvailableTicketTypeDTOsByEventId(1L);

        // Then
        assertEquals(1, result.size());
        // Test này cũng tự động test convertToDTO
        assertEquals("Standard Ticket", result.get(0).getName());
        verify(ticketTypeRepo, times(1)).findAvailableByEventIdIgnoreTime(1L);
    }
}
