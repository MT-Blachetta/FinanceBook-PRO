package com.financebook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financebook.entity.PaymentItem;
import com.financebook.entity.TransactionFeePlan;
import com.financebook.entity.TransactionFeeRecord;
import com.financebook.entity.User;
import com.financebook.repository.PaymentItemRepository;
import com.financebook.repository.TransactionFeePlanRepository;
import com.financebook.repository.TransactionFeeRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the transaction fee engine.
 * Covers table-mode interval selection, clamping, rounding, the minimum fee
 * threshold, fee application/refund, and the regression coefficient helper.
 */
@ExtendWith(MockitoExtension.class)
class FeeEngineServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionFeePlanRepository feePlanRepository;
    @Mock
    private TransactionFeeRecordRepository feeRecordRepository;
    @Mock
    private PaymentItemRepository paymentItemRepository;

    private FeeEngineService service;

    @BeforeEach
    void setUp() {
        service = new FeeEngineService(
                feePlanRepository, feeRecordRepository, paymentItemRepository, new ObjectMapper());
    }

    // ─── computeFee ──────────────────────────────────────────────────

    @Test
    void noFeePlanMeansNoFee() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("0");
    }

    @Test
    void defaultEmptyTablePlanChargesNothing() {
        when(feePlanRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(tablePlan("[0]", "{}")));

        assertThat(service.computeFee(new BigDecimal("50.00"), USER_ID))
                .isEqualByComparingTo("0");
    }

    @Test
    void tableModeAppliesFlatPercentageFromConstantCoefficient() {
        // Interval [0, ∞) with constant 5% fee (coefficient c0 = 0.05)
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")));

        BigDecimal fee = service.computeFee(new BigDecimal("200.00"), USER_ID);

        assertThat(fee).isEqualByComparingTo("10.00"); // 5% of 200
    }

    @Test
    void tableModeUsesAbsoluteValueForExpenses() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")));

        BigDecimal fee = service.computeFee(new BigDecimal("-200.00"), USER_ID);

        assertThat(fee).isEqualByComparingTo("10.00");
    }

    @Test
    void tableModePicksTheIntervalContainingTheAmount() {
        // [0,100): 1%, [100,∞): 10%
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0, 100]",
                "{\"0\": {\"maxFee\": 0.5, \"coefficients\": [0.01]}," +
                        " \"100\": {\"maxFee\": 0.5, \"coefficients\": [0.10]}}")));

        assertThat(service.computeFee(new BigDecimal("50.00"), USER_ID))
                .isEqualByComparingTo("0.50");   // 1% of 50
        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("10.00");  // boundary belongs to upper interval
        assertThat(service.computeFee(new BigDecimal("500.00"), USER_ID))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void feePercentageIsClampedToMaxFee() {
        // Constant coefficient 0.9 but maxFee 0.1 → effective 10%
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.9]}}")));

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void negativePolynomialResultIsClampedToZero() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [-0.5]}}")));

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("0");
    }

    @Test
    void feeBelowOneCentIsWaived() {
        // 5% of 0.10 = 0.005 → rounds to 0.01... boundary: use 0.05 amount → 0.0025 → rounds to 0.00
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")));

        assertThat(service.computeFee(new BigDecimal("0.05"), USER_ID))
                .isEqualByComparingTo("0");
    }

    @Test
    void feeIsRoundedToTwoDecimals() {
        // 5% of 33.33 = 1.6665 → 1.67
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")));

        assertThat(service.computeFee(new BigDecimal("33.33"), USER_ID))
                .isEqualByComparingTo("1.67");
    }

    @Test
    void formulaModeEvaluatesTheFormulaAsAbsoluteFee() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(formulaPlan("x * 0.1 + 0.5")));

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("10.50");
        // x is the absolute amount, so expenses are charged the same
        assertThat(service.computeFee(new BigDecimal("-100.00"), USER_ID))
                .isEqualByComparingTo("10.50");
    }

    @Test
    void formulaModePassesPaymentFrequencyAsY() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(formulaPlan("10 * y")));
        when(paymentItemRepository.countByUserId(USER_ID)).thenReturn(4L);
        when(paymentItemRepository.countByUserIdAndAbsAmountAtLeast(USER_ID, BigDecimal.ZERO)).thenReturn(4L);

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void formulaModeFeeIsCappedAtTheAmount() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(formulaPlan("x * 5")));

        assertThat(service.computeFee(new BigDecimal("20.00"), USER_ID))
                .isEqualByComparingTo("20.00");
    }

    @Test
    void invalidFormulaChargesNothing() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(formulaPlan("x +")));

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("0");
    }

    @Test
    void tableModeFindsIntervalsWithDecimalLimitsAndIntegerCoefficients() {
        // Keys are written by the admin panel's JavaScript String(number): "0", "10.5"
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0, 10.5]",
                "{\"0\": {\"maxFee\": 1, \"coefficients\": [0]}," +
                        " \"10.5\": {\"maxFee\": 0.5, \"coefficients\": [0, 0.1]}}")));
        when(paymentItemRepository.countByUserId(USER_ID)).thenReturn(2L);
        when(paymentItemRepository.countByUserIdAndAbsAmountAtLeast(eq(USER_ID), any())).thenReturn(1L);

        // freq = 0.5 → 0 + 0.1 * 0.5 = 5% of 100
        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("5.00");
    }

    @Test
    void tableModeIntervalWithoutRegressionChargesNothing() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"points\": [], \"coefficients\": null}}")));

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("0");
    }

    @Test
    void intervalKeyMatchesJavaScriptNumberFormatting() {
        assertThat(FeeEngineService.intervalKey(0.0)).isEqualTo("0");
        assertThat(FeeEngineService.intervalKey(100.0)).isEqualTo("100");
        assertThat(FeeEngineService.intervalKey(10.5)).isEqualTo("10.5");
        assertThat(FeeEngineService.intervalKey(1000.25)).isEqualTo("1000.25");
    }

    @Test
    void corruptIntervalJsonFallsBackToZeroFee() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]", "{not valid json")));

        assertThat(service.computeFee(new BigDecimal("100.00"), USER_ID))
                .isEqualByComparingTo("0");
    }

    // ─── getPaymentFrequency ─────────────────────────────────────────

    @Test
    void frequencyIsZeroWithoutAnyPayments() {
        when(paymentItemRepository.countByUserId(USER_ID)).thenReturn(0L);

        assertThat(service.getPaymentFrequency(USER_ID, BigDecimal.ZERO, null)).isZero();
        verify(paymentItemRepository, never()).countByUserIdAndAbsAmountAtLeast(anyLong(), any());
    }

    @Test
    void frequencyUsesOpenEndedCountWhenUpperBoundIsNull() {
        when(paymentItemRepository.countByUserId(USER_ID)).thenReturn(4L);
        when(paymentItemRepository.countByUserIdAndAbsAmountAtLeast(USER_ID, BigDecimal.TEN))
                .thenReturn(1L);

        assertThat(service.getPaymentFrequency(USER_ID, BigDecimal.TEN, null))
                .isCloseTo(0.25, within(1e-9));
    }

    @Test
    void frequencyUsesRangeCountWhenUpperBoundIsGiven() {
        when(paymentItemRepository.countByUserId(USER_ID)).thenReturn(10L);
        when(paymentItemRepository.countByUserIdAndAbsAmountInRange(
                USER_ID, BigDecimal.ZERO, BigDecimal.TEN)).thenReturn(5L);

        assertThat(service.getPaymentFrequency(USER_ID, BigDecimal.ZERO, BigDecimal.TEN))
                .isCloseTo(0.5, within(1e-9));
    }

    // ─── applyFeeToAmount ────────────────────────────────────────────

    @Test
    void feeIsSubtractedFromIncomeAndAddedToExpenseMagnitude() {
        assertThat(service.applyFeeToAmount(new BigDecimal("100.00"), new BigDecimal("0.50")))
                .isEqualByComparingTo("99.50");
        // Expense: -100 - 0.50 = -100.50 (fee increases the money spent)
        assertThat(service.applyFeeToAmount(new BigDecimal("-100.00"), new BigDecimal("0.50")))
                .isEqualByComparingTo("-100.50");
    }

    // ─── createFeeRecord / refundFeeRecord / recomputeFeeRecord ─────

    @Test
    void createFeeRecordAdjustsAmountAndPersistsRecord() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")));

        PaymentItem item = paymentItem(new BigDecimal("-100.00"));

        BigDecimal fee = service.createFeeRecord(item, USER_ID);

        assertThat(fee).isEqualByComparingTo("5.00");
        assertThat(item.getAmount()).isEqualByComparingTo("-105.00");

        ArgumentCaptor<TransactionFeeRecord> captor = ArgumentCaptor.forClass(TransactionFeeRecord.class);
        verify(feeRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getFeeAmount()).isEqualByComparingTo("5.00");
        assertThat(captor.getValue().getOriginalAmount()).isEqualByComparingTo("-100.00");
        verify(paymentItemRepository).save(item);
    }

    @Test
    void createFeeRecordDoesNothingWhenNoFeeApplies() {
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        PaymentItem item = paymentItem(new BigDecimal("-100.00"));

        BigDecimal fee = service.createFeeRecord(item, USER_ID);

        assertThat(fee).isEqualByComparingTo("0");
        assertThat(item.getAmount()).isEqualByComparingTo("-100.00");
        verify(feeRecordRepository, never()).save(any());
        verify(paymentItemRepository, never()).save(any());
    }

    @Test
    void refundFeeRecordDeletesRecordAndReturnsFee() {
        TransactionFeeRecord record = TransactionFeeRecord.builder()
                .feeAmount(new BigDecimal("2.50"))
                .originalAmount(new BigDecimal("-50.00"))
                .build();
        when(feeRecordRepository.findByPaymentItemId(42L)).thenReturn(Optional.of(record));

        assertThat(service.refundFeeRecord(42L)).isEqualByComparingTo("2.50");
        verify(feeRecordRepository).delete(record);
    }

    @Test
    void refundFeeRecordIsNoOpWithoutRecord() {
        when(feeRecordRepository.findByPaymentItemId(42L)).thenReturn(Optional.empty());

        assertThat(service.refundFeeRecord(42L)).isEqualByComparingTo("0");
        verify(feeRecordRepository, never()).delete(any());
    }

    @Test
    void recomputeFeeRecordUpdatesExistingRecordInPlace() {
        TransactionFeeRecord oldRecord = TransactionFeeRecord.builder()
                .feeAmount(new BigDecimal("5.00"))
                .originalAmount(new BigDecimal("-100.00"))
                .build();
        when(feeRecordRepository.findByPaymentItemId(7L)).thenReturn(Optional.of(oldRecord));
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(tablePlan(
                "[0]",
                "{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")));

        PaymentItem item = paymentItem(new BigDecimal("-200.00"));
        item.setId(7L);

        BigDecimal fee = service.recomputeFeeRecord(item, USER_ID);

        assertThat(fee).isEqualByComparingTo("10.00");
        assertThat(item.getAmount()).isEqualByComparingTo("-210.00");
        assertThat(oldRecord.getFeeAmount()).isEqualByComparingTo("10.00");
        assertThat(oldRecord.getOriginalAmount()).isEqualByComparingTo("-200.00");
        verify(feeRecordRepository).save(oldRecord);
        verify(feeRecordRepository, never()).delete(any());
    }

    @Test
    void recomputeFeeRecordRemovesRecordWhenNoFeeAppliesAnyMore() {
        TransactionFeeRecord oldRecord = TransactionFeeRecord.builder()
                .feeAmount(new BigDecimal("5.00"))
                .originalAmount(new BigDecimal("-100.00"))
                .build();
        when(feeRecordRepository.findByPaymentItemId(7L)).thenReturn(Optional.of(oldRecord));
        when(feePlanRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        PaymentItem item = paymentItem(new BigDecimal("-200.00"));
        item.setId(7L);

        assertThat(service.recomputeFeeRecord(item, USER_ID)).isEqualByComparingTo("0");
        assertThat(item.getAmount()).isEqualByComparingTo("-200.00");
        verify(feeRecordRepository).delete(oldRecord);
        verify(feeRecordRepository, never()).save(any());
    }

    // ─── computeRegressionCoefficients ───────────────────────────────

    @Test
    void noPointsYieldsIdentityScaledToMaxFee() {
        double[] coeffs = service.computeRegressionCoefficients(List.of(), 0.2);
        assertThat(coeffs).containsExactly(0.0, 0.2);
    }

    @Test
    void singlePointYieldsProportionalSlope() {
        double[] coeffs = service.computeRegressionCoefficients(
                List.of(Map.of("freq", 0.5, "fee", 0.1)), 0.2);
        assertThat(coeffs[0]).isCloseTo(0.0, within(1e-9));
        assertThat(coeffs[1]).isCloseTo(0.2, within(1e-9));
    }

    @Test
    void singlePointAtZeroFrequencyFallsBackToMaxFeeSlope() {
        double[] coeffs = service.computeRegressionCoefficients(
                List.of(Map.of("freq", 0.0, "fee", 0.1)), 0.3);
        assertThat(coeffs).containsExactly(0.0, 0.3);
    }

    @Test
    void twoPointsYieldLinearSlope() {
        double[] coeffs = service.computeRegressionCoefficients(
                List.of(Map.of("freq", 0.0, "fee", 0.0), Map.of("freq", 1.0, "fee", 0.1)), 0.5);
        assertThat(coeffs[1]).isCloseTo(0.1, within(1e-9));
    }

    @Test
    void twoPointsWithSameFrequencyFallBackToMaxFee() {
        double[] coeffs = service.computeRegressionCoefficients(
                List.of(Map.of("freq", 0.5, "fee", 0.0), Map.of("freq", 0.5, "fee", 0.1)), 0.5);
        assertThat(coeffs).containsExactly(0.0, 0.5);
    }

    @Test
    void threePointsFitAPolynomialThroughThePoints() {
        // Points on the parabola y = x²
        double[] coeffs = service.computeRegressionCoefficients(
                List.of(
                        Map.of("freq", 0.0, "fee", 0.0),
                        Map.of("freq", 0.5, "fee", 0.25),
                        Map.of("freq", 1.0, "fee", 1.0)),
                1.0);

        // Evaluate fitted polynomial at 0.5 → should be very close to 0.25
        double value = 0;
        for (int i = 0; i < coeffs.length; i++) {
            value += coeffs[i] * Math.pow(0.5, i);
        }
        assertThat(value).isCloseTo(0.25, within(1e-6));
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private static TransactionFeePlan tablePlan(String amountTableJson, String intervalDataJson) {
        TransactionFeePlan plan = new TransactionFeePlan();
        plan.setMode("table");
        plan.setAmountTableJson(amountTableJson);
        plan.setIntervalDataJson(intervalDataJson);
        return plan;
    }

    private static TransactionFeePlan formulaPlan(String formula) {
        TransactionFeePlan plan = new TransactionFeePlan();
        plan.setMode("formula");
        plan.setFormulaText(formula);
        return plan;
    }

    private static PaymentItem paymentItem(BigDecimal amount) {
        User user = new User();
        user.setId(USER_ID);
        PaymentItem item = new PaymentItem();
        item.setUser(user);
        item.setAmount(amount);
        return item;
    }
}
