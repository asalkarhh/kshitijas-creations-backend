package com.asalkartechworks.kshitijascreations.order.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record SubmitReviewsRequest(@NotEmpty List<@Valid SubmitReviewRequest> reviews) {}
