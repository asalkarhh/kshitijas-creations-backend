package com.asalkartechworks.kshitijascreations.order.dto;

import java.util.List;

public record ReviewDetails(String orderNumber, String customerName, List<ReviewProduct> products) {
    public record ReviewProduct(Long id, String name) {}
}
