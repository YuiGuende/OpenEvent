package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerIntegrationTest {

    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private PayOS payOS;

    private PaymentController paymentController;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        paymentController = new PaymentController(paymentService, orderService, payOS);
        mockMvc = MockMvcBuilders
                .standaloneSetup(paymentController)
                .build();
    }

    @Test
    void shouldReturn400WhenNoCurrentUserId() throws Exception {
        mockMvc.perform(post("/api/payments/create-for-order/1"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not logged in"));
    }

    @Test
    void shouldReturn400WhenOrderNotFound() throws Exception {
        when(orderService.getById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/payments/create-for-order/1")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void shouldReturn400WhenOrderDoesNotBelongToUser() throws Exception {
        Order order = createMockOrder(1L, 2L); // Order belongs to user 2, but current user is 1
        when(orderService.getById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(post("/api/payments/create-for-order/1")
                        .requestAttr("currentUserId", 1L)) // Different user ID
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order does not belong to current user"));
    }

    @Test
    void shouldReturnExistingPaymentWhenPaymentExistsAndPending() throws Exception {
        Order order = createMockOrder(1L, 1L); // Order belongs to user 1
        when(orderService.getById(1L)).thenReturn(Optional.of(order));

        Payment existingPayment = new Payment();
        existingPayment.setPaymentId(123L);
        existingPayment.setStatus(PaymentStatus.PENDING);
        existingPayment.setCheckoutUrl("https://checkout.example.com");
        existingPayment.setQrCode("qr123");
        existingPayment.setAmount(new BigDecimal("100000"));

        when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(existingPayment));

        mockMvc.perform(post("/api/payments/create-for-order/1")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.paymentId").value(123))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.example.com"))
                .andExpect(jsonPath("$.qrCode").value("qr123"));
    }

    @Test
    void shouldCreateNewPaymentWhenNoExistingPayment() throws Exception {
        Order order = createMockOrder(1L, 1L); // Order belongs to user 1
        when(orderService.getById(1L)).thenReturn(Optional.of(order));
        when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());

        Payment newPayment = new Payment();
        newPayment.setPaymentId(456L);
        newPayment.setStatus(PaymentStatus.PENDING);
        newPayment.setCheckoutUrl("https://newcheckout.example.com");
        newPayment.setQrCode("newqr123");
        newPayment.setAmount(new BigDecimal("100000"));

        when(paymentService.createPaymentLinkForOrder(any(Order.class), any(String.class), any(String.class)))
                .thenReturn(newPayment);

        mockMvc.perform(post("/api/payments/create-for-order/1")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.paymentId").value(456))
                .andExpect(jsonPath("$.checkoutUrl").value("https://newcheckout.example.com"))
                .andExpect(jsonPath("$.qrCode").value("newqr123"));
    }

    @Test
    void shouldReturnPaymentHistory() throws Exception {
        mockMvc.perform(get("/api/payments/history")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPaymentStatusByOrder() throws Exception {
        Order order = createMockOrder(1L, 1L); // Order belongs to user 1
        when(orderService.getById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/payments/status/order/1")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldCancelPaymentForOrder() throws Exception {
        Order order = createMockOrder(1L, 1L); // Order belongs to user 1
        when(orderService.getById(1L)).thenReturn(Optional.of(order));

        Payment payment = new Payment();
        payment.setPaymentId(789L);
        when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(payment));
        when(paymentService.cancelPayment(payment)).thenReturn(true);

        mockMvc.perform(post("/api/payments/cancel/order/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment cancelled successfully"));
    }

    private Order createMockOrder(Long orderId, Long accountId) {
        Order order = new Order();
        order.setOrderId(orderId);
        
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        
        Account account = new Account();
        account.setAccountId(accountId);
        customer.setAccount(account);
        
        order.setCustomer(customer);
        return order;
    }
}
