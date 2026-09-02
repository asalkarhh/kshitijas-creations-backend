package com.asalkartechworks.kshitijascreations.repository;

import com.asalkartechworks.kshitijascreations.domain.entity.Banner;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<Banner, Long>, JpaSpecificationExecutor<Banner> {

    @Query("SELECT b FROM Banner b WHERE b.active = true " +
            "AND (b.startAt IS NULL OR b.startAt <= :now) " +
            "AND (b.endAt IS NULL OR b.endAt >= :now) " +
            "ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findVisibleBanners(@Param("now") Instant now);
}
