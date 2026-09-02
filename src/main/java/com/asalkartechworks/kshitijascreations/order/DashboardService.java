package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.domain.entity.Order;
import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderSummary;
import com.asalkartechworks.kshitijascreations.order.dto.DashboardResponse;
import com.asalkartechworks.kshitijascreations.repository.OrderItemRepository;
import com.asalkartechworks.kshitijascreations.repository.OrderRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long totalProducts = productRepository.count();
        long availableProducts = productRepository.count(
                (root, query, cb) -> cb.and(cb.equal(root.get("available"), true), cb.equal(root.get("deleted"), false))
        );

        long totalOrders = orderRepository.count();
        long placedOrders = orderRepository.countByStatus(OrderStatus.PLACED);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        List<Order> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc();
        List<Long> orderIds = recentOrders.stream().map(Order::getId).toList();
        Map<Long, Integer> itemCounts = getItemCounts(orderIds);

        List<AdminOrderSummary> recentSummaries = recentOrders.stream()
                .map(order -> orderMapper.toSummary(order, itemCounts.getOrDefault(order.getId(), 0)))
                .toList();

        return new DashboardResponse(
                totalProducts,
                availableProducts,
                totalOrders,
                placedOrders,
                confirmedOrders,
                completedOrders,
                cancelledOrders,
                recentSummaries
        );
    }

    private Map<Long, Integer> getItemCounts(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderItemRepository.countByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }
}
