package com.luxmaxx.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luxmaxx.app.logic.Muscle
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.TextLo

private val BodyBase = Color(0xFF242833)
private val BodyEdge = Color(0x22FFFFFF)
private val Cold = Color(0xFF4A3412)
private val Hot = Color(0xFFFF5A3D)

fun heatColor(h: Float): Color = when {
    h <= 0.02f -> BodyBase
    h <= 1f -> lerp(Cold, Gold, h)
    else -> lerp(Gold, Hot, ((h - 1f) / 0.3f).coerceIn(0f, 1f))
}

/**
 * Силуэт тела спереди и сзади. heat: 0 — не нагружено, 1 — недельная норма, >1 — перегруз.
 */
@Composable
fun MuscleMap(heat: Map<Muscle, Float>, modifier: Modifier = Modifier, labels: Boolean = true) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(heat) { p.snapTo(0f); p.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(200f / 210f)) {
            val k = size.width / 200f
            fun h(m: Muscle) = (heat[m] ?: 0f) * p.value
            drawFigure(k, 0f, front = true, h = ::h)
            drawFigure(k, 100f * k, front = false, h = ::h)
        }
        if (labels) Row(Modifier.fillMaxWidth()) {
            Text("СПЕРЕДИ", style = MaterialTheme.typography.labelSmall, color = TextLo, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("СЗАДИ", style = MaterialTheme.typography.labelSmall, color = TextLo, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
}

/** Компактная легенда цветов. */
@Composable
fun HeatLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("0", style = MaterialTheme.typography.labelSmall, color = TextLo)
        Box(
            Modifier.width(110.dp).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(BodyBase, Cold, Gold, Hot)))
        )
        Text("норма+", style = MaterialTheme.typography.labelSmall, color = TextLo)
    }
}

