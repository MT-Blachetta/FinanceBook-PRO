package com.financebook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recipient management: normalization, uniqueness, isolation, and deletion
 * that preserves historical payment items.
 */
class RecipientIntegrationTest extends IntegrationTestBase {

    @Test
    void namesAreNormalizedAndDuplicatesRejected() throws Exception {
        String token = registerAndLogin("rec_norm");

        JsonNode created = createRecipient(token, "  Corner   Shop  ");
        assertThat(created.get("name").asText()).isEqualTo("Corner Shop");

        mockMvc.perform(post("/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "Corner  Shop"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whitespaceOnlyNameIsRejected() throws Exception {
        String token = registerAndLogin("rec_blank");

        mockMvc.perform(post("/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameNameIsAllowedForDifferentUsers() throws Exception {
        String tokenA = registerAndLogin("rec_multi_a");
        String tokenB = registerAndLogin("rec_multi_b");

        createRecipient(tokenA, "Landlord");
        createRecipient(tokenB, "Landlord"); // must not clash across users
    }

    @Test
    void updateToExistingNameIsRejected() throws Exception {
        String token = registerAndLogin("rec_update_dup");
        createRecipient(token, "First");
        long secondId = createRecipient(token, "Second").get("id").asLong();

        mockMvc.perform(put("/recipients/" + secondId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", " First "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotSeeOrEditForeignRecipients() throws Exception {
        String tokenA = registerAndLogin("rec_iso_a");
        String tokenB = registerAndLogin("rec_iso_b");
        long recipientOfA = createRecipient(tokenA, "Secret Partner").get("id").asLong();

        mockMvc.perform(get("/recipients/" + recipientOfA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/recipients/" + recipientOfA)
                        .header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "Hijacked"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/recipients/" + recipientOfA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingRecipientKeepsPaymentItemsWithoutTheLink() throws Exception {
        String token = registerAndLogin("rec_delete");
        long recipientId = createRecipient(token, "Old Employer").get("id").asLong();

        MvcResult itemResult = mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "amount", "2500.00",
                                "date", "2026-01-31T12:00:00",
                                "recipient_id", recipientId))))
                .andExpect(status().isOk())
                .andReturn();
        long itemId = parse(itemResult).get("id").asLong();

        mockMvc.perform(delete("/recipients/" + recipientId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        // The payment survives, but without a recipient
        MvcResult item = mockMvc.perform(get("/payment-items/" + itemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(item).get("recipient").isNull()).isTrue();
        assertThat(parse(item).get("amount").decimalValue()).isEqualByComparingTo("2500.00");

        mockMvc.perform(get("/recipients/" + recipientId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private JsonNode createRecipient(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", name))))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result);
    }
}
