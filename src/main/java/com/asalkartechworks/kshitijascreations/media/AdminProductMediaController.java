package com.asalkartechworks.kshitijascreations.media;

import com.asalkartechworks.kshitijascreations.domain.enums.MediaType;
import com.asalkartechworks.kshitijascreations.product.dto.ProductMediaResponse;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products/{productId}/media")
public class AdminProductMediaController {

    private final ProductMediaService productMediaService;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProductMediaResponse> upload(
            @PathVariable Long productId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) @Size(max = 300) String altText,
            @RequestParam MediaType mediaType,
            @RequestParam(value = "primaryMedia", required = false) Boolean primaryMedia,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(defaultValue = "0") @PositiveOrZero int displayOrder
    ) {
        List<MultipartFile> targetFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            targetFiles.addAll(files);
        } else if (file != null && !file.isEmpty()) {
            targetFiles.add(file);
        }

        boolean isPrimarySelected = Boolean.TRUE.equals(primaryMedia) || Boolean.TRUE.equals(isPrimary);

        return productMediaService.upload(productId, targetFiles, altText, mediaType, isPrimarySelected, displayOrder);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId, @PathVariable Long mediaId) {
        productMediaService.delete(productId, mediaId);
    }

    @PostMapping("/{mediaId}/primary")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPrimary(@PathVariable Long productId, @PathVariable Long mediaId) {
        productMediaService.setPrimary(productId, mediaId);
    }
}
