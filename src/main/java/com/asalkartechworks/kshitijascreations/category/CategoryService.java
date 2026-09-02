package com.asalkartechworks.kshitijascreations.category;

import com.asalkartechworks.kshitijascreations.api.ApiException;
import com.asalkartechworks.kshitijascreations.category.dto.AdminCategoryResponse;
import com.asalkartechworks.kshitijascreations.category.dto.CategoryRequest;
import com.asalkartechworks.kshitijascreations.category.dto.PublicCategoryResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.Category;
import com.asalkartechworks.kshitijascreations.repository.CategoryRepository;
import com.asalkartechworks.kshitijascreations.repository.ProductRepository;
import com.asalkartechworks.kshitijascreations.support.SlugGenerator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final SlugGenerator slugGenerator;

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> adminList() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(categoryMapper::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCategoryResponse> publicList() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(categoryMapper::toPublicResponse)
                .toList();
    }

    @Transactional
    public AdminCategoryResponse create(CategoryRequest request) {
        assertNameIsAvailable(request.name(), null);
        Category category = new Category();
        apply(category, request);
        category.setSlug(nextSlug(request.name(), null));
        return categoryMapper.toAdminResponse(categoryRepository.save(category));
    }

    @Transactional
    public AdminCategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        assertNameIsAvailable(request.name(), id);
        boolean nameChanged = !category.getName().equalsIgnoreCase(request.name().trim());
        apply(category, request);
        if (nameChanged) {
            category.setSlug(nextSlug(request.name(), id));
        }
        return categoryMapper.toAdminResponse(category);
    }

    @Transactional
    public void deactivate(Long id) {
        Category category = getCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CATEGORY_HAS_PRODUCTS",
                    "A category containing products cannot be deactivated",
                    Map.of("id", "Remove or move the category products first")
            );
        }
        category.setActive(false);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category not found", Map.of("id", "No category exists with this id")
        ));
    }

    private void assertNameIsAvailable(String rawName, Long id) {
        String name = rawName.trim();
        boolean exists = id == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_CATEGORY_NAME",
                    "A category with this name already exists",
                    Map.of("name", "Category name must be unique")
            );
        }
    }

    private String nextSlug(String name, Long currentId) {
        String base = slugGenerator.generate(name, "category");
        String candidate = base;
        int suffix = 2;
        while (categoryRepository.findBySlug(candidate)
                .filter(category -> !category.getId().equals(currentId))
                .isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(request.active());
    }
}
