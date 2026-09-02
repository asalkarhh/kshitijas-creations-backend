package com.asalkartechworks.kshitijascreations.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicProductResponse(
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
        ProductCategoryResponse category,
        List<ProductMediaResponse> media,
        ProductMediaResponse primaryImage
) {
}
