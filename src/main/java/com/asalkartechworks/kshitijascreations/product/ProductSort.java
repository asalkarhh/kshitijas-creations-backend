package com.asalkartechworks.kshitijascreations.product;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class ProductSort {

    private ProductSort() {
    }

    public static Sort from(String value) {
        String sort = value == null || value.isBlank() ? "newest" : value;
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "priceAsc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            case "displayOrder" -> Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name"));
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SORT",
                    "Unsupported sort value",
                    Map.of("sort", "Use newest, priceAsc, priceDesc, or displayOrder")
            );
        };
    }
}
