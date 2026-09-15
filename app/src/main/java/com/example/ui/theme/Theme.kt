package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BroadcastCyan,
    onPrimary = DarkObsidian,
    primaryContainer = GunmetalSurfaceElevated,
    onPrimaryContainer = BroadcastCyan,
    secondary = StudioAmber,
    onSecondary = DarkObsidian,
    tertiary = LiveTallyRed,
    background = DarkObsidian,
    onBackground = TextPrimary,
    surface = GunmetalSurface,
    onSurface = TextPrimary,
    surfaceVariant = GunmetalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GunmetalBorder,
    error = LiveTallyRed,
    onError = Color.White
)

@Composable
fun LocalCamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LocalCamTheme(content = content)
}

