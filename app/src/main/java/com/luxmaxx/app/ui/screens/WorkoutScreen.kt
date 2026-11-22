package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.data.SetEntry
import com.luxmaxx.app.logic.PlannedExercise
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.IconBadge
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ProgressRing
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldGradient
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(vm: AppViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val active by vm.activeDay.collectAsStateWithLifecycle()
    val program = vm.program(d) ?: return

    val day = program.days.firstOrNull { it.name == active }
    if (day != null) {
        ActiveWorkout(vm, day.name, day.exercises)
        return
    }

    val next = Programs.nextDay(program, d.workouts)
    var confirmDelete by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(program.title, overline = "Твоя программа", subtitle = program.description) }

        itemsIndexed(program.days) { i, wd ->
            val isNext = wd == next
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                            .background(if (isNext) GoldGradient else androidx.compose.ui.graphics.SolidColor(Surface1)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.titleLarge,
                            color = if (isNext) Color(0xFF1C1200) else TextLo)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(wd.name, style = MaterialTheme.typography.titleLarge, color = TextHi)
                        Text("${wd.exercises.size} упражнений · ${wd.exercises.sumOf { it.sets }} подходов",
                            style = MaterialTheme.typography.bodySmall, color = TextLo)
                    }
                    if (isNext) Pill("СЛЕДУЮЩАЯ")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    wd.exercises.forEach { ex ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isNext) Gold else TextLo))
                            Spacer(Modifier.width(10.dp))
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium, color = TextHi, modifier = Modifier.weight(1f))
                            Text("${ex.sets}×${ex.repMin}–${ex.repMax}", style = MaterialTheme.typography.labelLarge, color = TextLo)
                        }
                    }
                }
                GradientButton("Начать тренировку", onClick = { vm.startWorkout(wd.name) },
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.PlayArrow)
            }
        }

        item {
            SectionCard("Принципы роста", icon = Icons.Filled.Lightbulb, accent = Blue) {
                listOf(
                    "Последний подход — 1–2 повтора в запасе, почти до отказа",
                    "Отдых: базовые 2–3 мин, изоляция 60–90 сек",
                    "Разминка: 2–3 лёгких подхода перед первым упражнением",
                    "Не растёт 3 недели — спроси AI-тренера, он разберёт дневник",
                ).forEach {
                    Row { Text("→  ", color = Blue); Text(it, style = MaterialTheme.typography.bodyMedium, color = TextHi) }
                }
            }
        }

        if (d.workouts.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                IconBadge(Icons.Filled.History, Gold, 30.dp); Spacer(Modifier.width(10.dp))
                Text("История", style = MaterialTheme.typography.titleLarge, color = TextHi)
            }
        }
        items(d.workouts.sortedByDescending { it.id }.take(30), key = { it.id }) { w ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(w.dayName, style = MaterialTheme.typography.titleMedium, color = TextHi)
                        Text(w.date, style = MaterialTheme.typography.bodySmall, color = TextLo)
                    }
                    Pill("${Programs.volumeKg(w).toInt()} кг", Green)
                    IconButton(onClick = { confirmDelete = w.id }) { Icon(Icons.Filled.Delete, "Удалить", tint = TextLo) }
                }
                w.sets.groupBy { it.exercise }.forEach { (ex, s) ->
                    Row {
                        Text(ex, style = MaterialTheme.typography.bodySmall, color = TextLo, modifier = Modifier.weight(1f))
                        Text(s.joinToString("  ") { "${it.weight.fmt()}×${it.reps}" }, style = MaterialTheme.typography.bodySmall, color = TextHi)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = Surface1,
            title = { Text("Удалить тренировку?") },
            confirmButton = { TextButton(onClick = { vm.deleteWorkout(id); confirmDelete = null }) { Text("Удалить", color = Red) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ActiveWorkout(vm: AppViewModel, dayName: String, exercises: List<PlannedExercise>) {
    val d by vm.data.collectAsStateWithLifecycle()
    val sets by vm.activeSets.collectAsStateWithLifecycle()
    var restLeft by remember { mutableIntStateOf(0) }
    var restTotal by remember { mutableIntStateOf(1) }
    LaunchedEffect(restLeft) {
        if (restLeft > 0) { delay(1000); restLeft-- }
    }
    fun startRest(s: Int) { restTotal = s; restLeft = s }
    val planned = exercises.sumOf { it.sets }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(dayName, overline = "Идёт тренировка") {
                IconButton(onClick = { vm.cancelWorkout() }) { Icon(Icons.Filled.Close, "Отменить", tint = TextLo) }
            }
        }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(sets.size.toFloat() / planned, size = 84.dp, thickness = 9.dp, colors = listOf(Green, Blue)) {
                        Text("${sets.size}/$planned", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Объём", style = MaterialTheme.typography.labelSmall, color = TextLo)
                        Text("${sets.sumOf { it.weight * it.reps }.toInt()} кг", style = MaterialTheme.typography.headlineSmall, color = Green)
                    }
                    ProgressRing(if (restLeft > 0) restLeft.toFloat() / restTotal else 0f, size = 84.dp, thickness = 9.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Timer, null, tint = if (restLeft > 0) Gold else TextLo, modifier = Modifier.size(16.dp))
                            Text(if (restLeft > 0) "${restLeft / 60}:${"%02d".format(restLeft % 60)}" else "отдых",
                                style = MaterialTheme.typography.titleSmall, color = if (restLeft > 0) Gold else TextLo)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120, 180).forEach { s ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Surface1)
                                .clickable { startRest(s) }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (s < 120) "${s}с" else "${s / 60}м", color = TextHi, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
        items(exercises, key = { it.name }) { ex ->
            val last = Programs.lastSets(d.workouts, ex.name)
            val done = sets.withIndex().filter { it.value.exercise == ex.name }
            var w by remember { mutableStateOf(last.maxOfOrNull { it.weight }?.fmt()?.replace(',', '.') ?: "") }
            var r by remember { mutableStateOf("") }
            val complete = done.size >= ex.sets
            SectionCard(ex.name, icon = if (complete) Icons.Filled.Check else Icons.Filled.FitnessCenter,
                accent = if (complete) Green else Gold) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("${ex.sets} × ${ex.repMin}–${ex.repMax}", Blue)
                    Pill("${done.size}/${ex.sets} готово", if (complete) Green else TextLo)
                }
                if (ex.tip.isNotBlank()) Text("💡 ${ex.tip}", style = MaterialTheme.typography.bodySmall, color = TextLo)
                Text(Programs.progressionHint(ex, last), style = MaterialTheme.typography.bodySmall, color = Gold)
                if (done.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    done.forEach { (i, s) ->
                        Row(
                            Modifier.clip(CircleShape).background(Green.copy(alpha = 0.15f)).clickable { vm.removeSet(i) }
                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${s.weight.fmt()}×${s.reps}", color = Green, style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Filled.Close, "Удалить", tint = Green.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp).size(14.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(w, { w = it }, "Вес, кг", Modifier.weight(1f))
                    NumberField(r, { r = it }, "Повторы", Modifier.weight(1f), decimal = false)
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(GoldGradient).clickable {
                            val reps = r.toIntOrNull()
                            if (reps != null && reps > 0) {
                                vm.addSet(SetEntry(ex.name, w.toDoubleOrNull() ?: 0.0, reps))
                                r = ""
                                startRest(if (ex.repMax <= 8) 150 else 90)
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, "Добавить подход", tint = Color(0xFF1C1200)) }
                }
            }
        }
        item {
            GradientButton("Завершить тренировку", onClick = { vm.finishWorkout() }, enabled = sets.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Check)
            Spacer(Modifier.height(16.dp))
        }
    }
}
