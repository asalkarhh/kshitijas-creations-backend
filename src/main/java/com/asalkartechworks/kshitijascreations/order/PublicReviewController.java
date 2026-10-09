package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.order.dto.ReviewDetails;
import com.asalkartechworks.kshitijascreations.order.dto.SubmitReviewRequest;
import com.asalkartechworks.kshitijascreations.order.dto.SubmitReviewsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.asalkartechworks.kshitijascreations.repository.OrderRepository;
import com.asalkartechworks.kshitijascreations.order.dto.PublicProductReview;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class PublicReviewController {
    private final OrderService orderService;
    private final com.asalkartechworks.kshitijascreations.repository.OrderProductReviewRepository reviewRepository;

    @GetMapping("/product/{productId}")
    public List<PublicProductReview> productReviews(@PathVariable Long productId) {
        return reviewRepository.findByProductIdAndPublishedTrue(productId).stream().map(o -> new PublicProductReview(o.getRating(), o.getReviewText() == null ? "" : o.getReviewText(), o.getReviewerName(), o.getReviewerCity())).toList();
    }

    @GetMapping("/{token}")
    public ReviewDetails details(@PathVariable String token) { return orderService.reviewDetails(token); }

    @PostMapping("/{token}")
    public void submit(@PathVariable String token, @Valid @RequestBody SubmitReviewRequest request) {
        orderService.submitReviews(token, new SubmitReviewsRequest(List.of(request)));
    }

    @PostMapping("/{token}/all") public void submitAll(@PathVariable String token, @Valid @RequestBody SubmitReviewsRequest request) { orderService.submitReviews(token, request); }
}
