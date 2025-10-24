package com.group02.openevent.controller;

import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.config.TestWebConfig;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.VoucherService;
import com.group02.openevent.service.impl.OrderServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@WebMvcTest(OrderController.class)
@Import(TestWebConfig.class)
public class OrderControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    private OrderServiceImpl orderService;

    @MockBean
    private ICustomerRepo customerRepo;

    @MockBean
    private SessionInterceptor sessionInterceptor;
    @MockBean
    private IEventRepo eventRepo;
    @MockBean
    private ITicketTypeRepo ticketTypeRepo;
    @MockBean
    private VoucherService voucherService;

    private Long eventId;
    private Long customerId;
    private Long accountId;
    @BeforeEach
    public void initData() throws Exception {
        eventId = 1L;
        customerId = 1L;
        accountId = 1L;
    }

    @Test
    void checkRegistration_Unauthorized_MissingCurrentUserId() throws Exception {
        //Given
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not logged in"));
    }
    @Test
    void checkRegistration_Authorized_CustomerId() throws Exception {
        //Given
        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
        when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);
        
        //When
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .requestAttr("currentUserId", accountId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isRegistered").value(true));
    }
    @Test
    void checkRegistration_CustomerNotFound() throws Exception {
        // Given
        Long eventId = 5L;
        Long accountId = 10L;

        when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.empty());
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // When / Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .requestAttr("currentUserId", accountId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Customer not found"));

        // Verify: orderService.should NOT be called
        verify(orderService, never()).hasCustomerRegisteredForEvent(any(), any());
    }
    @Test
    void checkRegistration_CustomerAlreadyRegistered() throws Exception {
        // Given
        Long eventId = 5L;
        Long accountId = 10L;
        Long customerId = 100L;

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        // Giả lập repository và service
        when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
        when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // When / Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .requestAttr("currentUserId", accountId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isRegistered").value(true));

        // ✅ Verify: gọi đúng 1 lần, với đúng tham số
        verify(orderService, times(1)).hasCustomerRegisteredForEvent(customerId, eventId);
    }
    @Test
    void checkRegistration_CustomerNotRegistered() throws Exception {
        // Given
        Long eventId = 5L;
        Long accountId = 10L;
        Long customerId = 100L;

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
        when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(false);
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

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
    void checkRegistration_MissingEventId_ShouldReturn4xx() throws Exception {
        // Given
        Long accountId = 10L;
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // Khi gọi mà không có {eventId}
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/") // thiếu param
                        .requestAttr("currentUserId", accountId)
                        .contentType(MediaType.APPLICATION_JSON))
                // Tuỳ router có thể là 404 hoặc 400
                .andExpect(status().is4xxClientError());
    }
    @Test
    void checkRegistration_NegativeEventId_ShouldReturnBadRequest() throws Exception {
        // Given
        Long eventId = -1L;
        Long accountId = 10L;
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // When / Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .requestAttr("currentUserId", accountId)
                        .contentType(MediaType.APPLICATION_JSON))
                // Tuỳ controller có validation hay không
                .andExpect(status().is4xxClientError())
                .andDo(print());
    }
    @Test
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
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // When / Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .requestAttr("currentUserId", accountId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("DB error"));
    }
    @Test
    void checkRegistration_RepeatedCalls_ShouldReturnConsistentResults() throws Exception {
        // Given
        Long eventId = 5L;
        Long accountId = 10L;
        Long customerId = 100L;

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
        when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

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
    void checkRegistration_DifferentUsers_ShouldBeIsolated() throws Exception {
        // Given
        Long eventId = 5L;
        Customer c1 = new Customer(); c1.setCustomerId(100L);
        Customer c2 = new Customer(); c2.setCustomerId(200L);

        when(customerRepo.findByAccount_AccountId(10L)).thenReturn(Optional.of(c1));
        when(customerRepo.findByAccount_AccountId(20L)).thenReturn(Optional.of(c2));
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

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
    void checkRegistration_FullAuthChain_ShouldReturnExpectedJson() throws Exception {
        // Giả lập filter chain gán currentUserId
        when(sessionInterceptor.preHandle(any(), any(), any())).thenAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            req.setAttribute("currentUserId", 10L);
            return true;
        });

        Long eventId = 5L;
        Long customerId = 100L;
        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        when(customerRepo.findByAccount_AccountId(10L)).thenReturn(Optional.of(customer));
        when(orderService.hasCustomerRegisteredForEvent(customerId, eventId)).thenReturn(true);

        // When / Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/check-registration/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isRegistered").value(true));
    }
//    @Test
//    void checkRegistration_InterceptorBlocksRequest_ShouldReturnUnauthorized() throws Exception {
//        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(false);
//
//        mockMvc.perform(MockMvcRequestBuilders
//                        .get("/api/orders/check-registration/{eventId}", 5L)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isUnauthorized());
//
//        verifyNoInteractions(customerRepo);
//        verifyNoInteractions(orderService);
//    }














}
