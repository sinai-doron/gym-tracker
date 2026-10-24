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

@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    // Layout direction is handled by the system locale.
    // Hebrew locale ("iw") → Android sets RTL automatically.
    // English locale ("en") → LTR.
    // Do NOT force LayoutDirection here — it would override the locale switch.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
