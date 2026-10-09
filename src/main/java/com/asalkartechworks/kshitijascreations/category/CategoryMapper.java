package com.asalkartechworks.kshitijascreations.category;

import com.asalkartechworks.kshitijascreations.category.dto.AdminCategoryResponse;
import com.asalkartechworks.kshitijascreations.category.dto.PublicCategoryResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public AdminCategoryResponse toAdminResponse(Category category, long productCount) {
        return new AdminCategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.getDisplayOrder(), productCount, category.isActive(), category.getCreatedAt(), category.getUpdatedAt()
        );
    }

    public PublicCategoryResponse toPublicResponse(Category category) {
        return new PublicCategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription(), category.getDisplayOrder()
        );
    }
}
