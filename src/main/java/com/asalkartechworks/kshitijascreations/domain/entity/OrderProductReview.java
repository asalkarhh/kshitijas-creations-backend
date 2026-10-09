package com.asalkartechworks.kshitijascreations.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @Entity @Table(name = "order_product_reviews")
public class OrderProductReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="order_id", nullable=false) private Long orderId;
    @Column(name="product_id", nullable=false) private Long productId;
    @Column(nullable=false, precision=2, scale=1) private BigDecimal rating;
    @Column(name="review_text", length=2000) private String reviewText;
    @Column(name="reviewer_name", nullable=false, length=200) private String reviewerName;
    @Column(name="reviewer_city", nullable=false, length=120) private String reviewerCity;
    @Column(nullable=false) private boolean published;
}
