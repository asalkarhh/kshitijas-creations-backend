package com.asalkartechworks.kshitijascreations.product;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.api.PageResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.Category;
import com.asalkartechworks.kshitijascreations.domain.entity.Product;
import com.asalkartechworks.kshitijascreations.domain.entity.ProductMedia;
import com.asalkartechworks.kshitijascreations.product.dto.AdminProductResponse;
import com.asalkartechworks.kshitijascreations.product.dto.ProductRequest;
import com.asalkartechworks.kshitijascreations.product.dto.PublicProductResponse;
import com.asalkartechworks.kshitijascreations.repository.CategoryRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductMediaRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import com.asalkartechworks.kshitijascreations.support.SlugGenerator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final SlugGenerator slugGenerator;

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> adminList(
            String search,
            Long categoryId,
            Boolean available,
            Boolean featured,
            Pageable pageable
    ) {
        Page<Product> products = productRepository.findAll(
                ProductSpecifications.admin(search, categoryId, available, featured), pageable
        );
        Map<Long, List<ProductMedia>> mediaByProduct = mediaByProduct(products.getContent());
        return PageResponse.from(products.map(product -> productMapper.toAdminResponse(
                product, mediaByProduct.getOrDefault(product.getId(), List.of())
        )));
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicProductResponse> publicList(
            String search,
            String category,
            Boolean featured,
            Pageable pageable
    ) {
        Page<Product> products = productRepository.findAll(
                ProductSpecifications.publicProducts(search, category, featured), pageable
        );
        Map<Long, List<ProductMedia>> mediaByProduct = mediaByProduct(products.getContent());
        return PageResponse.from(products.map(product -> productMapper.toPublicResponse(
                product, mediaByProduct.getOrDefault(product.getId(), List.of())
        )));
    }

    @Transactional(readOnly = true)
    public PublicProductResponse publicBySlug(String slug) {
        Product product = productRepository.findBySlugAndDeletedFalseAndCategoryActiveTrue(slug)
                .orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "Product not found", "slug"));
        return productMapper.toPublicResponse(product,
                productMediaRepository.findAllByProductIdOrderByPrimaryMediaDescDisplayOrderAsc(product.getId()));
    }

    @Transactional
    public AdminProductResponse create(ProductRequest request) {
        assertProductCodeAvailable(request.productCode(), null);
        Product product = new Product();
        apply(product, request);
        product.setSlug(nextSlug(request.name(), null));
        return productMapper.toAdminResponse(productRepository.save(product), List.of());
    }

    @Transactional
    public AdminProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        assertProductCodeAvailable(request.productCode(), id);
        boolean nameChanged = !product.getName().equalsIgnoreCase(request.name().trim());
        apply(product, request);
        if (nameChanged) {
            product.setSlug(nextSlug(request.name(), id));
        }
        List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderByPrimaryMediaDescDisplayOrderAsc(id);
        return productMapper.toAdminResponse(product, media);
    }

    @Transactional
    public void softDelete(Long id) {
        getProduct(id).setDeleted(true);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "Product not found", "id"));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> notFound(
                "CATEGORY_NOT_FOUND", "Category not found", "categoryId"
        ));
    }

    private void apply(Product product, ProductRequest request) {
        product.setCategory(getCategory(request.categoryId()));
        product.setName(request.name().trim());
        product.setProductCode(normalizeProductCode(request.productCode()));
        product.setShortDescription(request.shortDescription());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailable(request.available());
        product.setFeatured(request.featured());
        product.setInstagramUrl(request.instagramUrl());
        product.setDisplayOrder(request.displayOrder());
    }

    private void assertProductCodeAvailable(String rawCode, Long id) {
        String code = normalizeProductCode(rawCode);
        boolean exists = id == null
                ? productRepository.existsByProductCodeIgnoreCase(code)
                : productRepository.existsByProductCodeIgnoreCaseAndIdNot(code, id);
        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_PRODUCT_CODE",
                    "A product with this product code already exists",
                    Map.of("productCode", "Product code must be unique")
            );
        }
    }

    private String nextSlug(String name, Long currentId) {
        String base = slugGenerator.generate(name, "product");
        String candidate = base;
        int suffix = 2;
        while (productRepository.findBySlug(candidate)
                .filter(product -> !product.getId().equals(currentId))
                .isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private Map<Long, List<ProductMedia>> mediaByProduct(Collection<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        return productMediaRepository.findAllByProductIdInOrderByPrimaryMediaDescDisplayOrderAsc(
                        products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(media -> media.getProduct().getId()));
    }

    private String normalizeProductCode(String productCode) {
        return productCode.trim().toUpperCase(Locale.ROOT);
    }

    private ApiException notFound(String code, String message, String field) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message, Map.of(field, message));
    }
}
