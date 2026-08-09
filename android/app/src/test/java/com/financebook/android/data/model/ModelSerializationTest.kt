package com.financebook.android.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the JSON contract with the Spring Boot backend
 * (snake_case fields, unknown fields tolerated, nulls omitted on write).
 */
class ModelSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `payment item parses backend snake_case payload`() {
        val payload = """
            {
              "id": 7,
              "amount": -105.00,
              "date": "2026-01-15T10:00:00",
              "periodic": false,
              "description": "insurance",
              "invoice_path": null,
              "recipient_id": 3,
              "recipient": {"id": 3, "name": "Insurer AG", "address": null},
              "categories": [
                {"id": 2, "name": "Food", "type_id": 1, "parent_id": null, "children": []}
              ],
              "standard_category_id": 2,
              "standard_category": {"id": 2, "name": "Food", "type_id": 1},
              "transaction_fee": 5.00,
              "unknown_future_field": "ignored"
            }
        """.trimIndent()

        val item = json.decodeFromString<PaymentItem>(payload)

        assertEquals(7L, item.id)
        assertEquals(-105.00, item.amount, 1e-9)
        assertTrue(item.isExpense)
        assertEquals("Insurer AG", item.recipient?.name)
        assertEquals(2L, item.standardCategoryId)
        assertEquals(5.00, item.transactionFee!!, 1e-9)
        assertEquals(1, item.categories.size)
    }

    @Test
    fun `create request omits null optional fields`() {
        val request = PaymentItemCreate(
            amount = "-10.00",
            date = "2026-01-15T10:00:00",
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"amount\":\"-10.00\""))
        assertTrue(!encoded.contains("recipient_id"))
        assertTrue(!encoded.contains("category_ids"))
    }

    @Test
    fun `create request uses snake_case for relations`() {
        val request = PaymentItemCreate(
            amount = "-10.00",
            date = "2026-01-15T10:00:00",
            recipientId = 4,
            categoryIds = listOf(1, 2),
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"recipient_id\":4"))
        assertTrue(encoded.contains("\"category_ids\":[1,2]"))
    }

    @Test
    fun `statistics payload parses including buckets`() {
        val payload = """
            {
              "total_income": 1000.0,
              "total_expenses": 500.0,
              "net_balance": 500.0,
              "total_fees": 12.5,
              "item_count": 3,
              "monthly": [
                {"month": "2026-01", "income": 1000.0, "expenses": 200.0, "net": 800.0}
              ],
              "by_category": [
                {"category_id": null, "category_name": "UNCLASSIFIED",
                 "income": 1000.0, "expenses": 0.0, "count": 1}
              ]
            }
        """.trimIndent()

        val stats = json.decodeFromString<Statistics>(payload)

        assertEquals(500.0, stats.netBalance, 1e-9)
        assertEquals("2026-01", stats.monthly.single().month)
        assertNull(stats.byCategory.single().categoryId)
        assertEquals("UNCLASSIFIED", stats.byCategory.single().categoryName)
    }

    @Test
    fun `jwt response maps access_token`() {
        val response = json.decodeFromString<JwtResponse>(
            """{"access_token": "abc.def.ghi", "token_type": "bearer"}""",
        )
        assertEquals("abc.def.ghi", response.accessToken)
    }
}
