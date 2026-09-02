package com.asalkartechworks.kshitijascreations.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminProductResponse(
        Long id,
        String name,
        String slug,
        String productCode,
        String shortDescription,
        String description,
        BigDecimal price,
        boolean available,
        boolean featured,
        String instagramUrl,
        int displayOrder,
        boolean deleted,
        ProductCategoryResponse category,
        List<ProductMediaResponse> media,
        Instant createdAt,
        Instant updatedAt
) {
}
