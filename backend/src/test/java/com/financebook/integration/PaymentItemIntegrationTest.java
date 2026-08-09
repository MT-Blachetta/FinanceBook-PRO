package com.financebook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.financebook.entity.TransactionFeePlan;
import com.financebook.repository.TransactionFeePlanRepository;
import com.financebook.repository.TransactionFeeRecordRepository;
import com.financebook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Payment item CRUD, filtering, validation edge cases, multi-user isolation
 * and the fee lifecycle (apply on create, recompute on amount change, refund
 * on delete).
 */
class PaymentItemIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TransactionFeePlanRepository feePlanRepository;
    @Autowired
    private TransactionFeeRecordRepository feeRecordRepository;
    @Autowired
    private UserRepository userRepository;

    // ─── Creation ────────────────────────────────────────────────────

    @Test
    void createWithoutCategoriesFallsBackToUnclassified() throws Exception {
        String token = registerAndLogin("pi_default_cat");

        JsonNode item = createItem(token, "-42.50", "2026-01-15T10:00:00", Map.of());

        assertThat(item.get("amount").decimalValue()).isEqualByComparingTo("-42.50");
        assertThat(item.get("categories").findValuesAsText("name")).contains("UNCLASSIFIED");
        assertThat(item.get("standard_category").get("name").asText()).isEqualTo("UNCLASSIFIED");
    }

    @Test
    void createWithTwoCategoriesOfSameTypeIsRejected() throws Exception {
        String token = registerAndLogin("pi_same_type");
        long standardTypeId = findStandardTypeId(token);
        long catA = createCategory(token, "Food", standardTypeId, null);
        long catB = createCategory(token, "Rent", standardTypeId, null);

        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "amount", "-10.00",
                                "date", "2026-01-15T10:00:00",
                                "category_ids", List.of(catA, catB)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only one category per type is allowed"));
    }

    @Test
    void createWithSubCentAmountIsRejected() throws Exception {
        String token = registerAndLogin("pi_subcent");

        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("amount", "-10.123", "date", "2026-01-15T10:00:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithAmountBeyondColumnPrecisionIsRejected() throws Exception {
        String token = registerAndLogin("pi_too_big");

        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("amount", "100000000.00", "date", "2026-01-15T10:00:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutAmountOrDateIsRejected() throws Exception {
        String token = registerAndLogin("pi_missing_fields");

        Map<String, Object> noAmount = new HashMap<>();
        noAmount.put("date", "2026-01-15T10:00:00");
        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(noAmount)))
                .andExpect(status().isBadRequest());

        Map<String, Object> noDate = new HashMap<>();
        noDate.put("amount", "-5.00");
        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(noDate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithOverlongDescriptionIsRejected() throws Exception {
        String token = registerAndLogin("pi_long_desc");

        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "amount", "-5.00",
                                "date", "2026-01-15T10:00:00",
                                "description", "x".repeat(1001)))))
                .andExpect(status().isBadRequest());
    }

    // ─── Multi-user isolation ───────────────────────────────────────

    @Test
    void usersCannotAccessEachOthersItems() throws Exception {
        String tokenA = registerAndLogin("pi_owner_a");
        String tokenB = registerAndLogin("pi_owner_b");

        long itemId = createItem(tokenA, "-10.00", "2026-01-15T10:00:00", Map.of()).get("id").asLong();

        mockMvc.perform(get("/payment-items/" + itemId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/payment-items/" + itemId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("amount", "-99.00"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/payment-items/" + itemId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        // User B's listing must not contain user A's item
        MvcResult listing = mockMvc.perform(get("/payment-items")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(listing)).isEmpty();
    }

    @Test
    void usingAnotherUsersRecipientIsRejected() throws Exception {
        String tokenA = registerAndLogin("pi_recipient_a");
        String tokenB = registerAndLogin("pi_recipient_b");

        MvcResult recipientResult = mockMvc.perform(post("/recipients")
                        .header("Authorization", bearer(tokenA))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "A-only shop"))))
                .andExpect(status().isOk())
                .andReturn();
        long recipientId = parse(recipientResult).get("id").asLong();

        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "amount", "-10.00",
                                "date", "2026-01-15T10:00:00",
                                "recipient_id", recipientId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/payment-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/payment-items")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("amount", "-1.00", "date", "2026-01-15T10:00:00"))))
                .andExpect(status().isUnauthorized());
    }

    // ─── Filtering ──────────────────────────────────────────────────

    @Test
    void conflictingDirectionFiltersAreRejected() throws Exception {
        String token = registerAndLogin("pi_conflict");

        mockMvc.perform(get("/payment-items?expenseOnly=true&incomeOnly=true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void directionFiltersSplitIncomesAndExpenses() throws Exception {
        String token = registerAndLogin("pi_direction");
        createItem(token, "-30.00", "2026-01-10T08:00:00", Map.of());
        createItem(token, "1200.00", "2026-01-01T08:00:00", Map.of());

        MvcResult expenses = mockMvc.perform(get("/payment-items?expenseOnly=true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertThat(parse(expenses)).hasSize(1);
        assertThat(parse(expenses).get(0).get("amount").decimalValue()).isNegative();

        MvcResult incomes = mockMvc.perform(get("/payment-items?incomeOnly=true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        assertThat(parse(incomes)).hasSize(1);
        assertThat(parse(incomes).get(0).get("amount").decimalValue()).isPositive();
    }

    @Test
    void dateRangeFilterLimitsResults() throws Exception {
        String token = registerAndLogin("pi_daterange");
        createItem(token, "-10.00", "2026-01-15T10:00:00", Map.of());
        createItem(token, "-20.00", "2026-02-15T10:00:00", Map.of());
        createItem(token, "-30.00", "2026-03-15T10:00:00", Map.of());

        MvcResult result = mockMvc.perform(
                        get("/payment-items?dateFrom=2026-02-01T00:00:00&dateTo=2026-02-28T23:59:59")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = parse(result);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("amount").decimalValue()).isEqualByComparingTo("-20.00");
    }

    @Test
    void invertedDateRangeIsRejected() throws Exception {
        String token = registerAndLogin("pi_daterange_bad");

        mockMvc.perform(get("/payment-items?dateFrom=2026-03-01T00:00:00&dateTo=2026-01-01T00:00:00")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedDateParameterIsRejected() throws Exception {
        String token = registerAndLogin("pi_date_bad");

        mockMvc.perform(get("/payment-items?dateFrom=not-a-date")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoryFilterIncludesDescendantCategories() throws Exception {
        String token = registerAndLogin("pi_cat_tree");
        long standardTypeId = findStandardTypeId(token);
        long parent = createCategory(token, "Transport", standardTypeId, null);
        long child = createCategory(token, "Fuel", standardTypeId, parent);

        createItem(token, "-55.00", "2026-01-15T10:00:00", Map.of("category_ids", List.of(child)));

        MvcResult result = mockMvc.perform(get("/payment-items?categoryIds=" + parent)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = parse(result);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("amount").decimalValue()).isEqualByComparingTo("-55.00");
    }

    // ─── Fee lifecycle ──────────────────────────────────────────────

    @Test
    void feeIsAppliedOnCreateRecomputedOnAmountChangeAndRefundedOnDelete() throws Exception {
        String token = registerAndLogin("pi_fees");
        givePercentFeePlan("pi_fees");

        // Create: 5% of 100 → fee 5.00, stored amount -105.00
        JsonNode created = createItem(token, "-100.00", "2026-01-15T10:00:00", Map.of());
        long itemId = created.get("id").asLong();
        assertThat(created.get("transaction_fee").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(created.get("amount").decimalValue()).isEqualByComparingTo("-105.00");

        // Update amount: fee recomputed on the new gross amount
        MvcResult updated = mockMvc.perform(put("/payment-items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("amount", "-200.00"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updatedItem = parse(updated);
        assertThat(updatedItem.get("transaction_fee").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(updatedItem.get("amount").decimalValue()).isEqualByComparingTo("-210.00");

        // Update without amount change: fee stays
        MvcResult renamed = mockMvc.perform(put("/payment-items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("description", "insurance"))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(renamed).get("transaction_fee").decimalValue()).isEqualByComparingTo("10.00");

        // Delete: fee record refunded
        mockMvc.perform(delete("/payment-items/" + itemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        assertThat(feeRecordRepository.findByPaymentItemId(itemId)).isEmpty();

        mockMvc.perform(get("/payment-items/" + itemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCanReplaceCategoriesAndClearThemBackToUnclassified() throws Exception {
        String token = registerAndLogin("pi_update_cats");
        long standardTypeId = findStandardTypeId(token);
        long food = createCategory(token, "Food", standardTypeId, null);

        long itemId = createItem(token, "-10.00", "2026-01-15T10:00:00",
                Map.of("category_ids", List.of(food))).get("id").asLong();

        // Clear categories → falls back to UNCLASSIFIED
        MvcResult cleared = mockMvc.perform(put("/payment-items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("category_ids", List.of()))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(cleared).get("categories").findValuesAsText("name"))
                .containsExactly("UNCLASSIFIED");
    }

    @Test
    void getUnknownItemReturnsNotFound() throws Exception {
        String token = registerAndLogin("pi_unknown");

        mockMvc.perform(get("/payment-items/999999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private JsonNode createItem(String token, String amount, String date, Map<String, Object> extra)
            throws Exception {
        Map<String, Object> body = new HashMap<>(extra);
        body.put("amount", amount);
        body.put("date", date);

        MvcResult result = mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result);
    }

    private long findStandardTypeId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/category-types")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode type : parse(result)) {
            if (type.get("name").asText().equals("standard")) {
                return type.get("id").asLong();
            }
        }
        throw new IllegalStateException("standard type missing");
    }

    private long createCategory(String token, String name, long typeId, Long parentId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type_id", typeId);
        if (parentId != null) {
            body.put("parent_id", parentId);
        }
        MvcResult result = mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asLong();
    }

    /** Give the user a 5% flat table-mode fee plan (maxFee 10%). */
    private void givePercentFeePlan(String username) {
        var user = userRepository.findByUsername(username).orElseThrow();
        feePlanRepository.save(TransactionFeePlan.builder()
                .user(user)
                .mode("table")
                .amountTableJson("[0]")
                .intervalDataJson("{\"0\": {\"maxFee\": 0.1, \"coefficients\": [0.05]}}")
                .build());
    }
}
