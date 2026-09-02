package com.asalkartechworks.kshitijascreations.repository;

import com.asalkartechworks.kshitijascreations.domain.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findAllByOrderIdWithProduct(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
    int countByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi.order.id, COUNT(oi) FROM OrderItem oi WHERE oi.order.id IN :orderIds GROUP BY oi.order.id")
    List<Object[]> countByOrderIds(@Param("orderIds") List<Long> orderIds);
}
