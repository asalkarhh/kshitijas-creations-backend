package com.asalkartechworks.kshitijascreations.media;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.domain.entity.Product;
import com.asalkartechworks.kshitijascreations.domain.entity.ProductMedia;
import com.asalkartechworks.kshitijascreations.domain.enums.MediaType;
import com.asalkartechworks.kshitijascreations.product.ProductMediaMapper;
import com.asalkartechworks.kshitijascreations.product.dto.ProductMediaResponse;
import com.asalkartechworks.kshitijascreations.repository.ProductMediaRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductMediaService {

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final MediaFileValidator mediaFileValidator;
    private final ProductMediaStorage productMediaStorage;
    private final ProductMediaMapper productMediaMapper;

    @Transactional
    public List<ProductMediaResponse> upload(
            Long productId,
            List<MultipartFile> files,
            String altText,
            MediaType mediaType,
            boolean primaryMedia,
            int displayOrder
    ) {
        Product product = productRepository.findById(productId).orElseThrow(() -> productNotFound(productId));
        if (product.isDeleted()) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_DELETED", "Cannot add media to a deleted product");
        }
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA", "At least one file is required", Map.of("files", "Required"));
        }

        List<MediaFileValidator.ValidatedMedia> validated = files.stream()
                .map(file -> mediaFileValidator.validate(file, mediaType))
                .toList();
        if (primaryMedia) {
            productMediaRepository.clearPrimaryMedia(productId);
        }

        List<String> writtenKeys = new ArrayList<>();
        try {
            List<ProductMedia> mediaRecords = new ArrayList<>();
            for (int index = 0; index < files.size(); index++) {
                ProductMediaStorage.StoredFile stored = productMediaStorage.store(
                        productId, files.get(index), validated.get(index).extension()
                );
                writtenKeys.add(stored.fileKey());

                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setMediaType(mediaType);
                media.setFileKey(stored.fileKey());
                media.setAltText(altText);
                media.setDisplayOrder(displayOrder + index);
                // A multipart request can nominate one primary item; the first file is that item.
                media.setPrimaryMedia(primaryMedia && index == 0);
                mediaRecords.add(media);
            }
            return productMediaRepository.saveAll(mediaRecords).stream()
                    .map(productMediaMapper::toResponse)
                    .toList();
        } catch (RuntimeException exception) {
            writtenKeys.forEach(productMediaStorage::deleteQuietly);
            throw exception;
        }
    }

    @Transactional
    public void delete(Long productId, Long mediaId) {
        ProductMedia media = productMediaRepository.findByIdAndProductId(mediaId, productId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PRODUCT_MEDIA_NOT_FOUND",
                        "Product media not found",
                        Map.of("mediaId", "No media exists for this product")
                ));
        String fileKey = media.getFileKey();
        String thumbnailKey = media.getThumbnailKey();
        productMediaRepository.delete(media);
        scheduleDeleteAfterCommit(fileKey);
        scheduleDeleteAfterCommit(thumbnailKey);
    }

    @Transactional
    public void setPrimary(Long productId, Long mediaId) {
        ProductMedia media = productMediaRepository.findByIdAndProductId(mediaId, productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_MEDIA_NOT_FOUND", "Product media not found"));
        productMediaRepository.clearPrimaryMedia(productId);
        media.setPrimaryMedia(true);
        productMediaRepository.save(media);
    }

    private void scheduleDeleteAfterCommit(String fileKey) {
        if (fileKey == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    productMediaStorage.deleteQuietly(fileKey);
                }
            });
        } else {
            productMediaStorage.deleteQuietly(fileKey);
        }
    }

    private ApiException productNotFound(Long productId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "Product not found",
                Map.of("productId", "No product exists with this id")
        );
    }
}
