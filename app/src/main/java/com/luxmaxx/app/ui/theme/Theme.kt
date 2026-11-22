package com.luxmaxx.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luxmaxx.app.R

// Палитра: глубокий графит + золото, холодные акценты для данных
val Bg = Color(0xFF0A0B0F)
val Surface1 = Color(0xFF14161D)
val Surface2 = Color(0xFF1B1E27)
val Stroke = Color(0xFF2A2E3A)
val TextHi = Color(0xFFF3F4F7)
val TextLo = Color(0xFF9097A6)

val Gold = Color(0xFFFFC14D)
val GoldDeep = Color(0xFFFF8A3D)
val Amber = Gold
val Green = Color(0xFF3DDC97)
val Blue = Color(0xFF5AB2FF)
val Violet = Color(0xFFA48BFF)
val Red = Color(0xFFFF6B6B)

val GoldGradient = Brush.linearGradient(listOf(Gold, GoldDeep))
val CardGradient = Brush.verticalGradient(listOf(Surface2, Surface1))
val BorderGradient = Brush.linearGradient(listOf(Color(0x55FFC14D), Color(0x14FFFFFF), Color(0x115AB2FF)))

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1C1200),
    primaryContainer = Color(0xFF3A2A08),
    onPrimaryContainer = Gold,
    secondary = Blue,
    secondaryContainer = Color(0xFF2A2410),
    onSecondaryContainer = Gold,
    tertiary = Green,
    background = Bg,
    onBackground = TextHi,
    surface = Bg,
    onSurface = TextHi,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextLo,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Color(0xFF232733),
    outline = Stroke,
    outlineVariant = Stroke,
    error = Red,
)

private fun unb(w: Int) = Font(R.font.unbounded, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
private fun man(w: Int) = Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Unbounded — широкий «спортивный» шрифт для заголовков и цифр. */
val Display = FontFamily(unb(400), unb(500), unb(600), unb(700), unb(800), unb(900))
/** Manrope — чистый современный шрифт для текста. */
val Body = FontFamily(man(400), man(500), man(600), man(700), man(800))

private fun d(size: Int, w: FontWeight, spacing: Float = 0f, line: Int = (size * 1.2).toInt()) =
    TextStyle(fontFamily = Display, fontWeight = w, fontSize = size.sp, letterSpacing = spacing.sp, lineHeight = line.sp)
private fun b(size: Int, w: FontWeight, spacing: Float = 0f, line: Int = (size * 1.45).toInt()) =
    TextStyle(fontFamily = Body, fontWeight = w, fontSize = size.sp, letterSpacing = spacing.sp, lineHeight = line.sp)

private val Type = Typography(
    displayLarge = d(52, FontWeight.Black, -1.5f),
    displayMedium = d(42, FontWeight.Black, -1.2f),
    displaySmall = d(34, FontWeight.Black, -1f),
    headlineLarge = d(28, FontWeight.ExtraBold, -0.8f),
    headlineMedium = d(24, FontWeight.ExtraBold, -0.6f),
    headlineSmall = d(20, FontWeight.Bold, -0.4f),
    titleLarge = d(17, FontWeight.Bold, -0.2f),
    titleMedium = b(16, FontWeight.ExtraBold, -0.1f, 22),
    titleSmall = b(14, FontWeight.Bold, 0f, 20),
    bodyLarge = b(16, FontWeight.Medium),
    bodyMedium = b(14, FontWeight.Medium, 0f, 21),
    bodySmall = b(12, FontWeight.Medium, 0.1f, 18),
    labelLarge = b(14, FontWeight.Bold, 0.1f, 18),
    labelMedium = b(12, FontWeight.Bold, 0.3f, 16),
    labelSmall = b(10, FontWeight.ExtraBold, 1.2f, 14),
)

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LuxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = Shape, content = content)
}
