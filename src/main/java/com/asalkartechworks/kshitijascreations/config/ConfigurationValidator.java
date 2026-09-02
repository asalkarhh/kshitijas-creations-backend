package com.asalkartechworks.kshitijascreations.config;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationValidator {

    private final AppProperties appProperties;

    @PostConstruct
    public void validate() {
        validateJwtSecret();
        validateWhatsappNumber();
        validateUploadDirectory();
        validatePublicBaseUrl();
        validateCorsOrigins();
        log.info("Configuration validation passed");
    }

    private void validateJwtSecret() {
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long for secure HS256/HS512 signatures.");
        }
    }

    private void validateWhatsappNumber() {
        String number = appProperties.getWhatsapp().getAdminNumber();
        if (number == null || number.isBlank()) {
            throw new IllegalStateException("WhatsApp admin number must be configured (app.whatsapp.admin-number).");
        }
        if (number.startsWith("+") || number.contains(" ")) {
            throw new IllegalStateException("WhatsApp admin number must be in country-code format without '+' or spaces (e.g. 918087818729).");
        }
    }

    private void validateUploadDirectory() {
        String dir = appProperties.getStorage().getUploadDirectory();
        if (dir == null || dir.isBlank()) {
            throw new IllegalStateException("Upload directory must be configured (app.storage.upload-directory).");
        }
        Path path = Path.of(dir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new IllegalStateException("Could not create upload directory: " + dir, e);
            }
        }
        if (!Files.isWritable(path)) {
            throw new IllegalStateException("Upload directory is not writable: " + dir);
        }
    }

    private void validatePublicBaseUrl() {
        String url = appProperties.getMedia().getPublicBaseUrl();
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            throw new IllegalStateException("Public media base URL must start with http:// or https://");
        }
    }

    private void validateCorsOrigins() {
        String origins = appProperties.getCors().getAllowedOrigins();
        if (origins == null || origins.contains("*")) {
            throw new IllegalStateException("CORS allowed origins must not use wildcard '*'. Specify exact origins.");
        }
    }
}

