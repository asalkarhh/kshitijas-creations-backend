package com.asalkartechworks.kshitijascreations.banner;

import com.asalkartechworks.kshitijascreations.banner.dto.AdminBannerResponse;
import com.asalkartechworks.kshitijascreations.banner.dto.PublicBannerResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.Banner;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

@Component
public class BannerMapper {

    private final com.asalkartechworks.kshitijascreations.media.MediaUrlResolver mediaUrlResolver;

    public BannerMapper(com.asalkartechworks.kshitijascreations.media.MediaUrlResolver mediaUrlResolver) {
        this.mediaUrlResolver = mediaUrlResolver;
    }

    public AdminBannerResponse toAdminResponse(Banner banner) {
        Instant now = Instant.now();
        return new AdminBannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getSubtitle(),
                mediaUrlResolver.resolve(banner.getImageKey()),
                mediaUrlResolver.resolve(banner.getMobileImageKey()),
                banner.getButtonText(),
                banner.getTargetUrl(),
                banner.getStartAt(),
                banner.getEndAt(),
                banner.getDisplayOrder(),
                banner.isActive(),
                BannerStatus.compute(banner.isActive(), banner.getStartAt(), banner.getEndAt(), now),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }

    public PublicBannerResponse toPublicResponse(Banner banner) {
        return new PublicBannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getSubtitle(),
                mediaUrlResolver.resolve(banner.getImageKey()),
                mediaUrlResolver.resolve(banner.getMobileImageKey()),
                banner.getButtonText(),
                banner.getTargetUrl()
        );
    }
}

