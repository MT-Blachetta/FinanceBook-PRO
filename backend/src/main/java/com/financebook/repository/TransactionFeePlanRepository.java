package com.financebook.repository;

import com.financebook.entity.TransactionFeePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for TransactionFeePlan entity operations.
 */
@Repository
public interface TransactionFeePlanRepository extends JpaRepository<TransactionFeePlan, Long> {
    
    Optional<TransactionFeePlan> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
