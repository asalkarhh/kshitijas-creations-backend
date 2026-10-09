package com.asalkartechworks.kshitijascreations.repository;
import com.asalkartechworks.kshitijascreations.domain.entity.OrderProductReview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderProductReviewRepository extends JpaRepository<OrderProductReview, Long> {
    List<OrderProductReview> findByOrderId(Long orderId);
    List<OrderProductReview> findByProductIdAndPublishedTrue(Long productId);
}
