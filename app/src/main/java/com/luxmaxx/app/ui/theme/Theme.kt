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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private val base = Typography()
private fun TextStyle.w(weight: FontWeight, spacing: Float = 0f) =
    copy(fontFamily = FontFamily.SansSerif, fontWeight = weight, letterSpacing = spacing.sp)

private val Type = Typography(
    displaySmall = base.displaySmall.w(FontWeight.Black, -1f),
    headlineLarge = base.headlineLarge.w(FontWeight.Black, -0.8f),
    headlineMedium = base.headlineMedium.w(FontWeight.ExtraBold, -0.6f),
    headlineSmall = base.headlineSmall.w(FontWeight.ExtraBold, -0.4f),
    titleLarge = base.titleLarge.w(FontWeight.Bold, -0.2f),
    titleMedium = base.titleMedium.w(FontWeight.Bold),
    titleSmall = base.titleSmall.w(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.w(FontWeight.Normal),
    bodyMedium = base.bodyMedium.w(FontWeight.Normal).copy(lineHeight = 21.sp),
    bodySmall = base.bodySmall.w(FontWeight.Normal).copy(lineHeight = 18.sp),
    labelLarge = base.labelLarge.w(FontWeight.SemiBold, 0.2f),
    labelMedium = base.labelMedium.w(FontWeight.SemiBold, 0.4f),
    labelSmall = base.labelSmall.w(FontWeight.Bold, 0.8f),
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
