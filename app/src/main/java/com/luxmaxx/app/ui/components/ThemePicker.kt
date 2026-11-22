package com.luxmaxx.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luxmaxx.app.ui.theme.AppTheme
import com.luxmaxx.app.ui.theme.PacificoFamily
import com.luxmaxx.app.ui.theme.Palette
import com.luxmaxx.app.ui.theme.Palettes

/** Сетка 2×2 из «живых» превью тем. Нажатие сразу применяет тему. */
@Composable
fun ThemePicker(current: AppTheme, onPick: (AppTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppTheme.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    ThemePreview(Palettes.of(t), t == current, Modifier.weight(1f)) { onPick(t) }
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(p: Palette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val src = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(src)
            .clip(shape)
            .background(p.bg)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) Brush.linearGradient(listOf(p.accent, p.accent2)) else Brush.linearGradient(listOf(p.stroke, p.stroke)), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
    ) {
        // мини-сцена
        Box(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                if (p.retro) {
                    val night = p.theme == AppTheme.MIAMI_NIGHT
                    val hz = h * 0.6f
                    val sky = if (night) listOf(Color(0xFF04020B), Color(0xFF3A0A4E)) else listOf(Color(0xFF1B0836), Color(0xFFE2457F), Color(0xFFFF9E5A))
                    drawRect(Brush.verticalGradient(sky, 0f, hz), size = Size(w, hz))
                    val r = w * 0.22f; val c = Offset(w * 0.5f, hz - r * 0.1f)
                    clipRect(0f, 0f, w, hz) {
                        drawCircle(Brush.verticalGradient(
                            if (night) listOf(Color(0xFFFF6AD5), Color(0xFF9B1BFF)) else listOf(Color(0xFFFFF07A), Color(0xFFFF3D8B)),
                            c.y - r, c.y + r), r, c)
                        for (i in 0 until 4) drawRect(sky.last(), Offset(c.x - r, c.y + i * r * 0.18f), Size(r * 2, (1.5f + i) * density))
                    }
                    drawRect(if (night) Color(0xFF14052A) else Color(0xFF2A0B44), Offset(0f, hz), Size(w, h - hz))
                    val g = if (night) Color(0xFF00E5FF) else Color(0xFFFF4FA3)
                    for (i in -6..6) drawLine(g.copy(alpha = 0.5f), Offset(w / 2 + i * w * 0.06f, hz), Offset(w / 2 + i * w * 0.4f, h), 1.2f * density)
                    for (k in 1..4) { val y = hz + (h - hz) * (k / 4f) * (k / 4f); drawLine(g.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), 1.2f * density) }
                    drawLine(g, Offset(0f, hz), Offset(w, hz), 1.8f * density)
                } else {
                    drawRect(p.bg)
                    drawCircle(Brush.radialGradient(listOf(p.blobs[0].copy(alpha = 0.6f), Color.Transparent), Offset(w * 0.85f, 0f), w * 0.8f), w * 0.8f, Offset(w * 0.85f, 0f))
                    // карточка с кольцом
                    drawRoundRect(p.surface2, Offset(w * 0.08f, h * 0.32f), Size(w * 0.84f, h * 0.56f), androidx.compose.ui.geometry.CornerRadius(12 * density))
                    val rc = Offset(w * 0.26f, h * 0.6f); val rr = h * 0.17f
                    drawCircle(p.textHi.copy(alpha = 0.1f), rr, rc, style = androidx.compose.ui.graphics.drawscope.Stroke(4 * density))
                    drawArc(Brush.linearGradient(listOf(p.accent, p.accent2)), -90f, 250f, false, Offset(rc.x - rr, rc.y - rr), Size(rr * 2, rr * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(4 * density, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawRoundRect(p.green, Offset(w * 0.46f, h * 0.5f), Size(w * 0.36f, 5 * density), androidx.compose.ui.geometry.CornerRadius(3 * density))
                    drawRoundRect(p.blue, Offset(w * 0.46f, h * 0.62f), Size(w * 0.26f, 5 * density), androidx.compose.ui.geometry.CornerRadius(3 * density))
                    drawRoundRect(p.violet, Offset(w * 0.46f, h * 0.74f), Size(w * 0.18f, 5 * density), androidx.compose.ui.geometry.CornerRadius(3 * density))
                }
            }
            Text(
                "Aa",
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                color = if (p.retro) Color.White else p.textHi,
                style = TextStyle(
                    fontFamily = p.display, fontSize = if (p.display == PacificoFamily) 22.sp else 20.sp,
                    fontWeight = if (p.display == PacificoFamily) FontWeight.Normal else FontWeight.Black,
                    shadow = if (p.glow) Shadow(p.accent, blurRadius = 16f) else null,
                ),
            )
            if (selected) Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(p.accent, p.accent2))),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = p.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Column(Modifier.background(p.surface1).fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text("${p.theme.emoji} ${p.theme.title}", color = p.textHi,
                style = TextStyle(fontFamily = p.body, fontWeight = FontWeight.Bold, fontSize = 13.sp))
            Text(p.theme.subtitle, color = p.textLo, style = TextStyle(fontFamily = p.body, fontWeight = FontWeight.Medium, fontSize = 11.sp))
        }
    }
}
