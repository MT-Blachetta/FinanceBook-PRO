package com.financebook.repository;

import com.financebook.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity operations.
 * Supports hierarchical category queries.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByUserId(Long userId);
    
    List<Category> findByUserIdAndTypeId(Long userId, Long typeId);
    
    List<Category> findByParentId(Long parentId);
    
    List<Category> findByUserIdAndParentIdIsNull(Long userId);
    
    Optional<Category> findByUserIdAndName(Long userId, String name);
    
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND c.id = :categoryId")
    Optional<Category> findByIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);
}
