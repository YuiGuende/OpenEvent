package com.group02.openevent.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lớp test cho OrderController sử dụng MockMvc.
 * ĐÃ CẬP NHẬT: Gom các test 'checkRegistration' vào @Nested class
 */
@WebMvcTest(controllers = OrderController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class OrderControllerTest {

    private static final String API_URL = "/api/orders/create-with-ticket-types";
    private static final Long VALID_CUSTOMER_ACCOUNT_ID = 1L;
    private static final Long CUSTOMER_ID = 100L;
    private static final Long EVENT_ID = 200L;
    private static final Long TICKET_TYPE_ID = 300L;
    private static final Long NEW_ORDER_ID = 123L;
    private static final Long OLD_ORDER_ID = 456L;
    @MockBean
    private IOrderRepo orderRepo;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private SessionInterceptor sessionInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ICustomerRepo customerRepo;

    @MockitoBean
    private VoucherService voucherService;

    private Customer sampleCustomer;
    private Order sampleCreatedOrder;
    private CreateOrderWithTicketTypeRequest validRequestDTO;
    private Event event;
    @BeforeEach
    void setUp() throws Exception {
        Account account = new Account();
        account.setAccountId(VALID_CUSTOMER_ACCOUNT_ID);
        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(CUSTOMER_ID);
        sampleCustomer.setAccount(account);

        sampleCreatedOrder = new Order();
        sampleCreatedOrder.setOrderId(NEW_ORDER_ID);
        sampleCreatedOrder.setTotalAmount(new BigDecimal("100.00"));
        sampleCreatedOrder.setStatus(OrderStatus.PENDING);
        sampleCreatedOrder.setCustomer(sampleCustomer);

        validRequestDTO = new CreateOrderWithTicketTypeRequest();
        validRequestDTO.setEventId(EVENT_ID);
        validRequestDTO.setTicketTypeId(TICKET_TYPE_ID);
        validRequestDTO.setParticipantName("Test User");
        validRequestDTO.setParticipantEmail("test@example.com");
        event = new Event();
        event.setId(100L);

        // Mock SessionInterceptor để cho phép tất cả request đi qua
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    /**
     * 1. Security & Authentication Cases
     */
    @Nested
    @DisplayName("1. Tests cho createWithTicketTypes (Security)")
    class SecurityTests {

        @Test
        @DisplayName("AUTH-001: Khi không đăng nhập, trả về 401")
        void test_AUTH_001_createOrder_whenNotLoggedIn_shouldReturn401() throws Exception {
            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            // Ghi đè mock chung, giả lập interceptor không tìm thấy "currentUserId"
            when(sessionInterceptor.preHandle(any(), any(), any())).thenAnswer(invocation -> {
                // Không setAttribute("currentUserId")
                return true;
            });

            // Khi gọi /create-with-ticket-types, controller sẽ đọc "currentUserId" là null
            // và trả về 401 (như logic trong code của bạn)
            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));

        }

        @Test
        @DisplayName("AUTH-002: Khi không tìm thấy Customer, trả về 404")
        void test_AUTH_002_createOrder_whenCustomerNotFound_shouldReturn404() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.empty());
            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Customer not found"));
        }
    }

    /**
     * 2. Core Business Logic Cases
     */
    @Nested
    @DisplayName("2. Tests cho createWithTicketTypes (Business Logic)")
    class BusinessLogicTests {

        @Test
        @DisplayName("BIZ-001 (Happy Path): Tạo đơn thành công, trả về 200")
        void test_BIZ_001_createOrder_happyPath_shouldReturn200AndOrder() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.empty());
            when(orderService.createOrderWithTicketTypes(any(CreateOrderWithTicketTypeRequest.class), eq(sampleCustomer)))
                    .thenReturn(sampleCreatedOrder);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.orderId").value(NEW_ORDER_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(orderService).createOrderWithTicketTypes(any(CreateOrderWithTicketTypeRequest.class), eq(sampleCustomer));
        }

        @Test
        @DisplayName("BIZ-002: Khi đã đăng ký event (PAID), trả về 400")
        void test_BIZ_002_createOrder_whenAlreadyRegistered_shouldReturn400() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(true);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You have already registered for this event"));

            verify(orderService, never()).createOrderWithTicketTypes(any(), any());
        }

        @Test
        @DisplayName("BIZ-003: Hủy đơn PENDING cũ và tạo đơn mới, trả về 200")
        void test_BIZ_003_createOrder_withExistingPendingOrder_shouldCancelOldAndCreateNew() throws Exception {
            Order oldPendingOrder = new Order();
            oldPendingOrder.setOrderId(OLD_ORDER_ID);

            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.of(oldPendingOrder));

            // Đây là cú pháp đúng cho void
            doNothing().when(orderService).cancelOrder(OLD_ORDER_ID);

            when(orderService.createOrderWithTicketTypes(any(CreateOrderWithTicketTypeRequest.class), eq(sampleCustomer)))
                    .thenReturn(sampleCreatedOrder);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(NEW_ORDER_ID));

            verify(orderService).cancelOrder(OLD_ORDER_ID);
            verify(orderService).createOrderWithTicketTypes(any(CreateOrderWithTicketTypeRequest.class), eq(sampleCustomer));
        }
    }

    /**
     * 3. Input Validation Cases
     */
    @Nested
    @DisplayName("3. Tests cho createWithTicketTypes (Validation)")
    class ValidationTests {

        @Test
        @DisplayName("VAL-001: Khi eventId là null, trả về 400")
        void test_VAL_001_createOrder_whenEventIdNull_shouldReturn400() throws Exception {
            validRequestDTO.setEventId(null);
            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("VAL-002: Khi ticketTypeId là null, trả về 400")
        void test_VAL_002_createOrder_whenTicketTypeIdNull_shouldReturn400() throws Exception {
            validRequestDTO.setTicketTypeId(null);
            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * 4. Exception & Edge Case Tests
     */
    @Nested
    @DisplayName("4. Tests cho createWithTicketTypes (Exceptions)")
    class EdgeCaseTests {

        @Test
        @DisplayName("EDGE-001: Khi currentUserId sai kiểu (ClassCastException), trả về 400")
        void test_EDGE_001_createOrder_whenAttrIsWrongType_shouldReturn400() throws Exception {
            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", "THIS-IS-A-STRING"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("cannot be cast to class java.lang.Long")));        }

        @Test
        @DisplayName("EDGE-002: Khi cancelOrder bị lỗi (RuntimeException), trả về 400")
        void test_EDGE_002_createOrder_whenCancelOrderFails_shouldReturn400() throws Exception {
            Order oldPendingOrder = new Order();
            oldPendingOrder.setOrderId(OLD_ORDER_ID);

            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.of(oldPendingOrder));

            doThrow(new RuntimeException("DB Connection Failed")).when(orderService).cancelOrder(OLD_ORDER_ID);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("DB Connection Failed"));

            verify(orderService, never()).createOrderWithTicketTypes(any(), any());
        }

        @Test
        @DisplayName("EDGE-003: Khi orderId trả về là null (NPE), trả về 400")
        void test_EDGE_003_createOrder_whenCreatedOrderIdIsNull_shouldReturn400() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.empty());

            sampleCreatedOrder.setOrderId(null);
            when(orderService.createOrderWithTicketTypes(any(), eq(sampleCustomer))).thenReturn(sampleCreatedOrder);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("NullPointerException")));
        }

        @Test
        @DisplayName("EDGE-004: Khi orderStatus trả về là null (NPE), trả về 400")
        void test_EDGE_004_createOrder_whenCreatedOrderStatusIsNull_shouldReturn400() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.empty());

            sampleCreatedOrder.setOrderId(NEW_ORDER_ID);
            sampleCreatedOrder.setStatus(null); // Status null
            when(orderService.createOrderWithTicketTypes(any(), eq(sampleCustomer))).thenReturn(sampleCreatedOrder);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("getStatus()\" is null")));        }

        @Test
        @DisplayName("EDGE-005: Khi Exception message là null, trả về 400 với tên class")
        void test_EDGE_005_createOrder_whenExceptionMessageIsNull_shouldReturn400() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.empty());

            when(orderService.createOrderWithTicketTypes(any(), eq(sampleCustomer)))
                    .thenThrow(new IllegalStateException()); // Exception không có message

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order creation failed: IllegalStateException"));
        }
    }

    /**
     * 5. Concurrency Simulation Tests
     */
    @Nested
    @DisplayName("5. Tests cho createWithTicketTypes (Concurrency)")
    class ConcurrencyTests {

        @Test
        @DisplayName("CON-001 (Simulate): Lỗi vi phạm DB, trả về 400")
        void test_CON_001_createOrder_whenDbConstraintFails_shouldReturn400() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.empty());

            when(orderService.createOrderWithTicketTypes(any(), eq(sampleCustomer)))
                    .thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed"));

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("UNIQUE constraint failed"));
        }

        @Test
        @DisplayName("CON-002 (Simulate): Hủy đơn đã bị hủy (Race), trả về 400")
        void test_CON_002_createOrder_whenCancellingAlreadyCancelledOrder_shouldReturn400() throws Exception {
            Order oldPendingOrder = new Order();
            oldPendingOrder.setOrderId(OLD_ORDER_ID);

            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.of(oldPendingOrder));

            doThrow(new IllegalStateException("Order already cancelled")).when(orderService).cancelOrder(OLD_ORDER_ID);

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Order already cancelled"));

            verify(orderService, never()).createOrderWithTicketTypes(any(), any());
        }

        @Test
        @DisplayName("CON-003 (TOCTOU): Hết vé (sau khi check), trả về 400")
        void test_CON_003_createOrder_whenTicketsBecomeUnavailable_shouldReturn400() throws Exception {
            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(sampleCustomer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(false);
            when(orderService.getPendingOrderForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(Optional.empty());

            when(orderService.createOrderWithTicketTypes(any(), eq(sampleCustomer)))
                    .thenThrow(new IllegalStateException("Tickets unavailable"));

            String jsonRequest = objectMapper.writeValueAsString(validRequestDTO);

            mockMvc.perform(post(API_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Tickets unavailable"));
        }
    }

    // =================================================================
    // LỚP MỚI ĐƯỢC THÊM VÀO THEO YÊU CẦU
    // =================================================================
    @Nested
    @DisplayName("6. Tests cho hàm: checkRegistration()")
    class CheckRegistrationTests {

        @Test
        @DisplayName("CHECK-REG-001: Khi không đăng nhập, trả về 401")
        void checkRegistration_Unauthorized_MissingCurrentUserId() throws Exception {
            //Given
            // Ghi đè mock chung, giả lập interceptor không tìm thấy "currentUserId"
            when(sessionInterceptor.preHandle(any(), any(), any())).thenAnswer(invocation -> {
                // Không setAttribute("currentUserId")
                return true;
            });

            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", EVENT_ID)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }
        @Test
        @DisplayName("CHECK-REG-002: Đăng nhập hợp lệ, đã đăng ký")
        void checkRegistration_Authorized_CustomerId() throws Exception {
            //Given
            Customer customer = new Customer();
            customer.setCustomerId(CUSTOMER_ID);

            when(customerRepo.findByAccount_AccountId(VALID_CUSTOMER_ACCOUNT_ID)).thenReturn(Optional.of(customer));
            when(orderService.hasCustomerRegisteredForEvent(CUSTOMER_ID, EVENT_ID)).thenReturn(true);

            //When
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", EVENT_ID)
                            .requestAttr("currentUserId", VALID_CUSTOMER_ACCOUNT_ID) // Giả lập interceptor đã chạy
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.isRegistered").value(true));
        }
        @Test
        @DisplayName("CHECK-REG-003: Khi không tìm thấy Customer, trả về 404")
        void checkRegistration_CustomerNotFound() throws Exception {
            // Given
            Long eventId = 5L;
            Long accountId = 10L;

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.empty());

            // When / Then
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Customer not found"));

            verify(orderService, never()).hasCustomerRegisteredForEvent(any(), any());
        }
        @Test
        @DisplayName("CHECK-REG-004: Customer đã đăng ký")
        void checkRegistration_CustomerAlreadyRegistered() throws Exception {
            // Given
            Long eventId = 5L;
            Long accountId = 10L;
            Long customerId = 100L;

            Customer customer = new Customer();
            customer.setCustomerId(customerId);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);

            // When / Then
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.isRegistered").value(true));

            verify(orderService, times(1)).hasCustomerRegisteredForEvent(customerId, eventId);
        }
        @Test
        @DisplayName("CHECK-REG-005: Customer chưa đăng ký")
        void checkRegistration_CustomerNotRegistered() throws Exception {
            // Given
            Long eventId = 5L;
            Long accountId = 10L;
            Long customerId = 100L;

            Customer customer = new Customer();
            customer.setCustomerId(customerId);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(false);

            // When / Then
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.isRegistered").value(false));

            verify(orderService, times(1)).hasCustomerRegisteredForEvent(customerId, eventId);
        }
        @Test
        @DisplayName("CHECK-REG-006: Thiếu PathVariable eventId, trả về 4xx")
        void checkRegistration_MissingEventId_ShouldReturn4xx() throws Exception {
            // Given
            Long accountId = 10L;

            // Khi gọi mà không có {eventId}
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/") // thiếu param
                            .requestAttr("currentUserId", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError()); // Thường là 404 Not Found
        }
        @Test
        @DisplayName("CHECK-REG-007: EventId âm, trả về 4xx")
        void checkRegistration_NegativeEventId_ShouldReturnBadRequest() throws Exception {
            // Given
            Long eventId = -1L;
            Long accountId = 10L;

            // When / Then
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError()) // Có thể là 400 nếu có validation, hoặc 404
                    .andDo(print());
        }
        @Test
        @DisplayName("CHECK-REG-008: Service ném Exception, trả về 400")
        void checkRegistration_ServiceThrowsException_ShouldReturnBadRequest() throws Exception {
            // Given
            Long eventId = 5L;
            Long accountId = 10L;
            Long customerId = 100L;

            Customer customer = new Customer();
            customer.setCustomerId(customerId);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(orderService.hasCustomerRegisteredForEvent(customerId, eventId))
                    .thenThrow(new RuntimeException("DB error"));

            // When / Then
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("DB error"));
        }
        @Test
        @DisplayName("CHECK-REG-009: Gọi lặp lại, kết quả nhất quán")
        void checkRegistration_RepeatedCalls_ShouldReturnConsistentResults() throws Exception {
            // Given
            Long eventId = 5L;
            Long accountId = 10L;
            Long customerId = 100L;

            Customer customer = new Customer();
            customer.setCustomerId(customerId);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);

            // When / Then (gọi 2 lần để test consistency)
            for (int i = 0; i < 2; i++) {
                mockMvc.perform(MockMvcRequestBuilders
                                .get("/api/orders/check-registration/{eventId}", eventId)
                                .requestAttr("currentUserId", accountId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.isRegistered").value(true));
            }

            verify(orderService, times(2)).hasCustomerRegisteredForEvent(customerId, eventId);
        }
        @Test
        @DisplayName("CHECK-REG-010: Các user khác nhau, kết quả độc lập")
        void checkRegistration_DifferentUsers_ShouldBeIsolated() throws Exception {
            // Given
            Long eventId = 5L;
            Customer c1 = new Customer(); c1.setCustomerId(100L);
            Customer c2 = new Customer(); c2.setCustomerId(200L);

            when(customerRepo.findByAccount_AccountId(10L)).thenReturn(Optional.of(c1));
            when(customerRepo.findByAccount_AccountId(20L)).thenReturn(Optional.of(c2));

            when(orderService.hasCustomerRegisteredForEvent(100L, eventId)).thenReturn(true);
            when(orderService.hasCustomerRegisteredForEvent(200L, eventId)).thenReturn(false);

            // User 1
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isRegistered").value(true));

            // User 2
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .requestAttr("currentUserId", 20L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isRegistered").value(false));

            verify(orderService).hasCustomerRegisteredForEvent(100L, eventId);
            verify(orderService).hasCustomerRegisteredForEvent(200L, eventId);
        }
        @Test
        @DisplayName("CHECK-REG-011: Test cả Interceptor (Full Auth Chain)")
        void checkRegistration_FullAuthChain_ShouldReturnExpectedJson() throws Exception {
            // Giả lập filter chain gán currentUserId
            when(sessionInterceptor.preHandle(any(), any(), any())).thenAnswer(invocation -> {
                HttpServletRequest req = invocation.getArgument(0);
                req.setAttribute("currentUserId", 10L); // Interceptor gán ID
                return true;
            });

            Long eventId = 5L;
            Long customerId = 100L;
            Customer customer = new Customer();
            customer.setCustomerId(customerId);

            when(customerRepo.findByAccount_AccountId(10L)).thenReturn(Optional.of(customer));
            when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);

            // When / Then (Không cần .requestAttr() nữa vì Interceptor đã làm)
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/orders/check-registration/{eventId}", eventId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.isRegistered").value(true));
        }
    }
}