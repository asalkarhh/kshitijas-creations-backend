package com.asalkartechworks.kshitijascreations.order.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productCode,
        String productImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
