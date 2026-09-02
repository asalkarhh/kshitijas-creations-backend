package com.asalkartechworks.kshitijascreations.banner;

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
public class BannerFileStorage {

    private final Path uploadRoot;

    public BannerFileStorage(AppProperties appProperties) {
        this.uploadRoot = Path.of(appProperties.getStorage().getUploadDirectory()).toAbsolutePath().normalize();
    }

    public String store(Long bannerId, MultipartFile file, String extension) {
        String fileName = UUID.randomUUID() + "." + extension;
        String fileKey = "banners/" + bannerId + "/" + fileName;
        Path target = resolveSafe(fileKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileKey;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupException) {
                log.warn("Could not clean up partial banner file", cleanupException);
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_STORAGE_FAILED", "Could not store uploaded banner image");
        }
    }

    public void deleteQuietly(String fileKey) {
        if (fileKey == null) {
            return;
        }
        try {
            Path resolved = resolveSafe(fileKey);
            Files.deleteIfExists(resolved);
        } catch (ApiException exception) {
            log.error("Refusing to delete an unsafe banner media key");
        } catch (IOException exception) {
            log.error("Could not delete banner file for key {}", fileKey, exception);
        }
    }

    private Path resolveSafe(String fileKey) {
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
}

