package com.asalkartechworks.kshitijascreations.domain.entity;

import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orders")
@NoArgsConstructor
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "mobile_number", nullable = false, length = 30)
    private String mobileNumber;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "customer_note", length = 1000)
    private String customerNote;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "whatsapp_opened", nullable = false)
    private boolean whatsappOpened;

    @Column(name = "viewed", nullable = false)
    private boolean viewed;

    @Column(name = "review_token", unique = true, length = 80)
    private String reviewToken;

    @Column(name = "review_used", nullable = false)
    private boolean reviewUsed;

    @Column(name = "review_rating", precision = 2, scale = 1)
    private BigDecimal reviewRating;

    @Column(name = "review_text", length = 2000)
    private String reviewText;
    @Column(name = "review_started", nullable = false) private boolean reviewStarted;
    @Column(name = "reviewer_name", length = 200) private String reviewerName;
    @Column(name = "reviewer_city", length = 120) private String reviewerCity;
    @Column(name = "review_product_id") private Long reviewProductId;
    @Column(name = "review_published", nullable = false) private boolean reviewPublished;
}
