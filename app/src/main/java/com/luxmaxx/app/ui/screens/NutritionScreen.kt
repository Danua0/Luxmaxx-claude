package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.luxmaxx.app.ai.EstimatedFood
import com.luxmaxx.app.today
import com.luxmaxx.app.ui.components.MacroBar
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red

private data class Preset(val name: String, val kcal: Int, val p: Int, val c: Int, val f: Int)

private val presets = listOf(
    Preset("Куриная грудка 150 г", 165, 35, 0, 3),
    Preset("3 яйца", 210, 18, 1, 15),
    Preset("Рис 100 г (сух.)", 345, 7, 76, 1),
    Preset("Гречка 100 г (сух.)", 330, 13, 68, 3),
    Preset("Овсянка 80 г", 300, 10, 52, 5),
    Preset("Творог 5% 200 г", 240, 34, 6, 10),
    Preset("Протеин 1 скуп", 120, 24, 3, 1),
    Preset("Банан", 105, 1, 27, 0),
    Preset("Молоко 2.5% 250 мл", 130, 7, 12, 6),
    Preset("Арахисовая паста 30 г", 180, 7, 6, 15),
    Preset("Говядина 150 г", 280, 39, 0, 14),
    Preset("Макароны 100 г (сух.)", 350, 12, 70, 2),
)

@Composable
fun NutritionScreen(vm: AppViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val busy by vm.aiBusy.collectAsStateWithLifecycle()
    val t = vm.targets(d) ?: return
    val foods = d.foods.filter { it.date == today() }
    val kcal = foods.sumOf { it.kcal }; val prot = foods.sumOf { it.protein }

    var name by remember { mutableStateOf("") }
    var k by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var aiText by remember { mutableStateOf("") }
    var estimated by remember { mutableStateOf<List<EstimatedFood>>(emptyList()) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Питание", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp))
        }
        item {
            SectionCard("Сегодня") {
                MacroBar("Калории", kcal, t.kcal, "ккал")
                MacroBar("Белок", prot, t.protein, "г", Green)
                MacroBar("Углеводы", foods.sumOf { it.carbs }, t.carbs, "г", Blue)
                MacroBar("Жиры", foods.sumOf { it.fat }, t.fat, "г", Red)
                val left = t.kcal - kcal
                Text(
                    when {
                        left > 300 -> "Осталось $left ккал и ${(t.protein - prot).coerceAtLeast(0)} г белка. Без профицита мышцы не растут!"
                        left > -200 -> "Норма выполнена 👌"
                        else -> "Перебор на ${-left} ккал — не страшно разово, но следи за темпом набора."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SectionCard("🤖 Опиши, что съел — AI посчитает") {
                OutlinedTextField(aiText, { aiText = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Напр.: тарелка борща, 2 куска хлеба и чай с сахаром") })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy && aiText.isNotBlank(), onClick = {
                        vm.estimateFood(aiText) { estimated = it }
                    }) { Text("Посчитать") }
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                }
                estimated.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.SemiBold)
                            Text("${e.kcal} ккал · Б${e.protein} Ж${e.fat} У${e.carbs}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (estimated.isNotEmpty()) FilledTonalButton(onClick = {
                    estimated.forEach { vm.addFood(it.name, it.kcal, it.protein, it.carbs, it.fat) }
                    estimated = emptyList(); aiText = ""
                }) { Text("Добавить всё") }
            }
        }
        item {
            SectionCard("Быстро добавить") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { pr ->
                        AssistChip(onClick = { vm.addFood(pr.name, pr.kcal, pr.p, pr.c, pr.f) }, label = { Text(pr.name) })
                    }
                }
            }
        }
        item {
            SectionCard("Своё блюдо") {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField(k, { k = it }, "Ккал", Modifier.weight(1f), decimal = false)
                    NumberField(p, { p = it }, "Б", Modifier.weight(1f), decimal = false)
                    NumberField(f, { f = it }, "Ж", Modifier.weight(1f), decimal = false)
                    NumberField(c, { c = it }, "У", Modifier.weight(1f), decimal = false)
                }
                Button(enabled = k.toIntOrNull() != null, onClick = {
                    vm.addFood(name.ifBlank { "Еда" }, k.toInt(), p.toIntOrNull() ?: 0, c.toIntOrNull() ?: 0, f.toIntOrNull() ?: 0)
                    name = ""; k = ""; p = ""; c = ""; f = ""
                }) { Text("Добавить") }
            }
        }
        if (foods.isNotEmpty()) item { Text("Съедено сегодня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(foods.reversed(), key = { it.id }) { fe ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fe.name)
                    Text("${fe.kcal} ккал · Б${fe.protein} Ж${fe.fat} У${fe.carbs}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.deleteFood(fe.id) }) { Icon(Icons.Filled.Delete, "Удалить") }
            }
        }
        item {
            SectionCard("Советы для набора") {
                Text("• Белок на каждый приём пищи: 30–40 г, 4–5 раз в день.")
                Text("• Не лезет еда? Пей калории: молоко, банан, овсянка, арахисовая паста в блендере = 800+ ккал.")
                Text("• Креатин моногидрат 3–5 г в день — самая изученная и безопасная добавка для силы и массы.")
                Text("• Меньше соли и сахара вечером — утром лицо будет менее отёчным.")
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }
}
