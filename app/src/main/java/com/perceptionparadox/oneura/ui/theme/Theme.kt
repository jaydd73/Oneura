package com.perceptionparadox.oneura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EnergeticDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),      // Electric Cyan (Outer Voice Chat)
    secondary = Color(0xFF7C4DFF),    // Deep Neon Violet (Inner Voice Monologue)
    tertiary = Color(0xFFFF4081),     // Vibrant Pink (Dream Mode REM)
    background = Color(0xFF0E0E12),   // Deep Space Background
    surface = Color(0xFF16161F)       // Card and Sheet Containers
)

private val EnergeticLightColorScheme = lightColorScheme(
    primary = Color(0xFF00B8D4),
    secondary = Color(0xFF6200EA),
    tertiary = Color(0xFFC51162),
    background = Color(0xFFF9F9FC),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun OneuraTheme(
    isDarkTheme: Boolean = true, // Defaulting to dark for that immersive AI feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) EnergeticDarkColorScheme else EnergeticLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Uses standard M3 Typography definitions
        content = content
    )
}
