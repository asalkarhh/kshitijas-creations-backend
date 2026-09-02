package com.asalkartechworks.kshitijascreations.order.dto;

import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import java.math.BigDecimal;

public record PlaceOrderResponse(
        String orderNumber,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        String whatsappUrl,
        String message
) {
}
