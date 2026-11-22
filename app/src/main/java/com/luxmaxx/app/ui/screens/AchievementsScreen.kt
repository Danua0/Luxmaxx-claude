package com.luxmaxx.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.logic.LevelInfo
import com.luxmaxx.app.logic.Progress
import com.luxmaxx.app.ui.components.Confetti
import com.luxmaxx.app.ui.components.Medal
import com.luxmaxx.app.ui.components.enter
import com.luxmaxx.app.ui.components.shimmer
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.CardGradient
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo

@Composable
fun AchievementsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsStateWithLifecycle()
    val protein = vm.targets(d)?.protein ?: 140
    val lvl = vm.levelInfo(d)
    val list = Progress.achievements.map { it to it.progress(d, protein).coerceIn(0f, 1f) }
        .sortedWith(compareByDescending<Pair<com.luxmaxx.app.logic.Achievement, Float>> { it.second >= 1f }.thenByDescending { it.second })
    val done = list.count { it.second >= 1f }

    LazyVerticalGrid(
        GridCells.Fixed(3),
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = TextHi) }
                Column {
                    Text("ЗАЛ СЛАВЫ", style = MaterialTheme.typography.labelSmall, color = Gold)
                    Text("Достижения", style = MaterialTheme.typography.headlineMedium, color = TextHi)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { LevelCard(lvl, done, list.size) }
        itemsIndexed(list) { i, (a, prog) ->
            val unlocked = prog >= 1f
            Column(
                Modifier.enter(i).clip(RoundedCornerShape(20.dp)).background(CardGradient)
                    .border(1.dp, if (unlocked) Brush.linearGradient(listOf(Color(a.color).copy(alpha = 0.7f), Color.Transparent)) else BorderGradient,
                        RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Medal(a.emoji, Color(a.color), unlocked, 64.dp, prog)
                Spacer(Modifier.height(6.dp))
                Text(a.title, style = MaterialTheme.typography.labelLarge, color = if (unlocked) TextHi else TextLo,
                    textAlign = TextAlign.Center, maxLines = 2)
                Text(if (unlocked) a.description else "${(prog * 100).toInt()}% · ${a.description}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                    color = TextLo, textAlign = TextAlign.Center, maxLines = 3)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun LevelCard(lvl: LevelInfo, done: Int? = null, total: Int? = null, modifier: Modifier = Modifier) {
    val next = Progress.nextRank(lvl.level)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2B1D07), Color(0xFF17120C), Color(0xFF15171E))))
            .border(1.dp, Brush.linearGradient(listOf(Gold.copy(alpha = 0.6f), Color.Transparent, Gold.copy(alpha = 0.25f))), RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.padding(end = 14.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Gold, GoldDeep))).shimmer(0.3f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1C1200))
                    Text("${lvl.level}", style = MaterialTheme.typography.displaySmall, color = Color(0xFF1C1200))
                }
            }
            Column(Modifier.weight(1f)) {
                Text("${lvl.rank.emoji} ${lvl.rank.title}", style = MaterialTheme.typography.headlineSmall, color = TextHi)
                Text("${lvl.xp} XP · до ${lvl.level + 1} уровня ${lvl.toNext} XP", style = MaterialTheme.typography.bodySmall, color = TextLo)
                if (next != null) Text("Следующий ранг: ${next.emoji} ${next.title} (ур. ${next.minLevel})",
                    style = MaterialTheme.typography.labelMedium, color = Gold)
            }
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color(0x22FFFFFF))) {
            Box(
                Modifier.fillMaxWidth(lvl.progress.coerceAtLeast(0.03f)).height(12.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(GoldDeep, Gold))).shimmer(0.5f, 1800)
            )
        }
        if (done != null && total != null) Text("Открыто достижений: $done из $total", style = MaterialTheme.typography.labelMedium, color = TextLo)
    }
}

/** Полноэкранный праздник с конфетти. */
@Composable
fun CelebrationOverlay(c: AppViewModel.Celebration, onDismiss: () -> Unit) {
    val scale = remember(c) { Animatable(0.3f) }
    LaunchedEffect(c) { scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 180f)) }
    LaunchedEffect(c) { kotlinx.coroutines.delay(4500); onDismiss() }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC05060A))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Confetti()
        Column(
            Modifier.padding(32.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Medal(c.emoji, Color(c.color), true, 150.dp)
            Spacer(Modifier.height(18.dp))
            Text(c.title, style = MaterialTheme.typography.headlineLarge, color = TextHi, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(c.subtitle, style = MaterialTheme.typography.bodyLarge, color = TextLo, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            Text("Нажми, чтобы продолжить", style = MaterialTheme.typography.labelSmall, color = Gold)
        }
    }
}

@Suppress("unused")
private val keepWidth = Modifier.width(1.dp)
