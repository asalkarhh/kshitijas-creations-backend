package com.asalkartechworks.kshitijascreations.banner;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.config.AppProperties;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class BannerImageValidator {

    private final AppProperties appProperties;

    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("desktopImage", "A non-empty image file is required");
        }
        if (file.getSize() > appProperties.getMedia().getMaxImageSize().toBytes()) {
            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "UPLOAD_TOO_LARGE",
                    "Banner images must not exceed 5 MB",
                    Map.of("image", "Banner images must not exceed 5 MB")
            );
        }
        byte[] header = readHeader(file);
        DetectedType detected = detect(header);
        if (detected == null) {
            throw invalid("image", "Only JPEG, PNG and WebP images are accepted");
        }
        if (!isAllowedContentType(file.getContentType(), detected)) {
            throw invalid("image", "File content type does not match the uploaded content");
        }
        if ((detected == DetectedType.JPEG || detected == DetectedType.PNG) && !isDecodableImage(file)) {
            throw invalid("image", "Image content could not be decoded");
        }
        return detected.extension;
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(64);
        } catch (IOException exception) {
            throw invalid("image", "Could not read uploaded file");
        }
    }

    private boolean isDecodableImage(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            return image != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private DetectedType detect(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return DetectedType.JPEG;
        }
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return DetectedType.PNG;
        }
        if (startsWithAscii(bytes, 0, "RIFF") && startsWithAscii(bytes, 8, "WEBP")) {
            return DetectedType.WEBP;
        }
        return null;
    }

    private boolean isAllowedContentType(String contentType, DetectedType detected) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return switch (detected) {
            case JPEG -> normalized.equals("image/jpeg") || normalized.equals("image/jpg");
            case PNG -> normalized.equals("image/png");
            case WEBP -> normalized.equals("image/webp");
        };
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((bytes[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private ApiException invalid(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA", message, Map.of(field, message));
    }

    private enum DetectedType {
        JPEG("jpg"),
        PNG("png"),
        WEBP("webp");

        private final String extension;

        DetectedType(String extension) {
            this.extension = extension;
        }
    }
}

