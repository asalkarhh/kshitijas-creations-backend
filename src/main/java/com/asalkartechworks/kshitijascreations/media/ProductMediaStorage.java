package com.asalkartechworks.kshitijascreations.media;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class ProductMediaStorage {

    private final Path uploadRoot;

    public ProductMediaStorage(AppProperties appProperties) {
        this.uploadRoot = Path.of(appProperties.getStorage().getUploadDirectory()).toAbsolutePath().normalize();
    }

    public StoredFile store(Long productId, MultipartFile file, String extension) {
        String fileName = UUID.randomUUID() + "." + extension;
        String fileKey = "products/" + productId + "/" + fileName;
        Path target = resolveSafe(fileKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(fileKey);
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupException) {
                log.warn("Could not clean up partial media file", cleanupException);
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_STORAGE_FAILED", "Could not store uploaded media");
        }
    }

    public void deleteQuietly(String fileKey) {
        if (fileKey == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(fileKey));
        } catch (ApiException exception) {
            log.error("Refusing to delete an unsafe media key");
        } catch (IOException exception) {
            log.error("Could not delete media file for key {}", fileKey, exception);
        }
    }

    public Path resolveSafe(String fileKey) {
        if (fileKey == null || fileKey.isBlank() || fileKey.contains("\\")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA_PATH", "Invalid media path");
        }
        Path relativePath = Path.of(fileKey).normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA_PATH", "Invalid media path");
        }
        Path resolved = uploadRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA_PATH", "Invalid media path");
        }
        return resolved;
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public record StoredFile(String fileKey) {
    }
}
