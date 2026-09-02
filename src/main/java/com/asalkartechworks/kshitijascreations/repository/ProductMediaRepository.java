package com.asalkartechworks.kshitijascreations.repository;

import com.asalkartechworks.kshitijascreations.domain.entity.ProductMedia;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findAllByProductIdOrderByPrimaryMediaDescDisplayOrderAsc(Long productId);

    List<ProductMedia> findAllByProductIdInOrderByPrimaryMediaDescDisplayOrderAsc(Collection<Long> productIds);

    Optional<ProductMedia> findByIdAndProductId(Long id, Long productId);

    @Modifying
    @Query("update ProductMedia media set media.primaryMedia = false where media.product.id = :productId and media.primaryMedia = true")
    int clearPrimaryMedia(@Param("productId") Long productId);
}
