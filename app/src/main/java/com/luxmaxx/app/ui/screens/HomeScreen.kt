package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.today
import com.luxmaxx.app.ui.components.LineChart
import com.luxmaxx.app.ui.components.MacroBar
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Green

@Composable
fun HomeScreen(vm: AppViewModel, onProfile: () -> Unit, onNavigate: (String) -> Unit) {
    val d by vm.data.collectAsStateWithLifecycle()
    val t = vm.targets(d) ?: return
    val program = vm.program(d) ?: return
    val foods = d.foods.filter { it.date == today() }
    val weight = vm.currentWeight(d)
    val rate = Calculator.weeklyRate(d.weights)
    val next = Programs.nextDay(program, d.workouts)
    val trainedToday = d.workouts.any { it.date == today() }
    val faceToday = d.faceChecks.any { it.date == today() }
    var newWeight by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Привет${d.profile?.name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}! 💪",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onProfile) { Icon(Icons.Filled.Person, "Профиль") }
            }
        }
        item {
            SectionCard("Сегодня") {
                MacroBar("Калории", foods.sumOf { it.kcal }, t.kcal, "ккал")
                MacroBar("Белок", foods.sumOf { it.protein }, t.protein, "г", Green)
                MacroBar("Углеводы", foods.sumOf { it.carbs }, t.carbs, "г", Blue)
                OutlinedButton(onClick = { onNavigate("food") }) { Text("Добавить еду") }
            }
        }
        item {
            SectionCard(if (trainedToday) "✅ Тренировка сегодня выполнена" else "Следующая тренировка") {
                Text("${program.title}: ${next.name}", fontWeight = FontWeight.SemiBold)
                Text(next.exercises.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Всего тренировок: ${d.workouts.size}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onNavigate("workout") }) { Text("В зал") }
            }
        }
        item {
            SectionCard("Вес: ${weight.fmt()} кг") {
                LineChart(d.weights.sortedBy { it.date }.takeLast(30).map { it.kg })
                Text(
                    (rate?.let { "Темп: ${if (it >= 0) "+" else ""}${"%.2f".format(it)} кг/нед. " } ?: "") +
                        Calculator.rateAdvice(rate, weight),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(newWeight, { newWeight = it }, "Вес сегодня", Modifier.weight(1f))
                    Button(onClick = {
                        newWeight.toDoubleOrNull()?.takeIf { it in 30.0..250.0 }?.let { vm.addWeight(it); newWeight = "" }
                    }) { Text("Записать") }
                }
            }
        }
        item {
            SectionCard(if (faceToday) "✅ Лицо сегодня отмечено" else "Лицо") {
                Text("Делай фото при одинаковом свете утром — так видна реальная динамика отёков и кожи.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { onNavigate("face") }) { Text(if (faceToday) "Открыть" else "Отметить состояние") }
            }
        }
        item {
            SectionCard("Ключевые правила роста") {
                listOf(
                    "🍗 Белок ${t.protein} г каждый день — главное для мышц",
                    "📈 Прогрессия: каждую неделю +1 повтор или +2.5 кг",
                    "😴 Сон 7–9 часов — рост идёт во время восстановления",
                    "⚖️ Набирай 0.25–0.5% веса в неделю, не быстрее",
                    "💧 Вода ≈ ${t.waterL} л — меньше отёков на лице",
                ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }
}
