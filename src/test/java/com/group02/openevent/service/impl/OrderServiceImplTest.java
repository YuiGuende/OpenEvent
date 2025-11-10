package com.group02.openevent.service.impl;

import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.dto.user.UserOrderDTO;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.TicketTypeService;
import com.group02.openevent.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

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

    private Customer customer;
    private Event event;
    private Order order;
    private TicketType ticketType;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long ORDER_ID = 30L;
    private static final Long TICKET_TYPE_ID = 20L;

    @BeforeEach
    void setUp() {
        Account account = new Account();
        account.setAccountId(1L);
        account.setEmail("test@example.com");
        User user = new User();
        user.setAccount(account);
        user.setUserId(1L);
        user.setName("Test Customer");
        customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setUser(user);

        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");

        ticketType = new TicketType();
        ticketType.setTicketTypeId(TICKET_TYPE_ID);
        ticketType.setPrice(BigDecimal.valueOf(100.0));
        ticketType.setEvent(event);

        order = new Order();
        order.setOrderId(ORDER_ID);
        order.setCustomer(customer);
        order.setEvent(event);
        order.setTicketType(ticketType);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.valueOf(110.0));
    }

    @Nested
    @DisplayName("createOrder Tests")
    class CreateOrderTests {
        @Test
        @DisplayName("TC-01: Create order successfully")
        void createOrder_Success() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest();
            request.setEventId(EVENT_ID);
            request.setParticipantName("John Doe");
            request.setParticipantEmail("john@example.com");
            request.setParticipantPhone("1234567890");
            request.setParticipantOrganization("Test Org");
            request.setNotes("Test notes");

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setOrderId(ORDER_ID);
                return saved;
            });

            // Act
            Order result = orderService.createOrder(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(result.getEvent()).isEqualTo(event);
            assertThat(result.getParticipantName()).isEqualTo("John Doe");
            assertThat(result.getParticipantEmail()).isEqualTo("john@example.com");
            verify(eventRepo).findById(EVENT_ID);
            verify(orderRepo).save(any(Order.class));
        }

        @Test
        @DisplayName("TC-02: Create order fails when event not found")
        void createOrder_EventNotFound() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest();
            request.setEventId(999L);

            when(eventRepo.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event not found");
            verify(orderRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById Tests")
    class GetByIdTests {
        @Test
        @DisplayName("TC-03: Get order by ID successfully")
        void getById_Success() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));

            // Act
            Optional<Order> result = orderService.getById(ORDER_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(order);
            verify(orderRepo).findById(ORDER_ID);
        }

        @Test
        @DisplayName("TC-04: Get order by ID returns empty when not found")
        void getById_NotFound() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act
            Optional<Order> result = orderService.getById(ORDER_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(orderRepo).findById(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("list Tests")
    class ListTests {
        @Test
        @DisplayName("TC-05: List orders with pagination")
        void list_WithPagination() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> expectedPage = new PageImpl<>(List.of(order), pageable, 1);

            when(orderRepo.findAll(pageable)).thenReturn(expectedPage);

            // Act
            Page<Order> result = orderService.list(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(order);
            verify(orderRepo).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {
        @Test
        @DisplayName("TC-06: Delete order successfully")
        void delete_Success() {
            // Arrange
            doNothing().when(orderRepo).deleteById(ORDER_ID);

            // Act
            orderService.delete(ORDER_ID);

            // Assert
            verify(orderRepo).deleteById(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("cancelOrder Tests")
    class CancelOrderTests {
        @Test
        @DisplayName("TC-07: Cancel pending order successfully")
        void cancelOrder_Pending_Success() {
            // Arrange
            order.setStatus(OrderStatus.PENDING);
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepo.save(any(Order.class))).thenReturn(order);
            doNothing().when(ticketTypeService).releaseTickets(TICKET_TYPE_ID, 1);

            // Act
            orderService.cancelOrder(ORDER_ID);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(ticketTypeService).releaseTickets(TICKET_TYPE_ID, 1);
            verify(orderRepo).save(order);
        }

        @Test
        @DisplayName("TC-08: Cancel order fails when order not found")
        void cancelOrder_OrderNotFound() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("TC-09: Cancel order fails when status is not PENDING")
        void cancelOrder_InvalidStatus() {
            // Arrange
            order.setStatus(OrderStatus.PAID);
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel order");
        }

        @Test
        @DisplayName("TC-10: Cancel order without ticket type")
        void cancelOrder_NoTicketType() {
            // Arrange
            order.setStatus(OrderStatus.PENDING);
            order.setTicketType(null);
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepo.save(any(Order.class))).thenReturn(order);

            // Act
            orderService.cancelOrder(ORDER_ID);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(ticketTypeService, never()).releaseTickets(any(), any());
            verify(orderRepo).save(order);
        }
    }

    @Nested
    @DisplayName("confirmOrder Tests")
    class ConfirmOrderTests {
        @Test
        @DisplayName("TC-11: Confirm pending order successfully")
        void confirmOrder_Pending_Success() {
            // Arrange
            order.setStatus(OrderStatus.PENDING);
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepo.save(any(Order.class))).thenReturn(order);
            doNothing().when(ticketTypeService).confirmPurchase(TICKET_TYPE_ID, 1);

            // Act
            orderService.confirmOrder(ORDER_ID);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(ticketTypeService).confirmPurchase(TICKET_TYPE_ID, 1);
            verify(orderRepo).save(order);
        }

        @Test
        @DisplayName("TC-12: Confirm order fails when order not found")
        void confirmOrder_OrderNotFound() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("TC-13: Confirm order fails when status is not PENDING")
        void confirmOrder_InvalidStatus() {
            // Arrange
            order.setStatus(OrderStatus.PAID);
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot confirm order");
        }

        @Test
        @DisplayName("TC-14: Confirm order without ticket type")
        void confirmOrder_NoTicketType() {
            // Arrange
            order.setStatus(OrderStatus.PENDING);
            order.setTicketType(null);
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepo.save(any(Order.class))).thenReturn(order);

            // Act
            orderService.confirmOrder(ORDER_ID);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(ticketTypeService, never()).confirmPurchase(any(), any());
            verify(orderRepo).save(order);
        }
    }

    @Nested
    @DisplayName("save Tests")
    class SaveTests {
        @Test
        @DisplayName("TC-15: Save order successfully")
        void save_Success() {
            // Arrange
            when(orderRepo.save(order)).thenReturn(order);

            // Act
            Order result = orderService.save(order);

            // Assert
            assertThat(result).isEqualTo(order);
            verify(orderRepo).save(order);
        }
    }

    @Nested
    @DisplayName("getPendingOrderForEvent Tests")
    class GetPendingOrderForEventTests {
        @Test
        @DisplayName("TC-16: Get pending order successfully")
        void getPendingOrderForEvent_Success() {
            // Arrange
            order.setStatus(OrderStatus.PENDING);
            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(order)));

            // Act
            Optional<Order> result = orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(order);
        }

        @Test
        @DisplayName("TC-17: Get pending order returns empty when not found")
        void getPendingOrderForEvent_NotFound() {
            // Arrange
            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>());

            // Act
            Optional<Order> result = orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TC-18: Get pending order returns empty when order is PAID")
        void getPendingOrderForEvent_OrderIsPaid() {
            // Arrange
            order.setStatus(OrderStatus.PAID);
            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(order)));

            // Act
            Optional<Order> result = orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOrdersByCustomer Tests")
    class GetOrdersByCustomerTests {
        @Test
        @DisplayName("TC-19: Get orders by customer successfully")
        void getOrdersByCustomer_Success() {
            // Arrange
            when(orderRepo.findByCustomer(customer)).thenReturn(new ArrayList<>(List.of(order)));

            // Act
            List<Order> result = orderService.getOrdersByCustomer(customer);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(order);
            verify(orderRepo).findByCustomer(customer);
        }
    }

    @Nested
    @DisplayName("getOrdersByCustomerId Tests")
    class GetOrdersByCustomerIdTests {
        @Test
        @DisplayName("TC-20: Get orders by customer ID successfully")
        void getOrdersByCustomerId_Success() {
            // Arrange
            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(order)));

            // Act
            List<Order> result = orderService.getOrdersByCustomerId(CUSTOMER_ID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(order);
            verify(orderRepo).findByCustomerId(CUSTOMER_ID);
        }
    }

    @Nested
    @DisplayName("getOrderDTOsByCustomerId Tests")
    class GetOrderDTOsByCustomerIdTests {
        @Test
        @DisplayName("TC-21: Get order DTOs by customer ID with status filter")
        void getOrderDTOsByCustomerId_WithStatusFilter() {
            // Arrange
            order.setStatus(OrderStatus.PAID);
            Order pendingOrder = new Order();
            pendingOrder.setOrderId(2L);
            pendingOrder.setStatus(OrderStatus.PENDING);
            pendingOrder.setEvent(event);
            pendingOrder.setCreatedAt(LocalDateTime.now());

            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(order, pendingOrder)));

            // Act
            List<UserOrderDTO> result = orderService.getOrderDTOsByCustomerId(CUSTOMER_ID, OrderStatus.PAID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("TC-22: Get order DTOs by customer ID without status filter")
        void getOrderDTOsByCustomerId_WithoutStatusFilter() {
            // Arrange
            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(order)));

            // Act
            List<UserOrderDTO> result = orderService.getOrderDTOsByCustomerId(CUSTOMER_ID, null);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("getOrderDTOsByCustomer Tests")
    class GetOrderDTOsByCustomerTests {
        @Test
        @DisplayName("TC-23: Get order DTOs by customer successfully")
        void getOrderDTOsByCustomer_Success() {
            // Arrange
            when(orderRepo.findByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(order)));

            // Act
            List<UserOrderDTO> result = orderService.getOrderDTOsByCustomer(customer, OrderStatus.PENDING);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderId()).isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("countUniqueParticipantsByEventId Tests")
    class CountUniqueParticipantsByEventIdTests {
        @Test
        @DisplayName("TC-24: Count unique participants successfully")
        void countUniqueParticipantsByEventId_Success() {
            // Arrange
            when(orderRepo.countConfirmedParticipantsByEventId(EVENT_ID)).thenReturn(5);

            // Act
            Integer result = orderService.countUniqueParticipantsByEventId(EVENT_ID);

            // Assert
            assertThat(result).isEqualTo(5);
            verify(orderRepo).countConfirmedParticipantsByEventId(EVENT_ID);
        }
    }

    @Nested
    @DisplayName("findConfirmedEventsByCustomerId Tests")
    class FindConfirmedEventsByCustomerIdTests {
        @Test
        @DisplayName("TC-25: Find confirmed events successfully")
        void findConfirmedEventsByCustomerId_Success() {
            // Arrange
            when(orderRepo.findEventsByCustomerId(CUSTOMER_ID)).thenReturn(new ArrayList<>(List.of(event)));

            // Act
            List<Event> result = orderService.findConfirmedEventsByCustomerId(CUSTOMER_ID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(event);
            verify(orderRepo).findEventsByCustomerId(CUSTOMER_ID);
        }
    }

    @Nested
    @DisplayName("createOrderWithTicketTypes Tests")
    class CreateOrderWithTicketTypesTests {
        private CreateOrderWithTicketTypeRequest request;
        private TicketType ticketType;

        @BeforeEach
        void setUp() {
            ticketType = new TicketType();
            ticketType.setTicketTypeId(1L);
            ticketType.setName("VIP");
            ticketType.setPrice(BigDecimal.valueOf(100000));
            ticketType.setTotalQuantity(10);
            ticketType.setSoldQuantity(0);

            request = new CreateOrderWithTicketTypeRequest();
            request.setEventId(EVENT_ID);
            request.setTicketTypeId(1L);
            request.setParticipantName("John Doe");
            request.setParticipantEmail("john@example.com");
            request.setParticipantPhone("0123456789");
            request.setParticipantOrganization("Org");
            request.setNotes("Test notes");
        }

        @Test
        @DisplayName("TC-26: Create order with ticket types successfully")
        void createOrderWithTicketTypes_Success() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(ticketTypeRepo.findById(1L)).thenReturn(Optional.of(ticketType));
            when(ticketTypeService.canPurchaseTickets(1L, 1)).thenReturn(true);
            when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
                Order savedOrder = invocation.getArgument(0);
                savedOrder.setOrderId(ORDER_ID);
                return savedOrder;
            });

            // Act
            Order result = orderService.createOrderWithTicketTypes(request, customer);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(result.getCustomer()).isEqualTo(customer);
            assertThat(result.getEvent()).isEqualTo(event);
            assertThat(result.getTicketType()).isEqualTo(ticketType);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(eventRepo).findById(EVENT_ID);
            verify(ticketTypeRepo).findById(1L);
            verify(ticketTypeService).canPurchaseTickets(1L, 1);
            verify(ticketTypeService).reserveTickets(1L);
            verify(orderRepo, atLeast(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("TC-27: Create order with ticket types fails when event not found")
        void createOrderWithTicketTypes_EventNotFound() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrderWithTicketTypes(request, customer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event not found");
            verify(eventRepo).findById(EVENT_ID);
            verify(ticketTypeRepo, never()).findById(any());
            verify(orderRepo, never()).save(any());
        }

        @Test
        @DisplayName("TC-28: Create order with ticket types fails when ticket type ID is null")
        void createOrderWithTicketTypes_TicketTypeIdNull() {
            // Arrange
            request.setTicketTypeId(null);
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrderWithTicketTypes(request, customer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one ticket type must be specified");
            verify(eventRepo).findById(EVENT_ID);
            verify(ticketTypeRepo, never()).findById(any());
        }

        @Test
        @DisplayName("TC-29: Create order with ticket types fails when ticket type not found")
        void createOrderWithTicketTypes_TicketTypeNotFound() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(ticketTypeRepo.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrderWithTicketTypes(request, customer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ticket type not found");
            verify(eventRepo).findById(EVENT_ID);
            verify(ticketTypeRepo).findById(1L);
            verify(orderRepo, never()).save(any());
        }

        @Test
        @DisplayName("TC-30: Create order with ticket types fails when cannot purchase")
        void createOrderWithTicketTypes_CannotPurchase() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(ticketTypeRepo.findById(1L)).thenReturn(Optional.of(ticketType));
            when(ticketTypeService.canPurchaseTickets(1L, 1)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrderWithTicketTypes(request, customer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot purchase ticket");
            verify(ticketTypeService).canPurchaseTickets(1L, 1);
            verify(ticketTypeService, never()).reserveTickets(any());
            verify(orderRepo, never()).save(any());
        }

        @Test
        @DisplayName("TC-31: Create order with ticket types and voucher successfully")
        void createOrderWithTicketTypes_WithVoucher_Success() {
            // Arrange
            request.setVoucherCode("DISCOUNT10");
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(ticketTypeRepo.findById(1L)).thenReturn(Optional.of(ticketType));
            when(ticketTypeService.canPurchaseTickets(1L, 1)).thenReturn(true);
            when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
                Order savedOrder = invocation.getArgument(0);
                savedOrder.setOrderId(ORDER_ID);
                return savedOrder;
            });

            // Act
            Order result = orderService.createOrderWithTicketTypes(request, customer);

            // Assert
            assertThat(result).isNotNull();
            verify(voucherService).applyVoucherToOrder("DISCOUNT10", result);
            verify(orderRepo, atLeast(2)).save(any(Order.class)); // Save twice: once initial, once after voucher
        }

        @Test
        @DisplayName("TC-32: Create order with ticket types continues when voucher fails")
        void createOrderWithTicketTypes_VoucherFails_ContinuesWithoutVoucher() {
            // Arrange
            request.setVoucherCode("INVALID");
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(ticketTypeRepo.findById(1L)).thenReturn(Optional.of(ticketType));
            when(ticketTypeService.canPurchaseTickets(1L, 1)).thenReturn(true);
            when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
                Order savedOrder = invocation.getArgument(0);
                savedOrder.setOrderId(ORDER_ID);
                return savedOrder;
            });
            doThrow(new RuntimeException("Invalid voucher")).when(voucherService).applyVoucherToOrder(any(), any());

            // Act
            Order result = orderService.createOrderWithTicketTypes(request, customer);

            // Assert - order should still be created despite voucher failure
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            verify(voucherService).applyVoucherToOrder("INVALID", result);
        }
    }
}

