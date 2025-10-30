package com.group02.openevent.service;

import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.controller.OrderController;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(OrderServiceImpl.class)
public class OrderServiceTest {
    @Autowired
    private OrderServiceImpl orderService;

    @MockBean
    private SessionInterceptor sessionInterceptor;
    @MockBean
    private IOrderRepo orderRepo;
    @MockBean
    private IEventRepo eventRepo ;
    @MockBean
    private ITicketTypeRepo ticketTypeRepo;
    @MockBean
    private  TicketTypeService ticketTypeService;
    @MockBean
    private VoucherService voucherService;
    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AISecurityService aiSecurityService;
    private Customer customer;
    private Event event;
    @BeforeEach
    public  void initData() throws Exception {
        customer = new Customer();
        customer.setCustomerId(1L);

        event = new Event();
        event.setId(100L);
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    }

    // TC-01: Registered (PAID order)
    @Test
    void TC01_HasCustomerRegistered_ShouldReturnTrue_WhenPaidOrderExists() {
        Order paid = new Order();
        paid.setCustomer(customer);
        paid.setEvent(event);
        paid.setStatus(OrderStatus.PAID);

        when(orderRepo.findByCustomerId(customer.getCustomerId()))
                .thenReturn(List.of(paid));

        boolean result = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

        assertTrue(result, "PAID order should be recognized as registered");
    }
    // TC-02: Pending – Not registered
    @Test
    void TC02_ShouldReturnFalse_WhenOnlyPendingOrderExists() {
        Order pending = new Order();
        pending.setCustomer(customer);
        pending.setEvent(event);
        pending.setStatus(OrderStatus.PENDING);

        when(orderRepo.findByCustomerId(customer.getCustomerId()))
                .thenReturn(List.of(pending));

        boolean result = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

        assertFalse(result, "Pending order should not count as registered");
    }   // TC-03: Cancelled – Not registered
    @Test
    void TC03_ShouldReturnFalse_WhenOnlyCancelledOrderExists() {
        Order cancelled = new Order();
        cancelled.setCustomer(customer);
        cancelled.setEvent(event);
        cancelled.setStatus(OrderStatus.CANCELLED);

        when(orderRepo.findByCustomerId(customer.getCustomerId()))
                .thenReturn(List.of(cancelled));

        boolean result = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

        assertFalse(result, "Cancelled order should not count as registered");
    }
    // TC-04: Empty list
    @Test
    void TC04_ShouldReturnFalse_WhenNoOrdersExist() {
        when(orderRepo.findByCustomerId(customer.getCustomerId()))
                .thenReturn(List.of());

        boolean result = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

        assertFalse(result, "No orders means not registered");
    }
    // TC-05: Multiple orders (1 PAID, 2 PENDING)
    @Test
    void TC05_ShouldReturnTrue_WhenMixedOrdersIncludePaid() {
        Order paid = new Order();
        paid.setCustomer(customer);
        paid.setEvent(event);
        paid.setStatus(OrderStatus.PAID);

        Order pending1 = new Order();
        pending1.setCustomer(customer);
        pending1.setEvent(event);
        pending1.setStatus(OrderStatus.PENDING);

        Order pending2 = new Order();
        pending2.setCustomer(customer);
        pending2.setEvent(event);
        pending2.setStatus(OrderStatus.PENDING);

        when(orderRepo.findByCustomerId(customer.getCustomerId()))
                .thenReturn(List.of(paid, pending1, pending2));

        boolean result = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), event.getId());

        assertTrue(result, "Any PAID order should make result true");
    }

}
