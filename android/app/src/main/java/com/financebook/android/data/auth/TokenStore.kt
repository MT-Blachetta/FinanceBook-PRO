package com.financebook.android.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "financebook_auth")

/**
 * Persists the JWT access token and exposes the current auth state.
 *
 * The token is additionally cached in memory so the OkHttp interceptor can
 * read it synchronously on the network thread.
 */
class TokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("access_token")

    private val _cachedToken = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> get() = _cachedToken

    val persistedToken: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    /** Load the persisted token into the in-memory cache (call at startup). */
    suspend fun initialize() {
        _cachedToken.value = persistedToken.first()
    }

    /** Synchronous read for the network interceptor. */
    fun currentToken(): String? = _cachedToken.value

    suspend fun saveToken(token: String) {
        _cachedToken.value = token
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        _cachedToken.value = null
        context.dataStore.edit { it.remove(tokenKey) }
    }

    /**
     * Clear from a non-suspending context (401 handling in the interceptor).
     * DataStore writes are fast; blocking here is acceptable for the rare
     * forced-logout path.
     */
    fun clearTokenBlocking() {
        runBlocking { clearToken() }
    }
}
