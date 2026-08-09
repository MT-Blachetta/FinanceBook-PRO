package com.financebook.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.financebook.android.ui.theme.Negative
import com.financebook.android.ui.theme.Positive
import java.util.Locale

/** Full-size centered loading spinner. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Inline error banner. */
@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
    )
}

/** Formats an amount like the web app: "-42.50 €" in red / green. */
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    forceNegativeColor: Boolean = false,
) {
    val negative = forceNegativeColor || amount < 0
    Text(
        text = formatAmount(amount),
        color = if (negative) Negative else Positive,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

fun formatAmount(amount: Double): String =
    String.format(Locale.GERMANY, "%,.2f €", amount)
