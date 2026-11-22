package com.luxmaxx.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luxmaxx.app.ui.theme.Bg
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Display
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.Violet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

// =====================================================================
//  Анимированный фон: медленно плавающие светящиеся «туманности»
// =====================================================================

@Composable
fun AnimatedBackdrop(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "a")
    val pulse by t.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Canvas(modifier.fillMaxSize()) {
        drawRect(Bg)
        val w = size.width; val h = size.height
        val ang = a * 2f * PI.toFloat()
        fun blob(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(
            Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center = Offset(cx, cy), radius = r),
            radius = r, center = Offset(cx, cy),
        )
        blob(w * (0.85f + 0.08f * cos(ang)), h * (0.02f + 0.04f * sin(ang)), w * 0.95f * pulse, Color(0x40FF9A3D))
        blob(w * (0.05f + 0.10f * sin(ang * 0.7f)), h * (0.30f + 0.05f * cos(ang)), w * 0.75f, Color(0x229A7BFF))
        blob(w * (0.90f + 0.06f * sin(ang * 1.3f)), h * (0.70f + 0.06f * cos(ang * 0.8f)), w * 0.7f / pulse, Color(0x1A5AB2FF))
        blob(w * (0.25f + 0.08f * cos(ang * 1.1f)), h * (1.02f), w * 0.8f, Color(0x1FFFC14D))
        // тонкая сетка-«грид» для глубины
        val step = 44.dp.toPx()
        var y = 0f
        while (y < h * 0.45f) {
            drawLine(Color.White.copy(alpha = 0.018f * (1 - y / (h * 0.45f))), Offset(0f, y), Offset(w, y), 1f)
            y += step
        }
        var x = 0f
        while (x < w) {
            drawLine(Color.White.copy(alpha = 0.014f), Offset(x, 0f), Offset(x, h * 0.45f), 1f)
            x += step
        }
    }
}

// =====================================================================
//  Логотип: золотое кольцо + гантель
// =====================================================================

@Composable
fun LogoMark(size: Dp = 64.dp, glow: Boolean = true, spin: Boolean = false) {
    val t = rememberInfiniteTransition(label = "logo")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot")
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val c = center
        if (glow) drawCircle(
            Brush.radialGradient(listOf(Gold.copy(alpha = 0.45f), Color.Transparent), center = c, radius = s * 0.75f),
            radius = s * 0.75f, center = c,
        )
        // кольцо
        rotate(if (spin) rot else -30f, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Gold, GoldDeep, Color(0xFFE0553A), Gold), center = c),
                radius = s * 0.44f, center = c, style = Stroke(s * 0.07f),
            )
        }
        drawCircle(Color(0xFF12141A), radius = s * 0.38f, center = c)
        // гантель
        val gold = Brush.linearGradient(listOf(Gold, GoldDeep), start = Offset(0f, c.y - s * 0.2f), end = Offset(0f, c.y + s * 0.2f))
        val bw = s * 0.07f
        fun plate(x: Float, hh: Float) = drawRoundRect(
            gold, topLeft = Offset(x - bw / 2, c.y - hh / 2), size = Size(bw, hh), cornerRadius = CornerRadius(bw / 2),
        )
        plate(c.x - s * 0.21f, s * 0.20f); plate(c.x - s * 0.13f, s * 0.32f)
        plate(c.x + s * 0.13f, s * 0.32f); plate(c.x + s * 0.21f, s * 0.20f)
        drawRoundRect(Color.White, topLeft = Offset(c.x - s * 0.10f, c.y - s * 0.025f), size = Size(s * 0.20f, s * 0.05f),
            cornerRadius = CornerRadius(s * 0.025f))
    }
}

@Composable
fun Wordmark(size: Int = 22) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = TextHi)) { append("LUX") }
            withStyle(SpanStyle(brush = Brush.linearGradient(listOf(Gold, GoldDeep)))) { append("MAXX") }
        },
        fontFamily = Display, fontWeight = FontWeight.Black, fontSize = size.sp, letterSpacing = (-0.5).sp,
    )
}

