package com.asalkartechworks.kshitijascreations.banner.dto;

import com.asalkartechworks.kshitijascreations.banner.BannerStatus;
import java.time.Instant;

public record AdminBannerResponse(
        Long id,
        String title,
        String subtitle,
        String desktopImageUrl,
        String mobileImageUrl,
        String buttonText,
        String targetUrl,
        Instant startAt,
        Instant endAt,
        int displayOrder,
        boolean active,
        BannerStatus currentStatus,
        Instant createdAt,
        Instant updatedAt
) {
}

