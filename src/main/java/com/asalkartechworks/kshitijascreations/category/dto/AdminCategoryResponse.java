package com.asalkartechworks.kshitijascreations.category.dto;

import java.time.Instant;

public record AdminCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        int displayOrder,
        long productCount,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
