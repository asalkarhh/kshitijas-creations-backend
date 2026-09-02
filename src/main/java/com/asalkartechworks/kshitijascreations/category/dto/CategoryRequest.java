package com.asalkartechworks.kshitijascreations.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 10_000) String description,
        @NotNull @PositiveOrZero Integer displayOrder,
        @NotNull Boolean active
) {
}
