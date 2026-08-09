package com.financebook.android.data.api

import com.financebook.android.BuildConfig
import com.financebook.android.data.auth.TokenStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds the Retrofit client:
 * • attaches the JWT to every request,
 * • clears the stored token on 401 so the UI drops back to the login screen,
 * • tolerant JSON parsing (unknown fields ignored, nulls omitted on write).
 */
object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun create(tokenStore: TokenStore): FinanceBookApi {
        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.currentToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }

            val response: Response = chain.proceed(request)
            if (response.code == 401 && token != null) {
                // Session expired: force logout so the UI returns to login
                tokenStore.clearTokenBlocking()
            }
            response
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FinanceBookApi::class.java)
    }
}
