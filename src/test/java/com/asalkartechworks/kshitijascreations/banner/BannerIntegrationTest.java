package com.asalkartechworks.kshitijascreations.banner;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asalkartechworks.kshitijascreations.IntegrationTestBase;
import com.asalkartechworks.kshitijascreations.repository.BannerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class BannerIntegrationTest extends IntegrationTestBase {

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
    private BannerRepository bannerRepository;

    @BeforeEach
    void cleanBanners() throws Exception {
        bannerRepository.deleteAll();
        Path uploads = Path.of("target/test-uploads/banners");
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

    // ---- 1. Create banner ----
    @Test
    void createBannerWithSchedule() throws Exception {
        MockMultipartFile desktop = pngFile("desktopImage");
        Instant startAt = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant endAt = Instant.now().plus(24, ChronoUnit.HOURS);

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(desktop)
                        .param("title", "Summer Sale")
                        .param("subtitle", "Up to 50% off")
                        .param("buttonText", "Shop Now")
                        .param("targetUrl", "https://example.com/sale")
                        .param("startAt", startAt.toString())
                        .param("endAt", endAt.toString())
                        .param("displayOrder", "1")
                        .param("active", "true")
                        .header("Authorization", bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Summer Sale"))
                .andExpect(jsonPath("$.subtitle").value("Up to 50% off"))
                .andExpect(jsonPath("$.desktopImageUrl", not(blankOrNullString())))
                .andExpect(jsonPath("$.desktopImageUrl").value(not(containsString("original"))))
                .andExpect(jsonPath("$.buttonText").value("Shop Now"))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com/sale"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.currentStatus").value("SCHEDULED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // ---- 2. Create banner without optional schedule ----
    @Test
    void createBannerWithoutSchedule() throws Exception {
        MockMultipartFile desktop = pngFile("desktopImage");

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(desktop)
                        .param("title", "Welcome Banner")
                        .header("Authorization", bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Welcome Banner"))
                .andExpect(jsonPath("$.startAt").isEmpty())
                .andExpect(jsonPath("$.endAt").isEmpty())
                .andExpect(jsonPath("$.displayOrder").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.currentStatus").value("ACTIVE"));
    }

    // ---- 3. Reject invalid date range ----
    @Test
    void rejectInvalidDateRange() throws Exception {
        MockMultipartFile desktop = pngFile("desktopImage");
        Instant startAt = Instant.now().plus(24, ChronoUnit.HOURS);
        Instant endAt = Instant.now().plus(1, ChronoUnit.HOURS);

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(desktop)
                        .param("title", "Bad Dates")
                        .param("startAt", startAt.toString())
                        .param("endAt", endAt.toString())
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.endAt").exists());
    }

    // ---- 4. Update without replacing images ----
    @Test
    void updateWithoutReplacingImages() throws Exception {
        long bannerId = createBanner("Original Title");

        MvcResult result = mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/banners/{id}", bannerId)
                        .param("title", "Updated Title")
                        .param("subtitle", "New subtitle")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.subtitle").value("New subtitle"))
                .andExpect(jsonPath("$.desktopImageUrl", not(blankOrNullString())))
                .andReturn();

        // Verify the desktop image URL is still present (preserved)
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertNotNull(response.get("desktopImageUrl").asText());
    }

    // ---- 5. Replace banner image ----
    @Test
    void replaceBannerImage() throws Exception {
        long bannerId = createBanner("Image Replace Test");

        // Get the original image URL
        MvcResult original = mockMvc.perform(get("/api/v1/admin/banners")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        String originalUrl = objectMapper.readTree(original.getResponse().getContentAsString())
                .get("content").get(0).get("desktopImageUrl").asText();

        // Replace the image
        MockMultipartFile newDesktop = pngFile("desktopImage");
        MvcResult updated = mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/banners/{id}", bannerId)
                        .file(newDesktop)
                        .param("title", "Image Replace Test")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        String updatedUrl = objectMapper.readTree(updated.getResponse().getContentAsString())
                .get("desktopImageUrl").asText();

        // URLs should be different (different UUID filenames)
        org.junit.jupiter.api.Assertions.assertNotEquals(originalUrl, updatedUrl);
    }

    // ---- 6. Delete banner and associated files ----
    @Test
    void deleteBannerAndFiles() throws Exception {
        long bannerId = createBanner("To Delete");

        mockMvc.perform(delete("/api/v1/admin/banners/{id}", bannerId)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(bannerRepository.findById(bannerId).isEmpty());

        // Verify files are cleaned up
        Path bannerDir = Path.of("target/test-uploads/banners/" + bannerId);
        if (Files.exists(bannerDir)) {
            try (var entries = Files.list(bannerDir)) {
                org.junit.jupiter.api.Assertions.assertEquals(0, entries.count());
            }
        }
    }

    // ---- 7. Unauthorized admin access ----
    @Test
    void unauthorizedAdminAccess() throws Exception {
        MockMultipartFile desktop = pngFile("desktopImage");

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(desktop)
                        .param("title", "No Auth"))
                .andExpect(status().isUnauthorized());
    }

    // ---- 8. Public API returns active banners ----
    @Test
    void publicApiReturnsActiveBanners() throws Exception {
        createBanner("Active Banner");

        mockMvc.perform(get("/api/v1/public/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Active Banner"))
                .andExpect(jsonPath("$[0].desktopImageUrl", not(blankOrNullString())))
                // Public response should not expose internal fields
                .andExpect(jsonPath("$[0].currentStatus").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    // ---- 9. Public API excludes inactive banners ----
    @Test
    void publicApiExcludesInactiveBanners() throws Exception {
        createBannerWithParams("Inactive Banner", null, null, false);

        mockMvc.perform(get("/api/v1/public/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    // ---- 10. Public API excludes future banners ----
    @Test
    void publicApiExcludesFutureBanners() throws Exception {
        Instant futureStart = Instant.now().plus(7, ChronoUnit.DAYS);
        createBannerWithParams("Future Banner", futureStart, null, true);

        mockMvc.perform(get("/api/v1/public/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    // ---- 11. Public API excludes expired banners ----
    @Test
    void publicApiExcludesExpiredBanners() throws Exception {
        Instant pastStart = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant pastEnd = Instant.now().minus(1, ChronoUnit.DAYS);
        createBannerWithParams("Expired Banner", pastStart, pastEnd, true);

        mockMvc.perform(get("/api/v1/public/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    // ---- 12. Correct banner status calculation ----
    @Test
    void correctBannerStatusCalculation() throws Exception {
        // Active banner (no schedule)
        createBanner("Always Active");

        // Scheduled banner (future start)
        Instant futureStart = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant futureEnd = Instant.now().plus(14, ChronoUnit.DAYS);
        createBannerWithParams("Scheduled", futureStart, futureEnd, true);

        // Expired banner (past end)
        Instant pastStart = Instant.now().minus(14, ChronoUnit.DAYS);
        Instant pastEnd = Instant.now().minus(1, ChronoUnit.DAYS);
        createBannerWithParams("Expired", pastStart, pastEnd, true);

        // Inactive banner
        createBannerWithParams("Inactive", null, null, false);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/banners")
                        .param("size", "10")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        // Verify each banner has a valid status
        for (JsonNode banner : content) {
            String title = banner.get("title").asText();
            String currentStatus = banner.get("currentStatus").asText();
            switch (title) {
                case "Always Active" -> org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", currentStatus);
                case "Scheduled" -> org.junit.jupiter.api.Assertions.assertEquals("SCHEDULED", currentStatus);
                case "Expired" -> org.junit.jupiter.api.Assertions.assertEquals("EXPIRED", currentStatus);
                case "Inactive" -> org.junit.jupiter.api.Assertions.assertEquals("INACTIVE", currentStatus);
            }
        }
    }

    // ---- 13. Invalid file type rejection ----
    @Test
    void invalidFileTypeRejected() throws Exception {
        MockMultipartFile text = new MockMultipartFile("desktopImage", "not-image.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(text)
                        .param("title", "Bad File")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEDIA"));
    }

    // ---- 14. Oversized image rejection ----
    @Test
    void oversizedImageRejected() throws Exception {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(ONE_PIXEL_PNG, 0, oversized, 0, ONE_PIXEL_PNG.length);
        MockMultipartFile image = new MockMultipartFile("desktopImage", "large.png", "image/png", oversized);

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(image)
                        .param("title", "Too Large")
                        .header("Authorization", bearer()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("UPLOAD_TOO_LARGE"));
    }

    // ---- 15. Unsafe target URL rejection ----
    @Test
    void unsafeTargetUrlRejected() throws Exception {
        MockMultipartFile desktop = pngFile("desktopImage");

        mockMvc.perform(multipart("/api/v1/admin/banners")
                        .file(desktop)
                        .param("title", "XSS Banner")
                        .param("targetUrl", "javascript:alert(1)")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.targetUrl").exists());
    }

    // ---- Helper methods ----

    private long createBanner(String title) throws Exception {
        return createBannerWithParams(title, null, null, true);
    }

    private long createBannerWithParams(String title, Instant startAt, Instant endAt, boolean active) throws Exception {
        MockMultipartFile desktop = pngFile("desktopImage");

        var request = multipart("/api/v1/admin/banners")
                .file(desktop)
                .param("title", title)
                .param("active", String.valueOf(active))
                .header("Authorization", bearer());

        if (startAt != null) {
            request.param("startAt", startAt.toString());
        }
        if (endAt != null) {
            request.param("endAt", endAt.toString());
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private MockMultipartFile pngFile(String paramName) {
        return new MockMultipartFile(paramName, "test-image.png", "image/png", ONE_PIXEL_PNG);
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

