package com.financebook.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.financebook.android.ui.navigation.FinanceBookNavHost
import com.financebook.android.ui.theme.FinanceBookTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as FinanceBookApp

        setContent {
            FinanceBookTheme {
                FinanceBookNavHost(
                    repository = app.repository,
                    tokenStore = app.tokenStore,
                )
            }
        }
    }
}
