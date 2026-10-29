package com.financebook.service;

import com.financebook.entity.PaymentItem;
import com.financebook.entity.TransactionFeePlan;
import com.financebook.entity.TransactionFeeRecord;
import com.financebook.repository.PaymentItemRepository;
import com.financebook.repository.TransactionFeePlanRepository;
import com.financebook.repository.TransactionFeeRecordRepository;
import com.financebook.util.FeeFormula;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Transaction fee computation engine for FinanceBook.
 * 
 * Provides functions to compute, apply, refund and recompute transaction
 * fees for payment items based on each user's fee plan.
 * 
 * Supports two fee-plan modes:
 * • "table" – amount-table intervals with per-interval regression curves
 * • "formula" – a single mathematical expression f(x, y)
 * 
 * Mirrors Python's fee_engine.py functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeEngineService {

    private final TransactionFeePlanRepository feePlanRepository;
    private final TransactionFeeRecordRepository feeRecordRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final ObjectMapper objectMapper;

    private static final BigDecimal MIN_FEE_THRESHOLD = new BigDecimal("0.01");

    /**
     * Compute the transaction fee for a given payment amount.
     * Mirrors Python's compute_fee() function.
     * 
     * @param amount Payment amount
     * @param userId User ID
     * @return Fee amount (or 0 if no fee applies)
     */
    public BigDecimal computeFee(BigDecimal amount, Long userId) {
        BigDecimal absAmount = amount.abs();

        // Load fee plan
        TransactionFeePlan plan = feePlanRepository.findByUserId(userId).orElse(null);
        if (plan == null) {
            return BigDecimal.ZERO; // No fee plan → no fees
        }

        BigDecimal fee = BigDecimal.ZERO;

        if ("formula".equals(plan.getMode())) {
            fee = computeFeeFormulaMode(plan, absAmount, userId);
        } else if ("table".equals(plan.getMode())) {
            fee = computeFeeTableMode(plan, absAmount, userId);
        }

        // Cap fee at 100% of absolute amount
        if (fee.compareTo(absAmount) > 0) {
            fee = absAmount;
        }

        // Round to 2 decimal places
        fee = fee.setScale(2, RoundingMode.HALF_UP);

        // If rounded fee < 0.01, no fee is charged
        if (fee.compareTo(MIN_FEE_THRESHOLD) < 0) {
            return BigDecimal.ZERO;
        }

        return fee;
    }

    /**
     * Compute fee using formula mode: the formula f(x, y) yields the absolute fee,
     * with x = absolute payment amount and y = payment frequency.
     * Mirrors Python's formula mode (fee = |f(x, y)|).
     */
    private BigDecimal computeFeeFormulaMode(TransactionFeePlan plan, BigDecimal absAmount, Long userId) {
        if (plan.getFormulaText() == null || plan.getFormulaText().isBlank()) {
            return BigDecimal.ZERO;
        }

        double freq = getPaymentFrequency(userId, BigDecimal.ZERO, null);

        OptionalDouble raw = FeeFormula.evaluate(plan.getFormulaText(), absAmount.doubleValue(), freq);
        if (raw.isEmpty()) {
            log.warn("Fee formula of user {} could not be evaluated, no fee applied", userId);
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.abs(raw.getAsDouble()));
    }

    /**
     * Key of an interval in intervalDataJson for a given lower limit.
     * Matches the admin panel's JavaScript String(number): 0 -> "0", 100 -> "100", 10.5 -> "10.5".
     */
    static String intervalKey(double lowerLimit) {
        return BigDecimal.valueOf(lowerLimit).stripTrailingZeros().toPlainString();
    }

    /**
     * Compute fee using table mode with regression.
     * Mirrors Python's table mode logic.
     */
    private BigDecimal computeFeeTableMode(TransactionFeePlan plan, BigDecimal absAmount, Long userId) {
        try {
            List<Double> amountTable = objectMapper.readValue(
                    plan.getAmountTableJson(),
                    new TypeReference<List<Double>>() {
                    });

            Map<String, Map<String, Object>> intervalData = objectMapper.readValue(
                    plan.getIntervalDataJson(),
                    new TypeReference<Map<String, Map<String, Object>>>() {
                    });

            if (amountTable.size() <= 1 && intervalData.isEmpty()) {
                return BigDecimal.ZERO; // Default table [0] with no interval data
            }

            // Find which interval the amount falls into
            String intervalKey = null;
            double lower = 0.0;
            Double upper = null;

            for (int i = 0; i < amountTable.size(); i++) {
                double lo = amountTable.get(i);
                Double hi = (i + 1 < amountTable.size()) ? amountTable.get(i + 1) : null;

                if (hi == null) {
                    // Last interval [lo, ∞)
                    if (absAmount.doubleValue() >= lo) {
                        intervalKey = intervalKey(lo);
                        lower = lo;
                        upper = null;
                        break;
                    }
                } else {
                    if (absAmount.doubleValue() >= lo && absAmount.doubleValue() < hi) {
                        intervalKey = intervalKey(lo);
                        lower = lo;
                        upper = hi;
                        break;
                    }
                }
            }

            if (intervalKey == null) {
                return BigDecimal.ZERO;
            }

            Map<String, Object> idata = intervalData.get(intervalKey);
            if (idata == null || idata.isEmpty()) {
                return BigDecimal.ZERO;
            }

            double maxFeePct = idata.get("maxFee") instanceof Number n ? n.doubleValue() : 0.1;

            // JSON numbers may be parsed as Integer or Double (e.g. [0, 0.05])
            if (!(idata.get("coefficients") instanceof List<?> rawCoefficients) || rawCoefficients.isEmpty()) {
                return BigDecimal.ZERO; // regression not run yet for this interval
            }
            List<Double> coefficients = rawCoefficients.stream()
                    .map(c -> ((Number) c).doubleValue())
                    .toList();

            // Compute payment frequency for this interval
            double freq = getPaymentFrequency(userId, BigDecimal.valueOf(lower),
                    upper != null ? BigDecimal.valueOf(upper) : null);

            // Evaluate regression polynomial
            double rawFeePct = evaluatePolynomial(coefficients, freq);

            // Clamp to [0, maxFeePct]
            rawFeePct = Math.max(0.0, Math.min(rawFeePct, maxFeePct));

            return absAmount.multiply(BigDecimal.valueOf(rawFeePct));

        } catch (Exception e) {
            log.error("Error computing table mode fee", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Compute payment frequency.
     * Mirrors Python's get_payment_frequency() function.
     * 
     * @param userId     User ID
     * @param lowerBound Lower bound of amount range
     * @param upperBound Upper bound of amount range (null for infinity)
     * @return Fraction of user's payments in the range [0.0, 1.0]
     */
    public double getPaymentFrequency(Long userId, BigDecimal lowerBound, BigDecimal upperBound) {
        long totalCount = paymentItemRepository.countByUserId(userId);
        if (totalCount == 0) {
            return 0.0;
        }

        long inRange = (upperBound == null)
                ? paymentItemRepository.countByUserIdAndAbsAmountAtLeast(userId, lowerBound)
                : paymentItemRepository.countByUserIdAndAbsAmountInRange(userId, lowerBound, upperBound);

        return (double) inRange / totalCount;
    }

    /**
     * Evaluate polynomial with given coefficients.
     * Coefficients are [c0, c1, c2, ...] for c0 + c1*x + c2*x² + ...
     */
    private double evaluatePolynomial(List<Double> coefficients, double x) {
        double result = 0.0;
        for (int i = 0; i < coefficients.size(); i++) {
            result += coefficients.get(i) * Math.pow(x, i);
        }
        return result;
    }

    /**
     * Apply fee to payment amount.
     * Mirrors Python's apply_fee_to_amount() function.
     * 
     * • Positive amount (income): amount - fee
     * • Negative amount (expense): amount - fee (e.g. -100 - 0.01 = -100.01)
     */
    public BigDecimal applyFeeToAmount(BigDecimal amount, BigDecimal fee) {
        return amount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Create fee record and apply fee to payment item.
     * Mirrors Python's create_fee_record() function.
     * 
     * @param paymentItem Payment item to apply fee to
     * @param userId      User ID
     * @return Fee amount applied
     */
    @Transactional
    public BigDecimal createFeeRecord(PaymentItem paymentItem, Long userId) {
        BigDecimal originalAmount = paymentItem.getAmount();
        BigDecimal fee = computeFee(originalAmount, userId);

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal adjusted = applyFeeToAmount(originalAmount, fee);
            paymentItem.setAmount(adjusted);
            paymentItemRepository.save(paymentItem);

            TransactionFeeRecord record = TransactionFeeRecord.builder()
                    .paymentItem(paymentItem)
                    .user(paymentItem.getUser())
                    .feeAmount(fee)
                    .originalAmount(originalAmount)
                    .build();

            feeRecordRepository.save(record);

            return fee;
        }

        return BigDecimal.ZERO;
    }

    /**
     * Recompute the fee for a payment item whose gross amount changed.
     *
     * The item's amount is expected to already carry the new gross amount. A fresh
     * fee is computed and applied; the existing fee record is updated, or removed
     * when no fee applies any more.
     *
     * @param paymentItem Payment item carrying the new gross amount
     * @param userId      User ID
     * @return Newly applied fee amount (0 if no fee applies)
     */
    @Transactional
    public BigDecimal recomputeFeeRecord(PaymentItem paymentItem, Long userId) {
        BigDecimal grossAmount = paymentItem.getAmount();
        BigDecimal fee = computeFee(grossAmount, userId);
        Optional<TransactionFeeRecord> existing = feeRecordRepository.findByPaymentItemId(paymentItem.getId());

        if (fee.compareTo(BigDecimal.ZERO) <= 0) {
            existing.ifPresent(feeRecordRepository::delete);
            return BigDecimal.ZERO;
        }

        paymentItem.setAmount(applyFeeToAmount(grossAmount, fee));
        paymentItemRepository.save(paymentItem);

        // Update the existing record in place (a payment item has at most one fee record)
        TransactionFeeRecord record = existing.orElseGet(() -> TransactionFeeRecord.builder()
                .paymentItem(paymentItem)
                .user(paymentItem.getUser())
                .build());
        record.setFeeAmount(fee);
        record.setOriginalAmount(grossAmount);
        feeRecordRepository.save(record);

        return fee;
    }

    /**
     * Refund fee for a deleted payment item.
     * Mirrors Python's refund_fee_record() function.
     * 
     * @param paymentItemId Payment item ID
     * @return Fee amount that was refunded
     */
    @Transactional
    public BigDecimal refundFeeRecord(Long paymentItemId) {
        TransactionFeeRecord record = feeRecordRepository.findByPaymentItemId(paymentItemId)
                .orElse(null);

        if (record != null) {
            BigDecimal fee = record.getFeeAmount();
            feeRecordRepository.delete(record);
            return fee;
        }

        return BigDecimal.ZERO;
    }

    /**
     * Compute regression coefficients from user-clicked points.
     * Mirrors Python's compute_regression_coefficients() function.
     * 
     * Uses Apache Commons Math for polynomial fitting.
     * 
     * @param points List of {freq: x, fee: y} points
     * @param maxFee Maximum fee percentage
     * @return Coefficients [c0, c1, c2, ...] for polynomial
     */
    public double[] computeRegressionCoefficients(List<Map<String, Double>> points, double maxFee) {
        if (points == null || points.isEmpty()) {
            // Identity function scaled to max_fee: f(x) = maxFee * x
            return new double[] { 0.0, maxFee };
        }

        if (points.size() == 1) {
            Map<String, Double> p = points.get(0);
            double x1 = p.get("freq");
            double y1 = p.get("fee");

            if (x1 == 0) {
                return new double[] { 0.0, maxFee };
            }

            double slope = y1 / x1;
            return new double[] { 0.0, slope };
        }

        if (points.size() == 2) {
            Map<String, Double> p1 = points.get(0);
            Map<String, Double> p2 = points.get(1);

            double x1 = p1.get("freq");
            double y1 = p1.get("fee");
            double x2 = p2.get("freq");
            double y2 = p2.get("fee");

            if (x2 == x1) {
                return new double[] { 0.0, maxFee };
            }

            double slope = (y2 - y1) / (x2 - x1);
            return new double[] { 0.0, slope };
        }

        // 3+ points: polynomial regression using Apache Commons Math
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (Map<String, Double> point : points) {
            obs.add(point.get("freq"), point.get("fee"));
        }

        int degree = Math.min(points.size() - 1, 5);
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);

        return fitter.fit(obs.toList());
    }
}
