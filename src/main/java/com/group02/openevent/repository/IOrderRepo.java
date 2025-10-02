package com.group02.openevent.repository;

import com.group02.openevent.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrderRepo extends JpaRepository<Order, Long> {
}


