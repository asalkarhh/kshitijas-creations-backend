package com.asalkartechworks.kshitijascreations.order.dto;

import java.math.BigDecimal;

public record PublicProductReview(
        BigDecimal rating,
        String text,
        String name,
        String city
) {}
