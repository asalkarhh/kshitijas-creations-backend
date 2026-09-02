package com.asalkartechworks.kshitijascreations.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 20, message = "Quantity must not exceed 20")
        int quantity
) {
}
