package com.asalkartechworks.kshitijascreations.banner.dto;

public record PublicBannerResponse(
        Long id,
        String title,
        String subtitle,
        String desktopImageUrl,
        String mobileImageUrl,
        String buttonText,
        String targetUrl
) {
}

