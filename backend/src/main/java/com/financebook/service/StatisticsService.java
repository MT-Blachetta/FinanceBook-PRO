package com.financebook.service;

import com.financebook.dto.response.StatisticsResponse;
import com.financebook.entity.PaymentItem;
import com.financebook.entity.TransactionFeeRecord;
import com.financebook.entity.User;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.PaymentItemRepository;
import com.financebook.repository.TransactionFeeRecordRepository;
import com.financebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Server-side aggregation of a user's payment items.
 *
 * Keeps totals, monthly buckets, and per-standard-category buckets consistent
 * across all clients (web, Android) instead of every frontend re-implementing
 * the math.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String UNCATEGORIZED_LABEL = "UNCLASSIFIED";

    private final PaymentItemRepository paymentItemRepository;
    private final TransactionFeeRecordRepository feeRecordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(LocalDateTime dateFrom, LocalDateTime dateTo, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ValidationException("date_from must not be after date_to");
        }

        List<PaymentItem> items = paymentItemRepository.findByUserIdOrderByDateDesc(user.getId()).stream()
                .filter(i -> (dateFrom == null || !i.getDate().isBefore(dateFrom))
                        && (dateTo == null || !i.getDate().isAfter(dateTo)))
                .toList();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        Map<String, StatisticsResponse.MonthlyBucket> monthly = new TreeMap<>();
        Map<Long, StatisticsResponse.CategoryBucket> byCategory = new LinkedHashMap<>();

        for (PaymentItem item : items) {
            BigDecimal amount = item.getAmount();
            boolean expense = amount.signum() < 0;

            if (expense) {
                totalExpenses = totalExpenses.add(amount.abs());
            } else {
                totalIncome = totalIncome.add(amount);
            }

            String monthKey = item.getDate().format(MONTH_FORMAT);
            StatisticsResponse.MonthlyBucket monthBucket = monthly.computeIfAbsent(monthKey,
                    key -> StatisticsResponse.MonthlyBucket.builder()
                            .month(key)
                            .income(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO)
                            .net(BigDecimal.ZERO)
                            .build());
            if (expense) {
                monthBucket.setExpenses(monthBucket.getExpenses().add(amount.abs()));
            } else {
                monthBucket.setIncome(monthBucket.getIncome().add(amount));
            }
            monthBucket.setNet(monthBucket.getNet().add(amount));

            Long categoryId = item.getStandardCategory() != null
                    ? item.getStandardCategory().getId()
                    : null;
            String categoryName = item.getStandardCategory() != null
                    ? item.getStandardCategory().getName()
                    : UNCATEGORIZED_LABEL;
            // LinkedHashMap tolerates a null key for the "no standard category" bucket
            StatisticsResponse.CategoryBucket categoryBucket = byCategory.computeIfAbsent(categoryId,
                    key -> StatisticsResponse.CategoryBucket.builder()
                            .categoryId(key)
                            .categoryName(categoryName)
                            .income(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO)
                            .count(0)
                            .build());
            if (expense) {
                categoryBucket.setExpenses(categoryBucket.getExpenses().add(amount.abs()));
            } else {
                categoryBucket.setIncome(categoryBucket.getIncome().add(amount));
            }
            categoryBucket.setCount(categoryBucket.getCount() + 1);
        }

        BigDecimal totalFees = feeRecordRepository.findByUserId(user.getId()).stream()
                .filter(record -> {
                    LocalDateTime date = record.getPaymentItem() != null
                            ? record.getPaymentItem().getDate()
                            : null;
                    if (date == null) {
                        return dateFrom == null && dateTo == null;
                    }
                    return (dateFrom == null || !date.isBefore(dateFrom))
                            && (dateTo == null || !date.isAfter(dateTo));
                })
                .map(TransactionFeeRecord::getFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StatisticsResponse.CategoryBucket> categoryBuckets = new ArrayList<>(byCategory.values());
        categoryBuckets.sort(Comparator.comparing(
                bucket -> bucket.getExpenses().add(bucket.getIncome()),
                Comparator.reverseOrder()));

        return StatisticsResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(totalIncome.subtract(totalExpenses))
                .totalFees(totalFees)
                .itemCount(items.size())
                .monthly(new ArrayList<>(monthly.values()))
                .byCategory(categoryBuckets)
                .build();
    }
}
