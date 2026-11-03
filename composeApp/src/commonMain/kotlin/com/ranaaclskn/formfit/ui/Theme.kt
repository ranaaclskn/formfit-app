package com.ranaaclskn.formfit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FfColors {
    val Background = Color(0xFF0B0F14)
    val Surface = Color(0xFF151B23)
    val SurfaceHigh = Color(0xFF1E2630)
    val Accent = Color(0xFF22D3EE)
    val Good = Color(0xFF22C55E)
    val Bad = Color(0xFFEF4444)
    val Warn = Color(0xFFF59E0B)
    val TextMuted = Color(0xFF94A3B8)
}

private val colors = darkColorScheme(
    primary = FfColors.Accent,
    onPrimary = Color(0xFF00222A),
    secondary = FfColors.Good,
    background = FfColors.Background,
    onBackground = Color.White,
    surface = FfColors.Surface,
    onSurface = Color.White,
    surfaceVariant = FfColors.SurfaceHigh,
    onSurfaceVariant = FfColors.TextMuted,
    surfaceContainer = FfColors.Surface,
    error = FfColors.Bad,
)

@Composable
fun FormFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