private fun DrawScope.drawFigure(k: Float, dx: Float, front: Boolean, h: (Muscle) -> Float) {
    fun o(x: Float, y: Float) = Offset(dx + x * k, y * k)

    fun part(x1: Float, y1: Float, x2: Float, y2: Float, r: Float, m: Muscle?, factor: Float = 1f) {
        val v = if (m == null) 0f else h(m) * factor
        val tl = o(x1, y1); val sz = Size((x2 - x1) * k, (y2 - y1) * k); val cr = CornerRadius(r * k)
        if (v > 0.45f) drawRoundRect(
            heatColor(v).copy(alpha = 0.28f * (v.coerceAtMost(1.3f) / 1.3f)),
            Offset(tl.x - 2.5f * k, tl.y - 2.5f * k), Size(sz.width + 5 * k, sz.height + 5 * k), CornerRadius(cr.x + 2.5f * k),
        )
        drawRoundRect(
            Brush.verticalGradient(listOf(heatColor(v), heatColor(v).copy(alpha = 0.75f)), startY = tl.y, endY = tl.y + sz.height),
            tl, sz, cr,
        )
        drawRoundRect(BodyEdge, tl, sz, cr, style = Stroke(0.6f * k))
    }

    fun circle(cx: Float, cy: Float, r: Float, m: Muscle?) {
        val v = if (m == null) 0f else h(m)
        if (v > 0.45f) drawCircle(heatColor(v).copy(alpha = 0.3f), (r + 3) * k, o(cx, cy))
        drawCircle(heatColor(v), r * k, o(cx, cy))
        drawCircle(BodyEdge, r * k, o(cx, cy), style = Stroke(0.6f * k))
    }

    fun poly(m: Muscle?, vararg pts: Pair<Float, Float>) {
        val v = if (m == null) 0f else h(m)
        val path = Path().apply {
            pts.forEachIndexed { i, (x, y) -> val q = o(x, y); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) }
            close()
        }
        drawPath(path, heatColor(v))
        drawPath(path, BodyEdge, style = Stroke(0.6f * k))
    }

    // голова и шея
    circle(50f, 13f, 9.5f, null)
    part(45.5f, 21f, 54.5f, 29f, 3f, null)

    if (front) {
        circle(31f, 38f, 8.5f, Muscle.SHOULDERS); circle(69f, 38f, 8.5f, Muscle.SHOULDERS)
        part(35f, 31f, 49.4f, 52f, 6f, Muscle.CHEST); part(50.6f, 31f, 65f, 52f, 6f, Muscle.CHEST)
        part(19.5f, 45f, 28.5f, 70f, 4.5f, Muscle.BICEPS); part(71.5f, 45f, 80.5f, 70f, 4.5f, Muscle.BICEPS)
        part(16f, 72f, 26f, 98f, 4.5f, Muscle.FOREARMS); part(74f, 72f, 84f, 98f, 4.5f, Muscle.FOREARMS)
        circle(20f, 103f, 4.5f, null); circle(80f, 103f, 4.5f, null)
        // кубики пресса
        val rows = listOf(54f to 63f, 64.5f to 73.5f, 75f to 84f)
        rows.forEach { (a, b) -> part(40f, a, 49.3f, b, 3f, Muscle.ABS); part(50.7f, a, 60f, b, 3f, Muscle.ABS) }
        part(35f, 54f, 39f, 86f, 2.5f, Muscle.ABS, 0.6f); part(61f, 54f, 65f, 86f, 2.5f, Muscle.ABS, 0.6f)
        part(37f, 86f, 63f, 98f, 6f, null)
        part(35f, 98f, 49.3f, 146f, 7f, Muscle.QUADS); part(50.7f, 98f, 65f, 146f, 7f, Muscle.QUADS)
        circle(42f, 150f, 4.5f, null); circle(58f, 150f, 4.5f, null)
        part(37f, 155f, 47f, 193f, 5f, Muscle.CALVES, 0.7f); part(53f, 155f, 63f, 193f, 5f, Muscle.CALVES, 0.7f)
        part(35.5f, 195f, 47.5f, 201f, 3f, null); part(52.5f, 195f, 64.5f, 201f, 3f, null)
    } else {
        poly(Muscle.TRAPS, 41f to 26f, 59f to 26f, 68f to 34f, 50f to 47f, 32f to 34f)
        circle(31f, 38f, 8.5f, Muscle.SHOULDERS); circle(69f, 38f, 8.5f, Muscle.SHOULDERS)
        poly(Muscle.BACK, 35f to 41f, 49.3f to 48f, 49.3f to 80f, 41f to 77f, 36f to 60f)
        poly(Muscle.BACK, 65f to 41f, 50.7f to 48f, 50.7f to 80f, 59f to 77f, 64f to 60f)
        part(19.5f, 45f, 28.5f, 70f, 4.5f, Muscle.TRICEPS); part(71.5f, 45f, 80.5f, 70f, 4.5f, Muscle.TRICEPS)
        part(16f, 72f, 26f, 98f, 4.5f, Muscle.FOREARMS); part(74f, 72f, 84f, 98f, 4.5f, Muscle.FOREARMS)
        circle(20f, 103f, 4.5f, null); circle(80f, 103f, 4.5f, null)
        part(41f, 80f, 59f, 92f, 4f, Muscle.LOWER_BACK)
        part(36f, 91f, 49.3f, 110f, 8f, Muscle.GLUTES); part(50.7f, 91f, 64f, 110f, 8f, Muscle.GLUTES)
        part(35.5f, 112f, 49.3f, 148f, 7f, Muscle.HAMS); part(50.7f, 112f, 64.5f, 148f, 7f, Muscle.HAMS)
        part(36f, 153f, 48f, 186f, 6f, Muscle.CALVES); part(52f, 153f, 64f, 186f, 6f, Muscle.CALVES)
        part(38f, 187f, 46f, 194f, 3f, null); part(54f, 187f, 62f, 194f, 3f, null)
        part(35.5f, 195f, 47.5f, 201f, 3f, null); part(52.5f, 195f, 64.5f, 201f, 3f, null)
    }
}

/** Мини-версия: только перед и зад рядом, без подписей. */
@Composable
fun MiniMuscleMap(heat: Map<Muscle, Float>, size: androidx.compose.ui.unit.Dp = 72.dp) {
    Box(Modifier.size(size).padding(2.dp)) { MuscleMap(heat, labels = false) }
}
