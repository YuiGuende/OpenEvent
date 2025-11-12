package com.group02.openevent.controller;

import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("PaymentController Integration Tests")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AISecurityService aiSecurityService;
    @MockBean
    private SessionInterceptor sessionInterceptor;
    @MockBean
    private PaymentService paymentService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private PayOS payOS;

    private Order order;
    private Payment payment;
    private Customer customer;
    private Account account;

    private static final Long ORDER_ID = 1L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long ACCOUNT_ID = 200L;
    private static final Long CUSTOMER_ID = 300L;

    @BeforeEach
    void setUp() throws Exception {
        account = new Account();
        account.setAccountId(ACCOUNT_ID);
        account.setEmail("test@example.com");

        User user = new User();
        user.setAccount(account);
        user.setUserId(1L);
        user.setEmail("test@example.com");
        customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setUser(user);

        order = new Order();
        order.setOrderId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(100000));

        payment = new Payment();
        payment.setPaymentId(PAYMENT_ID);
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(BigDecimal.valueOf(100000));
        payment.setCheckoutUrl("https://payos.vn/checkout");
        payment.setQrCode("QR_CODE_123");

        // Mock SessionInterceptor để cho phép tất cả request đi qua
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("createPaymentForOrder Tests")
    class CreatePaymentForOrderTests {
        @Test
        @DisplayName("TC-01: Create payment for order successfully")
        void createPaymentForOrder_Success() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());
            when(paymentService.createPaymentLinkForOrder(any(Order.class), anyString(), anyString()))
                    .thenReturn(payment);

            // Act & Assert
            mockMvc.perform(post("/api/payments/create-for-order/{orderId}", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://payos.vn/checkout"));

            verify(paymentService).createPaymentLinkForOrder(any(Order.class), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-02: Create payment returns existing payment if pending")
        void createPaymentForOrder_ExistingPending() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(payment));

            // Act & Assert
            mockMvc.perform(post("/api/payments/create-for-order/{orderId}", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID));

            verify(paymentService, never()).createPaymentLinkForOrder(any(), any(), any());
        }

        @Test
        @DisplayName("TC-03: Create payment fails when not logged in")
        void createPaymentForOrder_NotLoggedIn() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/create-for-order/{orderId}", ORDER_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }

        @Test
        @DisplayName("TC-04: Create payment fails when order not found")
        void createPaymentForOrder_OrderNotFound() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post("/api/payments/create-for-order/{orderId}", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        @DisplayName("TC-05: Create payment fails when order doesn't belong to user")
        void createPaymentForOrder_AccessDenied() throws Exception {
            // Arrange
            Account otherAccount = new Account();
            otherAccount.setAccountId(999L);
            User otherUser = new User();
            otherUser.setAccount(otherAccount);
            otherUser.setUserId(2L);
            Customer otherCustomer = new Customer();
            otherCustomer.setUser(otherUser);
            order.setCustomer(otherCustomer);

            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));

            // Act & Assert
            mockMvc.perform(post("/api/payments/create-for-order/{orderId}", ORDER_ID)
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order does not belong to current user"));
        }
    }

    @Nested
    @DisplayName("getPaymentHistory Tests")
    class GetPaymentHistoryTests {
        @Test
        @DisplayName("TC-06: Get payment history successfully")
        void getPaymentHistory_Success() throws Exception {
            // Arrange
            when(paymentService.getPaymentsByCustomerId(ACCOUNT_ID)).thenReturn(List.of(payment));

            // Act & Assert
            mockMvc.perform(get("/api/payments/history")
                            .requestAttr("currentUserId", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.payments").isArray())
                    .andExpect(jsonPath("$.payments[0].paymentId").value(PAYMENT_ID));

            verify(paymentService).getPaymentsByCustomerId(ACCOUNT_ID);
        }

        @Test
        @DisplayName("TC-07: Get payment history fails when not logged in")
        void getPaymentHistory_NotLoggedIn() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/payments/history"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }
    }

    @Nested
    @DisplayName("getPaymentStatusByOrder Tests")
    class GetPaymentStatusByOrderTests {
        @Test
        @DisplayName("TC-08: Get payment status successfully")
        void getPaymentStatusByOrder_Success() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(payment));

            // Act & Assert
            mockMvc.perform(get("/api/payments/status/order/{orderId}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                    .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                    .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                    .andExpect(jsonPath("$.checkoutUrl").value("https://payos.vn/checkout"));

            verify(orderService).getById(ORDER_ID);
            verify(paymentService).getPaymentByOrder(order);
        }

        @Test
        @DisplayName("TC-09: Get payment status when order has no payment")
        void getPaymentStatusByOrder_NoPayment() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/payments/status/order/{orderId}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                    .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                    .andExpect(jsonPath("$.paymentStatus").doesNotExist());
        }

        @Test
        @DisplayName("TC-10: Get payment status fails when order not found")
        void getPaymentStatusByOrder_OrderNotFound() throws Exception {
            // Arrange
            when(orderService.getById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/payments/status/order/{orderId}", ORDER_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }
    }

    @Nested
    @DisplayName("testWebhook Tests")
    class TestWebhookTests {
        @Test
        @DisplayName("TC-11: Test webhook endpoint successfully")
        void testWebhook_Success() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/payments/webhook/test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Webhook endpoint is accessible"));
        }
    }

    @Nested
    @DisplayName("extractOrderIdFromDescription Tests")
    class ExtractOrderIdFromDescriptionTests {
        @Test
        @DisplayName("TC-12: Handle webhook with valid description extracts order ID")
        void handleWebhook_ValidDescription_ExtractsOrderId() throws Exception {
            // Arrange - webhook body with description containing order ID
            String webhookBody = """
                {
                    "code": "00",
                    "desc": "Payment successful",
                    "data": {
                        "paymentLinkId": "12345",
                        "orderCode": 123,
                        "amount": 100000,
                        "description": "CSUO5KESD48 Order 1"
                    }
                }
                """;

            // Act & Assert - this will test the extractOrderIdFromDescription method indirectly
            mockMvc.perform(post("/api/payments/webhook")
                            .contentType("application/json")
                            .content(webhookBody))
                    .andExpect(status().isOk());

            // Verify that the service was called to find payment by order
            verify(paymentService, atLeastOnce()).getPaymentByOrderId(1L);
        }

        @Test
        @DisplayName("TC-13: Handle webhook with empty body returns success")
        void handleWebhook_EmptyBody_ReturnsSuccess() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/webhook")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0))
                    .andExpect(jsonPath("$.message").value("ok"));
        }

        @Test
        @DisplayName("TC-14: Handle webhook with null body returns success")
        void handleWebhook_NullBody_ReturnsSuccess() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/payments/webhook")
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0))
                    .andExpect(jsonPath("$.message").value("ok"));
        }

        @Test
        @DisplayName("TC-15: Handle webhook without data returns success")
        void handleWebhook_NoData_ReturnsSuccess() throws Exception {
            // Arrange
            String webhookBody = """
                {
                    "code": "00",
                    "desc": "Payment successful"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/payments/webhook")
                            .contentType("application/json")
                            .content(webhookBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(0));
        }
    }
}

