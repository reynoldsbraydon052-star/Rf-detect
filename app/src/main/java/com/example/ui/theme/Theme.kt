package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TacticalDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003D18),
    onPrimaryContainer = Color(0xFF86FFAC),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF003842),
    onSecondaryContainer = Color(0xFF9EFEFF),
    tertiary = Color(0xFFFFCC00),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000), // Pure AMOLED Deep Black (#000000) for zero pixel power draw
    onBackground = Color(0xFFE0F2E9),
    surface = Color(0xFF000000), // Pure AMOLED Deep Black (#000000)
    onSurface = Color(0xFFE0F2E9),
    surfaceVariant = Color(0xFF080D0A), // Minimal pitch dark surface tint
    onSurfaceVariant = Color(0xFFA1C1B1),
    surfaceContainer = Color(0xFF040605), // Deep black container
    surfaceContainerHigh = Color(0xFF080C0A),
    surfaceContainerHighest = Color(0xFF0D1410),
    outline = Color(0xFF1E3A2B),
    outlineVariant = Color(0xFF0F241A),
    error = Color(0xFFFF3366),
    onError = Color(0xFF000000)
)

@Composable
fun TacticalRadarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = Typography,
        content = content
    )
}


