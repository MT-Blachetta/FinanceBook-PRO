package com.financebook.android.data.api

import com.financebook.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit definition of the FinanceBook REST API
 * (same endpoints the React web frontend uses).
 */
interface FinanceBookApi {

    // ── Auth ─────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): JwtResponse

    @GET("auth/me")
    suspend fun me(): UserRead

    // ── Payment items ───────────────────────────────────────────────
    @GET("payment-items")
    suspend fun getPaymentItems(
        @Query("expenseOnly") expenseOnly: Boolean? = null,
        @Query("incomeOnly") incomeOnly: Boolean? = null,
        @Query("categoryIds") categoryIds: List<Long>? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
    ): List<PaymentItem>

    @GET("payment-items/{id}")
    suspend fun getPaymentItem(@Path("id") id: Long): PaymentItem

    @POST("payment-items")
    suspend fun createPaymentItem(@Body request: PaymentItemCreate): PaymentItem

    @PUT("payment-items/{id}")
    suspend fun updatePaymentItem(@Path("id") id: Long, @Body request: PaymentItemUpdate): PaymentItem

    @DELETE("payment-items/{id}")
    suspend fun deletePaymentItem(@Path("id") id: Long): Response<Unit>

    // ── Categories ──────────────────────────────────────────────────
    @GET("categories")
    suspend fun getCategories(): List<Category>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryCreate): Category

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // ── Category types ──────────────────────────────────────────────
    @GET("category-types")
    suspend fun getCategoryTypes(): List<CategoryType>

    @POST("category-types")
    suspend fun createCategoryType(@Body request: CategoryTypeCreate): CategoryType

    @DELETE("category-types/{id}")
    suspend fun deleteCategoryType(@Path("id") id: Long): Response<Unit>

    // ── Recipients ──────────────────────────────────────────────────
    @GET("recipients")
    suspend fun getRecipients(): List<Recipient>

    @POST("recipients")
    suspend fun createRecipient(@Body request: RecipientCreate): Recipient

    @DELETE("recipients/{id}")
    suspend fun deleteRecipient(@Path("id") id: Long): Response<Unit>

    // ── Statistics ──────────────────────────────────────────────────
    @GET("statistics")
    suspend fun getStatistics(
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
    ): Statistics
}
