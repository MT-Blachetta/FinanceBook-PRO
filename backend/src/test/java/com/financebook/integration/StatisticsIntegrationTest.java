package com.financebook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Server-side statistics endpoint: totals, monthly buckets, per-category
 * buckets, date-range restriction and multi-user isolation.
 */
class StatisticsIntegrationTest extends IntegrationTestBase {

    @Test
    void aggregatesTotalsMonthlyAndCategoryBuckets() throws Exception {
        String token = registerAndLogin("stats_user");

        long standardTypeId = findStandardTypeId(token);
        long food = createCategory(token, "Food", standardTypeId);

        createItem(token, "1000.00", "2026-01-05T09:00:00", null);
        createItem(token, "-200.00", "2026-01-10T09:00:00", food);
        createItem(token, "-300.00", "2026-02-01T09:00:00", food);

        MvcResult result = mockMvc.perform(get("/statistics")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stats = parse(result);

        assertThat(stats.get("total_income").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(stats.get("total_expenses").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(stats.get("net_balance").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(stats.get("item_count").asLong()).isEqualTo(3);

        // Monthly buckets sorted ascending
        JsonNode monthly = stats.get("monthly");
        assertThat(monthly).hasSize(2);
        assertThat(monthly.get(0).get("month").asText()).isEqualTo("2026-01");
        assertThat(monthly.get(0).get("income").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(monthly.get(0).get("expenses").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(monthly.get(0).get("net").decimalValue()).isEqualByComparingTo("800.00");
        assertThat(monthly.get(1).get("month").asText()).isEqualTo("2026-02");
        assertThat(monthly.get(1).get("net").decimalValue()).isEqualByComparingTo("-300.00");

        // Category buckets: Food carries the expenses, UNCLASSIFIED the income
        JsonNode byCategory = stats.get("by_category");
        assertThat(byCategory.findValuesAsText("category_name"))
                .contains("Food", "UNCLASSIFIED");
        for (JsonNode bucket : byCategory) {
            if (bucket.get("category_name").asText().equals("Food")) {
                assertThat(bucket.get("expenses").decimalValue()).isEqualByComparingTo("500.00");
                assertThat(bucket.get("count").asLong()).isEqualTo(2);
            }
        }
    }

    @Test
    void dateRangeRestrictsStatistics() throws Exception {
        String token = registerAndLogin("stats_range");
        createItem(token, "-100.00", "2026-01-15T10:00:00", null);
        createItem(token, "-999.00", "2026-06-15T10:00:00", null);

        MvcResult result = mockMvc.perform(
                        get("/statistics?dateFrom=2026-01-01T00:00:00&dateTo=2026-01-31T23:59:59")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stats = parse(result);

        assertThat(stats.get("total_expenses").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(stats.get("item_count").asLong()).isEqualTo(1);
    }

    @Test
    void invertedDateRangeIsRejected() throws Exception {
        String token = registerAndLogin("stats_bad_range");

        mockMvc.perform(get("/statistics?dateFrom=2026-06-01T00:00:00&dateTo=2026-01-01T00:00:00")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statisticsAreEmptyForFreshUsersAndIsolatedPerUser() throws Exception {
        String tokenA = registerAndLogin("stats_iso_a");
        String tokenB = registerAndLogin("stats_iso_b");

        createItem(tokenA, "-50.00", "2026-01-15T10:00:00", null);

        MvcResult result = mockMvc.perform(get("/statistics")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stats = parse(result);

        assertThat(stats.get("item_count").asLong()).isZero();
        assertThat(stats.get("total_expenses").decimalValue()).isEqualByComparingTo("0");
        assertThat(stats.get("monthly")).isEmpty();
    }

    @Test
    void statisticsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/statistics")).andExpect(status().isUnauthorized());
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private void createItem(String token, String amount, String date, Long categoryId) throws Exception {
        Map<String, Object> body = categoryId == null
                ? Map.of("amount", amount, "date", date)
                : Map.of("amount", amount, "date", date, "category_ids", List.of(categoryId));
        mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk());
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

    private long createCategory(String token, String name, long typeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", name, "type_id", typeId))))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asLong();
    }
}
