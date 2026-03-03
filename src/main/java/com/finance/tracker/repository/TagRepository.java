package com.finance.tracker.repository;

import com.finance.tracker.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByUserIdAndNormalizedName(Long userId, String normalizedName);

    boolean existsByUserIdAndNormalizedName(Long userId, String normalizedName);

    boolean existsByUserIdAndNormalizedNameAndIdNot(Long userId, String normalizedName, Long id);
}
