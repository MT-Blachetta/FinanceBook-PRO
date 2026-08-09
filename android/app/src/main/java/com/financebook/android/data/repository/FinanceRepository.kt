package com.financebook.android.data.repository

import com.financebook.android.data.api.FinanceBookApi
import com.financebook.android.data.auth.TokenStore
import com.financebook.android.data.model.*
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Single data access point for the UI layer.
 * Wraps API calls in [Result] and translates backend error bodies into
 * readable messages.
 */
class FinanceRepository(
    private val api: FinanceBookApi,
    private val tokenStore: TokenStore,
) {

    private val errorJson = Json { ignoreUnknownKeys = true }

    // ── Auth ─────────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequest(username.trim(), password))
        tokenStore.saveToken(response.accessToken)
    }.mapError()

    suspend fun logout() {
        tokenStore.clearToken()
    }

    // ── Payment items ───────────────────────────────────────────────

    suspend fun getPaymentItems(
        expenseOnly: Boolean = false,
        incomeOnly: Boolean = false,
        categoryIds: List<Long> = emptyList(),
    ): Result<List<PaymentItem>> = runCatching {
        api.getPaymentItems(
            expenseOnly = if (expenseOnly) true else null,
            incomeOnly = if (incomeOnly) true else null,
            categoryIds = categoryIds.ifEmpty { null },
        )
    }.mapError()

    suspend fun getPaymentItem(id: Long): Result<PaymentItem> =
        runCatching { api.getPaymentItem(id) }.mapError()

    suspend fun createPaymentItem(request: PaymentItemCreate): Result<PaymentItem> =
        runCatching { api.createPaymentItem(request) }.mapError()

    suspend fun updatePaymentItem(id: Long, request: PaymentItemUpdate): Result<PaymentItem> =
        runCatching { api.updatePaymentItem(id, request) }.mapError()

    suspend fun deletePaymentItem(id: Long): Result<Unit> = runCatching {
        val response = api.deletePaymentItem(id)
        if (!response.isSuccessful) throw HttpException(response)
    }.mapError()

    // ── Categories ──────────────────────────────────────────────────

    suspend fun getCategories(): Result<List<Category>> =
        runCatching { api.getCategories() }.mapError()

    suspend fun createCategory(request: CategoryCreate): Result<Category> =
        runCatching { api.createCategory(request) }.mapError()

    suspend fun deleteCategory(id: Long): Result<Unit> = runCatching {
        val response = api.deleteCategory(id)
        if (!response.isSuccessful) throw HttpException(response)
    }.mapError()

    // ── Category types ──────────────────────────────────────────────

    suspend fun getCategoryTypes(): Result<List<CategoryType>> =
        runCatching { api.getCategoryTypes() }.mapError()

    suspend fun createCategoryType(request: CategoryTypeCreate): Result<CategoryType> =
        runCatching { api.createCategoryType(request) }.mapError()

    suspend fun deleteCategoryType(id: Long): Result<Unit> = runCatching {
        val response = api.deleteCategoryType(id)
        if (!response.isSuccessful) throw HttpException(response)
    }.mapError()

    // ── Recipients ──────────────────────────────────────────────────

    suspend fun getRecipients(): Result<List<Recipient>> =
        runCatching { api.getRecipients() }.mapError()

    suspend fun createRecipient(request: RecipientCreate): Result<Recipient> =
        runCatching { api.createRecipient(request) }.mapError()

    // ── Statistics ──────────────────────────────────────────────────

    suspend fun getStatistics(): Result<Statistics> =
        runCatching { api.getStatistics() }.mapError()

    // ── Error translation ───────────────────────────────────────────

    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { throwable ->
        throw RuntimeException(throwable.toUserMessage(), throwable)
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> {
            val body = response()?.errorBody()?.string()
            val parsedMessage = body?.let {
                runCatching { errorJson.decodeFromString<ApiError>(it).message }.getOrNull()
            }
            parsedMessage ?: when (code()) {
                401 -> "Not authenticated - please log in again"
                403 -> "You are not allowed to do that"
                404 -> "Not found"
                else -> "Server error (${code()})"
            }
        }
        is java.net.ConnectException,
        is java.net.SocketTimeoutException,
        is java.net.UnknownHostException ->
            "Cannot reach the FinanceBook server - check your connection"
        else -> message ?: "Unexpected error"
    }
}
