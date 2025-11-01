package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PayOS payOS;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // Khởi tạo MockMvc với controller đã được tiêm (inject)
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    // =====================================================================
    // 1. TESTS FOR: create-for-order ENDPOINT
    // =====================================================================
    @Nested
    @DisplayName("Endpoint: POST /api/payments/create-for-order/{orderId}")
    class CreatePaymentLinkTests {

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
        // HELPER (for create-for-order)
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


    // =====================================================================
    // 2. TESTS FOR: webhook ENDPOINT
    // =====================================================================
    @Nested
    @DisplayName("Endpoint: POST /webhook (PayOS)")
    class WebhookTests {

        private Payment testPayment;
        private Order testOrder;

        @BeforeEach
        void setUp() {
            // Setup test order
            testOrder = new Order();
            testOrder.setOrderId(16L);
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setOriginalPrice(new BigDecimal("100000"));

            // Setup test payment
            testPayment = new Payment();
            testPayment.setPaymentId(1L);
            testPayment.setOrder(testOrder);
            testPayment.setStatus(PaymentStatus.PENDING);
            testPayment.setAmount(new BigDecimal("100000"));
        }

        /**
         * A. PayOS Test Webhooks
         */
        @Nested
        @DisplayName("A. PayOS Test Webhooks")
        class PayOSTestWebhooks {

            /**
             * TC-01: Empty Body - PayOS Connection Test
             * Given: null or empty webhook body
             * When: POST /webhook
             * Then: Returns {error:0, message:"ok", data:null} with 200 OK
             */
            @Test
            @DisplayName("TC-01: Empty webhook body returns success response")
            void testHandleWebhook_EmptyBody() {
                // GIVEN: null webhook body
                Map<String, Object> emptyBody = null;

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(emptyBody);

                // THEN: Returns {error:0, message:"ok", data:null}
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).containsEntry("error", 0);
                assertThat(response.getBody()).containsEntry("message", "ok");
                assertThat(response.getBody()).containsEntry("data", null);

                // Verify no service calls
                verify(paymentService, never()).getPaymentByOrderId(anyLong());
                verify(orderService, never()).save(any());
            }

            /**
             * TC-02: Missing Data Field
             * Given: Webhook body without "data" field
             * When: POST /webhook
             * Then: Returns {error:0, message:"ok", data:null} with 200 OK
             */
            @Test
            @DisplayName("TC-02: Missing data field returns ok response")
            void testHandleWebhook_MissingDataField() {
                // GIVEN: Webhook without data field
                Map<String, Object> webhookBody = new HashMap<>();
                webhookBody.put("code", "00");
                webhookBody.put("desc", "success");
                // No data field

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Returns ok with null data
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("error", 0);
                assertThat(response.getBody()).containsEntry("message", "ok");
                assertThat(response.getBody()).containsEntry("data", null);

                verify(paymentService, never()).getPaymentByOrderId(anyLong());
            }
        }

        /**
         * B. Valid Payment Webhooks
         */
        @Nested
        @DisplayName("B. Valid Payment Webhooks")
        class ValidPaymentWebhooks {

            /**
             * TC-03: Successful Payment with Valid OrderId
             * Given: Valid webhook with code=00, valid orderId in description
             * When: POST /webhook
             * Then: Payment & Order status → PAID, returns success
             */
            @Test
            @DisplayName("TC-03: Valid webhook updates payment and order to PAID")
            void testHandleWebhook_ValidPaymentSuccess() {
                // GIVEN: Valid webhook with code=00
                Map<String, Object> webhookBody = createValidWebhookBody(16L, "Order 16");

                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Payment & Order status → PAID
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);

                // Verify response structure
                assertThat(response.getBody()).containsEntry("error", 0);
                assertThat(response.getBody()).containsEntry("message", "ok");

                @SuppressWarnings("unchecked")
                Map<String, Object> dataResponse = (Map<String, Object>) response.getBody().get("data");
                assertThat(dataResponse.get("success")).isEqualTo(true);

                // Verify service calls
                verify(paymentService).getPaymentByOrderId(16L);
                verify(paymentService).updatePaymentStatus(
                        eq(testPayment), eq(PaymentStatus.PAID), isNull());
                verify(orderService).save(testOrder);
            }

            /**
             * TC-04: Extract OrderId from Description
             * Given: Description = "CSOC312JNL1 Order 16"
             * When: POST /webhook
             * Then: Successfully extracts orderId=16 and updates payment
             */
            @Test
            @DisplayName("TC-04: Successfully extracts orderId from description")
            void testHandleWebhook_ExtractOrderFromDescription() {
                // GIVEN: Description with order ID
                Map<String, Object> webhookBody = createValidWebhookBody(
                        999L, "CSOC312JNL1 Order 16");

                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Uses extracted orderId=16, not orderCode=999
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(paymentService).getPaymentByOrderId(16L);
                verify(paymentService, never()).getPaymentByOrder(any());
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            }

            /**
             * TC-05: Fallback to OrderCode
             * Given: Description doesn't contain orderId, use orderCode
             * When: POST /webhook
             * Then: Find payment by orderCode
             */
            @Test
            @DisplayName("TC-05: Fallback to orderCode when description has no orderId")
            void testHandleWebhook_FallbackToOrderCode() {
                // GIVEN: Description without "Order XX" pattern
                Map<String, Object> webhookBody = createValidWebhookBody(
                        16L, "Payment for event registration");

                // When description has no orderId, controller uses orderCode directly
                when(orderService.getById(16L))
                        .thenReturn(Optional.of(testOrder));
                when(paymentService.getPaymentByOrder(testOrder))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Uses orderCode=16
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(orderService).getById(16L);
                verify(paymentService).getPaymentByOrder(testOrder);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            }
        }

        /**
         * C. Payment Status Scenarios
         */
        @Nested
        @DisplayName("C. Payment Status Scenarios")
        class PaymentStatusScenarios {

            /**
             * TC-06: Already PAID - Idempotency
             * Given: Payment status = PAID
             * When: POST /webhook
             * Then: Skip update, returns success (idempotent)
             */
            @Test
            @DisplayName("TC-06: Already PAID payment skips update (idempotency)")
            void testHandleWebhook_AlreadyPaid() {
                // GIVEN: Payment already PAID
                testPayment.setStatus(PaymentStatus.PAID);
                testOrder.setStatus(OrderStatus.PAID);

                Map<String, Object> webhookBody = createValidWebhookBody(16L, "Order 16");
                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Skip update, returns success
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Verify no duplicate update
                verify(paymentService, never()).updatePaymentStatus(any(), any(), any());
                verify(orderService, never()).save(any());

                // Still returns success for idempotency
                @SuppressWarnings("unchecked")
                Map<String, Object> dataResponse = (Map<String, Object>) response.getBody().get("data");
                assertThat(dataResponse.get("success")).isEqualTo(true);
            }

            /**
             * TC-07: CANCELLED Payment - Override
             * Given: Payment was CANCELLED
             * When: POST /webhook
             * Then: Update to PAID (PayOS confirms payment received)
             */
            @Test
            @DisplayName("TC-07: CANCELLED payment can be updated to PAID")
            void testHandleWebhook_CancelledPayment() {
                // GIVEN: Payment was CANCELLED
                testPayment.setStatus(PaymentStatus.CANCELLED);
                testOrder.setStatus(OrderStatus.CANCELLED);

                Map<String, Object> webhookBody = createValidWebhookBody(16L, "Order 16");
                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Update to PAID (override cancellation)
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);

                verify(paymentService).updatePaymentStatus(
                        eq(testPayment), eq(PaymentStatus.PAID), isNull());
                verify(orderService).save(testOrder);
            }

            /**
             * TC-08: EXPIRED Payment - Override
             * Given: Payment was EXPIRED
             * When: POST /webhook
             * Then: Update to PAID (user paid after expiration)
             */
            @Test
            @DisplayName("TC-08: EXPIRED payment can be updated to PAID")
            void testHandleWebhook_ExpiredPayment() {
                // GIVEN: Payment was EXPIRED
                testPayment.setStatus(PaymentStatus.EXPIRED);
                testOrder.setStatus(OrderStatus.EXPIRED);

                Map<String, Object> webhookBody = createValidWebhookBody(16L, "Order 16");
                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Update to PAID
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);

                verify(paymentService).updatePaymentStatus(
                        eq(testPayment), eq(PaymentStatus.PAID), isNull());
            }
        }

        /**
         * D. Error Scenarios
         */
        @Nested
        @DisplayName("D. Error Scenarios")
        class ErrorScenarios {

            /**
             * TC-09: Payment Not Found
             * Given: Valid webhook but orderId doesn't exist
             * When: POST /webhook
             * Then: Returns {success:false, message:"Payment not found"} with 200 OK
             */
            @Test
            @DisplayName("TC-09: Payment not found returns success with error message")
            void testHandleWebhook_PaymentNotFound() {
                // GIVEN: Valid webhook but payment doesn't exist
                Map<String, Object> webhookBody = createValidWebhookBody(999L, "Order 999");

                when(paymentService.getPaymentByOrderId(999L))
                        .thenReturn(Optional.empty());
                when(orderService.getById(999L))
                        .thenReturn(Optional.empty());

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Returns success with "Payment not found" (PayOS requirement)
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("error", 0);

                @SuppressWarnings("unchecked")
                Map<String, Object> dataResponse =
                        (Map<String, Object>) response.getBody().get("data");
                assertThat(dataResponse.get("success")).isEqualTo(false);
                assertThat(dataResponse.get("message")).isEqualTo("Payment not found");

                // Verify no status update
                verify(paymentService, never()).updatePaymentStatus(any(), any(), any());
                verify(orderService, never()).save(any());
            }

            /**
             * TC-10: Order Not Found by OrderCode
             * Given: Description has no orderId, orderCode not found
             * When: POST /webhook
             * Then: Returns {success:false, message:"Payment not found"}
             */
            @Test
            @DisplayName("TC-10: Order not found by orderCode returns error")
            void testHandleWebhook_OrderNotFound() {
                // GIVEN: orderCode doesn't exist, description has no orderId
                Map<String, Object> webhookBody = createValidWebhookBody(
                        888L, "Some description");

                // Controller tries orderCode directly when no orderId in description
                when(orderService.getById(888L))
                        .thenReturn(Optional.empty());

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Returns payment not found
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                @SuppressWarnings("unchecked")
                Map<String, Object> dataResponse =
                        (Map<String, Object>) response.getBody().get("data");
                assertThat(dataResponse.get("success")).isEqualTo(false);
            }

            /**
             * TC-11: Service Exception Handling
             * Given: Service throws RuntimeException
             * When: POST /webhook
             * Then: Returns {error:0, success:false} with error message
             */
            @Test
            @DisplayName("TC-11: Service exception returns ok response with error")
            void testHandleWebhook_ServiceException() {
                // GIVEN: Service throws exception
                Map<String, Object> webhookBody = createValidWebhookBody(16L, "Order 16");

                when(paymentService.getPaymentByOrderId(16L))
                        .thenThrow(new RuntimeException("Database connection error"));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Returns ok with error info (PayOS requirement)
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("error", 0);
                assertThat(response.getBody()).containsEntry("message", "ok");

                @SuppressWarnings("unchecked")
                Map<String, Object> dataResponse =
                        (Map<String, Object>) response.getBody().get("data");
                assertThat(dataResponse.get("success")).isEqualTo(false);
                assertThat(dataResponse.get("error")).isNotNull();
                assertThat(dataResponse.get("error").toString())
                        .contains("Database connection error");
            }

            /**
             * TC-12: Invalid Data Format - ClassCastException
             * Given: Webhook data has invalid type (e.g. orderCode as String)
             * When: POST /webhook
             * Then: Catches exception, returns ok with error
             */
            @Test
            @DisplayName("TC-12: Invalid data format is handled gracefully")
            void testHandleWebhook_InvalidDataFormat() {
                // GIVEN: Invalid data format
                Map<String, Object> webhookBody = new HashMap<>();
                webhookBody.put("code", "00");

                Map<String, Object> data = new HashMap<>();
                data.put("orderCode", "not-a-number"); // Invalid type
                data.put("amount", "also-invalid");
                data.put("description", "Order 16");
                webhookBody.put("data", data);

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Returns ok with error (no crash)
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("error", 0);

                @SuppressWarnings("unchecked")
                Map<String, Object> dataResponse =
                        (Map<String, Object>) response.getBody().get("data");
                assertThat(dataResponse.get("success")).isEqualTo(false);
            }
        }

        /**
         * E. Edge Cases
         */
        @Nested
        @DisplayName("E. Edge Cases")
        class EdgeCases {

            /**
             * TC-13: Concurrent Webhooks - Race Condition
             * Given: Multiple webhooks for same payment
             * When: First webhook updates to PAID
             * Then: Second webhook skips update (idempotency)
             */
            @Test
            @DisplayName("TC-13: Concurrent webhooks handled with idempotency")
            void testHandleWebhook_ConcurrentWebhooks() {
                // GIVEN: Same payment, two webhook calls
                Map<String, Object> webhookBody = createValidWebhookBody(16L, "Order 16");

                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: First webhook call
                ResponseEntity<Map<String, Object>> response1 =
                        paymentController.handleWebhook(webhookBody);

                // THEN: First call updates status
                assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);

                // Reset mocks for second call
                reset(paymentService, orderService);

                // GIVEN: Payment now PAID
                testPayment.setStatus(PaymentStatus.PAID);
                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: Second webhook call (concurrent/duplicate)
                ResponseEntity<Map<String, Object>> response2 =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Second call skips update (idempotency)
                assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(paymentService, never()).updatePaymentStatus(any(), any(), any());
                verify(orderService, never()).save(any());
            }

            /**
             * TC-14: Amount Type Conversion
             * Given: Amount as Integer (not Long)
             * When: POST /webhook
             * Then: Correctly converts and processes
             */
            @Test
            @DisplayName("TC-14: Amount type conversion handled correctly")
            void testHandleWebhook_AmountTypeConversion() {
                // GIVEN: Amount as Integer
                Map<String, Object> webhookBody = new HashMap<>();
                webhookBody.put("code", "00");

                Map<String, Object> data = new HashMap<>();
                data.put("orderCode", 16); // Integer instead of Long
                data.put("amount", 100000); // Integer
                data.put("description", "Order 16");
                webhookBody.put("data", data);

                when(paymentService.getPaymentByOrderId(16L))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Correctly converts and processes
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);

                verify(paymentService).getPaymentByOrderId(16L);
            }

            /**
             * TC-15: Null Fields in Data
             * Given: Some fields in data are null
             * When: POST /webhook
             * Then: Handles gracefully without NPE
             */
            @Test
            @DisplayName("TC-15: Null fields in data handled gracefully")
            void testHandleWebhook_NullFieldsInData() {
                // GIVEN: Null fields (description and paymentLinkId are null)
                Map<String, Object> webhookBody = new HashMap<>();
                webhookBody.put("code", "00");

                Map<String, Object> data = new HashMap<>();
                data.put("orderCode", 16L);
                data.put("amount", 100000);
                data.put("description", null); // Null description
                data.put("paymentLinkId", null); // Null paymentLinkId
                webhookBody.put("data", data);

                // When description is null, controller uses orderCode
                when(orderService.getById(16L))
                        .thenReturn(Optional.of(testOrder));
                when(paymentService.getPaymentByOrder(testOrder))
                        .thenReturn(Optional.of(testPayment));

                // WHEN: POST webhook
                ResponseEntity<Map<String, Object>> response =
                        paymentController.handleWebhook(webhookBody);

                // THEN: Handles gracefully
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            }
        }

        /**
         * Helper method to create valid webhook body
         */
        private Map<String, Object> createValidWebhookBody(Long orderCode, String description) {
            Map<String, Object> webhookBody = new HashMap<>();
            webhookBody.put("code", "00");
            webhookBody.put("desc", "success");

            Map<String, Object> data = new HashMap<>();
            data.put("paymentLinkId", "test-payment-link-123");
            data.put("orderCode", orderCode);
            data.put("amount", 100000);
            data.put("description", description);
            data.put("accountNumber", "0989612290");
            data.put("reference", "FT25297007310012");
            data.put("transactionDateTime", "2025-10-24 16:33:00");

            webhookBody.put("data", data);

            return webhookBody;
        }
    }
}
