package com.luxmaxx.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luxmaxx.app.ui.theme.Bg
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.CardGradient
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.GoldGradient
import com.luxmaxx.app.ui.theme.Stroke as StrokeColor
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(24.dp)

/** Фон всего приложения: тёмный с тёплым свечением сверху. */
@Composable
fun ScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .background(
                Brush.radialGradient(
                    listOf(Color(0x33FF9F3D), Color(0x0FFFC14D), Color.Transparent),
                    center = Offset(900f, -150f), radius = 1300f,
                )
            ),
        content = content,
    )
}

/** Заголовок экрана: маленький надзаголовок + крупный жирный заголовок. */
@Composable
fun ScreenHeader(
    title: String,
    overline: String? = null,
    subtitle: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (overline != null) Text(overline.uppercase(), style = MaterialTheme.typography.labelSmall, color = Gold)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextHi)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextLo,
                modifier = Modifier.padding(top = 2.dp))
        }
        if (action != null) Row(verticalAlignment = Alignment.CenterVertically, content = action)
    }
}

/** Основная карточка: мягкий градиент + тонкая светящаяся рамка. */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = Gold,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardGradient)
            .border(1.dp, BorderGradient, CardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                IconBadge(icon, accent, 30.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextHi)
        }
        content()
    }
}

/** Акцентная карточка с золотым градиентом (для главного действия). */
@Composable
fun HeroCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(Color(0xFFFFC14D), Color(0xFFFF8A3D), Color(0xFFE0553A))))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun IconBadge(icon: ImageVector, color: Color = Gold, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.55f)) }
}

/** Кнопка с золотым градиентом. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (enabled) GoldGradient else Brush.linearGradient(listOf(StrokeColor, StrokeColor)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (enabled) Color(0xFF1C1200) else TextLo
        if (icon != null) { Icon(icon, null, tint = c, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = c, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

/** Плитка со статистикой. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, unit: String = "", accent: Color = Gold, hint: String? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardGradient)
            .border(1.dp, BorderGradient, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextLo, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
            if (unit.isNotEmpty()) Text(" $unit", style = MaterialTheme.typography.bodySmall, color = TextLo,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        if (hint != null) Text(hint, style = MaterialTheme.typography.labelMedium, color = TextLo, maxLines = 1)
    }
}

/** Кольцо прогресса с градиентом и анимацией. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    thickness: Dp = 12.dp,
    colors: List<Color> = listOf(Gold, GoldDeep),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val st = thickness.toPx()
            val arcSize = Size(this.size.width - st, this.size.height - st)
            val tl = Offset(st / 2, st / 2)
            drawArc(Color(0x1FFFFFFF), -90f, 360f, false, tl, arcSize, style = Stroke(st, cap = StrokeCap.Round))
            if (anim > 0f) drawArc(
                Brush.sweepGradient(colors + colors.first()), -90f, 360f * anim, false, tl, arcSize,
                style = Stroke(st, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** Ряд-показатель макронутриента с градиентной полосой. */
@Composable
fun MacroBar(label: String, value: Int, target: Int, unit: String, color: Color = Gold) {
    val frac = if (target > 0) value.toFloat() / target else 0f
    val anim by animateFloatAsState(frac.coerceIn(0f, 1f), tween(700), label = "bar")
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = TextHi, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.titleSmall, color = color)
            Text(" / $target $unit", style = MaterialTheme.typography.bodySmall, color = TextLo)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0x1AFFFFFF))) {
            Box(
                Modifier.fillMaxWidth(anim).height(10.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)))
            )
        }
    }
}

/** Маленькая «таблетка»-метка. */
@Composable
fun Pill(text: String, color: Color = Gold, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color, style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
fun luxFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold,
    unfocusedBorderColor = StrokeColor,
    focusedLabelColor = Gold,
    cursorColor = Gold,
    focusedContainerColor = Surface1,
    unfocusedContainerColor = Surface1,
)

@Composable
fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.replace(',', '.').filter { it.isDigit() || (decimal && it == '.') }) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = luxFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit, suffix: String = "") {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextHi, modifier = Modifier.weight(1f))
            val rounded = (value * 2).roundToInt() / 2f
            val shown = if (rounded % 1f == 0f) rounded.roundToInt().toString() else "%.1f".format(rounded)
            Pill("$shown$suffix")
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Color(0x22FFFFFF),
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** Плавный график с градиентной заливкой. */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier, color: Color = Gold, height: Dp = 130.dp) {
    if (values.size < 2) {
        Text("Нужно минимум 2 записи для графика", color = TextLo, style = MaterialTheme.typography.bodySmall)
        return
    }
    val min = values.min(); val max = values.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text("max %.1f".format(max), style = MaterialTheme.typography.labelSmall, color = TextLo, modifier = Modifier.weight(1f))
            Text("min %.1f".format(min), style = MaterialTheme.typography.labelSmall, color = TextLo, textAlign = TextAlign.End)
        }
        Canvas(Modifier.fillMaxWidth().height(height).padding(vertical = 8.dp)) {
            val stepX = size.width / (values.size - 1)
            fun pt(i: Int) = Offset(i * stepX, (size.height * (1 - (values[i] - min) / span)).toFloat())
            // сетка
            for (g in 0..3) {
                val y = size.height * g / 3
                drawLine(Color(0x12FFFFFF), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            val line = Path().apply {
                moveTo(pt(0).x, pt(0).y)
                for (i in 1 until values.size) {
                    val p0 = pt(i - 1); val p1 = pt(i)
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            val last = pt(values.lastIndex)
            drawCircle(color.copy(alpha = 0.25f), 9.dp.toPx(), last)
            drawCircle(color, 4.5.dp.toPx(), last)
        }
    }
}

/** Крупное число с подписью — для центра колец. */
@Composable
fun RingLabel(value: String, label: String, color: Color = TextHi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextLo)
    }
}

fun Double.fmt(): String = if (this % 1.0 == 0.0) this.toInt().toString() else "%.1f".format(this)
