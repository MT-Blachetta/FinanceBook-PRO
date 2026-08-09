package com.financebook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
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
 * Category and category-type management: normalization, ownership checks,
 * tree integrity (no cycles), and safe deletion semantics.
 */
class CategoryIntegrationTest extends IntegrationTestBase {

    // ─── Creation & validation ──────────────────────────────────────

    @Test
    void namesAreNormalizedAndDuplicatesRejected() throws Exception {
        String token = registerAndLogin("cat_norm");
        long typeId = standardTypeId(token);

        JsonNode created = createCategory(token, "  My   Category  ", typeId, null);
        assertThat(created.get("name").asText()).isEqualTo("My Category");

        // Same name modulo whitespace is a duplicate
        mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "My  Category", "type_id", typeId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whitespaceOnlyNameIsRejected() throws Exception {
        String token = registerAndLogin("cat_blank");
        long typeId = standardTypeId(token);

        mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "   ", "type_id", typeId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersCategoryTypeCannotBeUsed() throws Exception {
        String tokenA = registerAndLogin("cat_type_a");
        String tokenB = registerAndLogin("cat_type_b");
        long typeOfA = standardTypeId(tokenA);

        mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "Steal", "type_id", typeOfA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void parentMustBelongToSameType() throws Exception {
        String token = registerAndLogin("cat_parent_type");
        long standard = standardTypeId(token);
        long otherType = createType(token, "payment method");
        long parentInOther = createCategory(token, "Cash", otherType, null).get("id").asLong();

        mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Groceries",
                                "type_id", standard,
                                "parent_id", parentInOther))))
                .andExpect(status().isBadRequest());
    }

    // ─── Update: tree integrity ─────────────────────────────────────

    @Test
    void categoryCannotBecomeItsOwnParent() throws Exception {
        String token = registerAndLogin("cat_self_parent");
        long typeId = standardTypeId(token);
        long catId = createCategory(token, "Loop", typeId, null).get("id").asLong();

        mockMvc.perform(put("/categories/" + catId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("parent_id", catId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoryCannotBeMovedBelowItsOwnDescendant() throws Exception {
        String token = registerAndLogin("cat_cycle");
        long typeId = standardTypeId(token);
        long root = createCategory(token, "Root", typeId, null).get("id").asLong();
        long child = createCategory(token, "Child", typeId, root).get("id").asLong();
        long grandchild = createCategory(token, "Grandchild", typeId, child).get("id").asLong();

        mockMvc.perform(put("/categories/" + root)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("parent_id", grandchild))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Cannot move a category below one of its own descendants"));
    }

    @Test
    void renamingToExistingNameIsRejected() throws Exception {
        String token = registerAndLogin("cat_rename_dup");
        long typeId = standardTypeId(token);
        createCategory(token, "Existing", typeId, null);
        long other = createCategory(token, "Other", typeId, null).get("id").asLong();

        mockMvc.perform(put("/categories/" + other)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", " Existing "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void descendantsEndpointReturnsWholeSubtree() throws Exception {
        String token = registerAndLogin("cat_descendants");
        long typeId = standardTypeId(token);
        long root = createCategory(token, "A", typeId, null).get("id").asLong();
        long b = createCategory(token, "B", typeId, root).get("id").asLong();
        createCategory(token, "C", typeId, b);

        MvcResult result = mockMvc.perform(get("/categories/" + root + "/descendants")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(parse(result).findValuesAsText("name")).contains("B", "C");
    }

    // ─── Deletion ───────────────────────────────────────────────────

    @Test
    void deleteReparentsChildrenAndMovesPaymentsToUnclassified() throws Exception {
        String token = registerAndLogin("cat_delete");
        long typeId = standardTypeId(token);
        long parent = createCategory(token, "Parent", typeId, null).get("id").asLong();
        long child = createCategory(token, "Child", typeId, parent).get("id").asLong();

        // A payment linked to the parent category
        MvcResult itemResult = mockMvc.perform(post("/payment-items")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "amount", "-10.00",
                                "date", "2026-01-15T10:00:00",
                                "category_ids", List.of(parent)))))
                .andExpect(status().isOk())
                .andReturn();
        long itemId = parse(itemResult).get("id").asLong();

        mockMvc.perform(delete("/categories/" + parent)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        // Child became a root category
        MvcResult childResult = mockMvc.perform(get("/categories/" + child)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(childResult).get("parent_id").isNull()).isTrue();

        // Payment fell back to UNCLASSIFIED
        MvcResult item = mockMvc.perform(get("/payment-items/" + itemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(item).get("categories").findValuesAsText("name"))
                .containsExactly("UNCLASSIFIED");
        assertThat(parse(item).get("standard_category").get("name").asText())
                .isEqualTo("UNCLASSIFIED");
    }

    @Test
    void unclassifiedCannotBeDeleted() throws Exception {
        String token = registerAndLogin("cat_delete_uncl");
        long unclassifiedId = findCategoryIdByName(token, "UNCLASSIFIED");

        mockMvc.perform(delete("/categories/" + unclassifiedId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersCategoryCannotBeDeleted() throws Exception {
        String tokenA = registerAndLogin("cat_del_a");
        String tokenB = registerAndLogin("cat_del_b");
        long typeId = standardTypeId(tokenA);
        long catOfA = createCategory(tokenA, "Private", typeId, null).get("id").asLong();

        mockMvc.perform(delete("/categories/" + catOfA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());
    }

    // ─── Category types ─────────────────────────────────────────────

    @Test
    void typeNamesAreNormalizedAndDuplicatesRejected() throws Exception {
        String token = registerAndLogin("ctype_dup");

        MvcResult created = mockMvc.perform(post("/category-types")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "  VAT   rate "))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(created).get("name").asText()).isEqualTo("VAT rate");

        mockMvc.perform(post("/category-types")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", "VAT  rate"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void standardTypeCannotBeDeleted() throws Exception {
        String token = registerAndLogin("ctype_std");
        long typeId = standardTypeId(token);

        mockMvc.perform(delete("/category-types/" + typeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonEmptyTypeCannotBeDeletedButEmptyOneCan() throws Exception {
        String token = registerAndLogin("ctype_delete");
        long typeId = createType(token, "temp type");
        long catId = createCategory(token, "Temp cat", typeId, null).get("id").asLong();

        mockMvc.perform(delete("/category-types/" + typeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/categories/" + catId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/category-types/" + typeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private long standardTypeId(String token) throws Exception {
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

    private long createType(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/category-types")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("name", name))))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asLong();
    }

    private JsonNode createCategory(String token, String name, long typeId, Long parentId)
            throws Exception {
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
        return parse(result);
    }

    private long findCategoryIdByName(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/categories")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode category : parse(result)) {
            if (category.get("name").asText().equals(name)) {
                return category.get("id").asLong();
            }
        }
        throw new IllegalStateException("category " + name + " missing");
    }
}
