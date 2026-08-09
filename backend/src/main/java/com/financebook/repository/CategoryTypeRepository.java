package com.financebook.repository;

import com.financebook.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CategoryType entity operations.
 */
@Repository
public interface CategoryTypeRepository extends JpaRepository<CategoryType, Long> {
    
    List<CategoryType> findByUserId(Long userId);
    
    Optional<CategoryType> findByUserIdAndName(Long userId, String name);
}
