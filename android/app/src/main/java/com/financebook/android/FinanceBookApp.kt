package com.financebook.android

import android.app.Application
import com.financebook.android.data.api.ApiClient
import com.financebook.android.data.auth.TokenStore
import com.financebook.android.data.repository.FinanceRepository
import kotlinx.coroutines.runBlocking

/**
 * Application class with a small hand-rolled dependency container -
 * the app is deliberately framework-light (no Hilt) to stay easy to read.
 */
class FinanceBookApp : Application() {

    lateinit var tokenStore: TokenStore
        private set
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
        // Load the persisted token before any UI is shown so the start
        // destination (login vs. summary) is decided correctly.
        runBlocking { tokenStore.initialize() }
        repository = FinanceRepository(ApiClient.create(tokenStore), tokenStore)
    }
}
