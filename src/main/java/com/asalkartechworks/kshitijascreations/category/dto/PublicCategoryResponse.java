package com.asalkartechworks.kshitijascreations.category.dto;

public record PublicCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        int displayOrder
) {
}
