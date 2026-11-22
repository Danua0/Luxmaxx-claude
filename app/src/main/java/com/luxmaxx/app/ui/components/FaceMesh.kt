package com.luxmaxx.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.Violet
import java.io.File

/**
 * Фото лица с наложенной «сеткой» ML Kit: контуры лица, глаз, бровей, губ и носа.
 * Пока идёт анализ — бегает линия сканера.
 */
@Composable
fun FaceMeshPhoto(
    path: String,
    contours: List<List<Pair<Float, Float>>>,
    aspect: Float,
    scanning: Boolean,
    modifier: Modifier = Modifier,
) {
    val reveal = remember(contours) { Animatable(0f) }
    LaunchedEffect(contours) { if (contours.isNotEmpty()) reveal.animateTo(1f, tween(1400, easing = LinearEasing)) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().aspectRatio(1f).clip(shape).border(1.dp, BorderGradient, shape)
            .scanOverlay(scanning),
    ) {
        AsyncImage(File(path), "Фото лица", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (contours.isNotEmpty()) Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val a = aspect.coerceAtLeast(0.01f)
            val dispW: Float; val dispH: Float; val offX: Float; val offY: Float
            if (a > w / h) { dispH = h; dispW = h * a; offX = (w - dispW) / 2; offY = 0f }
            else { dispW = w; dispH = w / a; offX = 0f; offY = (h - dispH) / 2 }
            fun map(p: Pair<Float, Float>) = Offset(offX + p.first * dispW, offY + p.second * dispH)

            // лёгкое затемнение, чтобы сетка читалась
            drawRect(Color.Black.copy(alpha = 0.18f * reveal.value))
            contours.forEachIndexed { ci, pts ->
                val n = (pts.size * reveal.value).toInt().coerceAtMost(pts.size)
                if (n < 2) return@forEachIndexed
                val pathLine = Path().apply {
                    val first = map(pts[0]); moveTo(first.x, first.y)
                    for (i in 1 until n) { val q = map(pts[i]); lineTo(q.x, q.y) }
                    if (ci == 0 && n == pts.size) close() // овал лица замыкаем
                }
                val col = if (ci == 0) Gold else Violet
                drawPath(pathLine, col.copy(alpha = 0.25f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                drawPath(pathLine, col.copy(alpha = 0.9f), style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
                for (i in 0 until n step 2) drawCircle(Color.White.copy(alpha = 0.85f), 1.6.dp.toPx(), map(pts[i]))
            }
        }
    }
}
