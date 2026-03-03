package com.finance.tracker.repository;

import com.finance.tracker.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndNameAndIdNot(Long userId, String name, Long id);
}
