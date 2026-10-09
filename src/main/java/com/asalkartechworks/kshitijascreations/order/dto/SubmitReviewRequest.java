package com.asalkartechworks.kshitijascreations.order.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubmitReviewRequest(
        @NotNull @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rating,
        @NotNull Long productId,
        @Size(max = 2000) String text
) {}
