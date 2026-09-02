package com.asalkartechworks.kshitijascreations.media;

import com.asalkartechworks.kshitijascreations.config.AppProperties;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

@Component
public class MediaUrlResolver {

    private final String publicBaseUrl;

    public MediaUrlResolver(AppProperties appProperties) {
        String baseUrl = appProperties.getMedia().getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            this.publicBaseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.publicBaseUrl = baseUrl;
        }
    }

    public String resolve(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null; // The frontend should render a placeholder
        }
        
        // Prevent path traversal in the generated URL
        String sanitizedKey = fileKey.replace("../", "").replace("..\\", "");
        
        return publicBaseUrl + "/" + UriUtils.encodePath(sanitizedKey, StandardCharsets.UTF_8);
    }
}

