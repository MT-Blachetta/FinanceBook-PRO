package com.financebook.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API models. Field names mirror the backend's snake_case JSON
 * (Jackson SNAKE_CASE strategy on the Spring Boot side).
 */

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class JwtResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
)

@Serializable
data class UserRead(
    val id: Long,
    val username: String,
    val surname: String? = null,
    val prename: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class Recipient(
    val id: Long,
    val name: String,
    val address: String? = null,
)

@Serializable
data class RecipientCreate(
    val name: String,
    val address: String? = null,
)

@Serializable
data class Category(
    val id: Long,
    val name: String,
    @SerialName("type_id") val typeId: Long? = null,
    @SerialName("parent_id") val parentId: Long? = null,
    @SerialName("icon_file") val iconFile: String? = null,
    val children: List<Category> = emptyList(),
)

@Serializable
data class CategoryCreate(
    val name: String,
    @SerialName("type_id") val typeId: Long,
    @SerialName("parent_id") val parentId: Long? = null,
)

@Serializable
data class CategoryType(
    val id: Long,
    val name: String,
    val description: String? = null,
)

@Serializable
data class CategoryTypeCreate(
    val name: String,
    val description: String? = null,
)

@Serializable
data class PaymentItem(
    val id: Long,
    val amount: Double,
    /** ISO local date-time, e.g. "2026-01-15T10:00:00" */
    val date: String,
    val periodic: Boolean = false,
    val description: String? = null,
    @SerialName("invoice_path") val invoicePath: String? = null,
    @SerialName("recipient_id") val recipientId: Long? = null,
    val recipient: Recipient? = null,
    val categories: List<Category> = emptyList(),
    @SerialName("standard_category_id") val standardCategoryId: Long? = null,
    @SerialName("standard_category") val standardCategory: Category? = null,
    @SerialName("transaction_fee") val transactionFee: Double? = null,
) {
    val isExpense: Boolean get() = amount < 0
}

@Serializable
data class PaymentItemCreate(
    val amount: String,
    val date: String,
    val periodic: Boolean = false,
    val description: String? = null,
    @SerialName("recipient_id") val recipientId: Long? = null,
    @SerialName("category_ids") val categoryIds: List<Long>? = null,
)

@Serializable
data class PaymentItemUpdate(
    val amount: String? = null,
    val date: String? = null,
    val periodic: Boolean? = null,
    val description: String? = null,
    @SerialName("recipient_id") val recipientId: Long? = null,
    @SerialName("category_ids") val categoryIds: List<Long>? = null,
)

@Serializable
data class MonthlyBucket(
    val month: String,
    val income: Double,
    val expenses: Double,
    val net: Double,
)

@Serializable
data class CategoryBucket(
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("category_name") val categoryName: String,
    val income: Double,
    val expenses: Double,
    val count: Long,
)

@Serializable
data class Statistics(
    @SerialName("total_income") val totalIncome: Double,
    @SerialName("total_expenses") val totalExpenses: Double,
    @SerialName("net_balance") val netBalance: Double,
    @SerialName("total_fees") val totalFees: Double,
    @SerialName("item_count") val itemCount: Long,
    val monthly: List<MonthlyBucket> = emptyList(),
    @SerialName("by_category") val byCategory: List<CategoryBucket> = emptyList(),
)

@Serializable
data class ApiError(
    val status: Int? = null,
    val message: String? = null,
)
