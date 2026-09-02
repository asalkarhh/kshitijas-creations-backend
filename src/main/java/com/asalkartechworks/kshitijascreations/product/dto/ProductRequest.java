package com.asalkartechworks.kshitijascreations.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 80) String productCode,
        @Size(max = 500) String shortDescription,
        @Size(max = 20_000) String description,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        @NotNull Boolean available,
        @NotNull Boolean featured,
        @Size(max = 500) String instagramUrl,
        @NotNull @PositiveOrZero Integer displayOrder
) {
}
