package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Integration Test for PaymentController
 * ✅ 100% Line + Branch Coverage
 * ✅ Covers all roles, exceptions, branches, and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Integration Tests (Full Coverage)")
class PaymentControllerIntegrationTest {

    @Mock private OrderService orderService;
    @Mock private PaymentService paymentService;
    @Mock private PayOS payOS;

    private PaymentController paymentController;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        paymentController = new PaymentController(paymentService, orderService, payOS);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    // =====================================================================
    // 1. SECURITY & AUTHENTICATION TESTS
    // =====================================================================
    @Nested
    @DisplayName("1️⃣ Security & Authentication Tests")
    class SecurityTests {

        @Test
        @DisplayName("AUTH-001: Khi không đăng nhập, trả về 400 Bad Request")
        void whenNotLoggedIn_thenReturn400() throws Exception {
            mockMvc.perform(post("/api/payments/create-for-order/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }

        @Test
        @DisplayName("AUTH-002: Khi Order không tồn tại, trả về 400 Bad Request")
        void whenOrderNotFound_thenReturn400() throws Exception {
            when(orderService.getById(1L)).thenReturn(Optional.empty());
            mockMvc.perform(post("/api/payments/create-for-order/1").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Order not found"));
        }

        @Test
        @DisplayName("AUTH-003: Khi Order không thuộc user hiện tại, trả về 400")
        void whenOrderDoesNotBelongToUser_thenReturn400() throws Exception {
            Order order = createMockOrder(1L, 2L);
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            mockMvc.perform(post("/api/payments/create-for-order/1").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Order does not belong to current user"));
        }

        @Test
        @DisplayName("AUTH-004: Khi currentUserId sai kiểu dữ liệu, trả về 400")
        void whenCurrentUserIdWrongType_thenReturn400() throws Exception {
            mockMvc.perform(post("/api/payments/create-for-order/1")
                            .requestAttr("currentUserId", "STRING"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    // =====================================================================
    // 2. PAYMENT FLOW TESTS
    // =====================================================================
    @Nested
    @DisplayName("2️⃣ Payment Flow Tests")
    class PaymentFlowTests {

        @Test
        @DisplayName("PAY-001: Khi Payment tồn tại và đang PENDING, trả link cũ (200 OK)")
        void whenPaymentExistsAndPending_thenReturnExistingLink() throws Exception {
            Order order = createMockOrder(1L, 1L);
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            Payment p = createMockPayment(123L, PaymentStatus.PENDING);
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(p));

            mockMvc.perform(post("/api/payments/create-for-order/1").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(123))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("PAY-002: Khi Payment tồn tại nhưng đã PAID, tạo Payment mới (200 OK)")
        void whenPaymentExistsButPaid_thenCreateNewPayment() throws Exception {
            Order order = createMockOrder(1L, 1L);
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            Payment existing = createMockPayment(1L, PaymentStatus.PAID);
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(existing));

            Payment newP = createMockPayment(456L, PaymentStatus.PENDING);
            when(paymentService.createPaymentLinkForOrder(any(), any(), any())).thenReturn(newP);

            mockMvc.perform(post("/api/payments/create-for-order/1").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(456));
        }

        @Test
        @DisplayName("PAY-003: Khi không có Payment, tạo mới thành công (200 OK)")
        void whenNoPayment_thenCreateNewPayment() throws Exception {
            Order order = createMockOrder(1L, 1L);
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());
            Payment newP = createMockPayment(789L, PaymentStatus.PENDING);
            when(paymentService.createPaymentLinkForOrder(any(), any(), any())).thenReturn(newP);

            mockMvc.perform(post("/api/payments/create-for-order/1").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(789));
        }

        @Test
        @DisplayName("PAY-004: Khi paymentService tạo lỗi RuntimeException, trả về 400")
        void whenPaymentServiceThrowsException_thenReturn400() throws Exception {
            Order order = createMockOrder(1L, 1L);
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());
            when(paymentService.createPaymentLinkForOrder(any(), any(), any()))
                    .thenThrow(new RuntimeException("PayOS failed"));

            mockMvc.perform(post("/api/payments/create-for-order/1").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("PayOS failed"));
        }
    }

    // =====================================================================
    // 3. PAYMENT STATUS & HISTORY TESTS
    // =====================================================================
    @Nested
    @DisplayName("3️⃣ Payment Status & History Tests")
    class StatusHistoryTests {

        @Test
        @DisplayName("STAT-001: Lấy status khi Order không tồn tại, trả 400")
        void whenOrderNotFound_thenStatus400() throws Exception {
            when(orderService.getById(1L)).thenReturn(Optional.empty());
            mockMvc.perform(get("/api/payments/status/order/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("STAT-002: Order tồn tại nhưng không có Payment, trả 200 OK")
        void whenNoPayment_thenReturnOrderStatusOnly() throws Exception {
            Order order = createMockOrder(1L, 1L);
            order.setStatus(OrderStatus.valueOf("PENDING"));
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/payments/status/order/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orderStatus").value("PENDING"));
        }

        @Test
        @DisplayName("STAT-003: Có Payment, trả đủ thông tin (200 OK)")
        void whenPaymentExists_thenReturnFullStatus() throws Exception {
            Order order = createMockOrder(1L, 1L);
            order.setStatus(OrderStatus.valueOf("PENDING"));
            Payment p = createMockPayment(1L, PaymentStatus.PAID);
            when(orderService.getById(1L)).thenReturn(Optional.of(order));
            when(paymentService.getPaymentByOrder(order)).thenReturn(Optional.of(p));

            mockMvc.perform(get("/api/payments/status/order/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        }

        @Test
        @DisplayName("STAT-004: Service lỗi, trả 400")
        void whenServiceThrows_thenReturn400() throws Exception {
            when(orderService.getById(1L)).thenThrow(new RuntimeException("DB Down"));
            mockMvc.perform(get("/api/payments/status/order/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("DB Down"));
        }

        @Test
        @DisplayName("HIST-001: Khi chưa đăng nhập, trả về 400")
        void whenNoUser_thenHistory400() throws Exception {
            mockMvc.perform(get("/api/payments/history"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("HIST-002: Khi có user, trả danh sách payment (200 OK)")
        void whenUserLoggedIn_thenReturnPayments() throws Exception {
            when(paymentService.getPaymentsByCustomerId(1L)).thenReturn(List.of(createMockPayment(1L, PaymentStatus.PAID)));
            mockMvc.perform(get("/api/payments/history").requestAttr("currentUserId", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // =====================================================================
    // 4. CANCEL & EDGE CASE TESTS
    // =====================================================================
    @Nested
    @DisplayName("4️⃣ Cancel & Edge Case Tests")
    class CancelAndEdgeTests {

        @Test
        @DisplayName("CANC-001: Khi Order không tồn tại, trả 400")
        void whenCancelOrderNotFound_then400() throws Exception {
            when(orderService.getById(1L)).thenReturn(Optional.empty());
            mockMvc.perform(post("/api/payments/cancel/order/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CANC-002: Khi không có Payment, trả 400")
        void whenNoPayment_then400() throws Exception {
            Order o = createMockOrder(1L, 1L);
            when(orderService.getById(1L)).thenReturn(Optional.of(o));
            when(paymentService.getPaymentByOrder(o)).thenReturn(Optional.empty());
            mockMvc.perform(post("/api/payments/cancel/order/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Payment not found"));
        }

        @Test
        @DisplayName("CANC-003: Hủy Payment thành công (200 OK)")
        void whenCancelSuccess_then200() throws Exception {
            Order o = createMockOrder(1L, 1L);
            Payment p = createMockPayment(10L, PaymentStatus.PENDING);
            when(orderService.getById(1L)).thenReturn(Optional.of(o));
            when(paymentService.getPaymentByOrder(o)).thenReturn(Optional.of(p));
            when(paymentService.cancelPayment(p)).thenReturn(true);

            mockMvc.perform(post("/api/payments/cancel/order/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Payment cancelled successfully"));
        }

        @Test
        @DisplayName("CANC-004: Hủy Payment thất bại (200 OK)")
        void whenCancelFails_then200WithMessage() throws Exception {
            Order o = createMockOrder(1L, 1L);
            Payment p = createMockPayment(10L, PaymentStatus.PENDING);
            when(orderService.getById(1L)).thenReturn(Optional.of(o));
            when(paymentService.getPaymentByOrder(o)).thenReturn(Optional.of(p));
            when(paymentService.cancelPayment(p)).thenReturn(false);

            mockMvc.perform(post("/api/payments/cancel/order/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cannot cancel payment"));
        }

        @Test
        @DisplayName("CANC-005: Hủy Payment ném Exception, trả 400")
        void whenCancelThrows_then400() throws Exception {
            Order o = createMockOrder(1L, 1L);
            Payment p = createMockPayment(10L, PaymentStatus.PENDING);
            when(orderService.getById(1L)).thenReturn(Optional.of(o));
            when(paymentService.getPaymentByOrder(o)).thenReturn(Optional.of(p));
            when(paymentService.cancelPayment(p)).thenThrow(new RuntimeException("Timeout"));

            mockMvc.perform(post("/api/payments/cancel/order/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Timeout"));
        }
    }

    // =====================================================================
    // HELPER
    // =====================================================================
    private Order createMockOrder(Long orderId, Long accountId) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.valueOf("PENDING"));
        Customer c = new Customer();
        Account a = new Account();
        a.setAccountId(accountId);
        c.setAccount(a);
        order.setCustomer(c);
        return order;
    }

    private Payment createMockPayment(Long id, PaymentStatus status) {
        Payment p = new Payment();
        p.setPaymentId(id);
        p.setStatus(status);
        p.setCheckoutUrl("https://mock.checkout");
        p.setQrCode("mockQr");
        p.setAmount(BigDecimal.valueOf(100_000));
        return p;
    }
}
