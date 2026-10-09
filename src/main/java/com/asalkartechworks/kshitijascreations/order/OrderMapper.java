package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.domain.entity.Order;
import com.asalkartechworks.kshitijascreations.domain.entity.OrderItem;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderDetail;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderItemResponse;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderSummary;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

@Component
public class OrderMapper {

    private final com.asalkartechworks.kshitijascreations.media.MediaUrlResolver mediaUrlResolver;

    public OrderMapper(com.asalkartechworks.kshitijascreations.media.MediaUrlResolver mediaUrlResolver) {
        this.mediaUrlResolver = mediaUrlResolver;
    }

    public AdminOrderSummary toSummary(Order order, int itemCount) {
        return new AdminOrderSummary(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getMobileNumber(),
                order.getCity(),
                order.getTotalAmount(),
                itemCount,
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    public AdminOrderDetail toDetail(Order order, List<OrderItem> items) {
        return new AdminOrderDetail(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getMobileNumber(),
                order.getAddressLine(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getCustomerNote(),
                order.getSubtotal(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items.stream().map(this::toItemResponse).toList(), List.of()
                , order.getReviewRating(), order.getReviewText(), order.getReviewerName(), order.getReviewerCity(),
                order.getReviewProductId(), order.isReviewPublished(), order.isReviewUsed()
        );
    }

    public AdminOrderItemResponse toItemResponse(OrderItem item) {
        return new AdminOrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProductCode(),
                mediaUrlResolver.resolve(item.getProductImageKey()),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }
}
