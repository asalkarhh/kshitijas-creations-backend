package com.asalkartechworks.kshitijascreations.order.dto;

import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderDetail(
        Long id,
        String orderNumber,
        String customerName,
        String mobileNumber,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String customerNote,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        OrderStatus status,
        boolean viewed,
        Instant createdAt,
        Instant updatedAt,
        List<AdminOrderItemResponse> items, List<AdminProductReview> reviews
        , BigDecimal reviewRating, String reviewText, String reviewerName, String reviewerCity,
        Long reviewProductId, boolean reviewPublished, boolean reviewUsed
) {
}
