package com.asalkartechworks.kshitijascreations.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asalkartechworks.kshitijascreations.IntegrationTestBase;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class CatalogIntegrationTest extends IntegrationTestBase {

    private static final String ADMIN_EMAIL = "admin@kshitijascreations.test";
    private static final String ADMIN_PASSWORD = "CorrectPassword123!";
    private static final byte[] ONE_PIXEL_PNG = {
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
    void cleanCatalog() throws Exception {
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
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                });
            }
        }
    }

    @Test
    void createAndUpdateCategory() throws Exception {
        long categoryId = createCategory("Floral Jewellery", "Flowers", 2, true);

        mockMvc.perform(put("/api/v1/admin/categories/{id}", categoryId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Floral Accessories", "Updated", 1, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Floral Accessories"))
                .andExpect(jsonPath("$.slug").value("floral-accessories"))
                .andExpect(jsonPath("$.displayOrder").value(1));
    }

    @Test
    void duplicateCategoryIsRejected() throws Exception {
        createCategory("Bracelets", null, 0, true);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("bracelets", null, 1, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CATEGORY_NAME"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void publicCategoriesOnlyIncludeActiveCategories() throws Exception {
        createCategory("Later", null, 2, true);
        createCategory("Hidden", null, 0, false);
        createCategory("First", null, 1, true);

        mockMvc.perform(get("/api/v1/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("First"))
                .andExpect(jsonPath("$[1].name").value("Later"))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void createAndUpdateProduct() throws Exception {
        long categoryId = createCategory("Necklaces", null, 0, true);
        long productId = createProduct(categoryId, "Pearl Necklace", "PN-001", "Classic pearl", 1250, true, false);

        mockMvc.perform(put("/api/v1/admin/products/{id}", productId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, "Pearl Bridal Necklace", "PN-001", "Bridal", 1500, true, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pearl Bridal Necklace"))
                .andExpect(jsonPath("$.slug").value("pearl-bridal-necklace"))
                .andExpect(jsonPath("$.price").value(1500))
                .andExpect(jsonPath("$.featured").value(true));
    }

    @Test
    void duplicateProductCodeIsRejected() throws Exception {
        long categoryId = createCategory("Earrings", null, 0, true);
        createProduct(categoryId, "Gold Drop", "ED-001", "Drop", 500, true, false);

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, "Silver Drop", "ed-001", "Drop", 600, true, false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PRODUCT_CODE"))
                .andExpect(jsonPath("$.fieldErrors.productCode").exists());
    }

    @Test
    void softDeletedProductIsExcludedFromPublicListing() throws Exception {
        long categoryId = createCategory("Rings", null, 0, true);
        long productId = createProduct(categoryId, "Rose Ring", "RR-001", "Rose", 700, true, false);

        mockMvc.perform(delete("/api/v1/admin/products/{id}", productId).header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/public/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void publicProductsSupportSearchAndCategoryFiltering() throws Exception {
        long floral = createCategory("Floral", null, 0, true);
        long bridal = createCategory("Bridal", null, 1, true);
        createProduct(floral, "Floral Hair Pin", "FH-001", "Rose pin", 250, true, true);
        createProduct(bridal, "Pearl Tiara", "PT-001", "Bridal tiara", 1200, true, false);

        mockMvc.perform(get("/api/v1/public/products")
                        .param("search", "floral")
                        .param("category", "floral"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productCode").value("FH-001"));
    }

    @Test
    void adminCatalogRequestsRequireJwt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("No Auth", null, 0, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validImageUploadStoresBrowserUsableUrl() throws Exception {
        long productId = createUploadableProduct();
        MockMultipartFile image = new MockMultipartFile("files", "original-name.png", "image/png", ONE_PIXEL_PNG);

        MvcResult upload = mockMvc.perform(multipart("/api/v1/admin/products/{productId}/media", productId)
                        .file(image)
                        .header("Authorization", bearer())
                        .param("mediaType", "IMAGE")
                        .param("primaryMedia", "true")
                        .param("altText", "A pearl necklace"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].url", not(blankOrNullString())))
                .andExpect(jsonPath("$[0].url").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("original-name"))))
                .andExpect(jsonPath("$[0].primaryMedia").value(true))
                .andReturn();

        mockMvc.perform(get(response(upload).get(0).get("url").asText()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }

    @Test
    void invalidFileTypeIsRejected() throws Exception {
        long productId = createUploadableProduct();
        MockMultipartFile text = new MockMultipartFile("files", "not-image.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{productId}/media", productId)
                        .file(text)
                        .header("Authorization", bearer())
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEDIA"));
    }

    @Test
    void oversizedImageIsRejected() throws Exception {
        long productId = createUploadableProduct();
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(ONE_PIXEL_PNG, 0, oversized, 0, ONE_PIXEL_PNG.length);
        MockMultipartFile image = new MockMultipartFile("files", "large.png", "image/png", oversized);

        mockMvc.perform(multipart("/api/v1/admin/products/{productId}/media", productId)
                        .file(image)
                        .header("Authorization", bearer())
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("UPLOAD_TOO_LARGE"));
    }

    @Test
    void markingNewMediaPrimaryRemovesPreviousPrimary() throws Exception {
        long productId = createUploadableProduct();
        uploadImage(productId, "first.png", true);
        uploadImage(productId, "second.png", true);

        mockMvc.perform(get("/api/v1/public/products/pearl-necklace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media", hasSize(2)))
                .andExpect(jsonPath("$.media[0].primaryMedia").value(true))
                .andExpect(jsonPath("$.media[1].primaryMedia").value(false))
                .andExpect(jsonPath("$.primaryImage.primaryMedia").value(true));
    }

    @Test
    void publicProductDetailsAreResolvedBySlug() throws Exception {
        long categoryId = createCategory("Pearls", null, 0, true);
        createProduct(categoryId, "Pearl Necklace", "PN-DETAIL", "Handmade", 1500, true, true);

        mockMvc.perform(get("/api/v1/public/products/pearl-necklace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pearl Necklace"))
                .andExpect(jsonPath("$.category.slug").value("pearls"))
                .andExpect(jsonPath("$.productCode").value("PN-DETAIL"));
    }

    private long createUploadableProduct() throws Exception {
        long categoryId = createCategory("Necklaces", null, 0, true);
        return createProduct(categoryId, "Pearl Necklace", "PN-001", "Classic pearl", 1250, true, false);
    }

    private void uploadImage(long productId, String name, boolean primary) throws Exception {
        MockMultipartFile image = new MockMultipartFile("files", name, "image/png", ONE_PIXEL_PNG);
        mockMvc.perform(multipart("/api/v1/admin/products/{productId}/media", productId)
                        .file(image)
                        .header("Authorization", bearer())
                        .param("mediaType", "IMAGE")
                        .param("primaryMedia", String.valueOf(primary)))
                .andExpect(status().isCreated());
    }

    private long createCategory(String name, String description, int displayOrder, boolean active) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson(name, description, displayOrder, active)))
                .andExpect(status().isCreated())
                .andReturn();
        return response(result).get("id").asLong();
    }

    private long createProduct(
            long categoryId,
            String name,
            String productCode,
            String shortDescription,
            int price,
            boolean available,
            boolean featured
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, name, productCode, shortDescription, price, available, featured)))
                .andExpect(status().isCreated())
                .andReturn();
        return response(result).get("id").asLong();
    }

    private String bearer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + response(result).get("accessToken").asText();
    }

    private String categoryJson(String name, String description, int displayOrder, boolean active) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("displayOrder", displayOrder);
        body.put("active", active);
        return objectMapper.writeValueAsString(body);
    }

    private String productJson(
            long categoryId,
            String name,
            String productCode,
            String shortDescription,
            int price,
            boolean available,
            boolean featured
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("categoryId", categoryId);
        body.put("name", name);
        body.put("productCode", productCode);
        body.put("shortDescription", shortDescription);
        body.put("description", shortDescription + " description");
        body.put("price", BigDecimal.valueOf(price));
        body.put("available", available);
        body.put("featured", featured);
        body.put("instagramUrl", "https://instagram.com/kshitijascreations");
        body.put("displayOrder", 0);
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
