package com.asalkartechworks.kshitijascreations.media;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.config.AppProperties;
import com.asalkartechworks.kshitijascreations.domain.enums.MediaType;
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
public class MediaFileValidator {

    private final AppProperties appProperties;

    public ValidatedMedia validate(MultipartFile file, MediaType requestedType) {
        if (file == null || file.isEmpty()) {
            throw invalid("files", "At least one non-empty file is required");
        }
        if (requestedType == MediaType.IMAGE && file.getSize() > appProperties.getMedia().getMaxImageSize().toBytes()) {
            throw tooLarge("Image files must not exceed 5 MB");
        }
        if (requestedType == MediaType.VIDEO && file.getSize() > appProperties.getMedia().getMaxVideoSize().toBytes()) {
            throw tooLarge("Video files must not exceed 30 MB");
        }

        byte[] header = readHeader(file);
        DetectedType detected = detect(header);
        if (detected == null || detected.mediaType != requestedType) {
            throw invalid("files", "File content does not match the selected media type");
        }
        if (!isAllowedContentType(file.getContentType(), detected)) {
            throw invalid("files", "File content type does not match the uploaded content");
        }
        if ((detected == DetectedType.JPEG || detected == DetectedType.PNG) && !isDecodableImage(file)) {
            throw invalid("files", "Image content could not be decoded");
        }
        return new ValidatedMedia(detected.extension);
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(64);
        } catch (IOException exception) {
            throw invalid("files", "Could not read uploaded file");
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
        if (startsWithAscii(bytes, 4, "ftyp")) {
            return DetectedType.MP4;
        }
        if (startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3)) {
            return DetectedType.WEBM;
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
            case MP4 -> normalized.equals("video/mp4");
            case WEBM -> normalized.equals("video/webm");
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

    private ApiException tooLarge(String message) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", message, Map.of("files", message));
    }

    public record ValidatedMedia(String extension) {
    }

    private enum DetectedType {
        JPEG(MediaType.IMAGE, "jpg"),
        PNG(MediaType.IMAGE, "png"),
        WEBP(MediaType.IMAGE, "webp"),
        MP4(MediaType.VIDEO, "mp4"),
        WEBM(MediaType.VIDEO, "webm");

        private final MediaType mediaType;
        private final String extension;

        DetectedType(MediaType mediaType, String extension) {
            this.mediaType = mediaType;
            this.extension = extension;
        }
    }
}
