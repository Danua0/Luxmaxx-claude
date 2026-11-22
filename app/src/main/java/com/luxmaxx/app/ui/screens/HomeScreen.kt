package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.today
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.HeroCard
import com.luxmaxx.app.ui.components.IconBadge
import com.luxmaxx.app.ui.components.LineChart
import com.luxmaxx.app.ui.components.MacroBar
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ProgressRing
import com.luxmaxx.app.ui.components.RingLabel
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.StatTile
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldGradient
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.Violet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Dark = Color(0xFF1C1200)

@Composable
fun HomeScreen(vm: AppViewModel, onProfile: () -> Unit, onNavigate: (String) -> Unit) {
    val d by vm.data.collectAsStateWithLifecycle()
    val t = vm.targets(d) ?: return
    val program = vm.program(d) ?: return
    val foods = d.foods.filter { it.date == today() }
    val kcal = foods.sumOf { it.kcal }
    val weight = vm.currentWeight(d)
    val rate = Calculator.weeklyRate(d.weights)
    val next = Programs.nextDay(program, d.workouts)
    val trainedToday = d.workouts.any { it.date == today() }
    val faceToday = d.faceChecks.maxByOrNull { it.id }?.takeIf { it.date == today() }
    val monday = LocalDate.now().with(DayOfWeek.MONDAY).toString()
    val weekWorkouts = d.workouts.count { it.date >= monday }
    val routineDone = d.faceRoutineDone[today()].orEmpty().size
    var newWeight by remember { mutableStateOf("") }
    val name = d.profile?.name?.takeIf { it.isNotBlank() }
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = if (name != null) "Привет, $name" else "Привет 👋",
                overline = dateStr,
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(GoldGradient).clickable(onClick = onProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    Text((name?.first() ?: 'L').uppercase(), color = Dark, fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // ---------- Тренировка ----------
        item {
            HeroCard(onClick = { onNavigate("workout") }) {
                Text(if (trainedToday) "СЕГОДНЯ" else "СЛЕДУЮЩАЯ ТРЕНИРОВКА", style = MaterialTheme.typography.labelSmall,
                    color = Dark.copy(alpha = 0.7f))
                Text(if (trainedToday) "Тренировка выполнена ✓" else next.name,
                    style = MaterialTheme.typography.headlineMedium, color = Dark)
                Text(
                    if (trainedToday) "Отдыхай, ешь и спи — мышцы растут сейчас."
                    else "${next.exercises.size} упражнений · ≈ ${next.exercises.sumOf { it.sets } * 3} мин · ${program.title}",
                    style = MaterialTheme.typography.bodyMedium, color = Dark.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(CircleShape).background(Dark).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (trainedToday) "Открыть дневник" else "Начать", color = Gold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Gold, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$weekWorkouts / ${d.profile?.trainingDays ?: 3}", style = MaterialTheme.typography.headlineSmall, color = Dark)
                    Text("  за неделю", style = MaterialTheme.typography.bodySmall, color = Dark.copy(alpha = 0.7f))
                }
            }
        }

        // ---------- Питание ----------
        item {
            SectionCard("Питание сегодня", icon = Icons.Filled.Restaurant, modifier = Modifier.clickable { onNavigate("food") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(kcal.toFloat() / t.kcal, size = 128.dp, thickness = 13.dp) {
                        RingLabel("$kcal", "из ${t.kcal} ккал", color = Gold)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MacroBar("Белок", foods.sumOf { it.protein }, t.protein, "г", Green)
                        MacroBar("Углеводы", foods.sumOf { it.carbs }, t.carbs, "г", Blue)
                        MacroBar("Жиры", foods.sumOf { it.fat }, t.fat, "г", Violet)
                    }
                }
                val left = t.kcal - kcal
                Pill(if (left > 0) "Осталось $left ккал" else "Норма выполнена 👌", if (left > 0) Gold else Green)
            }
        }

        // ---------- Статистика ----------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Вес", weight.fmt(), Modifier.weight(1f), "кг")
                StatTile(
                    "Темп", rate?.let { (if (it >= 0) "+" else "") + "%.2f".format(it) } ?: "—", Modifier.weight(1f), "кг/нед",
                    accent = when {
                        rate == null -> TextLo
                        (rate / weight * 100) in 0.1..0.6 -> Green
                        else -> Red
                    },
                )
                StatTile("Всего", "${d.workouts.size}", Modifier.weight(1f), "трен.", accent = Blue)
            }
        }

        // ---------- Вес ----------
        item {
            SectionCard("Динамика веса", icon = Icons.Filled.MonitorWeight, accent = Blue) {
                LineChart(d.weights.sortedBy { it.date }.takeLast(30).map { it.kg }, color = Blue)
                Text(Calculator.rateAdvice(rate, weight), style = MaterialTheme.typography.bodySmall, color = TextLo)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(newWeight, { newWeight = it }, "Вес сегодня, кг", Modifier.weight(1f))
                    GradientButton("Записать", onClick = {
                        newWeight.toDoubleOrNull()?.takeIf { it in 30.0..250.0 }?.let { vm.addWeight(it); newWeight = "" }
                    })
                }
            }
        }

        // ---------- Лицо ----------
        item {
            SectionCard("Лицо", icon = Icons.Filled.Face, accent = Violet, modifier = Modifier.clickable { onNavigate("face") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(routineDone / 8f, size = 72.dp, thickness = 8.dp, colors = listOf(Violet, Blue)) {
                        Text("$routineDone/8", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        if (faceToday != null) {
                            Text("Отмечено сегодня", style = MaterialTheme.typography.titleSmall, color = Green)
                            Text("Кожа ${faceToday.skin}/10 · Высыпания ${faceToday.acne} · Отёки ${faceToday.puffiness}",
                                style = MaterialTheme.typography.bodySmall, color = TextLo)
                        } else {
                            Text("Сделай фото дня", style = MaterialTheme.typography.titleSmall, color = TextHi)
                            Text("Утром, при одном и том же свете — так видна реальная динамика.",
                                style = MaterialTheme.typography.bodySmall, color = TextLo)
                        }
                        Text("Уход за кожей сегодня", style = MaterialTheme.typography.labelMedium, color = Violet,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // ---------- Правила ----------
        item {
            SectionCard("Правила роста", icon = Icons.Filled.TrendingUp, accent = Green) {
                Rule(Icons.Filled.Restaurant, Green, "Белок ${t.protein} г", "каждый день — главный строительный материал")
                Rule(Icons.Filled.TrendingUp, Gold, "Прогрессия нагрузки", "каждую неделю +1 повтор или +2.5 кг")
                Rule(Icons.Filled.Bedtime, Violet, "Сон 7–9 часов", "рост мышц идёт во время восстановления")
                Rule(Icons.Filled.WaterDrop, Blue, "Вода ≈ ${t.waterL} л", "меньше отёков на лице по утрам")
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Rule(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, color, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextHi)
            Text(text, style = MaterialTheme.typography.bodySmall, color = TextLo)
        }
    }
}
