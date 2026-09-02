package com.asalkartechworks.kshitijascreations.product;

import com.asalkartechworks.kshitijascreations.domain.entity.ProductMedia;
import com.asalkartechworks.kshitijascreations.product.dto.ProductMediaResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

@Component
public class ProductMediaMapper {

    private final com.asalkartechworks.kshitijascreations.media.MediaUrlResolver mediaUrlResolver;

    public ProductMediaMapper(com.asalkartechworks.kshitijascreations.media.MediaUrlResolver mediaUrlResolver) {
        this.mediaUrlResolver = mediaUrlResolver;
    }

    public ProductMediaResponse toResponse(ProductMedia media) {
        return new ProductMediaResponse(
                media.getId(),
                media.getMediaType(),
                mediaUrlResolver.resolve(media.getFileKey()),
                mediaUrlResolver.resolve(media.getThumbnailKey()),
                media.getAltText(),
                media.getDisplayOrder(),
                media.isPrimaryMedia()
        );
    }
}
