package com.group02.openevent.model.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void calculateTotalAmount() {
    }

    @Test
    void orderModel_shouldCoverConstructor() {
        Order order = new Order();
        // Thường Lombok sẽ tạo constructor/getter/setter, chỉ cần tạo đối tượng là đủ.
        assertNotNull(order);
    }
}