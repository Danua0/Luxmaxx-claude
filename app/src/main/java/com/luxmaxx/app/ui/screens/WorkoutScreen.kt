package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.data.SetEntry
import com.luxmaxx.app.logic.PlannedExercise
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.theme.Amber
import com.luxmaxx.app.ui.theme.Green
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Программа: ${program.title}", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(program.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(program.days) { wd ->
            SectionCard(wd.name + if (wd == next) "  ← следующая" else "") {
                wd.exercises.forEach { ex ->
                    Text("• ${ex.name} — ${ex.sets}×${ex.repMin}-${ex.repMax}", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { vm.startWorkout(wd.name) }) { Text("Начать тренировку") }
            }
        }
        item {
            SectionCard("Принципы") {
                Text("• Последний подход — до 1–2 повторов в запасе (почти до отказа).")
                Text("• Отдых: базовые 2–3 мин, изоляция 60–90 сек.")
                Text("• Разминка: 2–3 лёгких подхода перед первым упражнением.")
                Text("• Не растёт 3 недели? Напиши AI-тренеру — разберёт твой дневник.")
            }
        }
        if (d.workouts.isNotEmpty()) item {
            Text("История", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(d.workouts.sortedByDescending { it.id }.take(30), key = { it.id }) { w ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${w.date} · ${w.dayName}", fontWeight = FontWeight.SemiBold)
                        Text("Объём: ${Programs.volumeKg(w).toInt()} кг · подходов: ${w.sets.size}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { confirmDelete = w.id }) { Icon(Icons.Filled.Delete, "Удалить") }
                }
                w.sets.groupBy { it.exercise }.forEach { (ex, s) ->
                    Text("$ex: ${s.joinToString { "${it.weight.fmt()}×${it.reps}" }}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Удалить тренировку?") },
            confirmButton = { TextButton(onClick = { vm.deleteWorkout(id); confirmDelete = null }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ActiveWorkout(vm: AppViewModel, dayName: String, exercises: List<PlannedExercise>) {
    val d by vm.data.collectAsStateWithLifecycle()
    val sets by vm.activeSets.collectAsStateWithLifecycle()
    var restLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(restLeft) {
        if (restLeft > 0) { delay(1000); restLeft-- }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(dayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.cancelWorkout() }) { Icon(Icons.Filled.Close, "Отменить") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (restLeft > 0) "Отдых: ${restLeft / 60}:${"%02d".format(restLeft % 60)}" else "Таймер отдыха:",
                    color = if (restLeft > 0) Amber else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold)
                listOf(60, 90, 120, 180).forEach { s ->
                    AssistChip(onClick = { restLeft = s }, label = { Text("${s}с") })
                }
            }
        }
        items(exercises, key = { it.name }) { ex ->
            val last = Programs.lastSets(d.workouts, ex.name)
            val done = sets.withIndex().filter { it.value.exercise == ex.name }
            var w by remember { mutableStateOf(last.maxOfOrNull { it.weight }?.fmt()?.replace(',', '.') ?: "") }
            var r by remember { mutableStateOf("") }
            SectionCard(ex.name) {
                Text("План: ${ex.sets} × ${ex.repMin}–${ex.repMax}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ex.tip.isNotBlank()) Text("💡 ${ex.tip}", style = MaterialTheme.typography.bodySmall)
                Text(Programs.progressionHint(ex, last), style = MaterialTheme.typography.bodySmall, color = Amber)
                done.forEach { (i, s) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✓ ${s.weight.fmt()} кг × ${s.reps}", color = Green, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.removeSet(i) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, "Удалить подход")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(w, { w = it }, "Вес, кг", Modifier.weight(1f))
                    NumberField(r, { r = it }, "Повторы", Modifier.weight(1f), decimal = false)
                    FilledTonalButton(onClick = {
                        val reps = r.toIntOrNull()
                        if (reps != null && reps > 0) {
                            vm.addSet(SetEntry(ex.name, w.toDoubleOrNull() ?: 0.0, reps))
                            r = ""
                            restLeft = if (ex.repMax <= 8) 150 else 90
                        }
                    }) { Text("+") }
                }
            }
        }
        item {
            Button(
                onClick = { vm.finishWorkout() },
                enabled = sets.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Завершить тренировку (${sets.size} подх.)") }
            OutlinedButton(onClick = { vm.cancelWorkout() }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Отменить")
            }
        }
    }
}
