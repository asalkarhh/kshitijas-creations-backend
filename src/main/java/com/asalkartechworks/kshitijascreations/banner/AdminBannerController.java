package com.asalkartechworks.kshitijascreations.banner;

import com.asalkartechworks.kshitijascreations.api.PageResponse;
import com.asalkartechworks.kshitijascreations.banner.dto.AdminBannerResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/banners")
public class AdminBannerController {

    private final BannerService bannerService;

    @GetMapping
    public PageResponse<AdminBannerResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "displayOrder") String sort
    ) {
        return bannerService.adminList(active, status, PageRequest.of(page, size, BannerSort.from(sort)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBannerResponse create(
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestPart("desktopImage") MultipartFile desktopImage,
            @RequestPart(value = "mobileImage", required = false) MultipartFile mobileImage,
            @RequestParam(required = false) String buttonText,
            @RequestParam(required = false) String targetUrl,
            @RequestParam(required = false) Instant startAt,
            @RequestParam(required = false) Instant endAt,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean active
    ) {
        return bannerService.create(title, subtitle, desktopImage, mobileImage,
                buttonText, targetUrl, startAt, endAt, displayOrder, active);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminBannerResponse update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestPart(value = "desktopImage", required = false) MultipartFile desktopImage,
            @RequestPart(value = "mobileImage", required = false) MultipartFile mobileImage,
            @RequestParam(required = false) String buttonText,
            @RequestParam(required = false) String targetUrl,
            @RequestParam(required = false) Instant startAt,
            @RequestParam(required = false) Instant endAt,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean active
    ) {
        return bannerService.update(id, title, subtitle, desktopImage, mobileImage,
                buttonText, targetUrl, startAt, endAt, displayOrder, active);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bannerService.delete(id);
    }
}

