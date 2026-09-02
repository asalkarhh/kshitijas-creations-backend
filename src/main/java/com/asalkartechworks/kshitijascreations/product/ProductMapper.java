package com.asalkartechworks.kshitijascreations.product;

import com.asalkartechworks.kshitijascreations.domain.entity.Category;
import com.asalkartechworks.kshitijascreations.domain.entity.Product;
import com.asalkartechworks.kshitijascreations.domain.entity.ProductMedia;
import com.asalkartechworks.kshitijascreations.domain.enums.MediaType;
import com.asalkartechworks.kshitijascreations.product.dto.AdminProductResponse;
import com.asalkartechworks.kshitijascreations.product.dto.ProductCategoryResponse;
import com.asalkartechworks.kshitijascreations.product.dto.ProductMediaResponse;
import com.asalkartechworks.kshitijascreations.product.dto.PublicProductResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    private final ProductMediaMapper productMediaMapper;

    public ProductMapper(ProductMediaMapper productMediaMapper) {
        this.productMediaMapper = productMediaMapper;
    }

    public AdminProductResponse toAdminResponse(Product product, List<ProductMedia> media) {
        return new AdminProductResponse(
                product.getId(), product.getName(), product.getSlug(), product.getProductCode(),
                product.getShortDescription(), product.getDescription(), product.getPrice(), product.isAvailable(),
                product.isFeatured(), product.getInstagramUrl(), product.getDisplayOrder(), product.isDeleted(),
                toCategory(product.getCategory()), toMedia(media), product.getCreatedAt(), product.getUpdatedAt()
        );
    }

    public PublicProductResponse toPublicResponse(Product product, List<ProductMedia> media) {
        List<ProductMediaResponse> mediaResponses = toMedia(media);
        ProductMediaResponse primaryImage = mediaResponses.stream()
                .filter(item -> item.primaryMedia() && item.mediaType() == MediaType.IMAGE)
                .findFirst()
                .orElseGet(() -> mediaResponses.stream()
                        .filter(item -> item.mediaType() == MediaType.IMAGE)
                        .findFirst()
                        .orElse(null));
        return new PublicProductResponse(
                product.getId(), product.getName(), product.getSlug(), product.getProductCode(),
                product.getShortDescription(), product.getDescription(), product.getPrice(), product.isAvailable(),
                product.isFeatured(), product.getInstagramUrl(), product.getDisplayOrder(), toCategory(product.getCategory()),
                mediaResponses, primaryImage
        );
    }

    private ProductCategoryResponse toCategory(Category category) {
        return new ProductCategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    private List<ProductMediaResponse> toMedia(List<ProductMedia> media) {
        return media.stream().map(productMediaMapper::toResponse).toList();
    }
}
