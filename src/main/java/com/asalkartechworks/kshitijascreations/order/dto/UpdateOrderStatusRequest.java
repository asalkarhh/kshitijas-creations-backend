package com.asalkartechworks.kshitijascreations.order.dto;

import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