// =====================================================================
//  Сплэш-экран
// =====================================================================

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(350))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f))
    }
    LaunchedEffect(Unit) { delay(450); tag.animateTo(1f, tween(600)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedBackdrop()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }) {
                LogoMark(132.dp, spin = true)
            }
            Spacer(Modifier.height(28.dp))
            Box(Modifier.graphicsLayer { this.alpha = tag.value; translationY = (1 - tag.value) * 30f }) { Wordmark(34) }
            Spacer(Modifier.height(10.dp))
            Text(
                "СТАНЬ ЛУЧШЕЙ ВЕРСИЕЙ СЕБЯ", style = MaterialTheme.typography.labelSmall, color = TextLo,
                modifier = Modifier.graphicsLayer { this.alpha = tag.value },
            )
        }
    }
}

// =====================================================================
//  Модификаторы: нажатие, шиммер, появление
// =====================================================================

/** Пружинное «вдавливание» при нажатии. Передай тот же interactionSource в clickable. */
fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
    this.graphicsLayer { scaleX = s; scaleY = s }
}

/** Бегущий блик поверх содержимого. */
fun Modifier.shimmer(alpha: Float = 0.35f, durationMs: Int = 2600): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(durationMs, easing = LinearEasing)), label = "x")
    this.drawWithContent {
        drawContent()
        val w = size.width
        val cx = x * w
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
                start = Offset(cx - w * 0.25f, 0f), end = Offset(cx + w * 0.25f, size.height),
            )
        )
    }
}

/** Появление снизу с затуханием, с задержкой по индексу. */
fun Modifier.enter(index: Int = 0): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(8) * 70).toLong())
        p.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer { alpha = p.value; translationY = (1 - p.value) * 60f }
}

// =====================================================================
//  Счётчик, который «накручивается»
// =====================================================================

@Composable
fun AnimatedNumber(value: Int, style: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier = Modifier, suffix: String = "") {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    Text("${anim.value.toInt()}$suffix", style = style, color = color, modifier = modifier)
}

// =====================================================================
//  Баннер-иллюстрация для экранов
// =====================================================================

@Composable
fun HeroBanner(
    title: String,
    subtitle: String,
    emoji: String,
    colors: List<Color> = listOf(Color(0xFF3A2208), Color(0xFF1A1410)),
    accent: Color = Gold,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val t = rememberInfiniteTransition(label = "banner")
    val float by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "s")
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(colors))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent, accent.copy(alpha = 0.2f))), shape),
    ) {
        // декоративная графика
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width * 0.82f, size.height * 0.5f)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent), c, size.height * 0.9f), size.height * 0.9f, c)
            rotate(spin, c) {
                for (i in 0 until 3) {
                    val r = size.height * (0.38f + i * 0.17f)
                    drawArc(
                        accent.copy(alpha = 0.35f - i * 0.09f), startAngle = i * 70f, sweepAngle = 220f - i * 40f, useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                        style = Stroke(width = (2.5f - i * 0.6f).dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            // диагональные полосы
            for (i in 0 until 6) {
                val x0 = size.width * (0.05f + i * 0.09f)
                drawLine(Color.White.copy(alpha = 0.025f), Offset(x0, size.height), Offset(x0 + size.height * 0.6f, 0f), 10.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = TextHi)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextHi.copy(alpha = 0.7f))
                content()
            }
            Spacer(Modifier.width(8.dp))
            Text(emoji, fontSize = 56.sp, modifier = Modifier.graphicsLayer { translationY = float })
        }
    }
}

// =====================================================================
//  Концентрические кольца (калории / белок / углеводы)
// =====================================================================

data class RingSpec(val progress: Float, val colors: List<Color>)

