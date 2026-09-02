package com.asalkartechworks.kshitijascreations.banner;

import org.springframework.data.domain.Sort;

public final class BannerSort {

    private BannerSort() {
    }

    public static Sort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort();
        }
        return switch (sort.trim().toLowerCase()) {
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"));
            default -> defaultSort();
        };
    }

    public static Sort defaultSort() {
        return Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("createdAt"));
    }
}

