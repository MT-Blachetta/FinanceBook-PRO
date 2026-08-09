package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated statistics for the current user's payment items.
 *
 * Conventions:
 * • totalIncome and totalExpenses are absolute (non-negative) values
 * • netBalance = totalIncome - totalExpenses
 * • monthly buckets are keyed "yyyy-MM" and sorted ascending
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private BigDecimal totalFees;
    private long itemCount;

    private List<MonthlyBucket> monthly;
    private List<CategoryBucket> byCategory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyBucket {
        private String month; // "yyyy-MM"
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryBucket {
        private Long categoryId; // null for items without a standard category
        private String categoryName;
        private BigDecimal income;
        private BigDecimal expenses;
        private long count;
    }
}