@Composable
fun ConcentricRings(rings: List<RingSpec>, size: Dp = 150.dp, thickness: Dp = 13.dp, gap: Dp = 4.dp, content: @Composable BoxScope.() -> Unit = {}) {
    val anims = rings.map { r ->
        val a = remember { Animatable(0f) }
        LaunchedEffect(r.progress) { a.animateTo(r.progress.coerceIn(0f, 1.0f), tween(1100, easing = FastOutSlowInEasing)) }
        a.value
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val st = thickness.toPx(); val g = gap.toPx()
            rings.forEachIndexed { i, r ->
                val inset = st / 2 + i * (st + g)
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val tl = Offset(inset, inset)
                drawArc(r.colors.first().copy(alpha = 0.14f), -90f, 360f, false, tl, arcSize, style = Stroke(st, cap = StrokeCap.Round))
                val sweep = 360f * anims[i]
                if (sweep > 0.5f) {
                    drawArc(
                        Brush.sweepGradient(r.colors + r.colors.first(), center = center), -90f, sweep, false, tl, arcSize,
                        style = Stroke(st, cap = StrokeCap.Round),
                    )
                    // светящаяся «голова» кольца
                    val ang = Math.toRadians((-90f + sweep).toDouble())
                    val rr = arcSize.width / 2
                    val head = Offset(center.x + (rr * cos(ang)).toFloat(), center.y + (rr * sin(ang)).toFloat())
                    drawCircle(r.colors.last().copy(alpha = 0.35f), st * 0.95f, head)
                    drawCircle(Color.White.copy(alpha = 0.85f), st * 0.22f, head)
                }
            }
        }
        content()
    }
}

// =====================================================================
//  Столбчатый график по дням
// =====================================================================

@Composable
fun BarChart(values: List<Int>, labels: List<String>, target: Int, modifier: Modifier = Modifier, color: Color = Gold, highlightLast: Boolean = true) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(values) { p.snapTo(0f); p.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val maxV = maxOf(values.maxOrNull() ?: 0, target).coerceAtLeast(1)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val bw = slot * 0.52f
            val ty = size.height * (1 - target.toFloat() / maxV)
            // линия цели
            var x = 0f
            while (x < size.width) { drawLine(TextLo.copy(alpha = 0.5f), Offset(x, ty), Offset(x + 8f, ty), 2f); x += 16f }
            values.forEachIndexed { i, v ->
                val h = size.height * (v.toFloat() / maxV) * p.value
                val left = i * slot + (slot - bw) / 2
                val last = highlightLast && i == values.lastIndex
                val hit = v >= target * 0.9f
                drawRoundRect(color.copy(alpha = 0.08f), Offset(left, 0f), Size(bw, size.height), CornerRadius(bw / 2))
                if (h > 0) drawRoundRect(
                    Brush.verticalGradient(
                        if (hit) listOf(Green, Green.copy(alpha = 0.5f)) else listOf(color, color.copy(alpha = if (last) 0.6f else 0.35f)),
                        startY = size.height - h, endY = size.height,
                    ),
                    Offset(left, size.height - h), Size(bw, h), CornerRadius(bw / 2),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(l, style = MaterialTheme.typography.labelSmall, color = if (i == labels.lastIndex) Gold else TextLo,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

// =====================================================================
//  Медаль для достижений
// =====================================================================

@Composable
fun Medal(emoji: String, color: Color, unlocked: Boolean, size: Dp = 72.dp, progress: Float = 0f) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val c = center
            if (unlocked) {
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent), c, s * 0.62f), s * 0.62f, c)
                // лучи
                for (i in 0 until 12) {
                    val a = i * 30.0 * PI / 180
                    drawLine(
                        color.copy(alpha = 0.35f),
                        Offset(c.x + (s * 0.36f * cos(a)).toFloat(), c.y + (s * 0.36f * sin(a)).toFloat()),
                        Offset(c.x + (s * 0.48f * cos(a)).toFloat(), c.y + (s * 0.48f * sin(a)).toFloat()),
                        s * 0.03f, StrokeCap.Round,
                    )
                }
            }
            // зубчатый «орден»
            val path = Path()
            val spikes = 16
            for (i in 0 until spikes * 2) {
                val r = if (i % 2 == 0) s * 0.40f else s * 0.35f
                val a = i * PI / spikes
                val px = c.x + (r * cos(a)).toFloat(); val py = c.y + (r * sin(a)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(
                path,
                if (unlocked) Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)), Offset(0f, 0f), Offset(s, s))
                else Brush.linearGradient(listOf(Color(0xFF2A2E3A), Color(0xFF1B1E27))),
            )
            drawCircle(if (unlocked) Color(0xFF15171E) else Color(0xFF14161D), s * 0.29f, c)
            drawCircle(
                if (unlocked) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                s * 0.29f, c, style = Stroke(s * 0.015f),
            )
            if (!unlocked && progress > 0f) drawArc(
                color.copy(alpha = 0.8f), -90f, 360f * progress.coerceIn(0f, 1f), false,
                Offset(c.x - s * 0.29f, c.y - s * 0.29f), Size(s * 0.58f, s * 0.58f), style = Stroke(s * 0.035f, cap = StrokeCap.Round),
            )
        }
        Text(emoji, fontSize = (size.value * 0.3f).sp, modifier = Modifier.graphicsLayer { alpha = if (unlocked) 1f else 0.3f })
    }
}

