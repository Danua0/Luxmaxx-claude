package com.luxmaxx.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFFFB300)
val Green = Color(0xFF4CAF50)
val Red = Color(0xFFE57373)
val Blue = Color(0xFF64B5F6)

private val Scheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1200),
    secondary = Blue,
    background = Color(0xFF0F1115),
    surface = Color(0xFF0F1115),
    surfaceVariant = Color(0xFF1C2028),
    surfaceContainer = Color(0xFF161A20),
    surfaceContainerHigh = Color(0xFF1C2028),
    onSurface = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFFA9B1BD),
    error = Red,
)

@Composable
fun LuxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
