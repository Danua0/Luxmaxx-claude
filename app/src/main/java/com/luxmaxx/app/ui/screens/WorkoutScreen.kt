package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.House
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
import com.luxmaxx.app.data.Equip
import com.luxmaxx.app.data.SetEntry
import com.luxmaxx.app.logic.HomeWorkouts
import com.luxmaxx.app.logic.Muscles
import com.luxmaxx.app.logic.PlannedExercise
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.ui.components.Bullet
import com.luxmaxx.app.ui.components.GoldChip
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.HeroBanner
import com.luxmaxx.app.ui.components.IconBadge
import com.luxmaxx.app.ui.components.MiniMuscleMap
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ProgressRing
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.enter
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldGradient
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.Surface2
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.Violet
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
        item {
            HeroBanner(program.title, program.description, "🏋️", modifier = Modifier.padding(top = 10.dp).enter(0)) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("${d.workouts.size} трен.", Gold)
                    Pill("PR: ${com.luxmaxx.app.logic.Progress.personalRecords(d)}", Green)
                }
            }
        }

        item { PlaceSelector(vm, d.trainAtHome, d.homeEquipment, Modifier.enter(1)) }

        itemsIndexed(program.days) { i, wd ->
            val isNext = wd == next
            SectionCard(modifier = Modifier.enter(i + 1)) {
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
                    MiniMuscleMap(Muscles.planHeat(wd.exercises), 64.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    wd.exercises.forEach { ex ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isNext) Gold else TextLo))
                            Spacer(Modifier.width(10.dp))
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium, color = TextHi, modifier = Modifier.weight(1f))
                            Muscles.primary(ex.name)?.let { m ->
                                Text(m.title, style = MaterialTheme.typography.labelSmall, color = TextLo.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = 8.dp))
                            }
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
    val swaps by vm.activeSwaps.collectAsStateWithLifecycle()
    var swapFor by remember { mutableStateOf<String?>(null) }
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
            ScreenHeader(dayName, overline = "🔴 Идёт тренировка") {
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
        items(exercises, key = { it.name }) { planned ->
            val ex = planned.copy(name = swaps[planned.name] ?: planned.name)
            val swapped = ex.name != planned.name
            val last = Programs.lastSets(d.workouts, ex.name)
            val done = sets.withIndex().filter { it.value.exercise == ex.name }
            var w by remember(ex.name) { mutableStateOf(last.maxOfOrNull { it.weight }?.fmt()?.replace(',', '.') ?: "") }
            var r by remember { mutableStateOf("") }
            val complete = done.size >= ex.sets
            SectionCard(ex.name, icon = if (complete) Icons.Filled.Check else Icons.Filled.FitnessCenter,
                accent = if (complete) Green else Gold) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Muscles.primary(ex.name)?.let { Pill("🎯 ${it.title}", Violet) }
                    Pill("${ex.sets} × ${ex.repMin}–${ex.repMax}", Blue)
                    Pill("${done.size}/${ex.sets} готово", if (complete) Green else TextLo)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("⇄ Заменить", Gold, Modifier.clickable { swapFor = planned.name })
                    if (swapped) Text("вместо: ${planned.name}", style = MaterialTheme.typography.labelMedium, color = TextLo, maxLines = 1)
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

    swapFor?.let { plannedName ->
        val current = swaps[plannedName] ?: plannedName
        SwapDialog(
            current = current,
            planned = plannedName,
            equipment = d.homeEquipment,
            onPick = { vm.swapExercise(plannedName, it); swapFor = null },
            onDismiss = { swapFor = null },
        )
    }
}


/** Переключатель «Зал / Дом» и выбор домашнего инвентаря. */
@Composable
private fun PlaceSelector(vm: AppViewModel, home: Boolean, equipment: Set<Equip>, modifier: Modifier = Modifier) {
    SectionCard("Где тренируешься", icon = if (home) Icons.Filled.House else Icons.Filled.FitnessCenter,
        accent = if (home) Green else Gold, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(4.dp),
        ) {
            listOf(false to "🏋️  Зал", true to "🏠  Дом").forEach { (h, label) ->
                val sel = h == home
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) GoldGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                        .clickable { vm.setTrainAtHome(h) }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) Color(0xFF1C1200) else TextLo)
                }
            }
        }
        if (home) {
            Text("Что есть дома? Без инвентаря тоже можно — подберу упражнения с весом тела.",
                style = MaterialTheme.typography.bodySmall, color = TextLo)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Equip.entries.forEach { e ->
                    GoldChip(e in equipment, { vm.toggleEquip(e) }) { Text("${e.emoji} ${e.title}") }
                }
            }
            Bullet("+1 подход в каждом упражнении и добивки в конце — дома объём важнее веса", Green)
            Bullet("Опускай вес за 3 секунды и делай паузу внизу — так даже лёгкая нагрузка растит мышцы", Green)
            Bullet("Последний подход — до отказа или за 1–2 повтора до него", Green)
        } else {
            Text("Во время тренировки любое упражнение можно заменить кнопкой «⇄ Заменить» — например, если тренажёр занят.",
                style = MaterialTheme.typography.bodySmall, color = TextLo)
        }
    }
}

/** Меню замены упражнения: домашние и зальные варианты на ту же мышцу. */
@Composable
private fun SwapDialog(current: String, planned: String, equipment: Set<Equip>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val (home, gym) = HomeWorkouts.swapOptions(current, equipment, Programs.allGymExercises())
    val muscle = Muscles.primary(current)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = {
            Column {
                Text("Замена упражнения", style = MaterialTheme.typography.titleLarge, color = TextHi)
                Text("${current}${muscle?.let { " · ${it.title}" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = TextLo)
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (current != planned) SwapOption("↩  Вернуть: $planned", "по плану", Gold) { onPick(planned) }
                if (home.isNotEmpty()) Text("🏠 ДОМА", style = MaterialTheme.typography.labelSmall, color = Green)
                home.forEach { alt ->
                    SwapOption(alt.name, (alt.equip?.let { "${it.emoji} ${it.title}" } ?: "вес тела") + " · ${alt.repMin}–${alt.repMax} повт.", Green) {
                        onPick(alt.name)
                    }
                }
                if (gym.isNotEmpty()) Text("🏋️ В ЗАЛЕ", style = MaterialTheme.typography.labelSmall, color = Gold)
                gym.forEach { g -> SwapOption(g, "зал", Gold) { onPick(g) } }
                if (home.isEmpty() && gym.isEmpty()) Text("Нет подходящих замен для этого упражнения", color = TextLo)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = Gold) } },
    )
}

@Composable
private fun SwapOption(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface2)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextHi)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextLo)
        }
        Text("⇄", color = color, style = MaterialTheme.typography.titleLarge)
    }
}
