package com.asalkartechworks.kshitijascreations.banner;

import com.asalkartechworks.kshitijascreations.banner.dto.PublicBannerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/banners")
public class PublicBannerController {

    private final BannerService bannerService;

    @GetMapping
    public List<PublicBannerResponse> list() {
        return bannerService.publicList();
    }
}

