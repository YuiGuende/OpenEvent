package com.group02.openevent.model.order;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }
    
    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return OrderStatus.PENDING; // Default
        }
        
        try {
            // Nếu là CONFIRMED trong database, map sang PAID
            if ("CONFIRMED".equalsIgnoreCase(dbData)) {
                return OrderStatus.PAID;
            }
            return OrderStatus.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Nếu không tìm thấy giá trị enum hợp lệ, trả về PENDING
            System.err.println("Warning: Unknown OrderStatus value in database: " + dbData + ". Using PENDING as default.");
            return OrderStatus.PENDING;
        }
    }
}

