package com.asalkartechworks.kshitijascreations.product.dto;

import com.asalkartechworks.kshitijascreations.domain.enums.MediaType;

public record ProductMediaResponse(
        Long id,
        MediaType mediaType,
        String url,
        String thumbnailUrl,
        String altText,
        int displayOrder,
        boolean primaryMedia
) {
}
