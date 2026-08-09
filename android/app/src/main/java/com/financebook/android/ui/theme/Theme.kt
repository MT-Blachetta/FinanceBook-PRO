package com.financebook.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark finance theme matching the web frontend
 * (black background, green income, red expense accents).
 */

val Positive = Color(0xFF2ECC71)
val Negative = Color(0xFFE74C3C)
val Surface = Color(0xFF1C1C1C)
val SurfaceVariant = Color(0xFF2A2A2A)
val CardBackground = Color(0xFF333333)
val TextPrimary = Color(0xFFEAEAEA)
val TextSecondary = Color(0xFF9E9E9E)
val AccentBlue = Color(0xFF2563EB)

private val DarkColors = darkColorScheme(
    primary = Positive,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Negative,
)

@Composable
fun FinanceBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The product design is dark-only, mirroring the web app.
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