// =====================================================================
//  Конфетти
// =====================================================================

private data class Particle(
    val x: Float, val y: Float, val vx: Float, val vy: Float,
    val size: Float, val color: Color, val rot: Float, val vr: Float, val circle: Boolean,
)

@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 140, durationMs: Long = 3200) {
    val palette = listOf(Gold, GoldDeep, Green, Blue, Violet, Red, Color.White)
    val particles = remember {
        List(count) {
            val ang = Random.nextDouble(-PI * 0.95, -PI * 0.05)
            val sp = Random.nextDouble(0.6, 1.6).toFloat()
            Particle(
                x = 0.5f + Random.nextFloat() * 0.1f - 0.05f, y = 0.55f,
                vx = (cos(ang) * sp).toFloat() * 0.9f, vy = (sin(ang) * sp).toFloat() * 1.6f,
                size = Random.nextFloat() * 9f + 6f, color = palette.random(),
                rot = Random.nextFloat() * 360f, vr = Random.nextFloat() * 720f - 360f, circle = Random.nextFloat() < 0.3f,
            )
        }
    }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (elapsed < durationMs) {
            withFrameNanos { now -> elapsed = (now - start) / 1_000_000 }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val t = elapsed / 1000f
        val fade = (1f - (elapsed.toFloat() / durationMs)).coerceIn(0f, 1f)
        particles.forEach { p ->
            val px = (p.x + p.vx * t * 0.55f) * size.width
            val py = (p.y + p.vy * t * 0.55f + 0.9f * t * t * 0.5f) * size.height
            if (py > size.height + 40) return@forEach
            if (p.circle) drawCircle(p.color.copy(alpha = fade), p.size / 2, Offset(px, py))
            else rotate(p.rot + p.vr * t, Offset(px, py)) {
                drawRect(p.color.copy(alpha = fade), Offset(px - p.size / 2, py - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}

// =====================================================================
//  Сканер лица: рамка-уголки + бегущая линия
// =====================================================================

fun Modifier.scanOverlay(active: Boolean, color: Color = Violet): Modifier = composed {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    this.drawWithContent {
        drawContent()
        // уголки
        val l = size.minDimension * 0.12f
        val sw = 3.dp.toPx()
        val m = 14.dp.toPx()
        val cs = listOf(
            Offset(m, m) to listOf(Offset(l, 0f), Offset(0f, l)),
            Offset(size.width - m, m) to listOf(Offset(-l, 0f), Offset(0f, l)),
            Offset(m, size.height - m) to listOf(Offset(l, 0f), Offset(0f, -l)),
            Offset(size.width - m, size.height - m) to listOf(Offset(-l, 0f), Offset(0f, -l)),
        )
        cs.forEach { (o, ds) -> ds.forEach { d -> drawLine(Color.White.copy(alpha = 0.85f), o, o + d, sw, StrokeCap.Round) } }
        if (active) {
            val yy = size.height * y
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.45f)), startY = yy - 120f, endY = yy),
                topLeft = Offset(0f, yy - 120f), size = Size(size.width, 120f),
            )
            drawLine(color, Offset(0f, yy), Offset(size.width, yy), 2.dp.toPx())
        }
    }
}

// =====================================================================
//  Простой Markdown для ответов AI: заголовки, списки, **жирный**
// =====================================================================

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, color: Color = TextHi, accent: Color = Gold) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.isBlank() -> Spacer(Modifier.height(2.dp))
                trimmed.startsWith("#") -> Text(
                    inline(trimmed.trimStart('#').trim(), accent), style = MaterialTheme.typography.titleMedium, color = accent,
                    modifier = Modifier.padding(top = 4.dp),
                )
                trimmed.matches(Regex("^[-*•]\\s+.*")) -> Row {
                    Box(Modifier.padding(top = 8.dp, end = 10.dp, start = if (line.startsWith("  ")) 14.dp else 0.dp)
                        .size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                    Text(inline(trimmed.replaceFirst(Regex("^[-*•]\\s+"), ""), accent), style = MaterialTheme.typography.bodyMedium, color = color)
                }
                trimmed.matches(Regex("^\\d+[.)]\\s+.*")) -> Row {
                    val num = trimmed.takeWhile { it.isDigit() }
                    Box(
                        Modifier.padding(end = 10.dp, top = 1.dp).size(20.dp).clip(RoundedCornerShape(7.dp)).background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(num, style = MaterialTheme.typography.labelSmall, color = accent) }
                    Text(inline(trimmed.replaceFirst(Regex("^\\d+[.)]\\s+"), ""), accent), style = MaterialTheme.typography.bodyMedium, color = color)
                }
                trimmed.startsWith("---") -> Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                else -> Text(inline(trimmed, accent), style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }
}

