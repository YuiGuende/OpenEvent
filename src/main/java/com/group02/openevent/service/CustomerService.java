package com.group02.openevent.service;

import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    Customer getCustomerByAccountId(Long id);
    Page<OrderDTO> getOrdersByEvent(Long eventId, OrderStatus status, Pageable pageable);
}
