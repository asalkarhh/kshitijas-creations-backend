package com.asalkartechworks.kshitijascreations.order;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.api.PageResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.Order;
import com.asalkartechworks.kshitijascreations.domain.entity.OrderItem;
import com.asalkartechworks.kshitijascreations.domain.entity.Product;
import com.asalkartechworks.kshitijascreations.domain.entity.ProductMedia;
import com.asalkartechworks.kshitijascreations.domain.enums.OrderStatus;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderDetail;
import com.asalkartechworks.kshitijascreations.order.dto.AdminOrderSummary;
import com.asalkartechworks.kshitijascreations.order.dto.OrderItemRequest;
import com.asalkartechworks.kshitijascreations.order.dto.PlaceOrderRequest;
import com.asalkartechworks.kshitijascreations.order.dto.PlaceOrderResponse;
import com.asalkartechworks.kshitijascreations.repository.OrderItemRepository;
import com.asalkartechworks.kshitijascreations.repository.OrderRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductMediaRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> FINAL_STATUSES = Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PLACED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final WhatsAppLinkService whatsAppLinkService;
    private final OrderMapper orderMapper;

    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {
        // Deduplicate items by productId, combining quantities
        Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            quantityByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // Validate combined quantities
        for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
            if (entry.getValue() > 20) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        "Combined quantity for product " + entry.getKey() + " exceeds maximum of 20",
                        Map.of("quantity", "Combined quantity must not exceed 20"));
            }
        }

        // Fetch all products in one query
        List<Long> productIds = new ArrayList<>(quantityByProduct.keySet());
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Validate each product and fetch primary image keys
        for (Long productId : productIds) {
            Product product = productsById.get(productId);
            if (product == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId,
                        Map.of("productId", "No product exists with id " + productId));
            }
            if (product.isDeleted()) {
                throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_DELETED",
                        "Product is no longer available: " + product.getName(),
                        Map.of("productId", "Product has been removed"));
            }
            if (!product.isAvailable()) {
                throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE",
                        "Product is currently unavailable: " + product.getName(),
                        Map.of("productId", "Product is not available"));
            }
        }

        // Fetch primary images for all products in one query
        Map<Long, String> primaryImageKeys = getPrimaryImageKeys(productIds);

        // Create order
        Order order = new Order();
        order.setOrderNumber(orderNumberGenerator.next());
        order.setCustomerName(request.customerName().trim());
        order.setMobileNumber(request.mobileNumber().trim());
        order.setAddressLine(request.addressLine().trim());
        order.setCity(request.city().trim());
        order.setState(request.state().trim());
        order.setPostalCode(request.postalCode().trim());
        order.setCustomerNote(request.customerNote());
        order.setStatus(OrderStatus.PLACED);
        order.setWhatsappOpened(false);

        // Calculate totals and create items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
            Product product = productsById.get(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductCode(product.getProductCode());
            item.setProductImageKey(primaryImageKeys.get(product.getId()));
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity);
            item.setLineTotal(lineTotal);
            items.add(item);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);

        orderRepository.save(order);
        orderItemRepository.saveAll(items);

        String whatsappUrl = whatsAppLinkService.generateLink(order, items);

        return new PlaceOrderResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getSubtotal(),
                order.getTotalAmount(),
                whatsappUrl,
                "Order submitted successfully"
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummary> adminList(
            String search,
            OrderStatus status,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        Specification<Order> spec = buildAdminSpec(search, status, fromDate, toDate);
        Page<Order> orders = orderRepository.findAll(spec, pageable);

        // Batch fetch item counts
        List<Long> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<Long, Integer> itemCounts = getItemCounts(orderIds);

        return PageResponse.from(orders.map(order ->
                orderMapper.toSummary(order, itemCounts.getOrDefault(order.getId(), 0))
        ));
    }

    @Transactional(readOnly = true)
    public AdminOrderDetail adminDetail(Long id) {
        Order order = getOrder(id);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdWithProduct(id);
        return orderMapper.toDetail(order, items);
    }

    @Transactional
    public AdminOrderDetail updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrder(id);
        OrderStatus currentStatus = order.getStatus();

        if (FINAL_STATUSES.contains(currentStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    currentStatus + " is a final status and cannot be changed",
                    Map.of("status", currentStatus + " orders cannot be modified"));
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + currentStatus + " to " + newStatus,
                    Map.of("status", "Allowed transitions from " + currentStatus + ": " + allowed));
        }

        order.setStatus(newStatus);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdWithProduct(id);
        return orderMapper.toDetail(order, items);
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found",
                Map.of("id", "No order exists with this id")
        ));
    }

    private Map<Long, String> getPrimaryImageKeys(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductMedia> allMedia = productMediaRepository
                .findAllByProductIdInOrderByPrimaryMediaDescDisplayOrderAsc(productIds);
        // For each product, take the first image (primary first due to ordering)
        Map<Long, String> result = new LinkedHashMap<>();
        for (ProductMedia media : allMedia) {
            result.putIfAbsent(media.getProduct().getId(), media.getFileKey());
        }
        return result;
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

    private Specification<Order> buildAdminSpec(String search, OrderStatus status, Instant fromDate, Instant toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate orderNumberMatch = cb.like(cb.lower(root.get("orderNumber")), pattern);
                Predicate customerNameMatch = cb.like(cb.lower(root.get("customerName")), pattern);
                Predicate mobileMatch = cb.like(root.get("mobileNumber"), "%" + search.trim() + "%");
                predicates.add(cb.or(orderNumberMatch, customerNameMatch, mobileMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