private fun inline(s: String, accent: Color) = buildAnnotatedString {
    var i = 0
    val src = s.replace("__", "**")
    while (i < src.length) {
        val start = src.indexOf("**", i)
        if (start < 0) { append(src.substring(i)); break }
        val end = src.indexOf("**", start + 2)
        if (end < 0) { append(src.substring(i)); break }
        append(src.substring(i, start))
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)) { append(src.substring(start + 2, end)) }
        i = end + 2
    }
}

// =====================================================================
//  Индикатор «печатает…»
// =====================================================================

@Composable
fun TypingDots(color: Color = Gold) {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(0f, 3f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "p")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val d = ((p - i).mod(3f))
            val k = if (d < 1f) sin(d * PI).toFloat() else 0f
            Box(Modifier.size(8.dp).graphicsLayer { translationY = -k * 8f; alpha = 0.4f + 0.6f * k }
                .clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

/** Пульсирующая сфера — «живой» AI. */
@Composable
fun AiOrb(size: Dp = 120.dp) {
    val t = rememberInfiniteTransition(label = "orb")
    val p by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "r")
    Canvas(Modifier.size(size)) {
        val c = center; val s = this.size.minDimension
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = 0.55f), Color.Transparent), c, s * 0.6f * p), s * 0.6f * p, c)
        rotate(r, c) {
            drawCircle(Brush.sweepGradient(listOf(Gold, Violet, Blue, Gold), c), s * 0.3f * p, c)
        }
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), Offset(c.x - s * 0.08f, c.y - s * 0.1f), s * 0.18f),
            s * 0.3f * p, c)
        rotate(-r * 1.5f, c) {
            drawArc(Color.White.copy(alpha = 0.5f), 0f, 80f, false, Offset(c.x - s * 0.42f, c.y - s * 0.42f), Size(s * 0.84f, s * 0.84f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Gold.copy(alpha = 0.6f), 180f, 60f, false, Offset(c.x - s * 0.42f, c.y - s * 0.42f), Size(s * 0.84f, s * 0.84f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Suppress("unused")
private fun DrawScope.noop() {}
