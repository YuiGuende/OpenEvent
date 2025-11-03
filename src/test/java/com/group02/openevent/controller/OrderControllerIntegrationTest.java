package com.group02.openevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("OrderController Integration Tests")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IOrderRepo orderRepo;
    @MockBean
    private SessionInterceptor sessionInterceptor;
    @MockBean
    private OrderService orderService;
    @MockBean
    private ICustomerRepo customerRepo;
    @MockBean
    private VoucherService voucherService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order order;
    private Customer customer;
    private Event event;

    private static final Long ORDER_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long CUSTOMER_ID = 100L;
    private static final Long ACCOUNT_ID = 200L;
    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AISecurityService aiSecurityService;
    @BeforeEach
    void setUp() throws Exception {
        Account account = new Account();
        account.setAccountId(ACCOUNT_ID);
        account.setEmail("test@example.com");

        customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setAccount(account);
        customer.setEmail("test@example.com");
        customer.setName("Test Customer");

        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");

        order = new Order();
        order.setOrderId(ORDER_ID);
        order.setCustomer(customer);
        order.setEvent(event);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(100000));

        // Mock SessionInterceptor để cho phép tất cả request đi qua
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {
        @Test
        @DisplayName("TC-01: Create order successfully")
        void create_Success() throws Exception {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest();
            request.setEventId(EVENT_ID);
            request.setParticipantName("John Doe");
            request.setParticipantEmail("john@example.com");

            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

            // Act & Assert
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(orderService).createOrder(any(CreateOrderRequest.class));
        }
    }

    @Nested
    @DisplayName("get Tests")
    class GetTests {
        @Test
        @DisplayName("TC-02: Get order by ID successfully")
        void get_Success() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));

            // Act & Assert
            mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(ORDER_ID));

            verify(orderService).getById(ORDER_ID);
        }

        @Test
        @DisplayName("TC-03: Get order returns 404 when not found")
        void get_NotFound() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("list Tests")
    class ListTests {
        @Test
        @DisplayName("TC-04: List orders with pagination")
        void list_WithPagination() throws Exception {
            // Arrange
            Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
            when(orderService.list(any())).thenReturn(page);

            // Act & Assert
            mockMvc.perform(get("/api/orders")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].orderId").value(ORDER_ID));

            verify(orderService).list(any());
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {
        @Test
        @DisplayName("TC-05: Delete order successfully")
        void delete_Success() throws Exception {
            // Arrange
            doNothing().when(orderService).delete(ORDER_ID);

            // Act & Assert
            mockMvc.perform(delete("/api/orders/{id}", ORDER_ID))
                    .andExpect(status().isNoContent());

            verify(orderService).delete(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("getMyOrders Tests")
    class GetMyOrdersTests {
        @Test
        @DisplayName("TC-06: Get my orders successfully")
        void getMyOrders_Success() throws Exception {
            // Arrange
            when(customerRepo.findByAccount_AccountId(ACCOUNT_ID)).thenReturn(Optional.of(customer));
            when(orderService.getOrdersByCustomer(customer)).thenReturn(List.of(order));

            // Act & Assert
            mockMvc.perform(get("/api/orders/my-orders")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orders").isArray())
                    .andExpect(jsonPath("$.orders[0].orderId").value(ORDER_ID));

            verify(orderService).getOrdersByCustomer(customer);
        }

        @Test
        @DisplayName("TC-07: Get my orders fails when not logged in")
        void getMyOrders_NotLoggedIn() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/orders/my-orders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }

        @Test
        @DisplayName("TC-08: Get my orders fails when customer not found")
        void getMyOrders_CustomerNotFound() throws Exception {
            // Arrange
            when(customerRepo.findByAccount_AccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/orders/my-orders")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Customer not found"));
        }
    }

    @Nested
    @DisplayName("cancelOrder Tests")
    class CancelOrderTests {
        @Test
        @DisplayName("TC-09: Cancel order successfully")
        void cancelOrder_Success() throws Exception {
            // Arrange
            doNothing().when(orderService).cancelOrder(ORDER_ID);

            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/cancel", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order cancelled successfully"));

            verify(orderService).cancelOrder(ORDER_ID);
        }

        @Test
        @DisplayName("TC-10: Cancel order fails when not logged in")
        void cancelOrder_NotLoggedIn() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/cancel", ORDER_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("TC-11: Cancel order fails when order not found")
        void cancelOrder_OrderNotFound() throws Exception {
            // Arrange
            doThrow(new IllegalArgumentException("Order not found"))
                    .when(orderService).cancelOrder(ORDER_ID);

            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/cancel", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("confirmOrder Tests")
    class ConfirmOrderTests {
        @Test
        @DisplayName("TC-12: Confirm order successfully")
        void confirmOrder_Success() throws Exception {
            // Arrange
            doNothing().when(orderService).confirmOrder(ORDER_ID);

            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/confirm", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order confirmed successfully"));

            verify(orderService).confirmOrder(ORDER_ID);
        }

        @Test
        @DisplayName("TC-13: Confirm order fails when not logged in")
        void confirmOrder_NotLoggedIn() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/confirm", ORDER_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("applyVoucher Tests")
    class ApplyVoucherTests {
        @Test
        @DisplayName("TC-14: Apply voucher successfully")
        void applyVoucher_Success() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));
            when(voucherService.applyVoucherToOrder(anyString(), any(Order.class)))
                    .thenReturn(new com.group02.openevent.model.voucher.VoucherUsage());
            when(orderService.save(any(Order.class))).thenReturn(order);

            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/apply-voucher", ORDER_ID)
                            .param("voucherCode", "SALE10")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Voucher applied successfully"));

            verify(voucherService).applyVoucherToOrder(eq("SALE10"), any(Order.class));
        }

        @Test
        @DisplayName("TC-15: Apply voucher fails when order not found")
        void applyVoucher_OrderNotFound() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/apply-voucher", ORDER_ID)
                            .param("voucherCode", "SALE10")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        @DisplayName("TC-16: Apply voucher fails when order doesn't belong to user")
        void applyVoucher_AccessDenied() throws Exception {
            // Arrange
            Account otherAccount = new Account();
            otherAccount.setAccountId(999L);
            Customer otherCustomer = new Customer();
            otherCustomer.setAccount(otherAccount);
            order.setCustomer(otherCustomer);

            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));

            // Act & Assert
            mockMvc.perform(post("/api/orders/{orderId}/apply-voucher", ORDER_ID)
                            .param("voucherCode", "SALE10")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }
    }

    @Nested
    @DisplayName("getAvailableVouchers Tests")
    class GetAvailableVouchersTests {
        @Test
        @DisplayName("TC-17: Get available vouchers successfully")
        void getAvailableVouchers_Success() throws Exception {
            // Arrange
            when(voucherService.getAvailableVouchers()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/orders/available-vouchers")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.vouchers").isArray());

            verify(voucherService).getAvailableVouchers();
        }

        @Test
        @DisplayName("TC-18: Get available vouchers fails when not logged in")
        void getAvailableVouchers_NotLoggedIn() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/orders/available-vouchers"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("checkVoucher Tests")
    class CheckVoucherTests {
        @Test
        @DisplayName("TC-19: Check voucher successfully - available")
        void checkVoucher_Available() throws Exception {
            // Arrange
            com.group02.openevent.model.voucher.Voucher voucher = new com.group02.openevent.model.voucher.Voucher();
            voucher.setVoucherId(1L);
            voucher.setCode("SALE10");
            voucher.setDiscountAmount(BigDecimal.valueOf(10000));
            voucher.setDescription("Discount 10,000 VND");
            when(voucherService.isVoucherAvailable("SALE10")).thenReturn(true);
            when(voucherService.getVoucherByCode("SALE10"))
                    .thenReturn(Optional.of(voucher));

            // Act & Assert
            mockMvc.perform(get("/api/orders/check-voucher")
                            .param("voucherCode", "SALE10")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.available").value(true));

            verify(voucherService).isVoucherAvailable("SALE10");
        }

        @Test
        @DisplayName("TC-20: Check voucher successfully - not available")
        void checkVoucher_NotAvailable() throws Exception {
            // Arrange
            when(voucherService.isVoucherAvailable("INVALID")).thenReturn(false);

            // Act & Assert
            mockMvc.perform(get("/api/orders/check-voucher")
                            .param("voucherCode", "INVALID")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.available").value(false));
        }
    }
}

