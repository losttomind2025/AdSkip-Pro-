package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberShieldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E7490),
    onSecondaryContainer = Color.White,
    tertiary = AmberWarning,
    background = ShieldDarkBg,
    onBackground = TextPrimary,
    surface = ShieldSurface,
    onSurface = TextPrimary,
    surfaceVariant = ShieldSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ShieldBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our sleek custom cyber theme consistently
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CyberShieldColorScheme,
        typography = Typography,
        content = content
    )
}
