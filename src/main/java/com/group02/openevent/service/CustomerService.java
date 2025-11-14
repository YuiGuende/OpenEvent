package com.group02.openevent.service;

import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomerService {
    Optional<Customer> findByUserId(Long userId);
    Customer save(Customer customer);
    Customer getOrCreateByUserId(Long userId);
    Customer getCustomerByAccountId(Long id);
    Customer getCurrentCustomer(jakarta.servlet.http.HttpSession session);
    Page<OrderDTO> getOrdersByEvent(Long eventId, OrderStatus status, Pageable pageable);
}