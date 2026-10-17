package com.asalkartechworks.kshitijascreations.order;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asalkartechworks.kshitijascreations.IntegrationTestBase;
import com.asalkartechworks.kshitijascreations.domain.entity.Order;
import com.asalkartechworks.kshitijascreations.domain.entity.OrderItem;
import com.asalkartechworks.kshitijascreations.repository.CategoryRepository;
import com.asalkartechworks.kshitijascreations.repository.OrderItemRepository;
import com.asalkartechworks.kshitijascreations.repository.OrderRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class OrderIntegrationTest extends IntegrationTestBase {

    private static final String ADMIN_EMAIL = "admin@kshitijascreations.test";
    private static final String ADMIN_PASSWORD = "CorrectPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanOrders() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    // ---- 1. Successful guest order ----
    @Test
    void successfulGuestOrder() throws Exception {
        long productId = createAvailableProduct("Floral Jewellery", "FJ-001", 699);

        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(startsWith("KC-")))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.subtotal").value(1398.00))
                .andExpect(jsonPath("$.totalAmount").value(1398.00))
                .andExpect(jsonPath("$.whatsappUrl", not(blankOrNullString())))
                .andExpect(jsonPath("$.whatsappUrl").value(startsWith("https://wa.me/")))
                .andExpect(jsonPath("$.message").value("Order submitted successfully"));
    }

    // ---- 2. Empty cart rejection ----
    @Test
    void emptyCartRejected() throws Exception {
        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJsonWithItems(List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    // ---- 3. Invalid mobile number ----
    @Test
    void invalidMobileNumber() throws Exception {
        long productId = createAvailableProduct("Ring", "RNG-001", 500);

        Map<String, Object> body = orderMap(productId, 1);
        body.put("mobileNumber", "1234567890");

        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.mobileNumber").exists());
    }

    // ---- 4. Invalid PIN code ----
    @Test
    void invalidPinCode() throws Exception {
        long productId = createAvailableProduct("Ring", "RNG-002", 500);

        Map<String, Object> body = orderMap(productId, 1);
        body.put("postalCode", "12345");

        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.postalCode").exists());
    }

    // ---- 5. Missing product ----
    @Test
    void missingProduct() throws Exception {
        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(99999L, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    // ---- 6. Deleted product ----
    @Test
    void deletedProduct() throws Exception {
        long productId = createAvailableProduct("Deleted Ring", "DR-001", 500);
        // Soft delete
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/products/{id}", productId)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_DELETED"));
    }

    // ---- 7. Unavailable product ----
    @Test
    void unavailableProduct() throws Exception {
        long categoryId = createCategory("TestCat");
        long productId = createProductWithAvailability(categoryId, "Unavailable Ring", "UR-001", 500, false);

        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));
    }

    // ---- 8. Quantity validation ----
    @Test
    void quantityExceedsMaximum() throws Exception {
        long productId = createAvailableProduct("Ring", "RNG-003", 500);

        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 21)))
                .andExpect(status().isBadRequest());
    }

    // ---- 9. Backend price calculation ----
    @Test
    void backendPriceCalculation() throws Exception {
        long productId = createAvailableProduct("Necklace", "NK-001", 1250);

        MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 3)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(0, new BigDecimal("3750.00").compareTo(new BigDecimal(response.get("subtotal").asText())));
        Assertions.assertEquals(0, new BigDecimal("3750.00").compareTo(new BigDecimal(response.get("totalAmount").asText())));
    }

    // ---- 10. Multiple-item total ----
    @Test
    void multipleItemTotal() throws Exception {
        long product1 = createAvailableProduct("Ring A", "RA-001", 500);
        long product2 = createAvailableProduct("Ring B", "RB-001", 750);

        List<Map<String, Object>> items = List.of(
                Map.of("productId", product1, "quantity", 2),
                Map.of("productId", product2, "quantity", 1)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJsonWithItems(items)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        // 500*2 + 750*1 = 1750
        Assertions.assertEquals(0, new BigDecimal("1750.00").compareTo(new BigDecimal(response.get("totalAmount").asText())));
    }

    // ---- 11. Immutable order-item snapshot ----
    @Test
    void immutableOrderItemSnapshot() throws Exception {
        long productId = createAvailableProduct("Snapshot Ring", "SR-001", 999);

        MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("orderNumber").asText();
        Optional<Order> order = orderRepository.findByOrderNumber(orderNumber);
        Assertions.assertTrue(order.isPresent());

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.get().getId());
        Assertions.assertEquals(1, items.size());
        Assertions.assertEquals("Snapshot Ring", items.get(0).getProductName());
        Assertions.assertEquals("SR-001", items.get(0).getProductCode());
        Assertions.assertEquals(new BigDecimal("999.00"), items.get(0).getUnitPrice());
    }

    // ---- 12. Unique concurrency-safe order number ----
    @Test
    void uniqueOrderNumbers() throws Exception {
        long productId = createAvailableProduct("Order Test", "OT-001", 100);

        List<String> orderNumbers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson(productId, 1)))
                    .andExpect(status().isCreated())
                    .andReturn();
            orderNumbers.add(objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("orderNumber").asText());
        }

        // All order numbers should be unique
        Assertions.assertEquals(3, orderNumbers.stream().distinct().count());
        // All should start with KC-
        orderNumbers.forEach(n -> Assertions.assertTrue(n.startsWith("KC-")));
    }

    // ---- 13. WhatsApp message encoding ----
    @Test
    void whatsappMessageEncoding() throws Exception {
        long productId = createAvailableProduct("Floral Jewellery", "FJ-002", 699);

        MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 2)))
                .andExpect(status().isCreated())
                .andReturn();

        String whatsappUrl = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("whatsappUrl").asText();

        Assertions.assertTrue(whatsappUrl.startsWith("https://wa.me/"));
        Assertions.assertTrue(whatsappUrl.contains("text="));

        // Decode and verify message contents
        String encoded = whatsappUrl.substring(whatsappUrl.indexOf("text=") + 5);
        String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        Assertions.assertTrue(decoded.contains("Floral Jewellery"));
        Assertions.assertTrue(decoded.contains("Saurabh Asalkar"));
        Assertions.assertTrue(decoded.contains("KC-"));
    }

    // ---- 14. Order saved independently of WhatsApp ----
    @Test
    void orderSavedIndependentlyOfWhatsApp() throws Exception {
        long productId = createAvailableProduct("Saved Ring", "SVR-001", 500);

        MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("orderNumber").asText();

        // Order is persisted regardless of WhatsApp
        Optional<Order> order = orderRepository.findByOrderNumber(orderNumber);
        Assertions.assertTrue(order.isPresent());
        Assertions.assertEquals("PLACED", order.get().getStatus().name());
    }

    // ---- 15. Admin order listing ----
    @Test
    void adminOrderListing() throws Exception {
        long productId = createAvailableProduct("List Test", "LT-001", 100);
        placeTestOrder(productId);
        placeTestOrder(productId);

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderNumber").exists())
                .andExpect(jsonPath("$.content[0].itemCount").value(1));
    }

    // ---- 16. Search and status filters ----
    @Test
    void searchAndStatusFilters() throws Exception {
        long productId = createAvailableProduct("Filter Test", "FT-001", 100);
        placeTestOrder(productId);

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "PLACED")
                        .param("search", "Saurabh")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "CONFIRMED")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- 17. Admin order details ----
    @Test
    void adminOrderDetails() throws Exception {
        long productId = createAvailableProduct("Detail Test", "DT-001", 750);
        String orderNumber = placeTestOrder(productId);

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();

        mockMvc.perform(get("/api/v1/admin/orders/{id}", order.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.customerName").value("Saurabh Asalkar"))
                .andExpect(jsonPath("$.mobileNumber").value("8087818729"))
                .andExpect(jsonPath("$.addressLine").value("Complete delivery address"))
                .andExpect(jsonPath("$.city").value("Vita"))
                .andExpect(jsonPath("$.state").value("Maharashtra"))
                .andExpect(jsonPath("$.postalCode").value("415311"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Detail Test"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(750.00));
    }

    // ---- 18. Valid status transition ----
    @Test
    void validStatusTransition() throws Exception {
        long productId = createAvailableProduct("Transition Test", "TT-001", 100);
        String orderNumber = placeTestOrder(productId);
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ---- 19. Invalid status transition ----
    @Test
    void invalidStatusTransition() throws Exception {
        long productId = createAvailableProduct("Bad Transition", "BT-001", 100);
        String orderNumber = placeTestOrder(productId);
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();

        // PLACED → COMPLETED is not allowed
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    // ---- 20. Unauthorized admin access ----
    @Test
    void unauthorizedAdminAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    // ---- 21. Dashboard counts ----
    @Test
    void dashboardCounts() throws Exception {
        long productId = createAvailableProduct("Dashboard Test", "DB-001", 100);
        placeTestOrder(productId);
        placeTestOrder(productId);

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(1))
                .andExpect(jsonPath("$.availableProducts").value(1))
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.placedOrders").value(2))
                .andExpect(jsonPath("$.confirmedOrders").value(0))
                .andExpect(jsonPath("$.unviewedOrders").value(2))
                .andExpect(jsonPath("$.recentOrders", hasSize(2)));
    }

    // ---- 22. Unviewed count reduces on detail view ----
    @Test
    void unviewedCountReducesWhenOrderViewed() throws Exception {
        long productId = createAvailableProduct("Unviewed Test", "UV-001", 100);
        String orderNum1 = placeTestOrder(productId);
        placeTestOrder(productId);

        // Initially 2 unviewed orders
        mockMvc.perform(get("/api/v1/admin/orders/unviewed-count")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        Order order1 = orderRepository.findByOrderNumber(orderNum1).orElseThrow();

        // Admin views order 1 details
        mockMvc.perform(get("/api/v1/admin/orders/{id}", order1.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewed").value(true));

        // Unviewed count drops to 1
        mockMvc.perform(get("/api/v1/admin/orders/unviewed-count")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    // ---- Helper methods ----

    private long createAvailableProduct(String name, String productCode, int price) throws Exception {
        long categoryId = createCategory("TestCategory");
        return createProductWithAvailability(categoryId, name, productCode, price, true);
    }

    private long createCategory(String name) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name + System.nanoTime());
        body.put("displayOrder", 0);
        body.put("active", true);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createProductWithAvailability(long categoryId, String name, String productCode, int price, boolean available) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categoryId", categoryId);
        body.put("name", name);
        body.put("productCode", productCode);
        body.put("shortDescription", "Test product");
        body.put("description", "Test product description");
        body.put("price", BigDecimal.valueOf(price));
        body.put("available", available);
        body.put("featured", false);
        body.put("displayOrder", 0);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String placeTestOrder(long productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("orderNumber").asText();
    }

    private String orderJson(long productId, int quantity) throws Exception {
        return orderJsonWithItems(List.of(Map.of("productId", productId, "quantity", quantity)));
    }

    private String orderJsonWithItems(List<? extends Map<String, ?>> items) throws Exception {
        Map<String, Object> body = orderMap(items.isEmpty() ? 1L : (Long) items.get(0).get("productId"), 1);
        body.put("items", items);
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> orderMap(long productId, int quantity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "Saurabh Asalkar");
        body.put("mobileNumber", "8087818729");
        body.put("addressLine", "Complete delivery address");
        body.put("city", "Vita");
        body.put("state", "Maharashtra");
        body.put("postalCode", "415311");
        body.put("customerNote", "Optional instructions");
        body.put("items", List.of(Map.of("productId", productId, "quantity", quantity)));
        return body;
    }

    private String bearer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
