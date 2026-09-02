package com.asalkartechworks.kshitijascreations.product;

import com.asalkartechworks.kshitijascreations.api.PageResponse;
import com.asalkartechworks.kshitijascreations.product.dto.PublicProductResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/products")
public class PublicProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<PublicProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "category") String categorySlug,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return productService.publicList(
                search, categorySlug, featured, PageRequest.of(page, size, ProductSort.from(sort))
        );
    }

    @GetMapping("/{slug}")
    public PublicProductResponse bySlug(@PathVariable String slug) {
        return productService.publicBySlug(slug);
    }
}
