package com.asalkartechworks.kshitijascreations.product;

import com.asalkartechworks.kshitijascreations.domain.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> admin(String search, Long categoryId, Boolean available, Boolean featured) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("productCode")), pattern),
                        builder.like(builder.lower(root.get("shortDescription")), pattern)
                ));
            }
            if (categoryId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("category").get("id"), categoryId));
            }
            if (available != null) {
                predicate = builder.and(predicate, builder.equal(root.get("available"), available));
            }
            if (featured != null) {
                predicate = builder.and(predicate, builder.equal(root.get("featured"), featured));
            }
            return predicate;
        };
    }

    public static Specification<Product> publicProducts(String search, String categorySlug, Boolean featured) {
        return (root, query, builder) -> {
            Join<Object, Object> category = root.join("category", JoinType.INNER);
            var predicate = builder.and(
                    builder.isFalse(root.get("deleted")),
                    builder.isTrue(category.get("active"))
            );
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("productCode")), pattern),
                        builder.like(builder.lower(root.get("shortDescription")), pattern)
                ));
            }
            if (StringUtils.hasText(categorySlug)) {
                predicate = builder.and(predicate, builder.equal(category.get("slug"), categorySlug.trim()));
            }
            if (featured != null) {
                predicate = builder.and(predicate, builder.equal(root.get("featured"), featured));
            }
            return predicate;
        };
    }
}
