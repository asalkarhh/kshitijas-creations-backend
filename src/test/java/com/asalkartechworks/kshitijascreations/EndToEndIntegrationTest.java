package com.asalkartechworks.kshitijascreations;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asalkartechworks.kshitijascreations.repository.CategoryRepository;
import com.asalkartechworks.kshitijascreations.repository.OrderItemRepository;
import com.asalkartechworks.kshitijascreations.repository.OrderRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductMediaRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class EndToEndIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanup() throws Exception {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productMediaRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        Path uploads = Path.of("target/test-uploads");
        if (Files.exists(uploads)) {
            try (var paths = Files.walk(uploads)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignore) {
                    }
                });
            }
        }
    }

    @Test
    void endToEndCheckoutFlow() throws Exception {
        // 1. Admin login
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "admin@kshitijascreations.test", "password", "CorrectPassword123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = "Bearer " + objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        // 2. Create category
        Map<String, Object> catBody = new LinkedHashMap<>();
        catBody.put("name", "E2E Necklaces");
        catBody.put("displayOrder", 1);
        catBody.put("active", true);

        MvcResult catResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catBody)))
                .andExpect(status().isCreated())
                .andReturn();
        long categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Create product
        Map<String, Object> prodBody = new LinkedHashMap<>();
        prodBody.put("categoryId", categoryId);
        prodBody.put("name", "E2E Pearl Set");
        prodBody.put("productCode", "E2E-001");
        prodBody.put("shortDescription", "Beautiful pearl set");
        prodBody.put("description", "Full description");
        prodBody.put("price", BigDecimal.valueOf(1500));
        prodBody.put("available", true);
        prodBody.put("featured", true);
        prodBody.put("displayOrder", 1);

        MvcResult prodResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodBody)))
                .andExpect(status().isCreated())
                .andReturn();
        long productId = objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. Upload product image
        byte[] dummyImage = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF,
                (byte) 0xC0, (byte) 0xF0, 0x1F, 0x00, 0x05, 0x00, 0x01,
                (byte) 0xFF, (byte) 0x89, (byte) 0x99, 0x3D, 0x1D, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
        MockMultipartFile image = new MockMultipartFile("files", "test.png", "image/png", dummyImage);

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/media", productId)
                        .file(image)
                        .header("Authorization", token)
                        .param("mediaType", "IMAGE")
                        .param("primaryMedia", "true"))
                .andExpect(status().isCreated());

        // 5. Public product listing
        mockMvc.perform(get("/api/v1/public/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].primaryImage").exists());

        // 6. Submit guest order
        Map<String, Object> orderReq = new LinkedHashMap<>();
        orderReq.put("customerName", "Test Customer");
        orderReq.put("mobileNumber", "9876543210");
        orderReq.put("addressLine", "Test Address 123");
        orderReq.put("city", "Mumbai");
        orderReq.put("state", "Maharashtra");
        orderReq.put("postalCode", "400001");
        orderReq.put("items", List.of(Map.of("productId", productId, "quantity", 2)));

        MvcResult orderResult = mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.whatsappUrl").exists())
                .andReturn();
        JsonNode orderResponseNode = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        long orderId = Long.parseLong(orderResponseNode.get("orderNumber").asText().split("-")[2]); // Hacky way to get ID approx, actually let's get by API
        String orderNumber = orderResponseNode.get("orderNumber").asText();

        // 7. Verify server-side order total (1500 * 2 = 3000)
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("3000.00").compareTo(new BigDecimal(orderResponseNode.get("totalAmount").asText())));

        // 8. View order as admin
        MvcResult listResult = mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", token)
                        .param("search", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();
        long actualOrderId = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();

        // 9. Update order status
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", actualOrderId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
