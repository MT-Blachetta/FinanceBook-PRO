package com.financebook.repository;

import com.financebook.entity.PaymentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for PaymentItem entity operations.
 * Supports filtering by user, amount type (income/expense), and categories.
 */
@Repository
public interface PaymentItemRepository extends JpaRepository<PaymentItem, Long> {

    List<PaymentItem> findByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT p FROM PaymentItem p WHERE p.user.id = :userId AND p.amount < 0 ORDER BY p.date DESC")
    List<PaymentItem> findExpensesByUserIdOrderByDateDesc(@Param("userId") Long userId);

    @Query("SELECT p FROM PaymentItem p WHERE p.user.id = :userId AND p.amount >= 0 ORDER BY p.date DESC")
    List<PaymentItem> findIncomesByUserIdOrderByDateDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM PaymentItem p " +
            "JOIN p.categories c " +
            "WHERE p.user.id = :userId AND c.id IN :categoryIds " +
            "ORDER BY p.date DESC")
    List<PaymentItem> findByUserIdAndCategoryIdsOrderByDateDesc(
            @Param("userId") Long userId,
            @Param("categoryIds") Set<Long> categoryIds);

    @Query("SELECT DISTINCT p FROM PaymentItem p " +
            "JOIN p.categories c " +
            "WHERE p.user.id = :userId AND c.id IN :categoryIds " +
            "AND p.amount < 0 " +
            "ORDER BY p.date DESC")
    List<PaymentItem> findExpensesByUserIdAndCategoryIdsOrderByDateDesc(
            @Param("userId") Long userId,
            @Param("categoryIds") Set<Long> categoryIds);

    @Query("SELECT DISTINCT p FROM PaymentItem p " +
            "JOIN p.categories c " +
            "WHERE p.user.id = :userId AND c.id IN :categoryIds " +
            "AND p.amount >= 0 " +
            "ORDER BY p.date DESC")
    List<PaymentItem> findIncomesByUserIdAndCategoryIdsOrderByDateDesc(
            @Param("userId") Long userId,
            @Param("categoryIds") Set<Long> categoryIds);

    Optional<PaymentItem> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(p) FROM PaymentItem p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // Two dedicated count queries instead of one with a nullable :upperBound -
    // "(:param IS NULL OR ...)" on a numeric parameter fails on PostgreSQL with
    // "could not determine data type of parameter".
    @Query("SELECT COUNT(p) FROM PaymentItem p WHERE p.user.id = :userId " +
            "AND ABS(p.amount) >= :lowerBound AND ABS(p.amount) < :upperBound")
    long countByUserIdAndAbsAmountInRange(
            @Param("userId") Long userId,
            @Param("lowerBound") BigDecimal lowerBound,
            @Param("upperBound") BigDecimal upperBound);

    @Query("SELECT COUNT(p) FROM PaymentItem p WHERE p.user.id = :userId " +
            "AND ABS(p.amount) >= :lowerBound")
    long countByUserIdAndAbsAmountAtLeast(
            @Param("userId") Long userId,
            @Param("lowerBound") BigDecimal lowerBound);

    List<PaymentItem> findByRecipientId(Long recipientId);

    List<PaymentItem> findByStandardCategoryId(Long standardCategoryId);

    @Query("SELECT p FROM PaymentItem p JOIN p.categories c WHERE c.id = :categoryId")
    List<PaymentItem> findByLinkedCategoryId(@Param("categoryId") Long categoryId);
}
