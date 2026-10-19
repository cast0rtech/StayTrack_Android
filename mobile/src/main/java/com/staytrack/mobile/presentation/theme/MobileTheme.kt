package com.staytrack.mobile.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF080B11)
val DarkSurface = Color(0xFF0E1422)
val DarkSurfaceElevated = Color(0xFF141C2E)
val DarkSurfaceBorder = Color(0x3300F0FF)

val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFFA855F7)
val CyberBlue = Color(0xFF3B82F6)
val EmeraldGreen = Color(0xFF10B981)
val CrimsonRed = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val CardGlowBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x6600F0FF),
        Color(0x223B82F6),
        Color(0x66A855F7)
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C2436),
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF26123E),
    onSecondaryContainer = NeonPurple,
    tertiary = CyberBlue,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder
)

@Composable
fun StayTrackMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
