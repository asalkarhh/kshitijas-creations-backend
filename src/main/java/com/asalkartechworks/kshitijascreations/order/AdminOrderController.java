package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.api.PageResponse;
import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderDetail;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderSummary;
import com.asalkartechworks.kshitijascreations.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import com.asalkartechworks.kshitijascreations.order.dto.ReviewLinkResponse;
import com.asalkartechworks.kshitijascreations.config.AppProperties;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final AppProperties appProperties;

    @GetMapping
    public PageResponse<AdminOrderSummary> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Sort sorting = switch (sort) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"));
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };
        return orderService.adminList(search, status, fromDate, toDate, PageRequest.of(page, size, sorting));
    }

    @GetMapping("/unviewed-count")
    public Map<String, Long> unviewedCount() {
        return Map.of("count", orderService.countUnviewed());
    }

    @GetMapping("/{id}")
    public AdminOrderDetail detail(@PathVariable Long id) {
        return orderService.adminDetail(id);
    }

    @PatchMapping("/{id}/status")
    public AdminOrderDetail updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/review-link")
    public ReviewLinkResponse reviewLink(@PathVariable Long id, @RequestHeader(value = "Origin", required = false) String origin) {
        return orderService.createReviewLink(id, appProperties.getPublicSiteUrl());
    }

    @PatchMapping("/{id}/review")
    public void publishReview(@PathVariable Long id, @RequestParam boolean published) {
        orderService.setReviewPublished(id, published);
    }

    @PatchMapping("/review/{reviewId}")
    public void publishProductReview(@PathVariable Long reviewId, @RequestParam boolean published) { orderService.setProductReviewPublished(reviewId, published); }
}
