package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.ai.EstimatedFood
import com.luxmaxx.app.today
import com.luxmaxx.app.ui.components.AnimatedNumber
import com.luxmaxx.app.ui.components.BarChart
import com.luxmaxx.app.ui.components.Bullet
import com.luxmaxx.app.ui.components.ConcentricRings
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.HeroBanner
import com.luxmaxx.app.ui.components.MacroBar
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ProgressRing
import com.luxmaxx.app.ui.components.RingLabel
import com.luxmaxx.app.ui.components.RingSpec
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.enter
import com.luxmaxx.app.ui.components.luxFieldColors
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.Violet

private data class Preset(val name: String, val kcal: Int, val p: Int, val c: Int, val f: Int, val emoji: String = "🍽")

private val presets = listOf(
    Preset("Куриная грудка 150 г", 165, 35, 0, 3, "🍗"),
    Preset("3 яйца", 210, 18, 1, 15, "🥚"),
    Preset("Рис 100 г (сух.)", 345, 7, 76, 1, "🍚"),
    Preset("Гречка 100 г (сух.)", 330, 13, 68, 3, "🌾"),
    Preset("Овсянка 80 г", 300, 10, 52, 5, "🥣"),
    Preset("Творог 5% 200 г", 240, 34, 6, 10, "🧀"),
    Preset("Протеин 1 скуп", 120, 24, 3, 1, "🥤"),
    Preset("Банан", 105, 1, 27, 0, "🍌"),
    Preset("Молоко 2.5% 250 мл", 130, 7, 12, 6, "🥛"),
    Preset("Арахисовая паста 30 г", 180, 7, 6, 15, "🥜"),
    Preset("Говядина 150 г", 280, 39, 0, 14, "🥩"),
    Preset("Макароны 100 г (сух.)", 350, 12, 70, 2, "🍝"),
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
            HeroBanner("Питание", "Цель: ${t.kcal} ккал · ${t.protein} г белка в день", "🥩", accent = Green, modifier = Modifier.padding(top = 10.dp).enter(0))
        }
        item {
            SectionCard(modifier = Modifier.enter(1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConcentricRings(
                        listOf(
                            RingSpec(kcal.toFloat() / t.kcal, listOf(GoldDeep, Gold)),
                            RingSpec(prot.toFloat() / t.protein, listOf(Green.copy(alpha = 0.6f), Green)),
                            RingSpec(foods.sumOf { it.carbs }.toFloat() / t.carbs.coerceAtLeast(1), listOf(Blue.copy(alpha = 0.6f), Blue)),
                            RingSpec(foods.sumOf { it.fat }.toFloat() / t.fat.coerceAtLeast(1), listOf(Violet.copy(alpha = 0.6f), Violet)),
                        ),
                        size = 140.dp, thickness = 10.dp, gap = 3.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedNumber(kcal, MaterialTheme.typography.titleLarge, Gold)
                            Text("из ${t.kcal}", style = MaterialTheme.typography.labelSmall, color = TextLo)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MacroBar("Белок", prot, t.protein, "г", Green)
                        MacroBar("Углеводы", foods.sumOf { it.carbs }, t.carbs, "г", Blue)
                        MacroBar("Жиры", foods.sumOf { it.fat }, t.fat, "г", Violet)
                    }
                }
                val left = t.kcal - kcal
                Text(
                    when {
                        left > 300 -> "Осталось $left ккал и ${(t.protein - prot).coerceAtLeast(0)} г белка. Без профицита мышцы не растут!"
                        left > -200 -> "Норма выполнена 👌"
                        else -> "Перебор на ${-left} ккал — не страшно разово, но следи за темпом набора."
                    },
                    style = MaterialTheme.typography.bodySmall, color = TextLo,
                )
            }
        }
        item {
            val days = (6 downTo 0).map { java.time.LocalDate.now().minusDays(it.toLong()) }
            val wd = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
            SectionCard("Калории за неделю", icon = Icons.Filled.BarChart, accent = Gold, modifier = Modifier.enter(2)) {
                BarChart(
                    days.map { day -> d.foods.filter { it.date == day.toString() }.sumOf { it.kcal } },
                    days.map { wd[it.dayOfWeek.value - 1] }, t.kcal,
                )
                Text("Пунктир — цель. Зелёные столбики — норма выполнена.", style = MaterialTheme.typography.bodySmall, color = TextLo)
            }
        }
        item {
            SectionCard("Опиши, что съел — AI посчитает", icon = Icons.Filled.AutoAwesome, accent = Violet) {
                OutlinedTextField(aiText, { aiText = it }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = luxFieldColors(),
                    label = { Text("Напр.: тарелка борща, 2 куска хлеба и чай с сахаром") })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton(if (busy) "Считаю…" else "Посчитать", enabled = !busy && aiText.isNotBlank(), onClick = {
                        vm.estimateFood(aiText) { estimated = it }
                    }, icon = Icons.Filled.AutoAwesome)
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
            SectionCard("Быстро добавить", icon = Icons.Filled.Bolt) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { pr ->
                        Column(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(Surface1)
                                .border(1.dp, Stroke, RoundedCornerShape(14.dp))
                                .clickable { vm.addFood(pr.name, pr.kcal, pr.p, pr.c, pr.f); vm.message.value = "+ ${pr.name}" }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("${pr.emoji}  ${pr.name}", style = MaterialTheme.typography.labelLarge, color = TextHi)
                            Text("${pr.kcal} ккал · Б${pr.p}", style = MaterialTheme.typography.labelMedium, color = TextLo)
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Своё блюдо", icon = Icons.Filled.Edit, accent = Blue) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = luxFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField(k, { k = it }, "Ккал", Modifier.weight(1f), decimal = false)
                    NumberField(p, { p = it }, "Б", Modifier.weight(1f), decimal = false)
                    NumberField(f, { f = it }, "Ж", Modifier.weight(1f), decimal = false)
                    NumberField(c, { c = it }, "У", Modifier.weight(1f), decimal = false)
                }
                GradientButton("Добавить", enabled = k.toIntOrNull() != null, onClick = {
                    vm.addFood(name.ifBlank { "Еда" }, k.toInt(), p.toIntOrNull() ?: 0, c.toIntOrNull() ?: 0, f.toIntOrNull() ?: 0)
                    name = ""; k = ""; p = ""; c = ""; f = ""
                }, modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
            }
        }
        if (foods.isNotEmpty()) item {
            SectionCard("Съедено сегодня", icon = Icons.Filled.Restaurant, accent = Green) {
                foods.reversed().forEach { fe ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fe.name, style = MaterialTheme.typography.bodyMedium, color = TextHi)
                            Text("Б${fe.protein} · Ж${fe.fat} · У${fe.carbs}", style = MaterialTheme.typography.bodySmall, color = TextLo)
                        }
                        Pill("${fe.kcal} ккал")
                        IconButton(onClick = { vm.deleteFood(fe.id) }) { Icon(Icons.Filled.Delete, "Удалить", tint = TextLo) }
                    }
                }
            }
        }
        item {
            SectionCard("Советы для набора", icon = Icons.Filled.Lightbulb, accent = Blue) {
                Bullet("Белок на каждый приём пищи: 30–40 г, 4–5 раз в день.", Blue)
                Bullet("Не лезет еда? Пей калории: молоко, банан, овсянка, арахисовая паста в блендере = 800+ ккал.", Blue)
                Bullet("Креатин моногидрат 3–5 г в день — самая изученная и безопасная добавка для силы и массы.", Blue)
                Bullet("Меньше соли и сахара вечером — утром лицо будет менее отёчным.", Blue)
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }
}
