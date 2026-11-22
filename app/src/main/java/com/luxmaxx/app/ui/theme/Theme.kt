package com.luxmaxx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.luxmaxx.app.R

// =====================================================================
//  Шрифты
// =====================================================================

private fun unb(w: Int) = Font(R.font.unbounded, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
private fun man(w: Int) = Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
private fun exo(w: Int) = Font(R.font.exo2, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Unbounded — широкий «спортивный» шрифт для заголовков и цифр. */
val UnboundedFamily = FontFamily(unb(400), unb(500), unb(600), unb(700), unb(800), unb(900))
/** Manrope — чистый современный шрифт для текста. */
val ManropeFamily = FontFamily(man(400), man(500), man(600), man(700), man(800))
/** Pacifico — неоновый рукописный шрифт вывесок 80-х. */
val PacificoFamily = FontFamily(Font(R.font.pacifico, FontWeight.Normal))
/** Exo 2 — футуристичный шрифт для текста в неоновых темах. */
val ExoFamily = FontFamily(exo(400), exo(500), exo(600), exo(700), exo(800), exo(900))
/** Russo One — жирные «аркадные» цифры. */
val RussoFamily = FontFamily(Font(R.font.russo_one, FontWeight.Normal))

// =====================================================================
//  Палитры
// =====================================================================

enum class AppTheme(val title: String, val subtitle: String, val emoji: String) {
    GOLD("Золото", "Тёмная классика", "🏆"),
    LIGHT("Светлая", "Чистый день", "☀️"),
    MIAMI("Неон Майами", "Закат 80-х", "🌴"),
    MIAMI_NIGHT("Майами ночь", "Неон в темноте", "🌃"),
}

data class Palette(
    val theme: AppTheme,
    val light: Boolean,
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val stroke: Color,
    val textHi: Color,
    val textLo: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val onAccent: Color,
    val green: Color,
    val blue: Color,
    val violet: Color,
    val red: Color,
    val blobs: List<Color>,
    val retro: Boolean = false,
    val display: FontFamily = UnboundedFamily,   // крупные заголовки
    val numbers: FontFamily = UnboundedFamily,   // цифры и подзаголовки
    val body: FontFamily = ManropeFamily,        // текст
    val glow: Boolean = false,                   // неоновое свечение заголовков
)

object Palettes {
    val gold = Palette(
        AppTheme.GOLD, light = false,
        bg = Color(0xFF0A0B0F), surface1 = Color(0xFF14161D), surface2 = Color(0xFF1B1E27), stroke = Color(0xFF2A2E3A),
        textHi = Color(0xFFF3F4F7), textLo = Color(0xFF9097A6),
        accent = Color(0xFFFFC14D), accent2 = Color(0xFFFF8A3D), accent3 = Color(0xFFE0553A), onAccent = Color(0xFF1C1200),
        green = Color(0xFF3DDC97), blue = Color(0xFF5AB2FF), violet = Color(0xFFA48BFF), red = Color(0xFFFF6B6B),
        blobs = listOf(Color(0x40FF9A3D), Color(0x229A7BFF), Color(0x1A5AB2FF), Color(0x1FFFC14D)),
    )
    val light = Palette(
        AppTheme.LIGHT, light = true,
        bg = Color(0xFFF5F2EC), surface1 = Color(0xFFFFFFFF), surface2 = Color(0xFFFFFCF7), stroke = Color(0xFFE6DFD3),
        textHi = Color(0xFF17181C), textLo = Color(0xFF6A6F7B),
        accent = Color(0xFFF0A21A), accent2 = Color(0xFFF26A2E), accent3 = Color(0xFFE0453A), onAccent = Color(0xFF241500),
        green = Color(0xFF14A86C), blue = Color(0xFF2F7FF5), violet = Color(0xFF7A57F2), red = Color(0xFFE5484D),
        blobs = listOf(Color(0x33FFB547), Color(0x22A78BFF), Color(0x1F5AB2FF), Color(0x26FFC14D)),
    )
    val miami = Palette(
        AppTheme.MIAMI, light = false,
        bg = Color(0xFF1E0B33), surface1 = Color(0xFF2A1143), surface2 = Color(0xFF361853), stroke = Color(0xFF55307A),
        textHi = Color(0xFFFFF1FA), textLo = Color(0xFFD0A9DF),
        accent = Color(0xFFFF4FA3), accent2 = Color(0xFFFF9A45), accent3 = Color(0xFFFFD166), onAccent = Color(0xFF2A0626),
        green = Color(0xFF2CF6D2), blue = Color(0xFF3ED9FF), violet = Color(0xFFB98CFF), red = Color(0xFFFF5C7A),
        blobs = listOf(Color(0x55FF4FA3), Color(0x40FF9A45), Color(0x3326D9FF), Color(0x337B2CBF)),
        retro = true, display = PacificoFamily, numbers = RussoFamily, body = ExoFamily, glow = true,
    )
    val miamiNight = Palette(
        AppTheme.MIAMI_NIGHT, light = false,
        bg = Color(0xFF07040F), surface1 = Color(0xFF110A1F), surface2 = Color(0xFF1A1030), stroke = Color(0xFF3A2358),
        textHi = Color(0xFFF7EEFF), textLo = Color(0xFF9F8BBB),
        accent = Color(0xFFFF2E97), accent2 = Color(0xFFB026FF), accent3 = Color(0xFF00E5FF), onAccent = Color(0xFFFFFFFF),
        green = Color(0xFF00F5C4), blue = Color(0xFF00D1FF), violet = Color(0xFFC77DFF), red = Color(0xFFFF3B6B),
        blobs = listOf(Color(0x40FF2E97), Color(0x33B026FF), Color(0x2600E5FF), Color(0x22FF2E97)),
        retro = true, display = PacificoFamily, numbers = RussoFamily, body = ExoFamily, glow = true,
    )

    fun of(t: AppTheme) = when (t) {
        AppTheme.GOLD -> gold
        AppTheme.LIGHT -> light
        AppTheme.MIAMI -> miami
        AppTheme.MIAMI_NIGHT -> miamiNight
    }
}

/** Текущая палитра. Это snapshot-состояние: смена перерисовывает весь интерфейс. */
object ThemeState {
    var palette by mutableStateOf(Palettes.gold)
    fun apply(t: AppTheme) { if (palette.theme != t) palette = Palettes.of(t) }
}

private val P: Palette get() = ThemeState.palette

// =====================================================================
//  Токены (читаются из текущей палитры)
// =====================================================================

val Bg: Color get() = P.bg
val Surface1: Color get() = P.surface1
val Surface2: Color get() = P.surface2
val Stroke: Color get() = P.stroke
val TextHi: Color get() = P.textHi
val TextLo: Color get() = P.textLo

/** Главный акцент (в «Золоте» — золотой, в «Майами» — неоновый розовый). */
val Gold: Color get() = P.accent
val GoldDeep: Color get() = P.accent2
val Accent3: Color get() = P.accent3
val OnAccent: Color get() = P.onAccent
val Amber: Color get() = P.accent
val Green: Color get() = P.green
val Blue: Color get() = P.blue
val Violet: Color get() = P.violet
val Red: Color get() = P.red

/** Полупрозрачная дорожка (фон полос прогресса, сетки). */
val Track: Color get() = P.textHi.copy(alpha = if (P.light) 0.08f else 0.10f)
val IsLight: Boolean get() = P.light
val IsRetro: Boolean get() = P.retro
val Display: FontFamily get() = P.display
val Numbers: FontFamily get() = P.numbers

val GoldGradient: Brush get() = Brush.linearGradient(listOf(Gold, GoldDeep))
val HeroGradient: Brush get() = Brush.linearGradient(listOf(Gold, GoldDeep, Accent3))
val CardGradient: Brush get() = Brush.verticalGradient(listOf(Surface2, Surface1))
val BorderGradient: Brush get() = Brush.linearGradient(
    listOf(Gold.copy(alpha = if (P.light) 0.45f else 0.33f), TextHi.copy(alpha = 0.08f), Blue.copy(alpha = 0.07f))
)

/** Мягкий фон баннера/карточки уровня, подкрашенный акцентом. */
fun tinted(accent: Color, amount: Float = 0.22f): List<Color> =
    listOf(lerp(Surface2, accent, amount), lerp(Surface1, accent, amount * 0.25f), Surface1)

// =====================================================================
//  Типографика
// =====================================================================

private fun typography(p: Palette): Typography {
    val glow = if (p.glow) Shadow(p.accent.copy(alpha = 0.85f), blurRadius = 22f) else null
    fun disp(size: Int, w: FontWeight, spacing: Float, line: Int = (size * 1.25).toInt()) =
        if (p.display == PacificoFamily)
            TextStyle(fontFamily = p.display, fontWeight = FontWeight.Normal, fontSize = (size + 2).sp, lineHeight = (line + 10).sp, shadow = glow)
        else TextStyle(fontFamily = p.display, fontWeight = w, fontSize = size.sp, letterSpacing = spacing.sp, lineHeight = line.sp)
    fun num(size: Int, w: FontWeight, spacing: Float, line: Int = (size * 1.2).toInt()) =
        TextStyle(fontFamily = p.numbers, fontWeight = if (p.numbers == RussoFamily) FontWeight.Normal else w,
            fontSize = size.sp, letterSpacing = (if (p.numbers == RussoFamily) 0.3f else spacing).sp, lineHeight = line.sp,
            shadow = if (p.glow) Shadow(p.accent.copy(alpha = 0.5f), blurRadius = 12f) else null)
    fun b(size: Int, w: FontWeight, spacing: Float = 0f, line: Int = (size * 1.45).toInt()) =
        TextStyle(fontFamily = p.body, fontWeight = w, fontSize = size.sp, letterSpacing = spacing.sp, lineHeight = line.sp)
    return Typography(
        displayLarge = num(52, FontWeight.Black, -1.5f),
        displayMedium = num(42, FontWeight.Black, -1.2f),
        displaySmall = num(34, FontWeight.Black, -1f),
        headlineLarge = disp(28, FontWeight.ExtraBold, -0.8f),
        headlineMedium = disp(24, FontWeight.ExtraBold, -0.6f),
        headlineSmall = num(20, FontWeight.Bold, -0.4f),
        titleLarge = num(17, FontWeight.Bold, -0.2f),
        titleMedium = b(16, FontWeight.ExtraBold, -0.1f, 22),
        titleSmall = b(14, FontWeight.Bold, 0f, 20),
        bodyLarge = b(16, FontWeight.Medium),
        bodyMedium = b(14, FontWeight.Medium, 0f, 21),
        bodySmall = b(12, FontWeight.Medium, 0.1f, 18),
        labelLarge = b(14, FontWeight.Bold, 0.1f, 18),
        labelMedium = b(12, FontWeight.Bold, 0.3f, 16),
        labelSmall = b(10, FontWeight.ExtraBold, 1.2f, 14),
    )
}

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LuxTheme(content: @Composable () -> Unit) {
    val p = ThemeState.palette
    val type = remember(p) { typography(p) }
    val scheme = remember(p) {
        if (p.light) lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.blue, tertiary = p.green,
            background = p.bg, onBackground = p.textHi, surface = p.bg, onSurface = p.textHi,
            surfaceVariant = p.surface2, onSurfaceVariant = p.textLo,
            surfaceContainerLowest = p.bg, surfaceContainerLow = p.surface1, surfaceContainer = p.surface1,
            surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
            outline = p.stroke, outlineVariant = p.stroke, error = p.red,
        ) else darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.blue, tertiary = p.green,
            secondaryContainer = lerp(p.surface2, p.accent, 0.25f), onSecondaryContainer = p.textHi,
            background = p.bg, onBackground = p.textHi, surface = p.bg, onSurface = p.textHi,
            surfaceVariant = p.surface2, onSurfaceVariant = p.textLo,
            surfaceContainerLowest = p.bg, surfaceContainerLow = p.surface1, surfaceContainer = p.surface1,
            surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
            outline = p.stroke, outlineVariant = p.stroke, error = p.red,
        )
    }
    // Тёмные значки статус-бара в светлой теме
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = p.light
            isAppearanceLightNavigationBars = p.light
        }
    }
    MaterialTheme(colorScheme = scheme, typography = type, shapes = Shape) {
        // Цвет текста по умолчанию берётся из темы, иначе Text без цвета рисуется чёрным.
        CompositionLocalProvider(
            LocalContentColor provides p.textHi,
            LocalTextStyle provides type.bodyMedium,
            content = content,
        )
    }
}
