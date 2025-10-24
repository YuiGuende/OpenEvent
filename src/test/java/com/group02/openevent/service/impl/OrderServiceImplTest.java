package com.group02.openevent.service.impl;

import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.TicketTypeService;
import com.group02.openevent.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested; // Import Nested
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 * ĐÃ ĐƯỢC REFACTOR VỚI @NESTED
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    // --- Class Under Test ---
    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    // --- Mocks for Dependencies ---
    @Mock
    private IOrderRepo orderRepo;
    @Mock
    private IEventRepo eventRepo;
    @Mock
    private ITicketTypeRepo ticketTypeRepo;
    @Mock
    private TicketTypeService ticketTypeService;
    @Mock
    private VoucherService voucherService;

    // --- Test Data and Constants ---
    private Customer customer;
    private Event event;
    private Host host;
    private TicketType ticketType;
    private CreateOrderWithTicketTypeRequest request;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long TICKET_TYPE_ID = 20L;
    private static final Long ORDER_ID = 30L;

    /**
     * Sets up common mocks and test data before each test.
     * This corresponds to the "Given" section of many happy-path tests.
     */
    @BeforeEach
    void setUp() {
        // 1. Setup default Customer
        customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setEmail("test@customer.com");

        // 2. Setup default Host
        host = new Host();
        host.setId(100L);
        host.setHostDiscountPercent(BigDecimal.ZERO); // Default no discount

        // 3. Setup default Event
        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");
        event.setHost(host);

        // 4. Setup default TicketType
        ticketType = new TicketType();
        ticketType.setTicketTypeId(TICKET_TYPE_ID);
        ticketType.setEvent(event);
        ticketType.setPrice(BigDecimal.valueOf(100.0)); // Base price
        ticketType.setTotalQuantity(50);

        // 5. Setup default Request
        request = new CreateOrderWithTicketTypeRequest();
        request.setEventId(EVENT_ID);
        request.setTicketTypeId(TICKET_TYPE_ID);
        request.setParticipantName("Test Participant");
        request.setParticipantEmail("participant@test.com");

        // 6. Common Mocks (cho happy path)
        when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(ticketTypeRepo.findById(TICKET_TYPE_ID)).thenReturn(Optional.of(ticketType));
        when(ticketTypeService.canPurchaseTickets(TICKET_TYPE_ID, 1)).thenReturn(true);

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            if (savedOrder.getOrderId() == null) {
                savedOrder.setOrderId(ORDER_ID);
            }
            savedOrder.calculateTotalAmount();
            return savedOrder;
        });
    }

    // =================================================================
    // Tests cho hàm: createOrderWithTicketTypes
    // =================================================================
    @Nested
    @DisplayName("Tests cho hàm: createOrderWithTicketTypes")
    class CreateOrderWithTicketTypesTests {

        @Nested
        @DisplayName("Happy Paths")
        class HappyPaths {
            @Test
            @DisplayName("TC01: Happy Path - Create order without voucher")
            void tc01_createOrder_happyPath_noVoucher() {
                // Arrange
                request.setVoucherCode(null); // No voucher
                ticketType.setPrice(BigDecimal.valueOf(150.0));

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
                assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
                assertThat(result.getCustomer()).isEqualTo(customer);
                assertThat(result.getEvent()).isEqualTo(event);
                assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(165.0));

                // Verify mocks
                verify(ticketTypeService).reserveTickets(TICKET_TYPE_ID);
                verify(orderRepo, times(1)).save(any(Order.class));
                verify(voucherService, never()).applyVoucherToOrder(anyString(), any(Order.class));
            }

            @Test
            @DisplayName("TC02: Happy Path - Create order with valid voucher")
            void tc02_createOrder_happyPath_withValidVoucher() {
                // Arrange
                request.setVoucherCode("SALE10");
                ticketType.setPrice(BigDecimal.valueOf(200.0));
                doAnswer(invocation -> {
                    Order order = invocation.getArgument(1);
                    order.setVoucherDiscountAmount(BigDecimal.valueOf(20.0));
                    return null;
                }).when(voucherService).applyVoucherToOrder(eq("SALE10"), any(Order.class));

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
                assertThat(result.getVoucherDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
                assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(198.0));

                // Verify mocks
                verify(ticketTypeService).reserveTickets(TICKET_TYPE_ID);
                verify(voucherService).applyVoucherToOrder(eq("SALE10"), any(Order.class));
                verify(orderRepo, times(2)).save(any(Order.class));
            }
        }

        @Nested
        @DisplayName("Error Handling (Lỗi đầu vào & Điều kiện thất bại)")
        class ErrorHandling {
            @Test
            @DisplayName("TC03: Error - Event not found")
            void tc03_createOrder_error_eventNotFound() {
                // Arrange
                long NON_EXISTENT_ID = 99L;
                request.setEventId(NON_EXISTENT_ID);
                when(eventRepo.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> orderServiceImpl.createOrderWithTicketTypes(request, customer))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Event not found");

                verify(orderRepo, never()).save(any());
            }

            @Test
            @DisplayName("TC04: Error - No ticket type in request")
            void tc04_createOrder_error_noTicketType() {
                // Arrange
                request.setTicketTypeId(null);

                // Act & Assert
                assertThatThrownBy(() -> orderServiceImpl.createOrderWithTicketTypes(request, customer))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("At least one ticket type must be specified");

                verify(orderRepo, never()).save(any());
            }

            @Test
            @DisplayName("TC05: Error - Ticket type not found")
            void tc05_createOrder_error_ticketTypeNotFound() {
                // Arrange
                long NON_EXISTENT_ID = 99L;
                request.setTicketTypeId(NON_EXISTENT_ID);
                when(ticketTypeRepo.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> orderServiceImpl.createOrderWithTicketTypes(request, customer))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Ticket type not found");

                verify(orderRepo, never()).save(any());
            }

            @Test
            @DisplayName("TC06: Error - Ticket unavailable")
            void tc06_createOrder_error_ticketUnavailable() {
                // Arrange
                when(ticketTypeService.canPurchaseTickets(TICKET_TYPE_ID, 1)).thenReturn(false);

                // Act & Assert
                assertThatThrownBy(() -> orderServiceImpl.createOrderWithTicketTypes(request, customer))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Cannot purchase ticket");

                verify(orderRepo, never()).save(any());
            }

            @Test
            @DisplayName("TC09: Error - Exception during reserveTickets")
            void tc09_createOrder_error_reserveTicketsFails() {
                // Arrange
                doThrow(new IllegalStateException("Reservation database locked"))
                        .when(ticketTypeService).reserveTickets(TICKET_TYPE_ID);

                // Act & Assert
                assertThatThrownBy(() -> orderServiceImpl.createOrderWithTicketTypes(request, customer))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Reservation database locked");

                verify(orderRepo, never()).save(any(Order.class));
            }

            @Test
            @DisplayName("TC12: Error - Giá vé bị null (Lỗi NullPointerException)")
            void tc12_createOrder_error_nullTicketPrice() {
                // Arrange
                ticketType.setPrice(null);

                // Act & Assert
                assertThatThrownBy(() -> orderServiceImpl.createOrderWithTicketTypes(request, customer))
                        .isInstanceOf(NullPointerException.class);

                verify(orderRepo, never()).save(any(Order.class));
            }
        }

        @Nested
        @DisplayName("Edge Cases & Special Logic")
        class EdgeCases {

            @Test
            @DisplayName("TC07: Edge - VoucherService throws exception (đã catch)")
            void tc07_createOrder_edge_voucherServiceThrowsException() {
                // Arrange
                request.setVoucherCode("EXPIRED10");
                ticketType.setPrice(BigDecimal.valueOf(300.0));
                doThrow(new RuntimeException("Voucher expired"))
                        .when(voucherService).applyVoucherToOrder(eq("EXPIRED10"), any(Order.class));

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
                // Total = 300.0 (price) + 30.0 (VAT) = 330.0 (không được giảm)
                assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(330.0));

                // Verify
                verify(voucherService).applyVoucherToOrder(eq("EXPIRED10"), any(Order.class));
                // Chỉ save 1 lần, lần 2 bị catch
                verify(orderRepo, times(1)).save(any(Order.class));
            }

            @Test
            @DisplayName("TC08: Edge - Host discount applied")
            void tc08_createOrder_edge_hostDiscountApplied() {
                // Arrange
                host.setHostDiscountPercent(BigDecimal.valueOf(20.0)); // 20% discount
                ticketType.setPrice(BigDecimal.valueOf(300.0));
                request.setVoucherCode(null);

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getHostDiscountPercent()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
                // Total: (300.0 - 60.0) * 1.1 = 264.0
                assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(264.0));

                verify(orderRepo, times(1)).save(any(Order.class));
            }

            @Test
            @DisplayName("TC10: Edge - Tổng chiết khấu (Host + Voucher) lớn hơn giá vé")
            void tc10_createOrder_edge_discountsGreaterThanPrice() {
                // Arrange
                // 1. Giá vé 100.0 (từ setUp)
                // 2. Chiết khấu Host 20% (20.0)
                host.setHostDiscountPercent(BigDecimal.valueOf(20.0));
                // 3. Voucher 90.0
                request.setVoucherCode("BIGSALE");
                doAnswer(invocation -> {
                    Order order = invocation.getArgument(1);
                    order.setVoucherDiscountAmount(BigDecimal.valueOf(90.0));
                    return null;
                }).when(voucherService).applyVoucherToOrder(eq("BIGSALE"), any(Order.class));

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                // Total discount (110.0) > Price (100.0) => Total = 0
                assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(result.getOriginalPrice()).isEqualByComparingTo("100.0");
                assertThat(result.getHostDiscountAmount()).isEqualByComparingTo("20.0");
                assertThat(result.getVoucherDiscountAmount()).isEqualByComparingTo("90.0");

                verify(orderRepo, times(2)).save(any(Order.class));
            }

            @Test
            @DisplayName("TC11: Edge - Vé miễn phí (Giá vé = 0)")
            void tc11_createOrder_edge_freeTicket() {
                // Arrange
                ticketType.setPrice(BigDecimal.ZERO);
                host.setHostDiscountPercent(BigDecimal.valueOf(10.0));
                request.setVoucherCode("FREESALE");
                doAnswer(invocation -> {
                    Order order = invocation.getArgument(1);
                    order.setVoucherDiscountAmount(BigDecimal.valueOf(10.0));
                    return null;
                }).when(voucherService).applyVoucherToOrder(eq("FREESALE"), any(Order.class));

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(result.getOriginalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(result.getHostDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);

                verify(orderRepo, times(2)).save(any(Order.class));
            }

            @Test
            @DisplayName("TC13: Edge - Mã Voucher chỉ chứa khoảng trắng (Whitespace)")
            void tc13_createOrder_edge_whitespaceVoucherCode() {
                // Arrange
                request.setVoucherCode("    ");

                // Act
                Order result = orderServiceImpl.createOrderWithTicketTypes(request, customer);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

                // Verify
                verify(voucherService, never()).applyVoucherToOrder(anyString(), any(Order.class));
                verify(orderRepo, times(1)).save(any(Order.class));
            }
        }
    }


    // =================================================================
    // Tests cho hàm: hasCustomerRegisteredForEvent
    // =================================================================
    @Nested
    @DisplayName("Tests cho hàm: hasCustomerRegisteredForEvent")
    class HasCustomerRegisteredForEventTests {

        @Test
        @DisplayName("TC14: hasRegistered - Trả về true khi có đơn PAID")
        void tc14_hasCustomerRegistered_ShouldReturnTrue_WhenPaidOrderExists() {
            // Arrange
            Order paid = new Order();
            paid.setCustomer(customer);
            paid.setEvent(event);
            paid.setStatus(OrderStatus.PAID);

            when(orderRepo.findByCustomerId(customer.getCustomerId()))
                    .thenReturn(List.of(paid));

            // Act
            boolean result = orderServiceImpl.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

            // Assert
            assertTrue(result, "PAID order should be recognized as registered");
        }

        @Test
        @DisplayName("TC15: hasRegistered - Trả về false khi chỉ có đơn PENDING")
        void tc15_hasCustomerRegistered_ShouldReturnFalse_WhenOnlyPendingOrderExists() {
            // Arrange
            Order pending = new Order();
            pending.setCustomer(customer);
            pending.setEvent(event);
            pending.setStatus(OrderStatus.PENDING);

            when(orderRepo.findByCustomerId(customer.getCustomerId()))
                    .thenReturn(List.of(pending));

            // Act
            boolean result = orderServiceImpl.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

            // Assert
            assertFalse(result, "Pending order should not count as registered");
        }

        @Test
        @DisplayName("TC16: hasRegistered - Trả về false khi chỉ có đơn CANCELLED")
        void tc16_hasCustomerRegistered_ShouldReturnFalse_WhenOnlyCancelledOrderExists() {
            // Arrange
            Order cancelled = new Order();
            cancelled.setCustomer(customer);
            cancelled.setEvent(event);
            cancelled.setStatus(OrderStatus.CANCELLED);

            when(orderRepo.findByCustomerId(customer.getCustomerId()))
                    .thenReturn(List.of(cancelled));

            // Act
            boolean result = orderServiceImpl.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

            // Assert
            assertFalse(result, "Cancelled order should not count as registered");
        }

        @Test
        @DisplayName("TC17: hasRegistered - Trả về false khi không có đơn nào")
        void tc17_hasCustomerRegistered_ShouldReturnFalse_WhenNoOrdersExist() {
            // Arrange
            when(orderRepo.findByCustomerId(customer.getCustomerId()))
                    .thenReturn(List.of());

            // Act
            boolean result = orderServiceImpl.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

            // Assert
            assertFalse(result, "No orders means not registered");
        }

        @Test
        @DisplayName("TC18: hasRegistered - Trả về true khi có nhiều đơn (PENDING, PAID)")
        void tc18_hasCustomerRegistered_ShouldReturnTrue_WhenMixedOrdersIncludePaid() {
            // Arrange
            Order paid = new Order();
            paid.setCustomer(customer);
            paid.setEvent(event);
            paid.setStatus(OrderStatus.PAID);
            Order pending1 = new Order();
            pending1.setCustomer(customer);
            pending1.setEvent(event);
            pending1.setStatus(OrderStatus.PENDING);

            when(orderRepo.findByCustomerId(customer.getCustomerId()))
                    .thenReturn(List.of(paid, pending1));

            // Act
            boolean result = orderServiceImpl.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

            // Assert
            assertTrue(result, "Any PAID order should make result true");
        }
    }
}