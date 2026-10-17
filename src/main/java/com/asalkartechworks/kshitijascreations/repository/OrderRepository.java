package com.asalkartechworks.kshitijascreations.repository;

import com.asalkartechworks.kshitijascreations.domain.entity.Order;
import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByReviewToken(String reviewToken);
    List<Order> findByReviewProductIdAndReviewPublishedTrue(Long productId);

    long countByStatus(OrderStatus status);

    long countByViewedFalse();

    List<Order> findTop5ByOrderByCreatedAtDesc();

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderSequence();
}
