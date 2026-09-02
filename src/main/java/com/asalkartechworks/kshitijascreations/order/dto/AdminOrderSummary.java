package com.asalkartechworks.kshitijascreations.order.dto;

import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderSummary(
        Long id,
        String orderNumber,
        String customerName,
        String mobileNumber,
        String city,
        BigDecimal totalAmount,
        int itemCount,
        OrderStatus status,
        Instant createdAt
) {
}
