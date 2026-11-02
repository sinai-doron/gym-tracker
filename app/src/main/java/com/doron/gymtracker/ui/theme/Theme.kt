package com.doron.gymtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = TextOnPurple,
    primaryContainer = PurpleLight,
    onPrimaryContainer = PurpleDark,
    background = LavenderBackground,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    surfaceVariant = LightSurface,
    outline = Color(0xFFD0C8F0)
)

/**
 * The layout direction follows the app's language (right to left in Hebrew, left to right in
 * English): Android sets it from the locale, and the manifest declares supportsRtl.
 */
@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
