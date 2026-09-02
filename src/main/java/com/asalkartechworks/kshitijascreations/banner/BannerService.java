package com.asalkartechworks.kshitijascreations.banner;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.api.PageResponse;
import com.asalkartechworks.kshitijascreations.banner.dto.AdminBannerResponse;
import com.asalkartechworks.kshitijascreations.banner.dto.PublicBannerResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.Banner;
import com.asalkartechworks.kshitijascreations.repository.BannerRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final BannerFileStorage bannerFileStorage;
    private final BannerImageValidator bannerImageValidator;

    @Transactional(readOnly = true)
    public PageResponse<AdminBannerResponse> adminList(
            Boolean active,
            String status,
            Pageable pageable
    ) {
        Specification<Banner> spec = buildAdminSpec(active, status);
        Page<Banner> banners = bannerRepository.findAll(spec, pageable);
        return PageResponse.from(banners.map(bannerMapper::toAdminResponse));
    }

    @Transactional(readOnly = true)
    public List<PublicBannerResponse> publicList() {
        return bannerRepository.findVisibleBanners(Instant.now()).stream()
                .map(bannerMapper::toPublicResponse)
                .toList();
    }

    @Transactional
    public AdminBannerResponse create(
            String title,
            String subtitle,
            MultipartFile desktopImage,
            MultipartFile mobileImage,
            String buttonText,
            String targetUrl,
            Instant startAt,
            Instant endAt,
            Integer displayOrder,
            Boolean active
    ) {
        validateTitle(title);
        validateDateRange(startAt, endAt);
        validateTargetUrl(targetUrl);

        if (desktopImage == null || desktopImage.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Desktop image is required",
                    Map.of("desktopImage", "Desktop image is required"));
        }

        Banner banner = new Banner();
        banner.setTitle(title.trim());
        banner.setSubtitle(subtitle);
        banner.setButtonText(buttonText);
        banner.setTargetUrl(targetUrl);
        banner.setStartAt(startAt);
        banner.setEndAt(endAt);
        banner.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        banner.setActive(active != null ? active : true);

        // Persist first to get the ID for the file path
        banner.setImageKey("placeholder");
        banner = bannerRepository.saveAndFlush(banner);

        String desktopExtension = bannerImageValidator.validate(desktopImage);
        String desktopKey = bannerFileStorage.store(banner.getId(), desktopImage, desktopExtension);
        banner.setImageKey(desktopKey);

        if (mobileImage != null && !mobileImage.isEmpty()) {
            String mobileExtension = bannerImageValidator.validate(mobileImage);
            String mobileKey = bannerFileStorage.store(banner.getId(), mobileImage, mobileExtension);
            banner.setMobileImageKey(mobileKey);
        }

        return bannerMapper.toAdminResponse(banner);
    }

    @Transactional
    public AdminBannerResponse update(
            Long id,
            String title,
            String subtitle,
            MultipartFile desktopImage,
            MultipartFile mobileImage,
            String buttonText,
            String targetUrl,
            Instant startAt,
            Instant endAt,
            Integer displayOrder,
            Boolean active
    ) {
        Banner banner = getBanner(id);
        validateTitle(title);
        validateDateRange(startAt, endAt);
        validateTargetUrl(targetUrl);

        banner.setTitle(title.trim());
        banner.setSubtitle(subtitle);
        banner.setButtonText(buttonText);
        banner.setTargetUrl(targetUrl);
        banner.setStartAt(startAt);
        banner.setEndAt(endAt);
        if (displayOrder != null) {
            banner.setDisplayOrder(displayOrder);
        }
        if (active != null) {
            banner.setActive(active);
        }

        if (desktopImage != null && !desktopImage.isEmpty()) {
            String oldKey = banner.getImageKey();
            String extension = bannerImageValidator.validate(desktopImage);
            String newKey = bannerFileStorage.store(banner.getId(), desktopImage, extension);
            banner.setImageKey(newKey);
            scheduleDeleteAfterCommit(oldKey);
        }

        if (mobileImage != null && !mobileImage.isEmpty()) {
            String oldKey = banner.getMobileImageKey();
            String extension = bannerImageValidator.validate(mobileImage);
            String newKey = bannerFileStorage.store(banner.getId(), mobileImage, extension);
            banner.setMobileImageKey(newKey);
            scheduleDeleteAfterCommit(oldKey);
        }

        return bannerMapper.toAdminResponse(banner);
    }

    @Transactional
    public void delete(Long id) {
        Banner banner = getBanner(id);
        String imageKey = banner.getImageKey();
        String mobileImageKey = banner.getMobileImageKey();
        bannerRepository.delete(banner);
        scheduleDeleteAfterCommit(imageKey);
        scheduleDeleteAfterCommit(mobileImageKey);
    }

    private Banner getBanner(Long id) {
        return bannerRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "BANNER_NOT_FOUND", "Banner not found", Map.of("id", "No banner exists with this id")
        ));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Title is required",
                    Map.of("title", "Title is required"));
        }
        if (title.trim().length() > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Title must not exceed 200 characters",
                    Map.of("title", "Title must not exceed 200 characters"));
        }
    }

    private void validateDateRange(Instant startAt, Instant endAt) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "End date must be after start date",
                    Map.of("endAt", "End date must be after start date"));
        }
    }

    private void validateTargetUrl(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return;
        }
        String trimmed = targetUrl.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("/") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Target URL must be a relative path or use http/https",
                Map.of("targetUrl", "Only relative paths, http and https URLs are allowed"));
    }

    private Specification<Banner> buildAdminSpec(Boolean active, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (status != null && !status.isBlank()) {
                Instant now = Instant.now();
                switch (status.toUpperCase(Locale.ROOT)) {
                    case "INACTIVE" -> predicates.add(cb.equal(root.get("active"), false));
                    case "SCHEDULED" -> {
                        predicates.add(cb.equal(root.get("active"), true));
                        predicates.add(cb.greaterThan(root.get("startAt"), now));
                    }
                    case "ACTIVE" -> {
                        predicates.add(cb.equal(root.get("active"), true));
                        predicates.add(cb.or(cb.isNull(root.get("startAt")), cb.lessThanOrEqualTo(root.get("startAt"), now)));
                        predicates.add(cb.or(cb.isNull(root.get("endAt")), cb.greaterThanOrEqualTo(root.get("endAt"), now)));
                    }
                    case "EXPIRED" -> {
                        predicates.add(cb.equal(root.get("active"), true));
                        predicates.add(cb.lessThan(root.get("endAt"), now));
                    }
                    default -> { }
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void scheduleDeleteAfterCommit(String fileKey) {
        if (fileKey == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    bannerFileStorage.deleteQuietly(fileKey);
                }
            });
        } else {
            bannerFileStorage.deleteQuietly(fileKey);
        }
    }
}

