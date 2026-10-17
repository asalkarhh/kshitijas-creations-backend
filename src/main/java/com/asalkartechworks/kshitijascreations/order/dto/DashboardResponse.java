package com.asalkartechworks.kshitijascreations.order.dto;

import java.util.List;

public record DashboardResponse(
        long totalProducts,
        long availableProducts,
        long totalOrders,
        long placedOrders,
        long confirmedOrders,
        long completedOrders,
        long cancelledOrders,
        long unviewedOrders,
        List<AdminOrderSummary> recentOrders
) {
}
