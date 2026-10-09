package com.asalkartechworks.kshitijascreations.order.dto;
import java.math.BigDecimal;
public record AdminProductReview(Long id, Long productId, String productName, BigDecimal rating, String text, String reviewerName, String reviewerCity, boolean published) {}
