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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.logic.Muscles
import com.luxmaxx.app.logic.Programs
import com.luxmaxx.app.logic.Progress
import com.luxmaxx.app.today
import com.luxmaxx.app.ui.components.AnimatedNumber
import com.luxmaxx.app.ui.components.ConcentricRings
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.HeatLegend
import com.luxmaxx.app.ui.components.HeroCard
import com.luxmaxx.app.ui.components.IconBadge
import com.luxmaxx.app.ui.components.LineChart
import com.luxmaxx.app.ui.components.LogoMark
import com.luxmaxx.app.ui.components.Medal
import com.luxmaxx.app.ui.components.MuscleMap
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ProgressRing
import com.luxmaxx.app.ui.components.RingSpec
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.StatTile
import com.luxmaxx.app.ui.components.Wordmark
import com.luxmaxx.app.ui.components.enter
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.theme.AppTheme
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.CardGradient
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.GoldGradient
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.OnAccent
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.Surface2
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.ThemeState
import com.luxmaxx.app.ui.theme.Track
import com.luxmaxx.app.ui.theme.Violet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel, onProfile: () -> Unit, onNavigate: (String) -> Unit) {
    val d by vm.data.collectAsStateWithLifecycle()
    val t = vm.targets(d) ?: return
    val program = vm.program(d) ?: return
    val foods = d.foods.filter { it.date == today() }
    val kcal = foods.sumOf { it.kcal }
    val prot = foods.sumOf { it.protein }
    val carbs = foods.sumOf { it.carbs }
    val weight = vm.currentWeight(d)
    val rate = Calculator.weeklyRate(d.weights)
    val next = Programs.nextDay(program, d.workouts)
    val trainedToday = d.workouts.any { it.date == today() }
    val faceToday = d.faceChecks.maxByOrNull { it.id }?.takeIf { it.date == today() }
    val monday = LocalDate.now().with(DayOfWeek.MONDAY)
    val weekWorkouts = d.workouts.count { it.date >= monday.toString() }
    val routineDone = d.faceRoutineDone[today()].orEmpty().size
    var newWeight by remember { mutableStateOf("") }
    val name = d.profile?.name?.takeIf { it.isNotBlank() }
    val ru = Locale("ru")
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ru))
    val lvl = vm.levelInfo(d)
    val streak = Progress.streak(d)
    val weekSets = Muscles.weeklySets(d.workouts)
    val heat = Muscles.heat(weekSets)
    val lagging = Muscles.lagging(weekSets)
    val ach = Progress.achievements.map { it to it.progress(d, t.protein).coerceIn(0f, 1f) }
    val unlockedCount = ach.count { it.second >= 1f }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- Шапка ----------
        item {
            Row(Modifier.padding(top = 10.dp).enter(0), verticalAlignment = Alignment.CenterVertically) {
                LogoMark(34.dp, glow = false)
                Spacer(Modifier.width(8.dp))
                Wordmark(18)
                Spacer(Modifier.weight(1f))
                // быстрое переключение темы по кругу
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Surface2).border(1.dp, Stroke, CircleShape)
                        .clickable {
                            val all = AppTheme.entries
                            vm.setTheme(all[(ThemeState.palette.theme.ordinal + 1) % all.size])
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(ThemeState.palette.theme.emoji, fontSize = 16.sp) }
                Spacer(Modifier.width(8.dp))
                if (streak > 0) Row(
                    Modifier.clip(CircleShape).background(Red.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Text(" $streak", style = MaterialTheme.typography.labelLarge, color = Red)
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                    ProgressRing(lvl.progress, size = 46.dp, thickness = 3.dp) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(GoldGradient), contentAlignment = Alignment.Center) {
                            Text((name?.first() ?: 'L').uppercase(), color = OnAccent, fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.enter(1)) {
                Text(dateStr.uppercase(ru), style = MaterialTheme.typography.labelSmall, color = Gold)
                Text(if (name != null) "Погнали, $name" else "Погнали 🔥", style = MaterialTheme.typography.headlineLarge, color = TextHi)
            }
        }

        // ---------- Уровень ----------
        item {
            Box(Modifier.enter(2).clickable { onNavigate("achievements") }) { LevelCard(lvl) }
        }

        // ---------- Неделя ----------
        item {
            WeekStrip(d.workouts.map { it.date }.toSet(),
                (d.foods.map { it.date } + d.faceChecks.map { it.date } + d.weights.map { it.date }).toSet(),
                monday, Modifier.enter(3))
        }

        // ---------- Тренировка ----------
        item {
            HeroCard(modifier = Modifier.enter(4), onClick = { onNavigate("workout") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (trainedToday) "СЕГОДНЯ" else "СЛЕДУЮЩАЯ ТРЕНИРОВКА", style = MaterialTheme.typography.labelSmall,
                            color = OnAccent.copy(alpha = 0.65f))
                        Text(if (trainedToday) "Выполнено ✓" else next.name, style = MaterialTheme.typography.headlineMedium, color = OnAccent)
                        Text(
                            if (trainedToday) "Отдыхай, ешь и спи — мышцы растут сейчас"
                            else "${next.exercises.size} упражнений · ≈${next.exercises.sumOf { it.sets } * 3} мин",
                            style = MaterialTheme.typography.bodySmall, color = OnAccent.copy(alpha = 0.75f),
                        )
                    }
                    Text(if (trainedToday) "🏆" else "🏋️", fontSize = 54.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(CircleShape).background(OnAccent).padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (trainedToday) "Дневник" else "Начать", color = Gold, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Gold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$weekWorkouts/${d.profile?.trainingDays ?: 3}", style = MaterialTheme.typography.headlineSmall, color = OnAccent)
                        Text("за неделю", style = MaterialTheme.typography.labelSmall, color = OnAccent.copy(alpha = 0.65f))
                    }
                }
            }
        }

        // ---------- Питание: тройные кольца ----------
        item {
            SectionCard("Питание сегодня", icon = Icons.Filled.Restaurant, modifier = Modifier.enter(5).clickable { onNavigate("food") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConcentricRings(
                        listOf(
                            RingSpec(kcal.toFloat() / t.kcal, listOf(GoldDeep, Gold)),
                            RingSpec(prot.toFloat() / t.protein, listOf(Green.copy(alpha = 0.6f), Green)),
                            RingSpec(carbs.toFloat() / t.carbs.coerceAtLeast(1), listOf(Blue.copy(alpha = 0.6f), Blue)),
                        ),
                        size = 140.dp, thickness = 12.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedNumber(kcal, MaterialTheme.typography.headlineSmall, TextHi)
                            Text("ККАЛ", style = MaterialTheme.typography.labelSmall, color = TextLo)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RingLegend(Gold, "Калории", kcal, t.kcal, "")
                        RingLegend(Green, "Белок", prot, t.protein, "г")
                        RingLegend(Blue, "Углеводы", carbs, t.carbs, "г")
                    }
                }
            }
        }

        // ---------- Статистика ----------
        item {
            Row(Modifier.enter(6), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Вес", weight.fmt(), Modifier.weight(1f), "кг")
                StatTile(
                    "Темп", rate?.let { (if (it >= 0) "+" else "") + "%.2f".format(it) } ?: "—", Modifier.weight(1f), "кг/н",
                    accent = rate.let { r ->
                        if (r == null) TextLo else if ((r / weight * 100) in 0.1..0.6) Green else Red
                    },
                )
                StatTile("Трен.", "${d.workouts.size}", Modifier.weight(1f), accent = Blue)
            }
        }

        // ---------- Карта мышц ----------
        item {
            SectionCard("Прокачка за неделю", icon = Icons.Filled.Accessibility, accent = GoldDeep, modifier = Modifier.enter(7)) {
                MuscleMap(heat, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { HeatLegend() }
                if (d.workouts.isEmpty()) Text("Начни тренироваться — мышцы на силуэте начнут светиться.",
                    style = MaterialTheme.typography.bodySmall, color = TextLo)
                else if (lagging.isNotEmpty()) Text("Отстают на этой неделе: ${lagging.joinToString { it.title.lowercase() }}",
                    style = MaterialTheme.typography.bodySmall, color = Gold)
                else Text("Все основные группы получили норму — красавчик 🔥", style = MaterialTheme.typography.bodySmall, color = Green)
            }
        }

        // ---------- Ачивки ----------
        item {
            SectionCard("Достижения · $unlockedCount/${ach.size}", icon = Icons.Filled.EmojiEvents, modifier = Modifier.enter(8)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ach.sortedByDescending { it.second }.take(10)) { (a, p) ->
                        Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Medal(a.emoji, Color(a.color), p >= 1f, 64.dp, p)
                            Text(a.title, style = MaterialTheme.typography.labelMedium, color = if (p >= 1f) TextHi else TextLo,
                                maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
                Text("Все достижения →", style = MaterialTheme.typography.labelLarge, color = Gold,
                    modifier = Modifier.clickable { onNavigate("achievements") })
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

        // ---------- Образ ----------
        item {
            SectionCard("Образ", icon = Icons.Filled.Face, accent = Violet, modifier = Modifier.clickable { onNavigate("face") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(routineDone / 8f, size = 72.dp, thickness = 8.dp, colors = listOf(Violet, Blue)) {
                        Text("$routineDone/8", style = MaterialTheme.typography.titleMedium, color = TextHi)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        if (faceToday != null) {
                            Text("Лицо отмечено ✓", style = MaterialTheme.typography.titleSmall, color = Green)
                            Text("Кожа ${faceToday.skin}/10 · Высыпания ${faceToday.acne} · Отёки ${faceToday.puffiness}",
                                style = MaterialTheme.typography.bodySmall, color = TextLo)
                        } else {
                            Text("Сделай фото дня 📸", style = MaterialTheme.typography.titleSmall, color = TextHi)
                            Text("Утром, при одном свете — так видна реальная динамика",
                                style = MaterialTheme.typography.bodySmall, color = TextLo)
                        }
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("💈 Волосы", Violet); Pill("🧥 Стиль", Blue)
                            if (d.looks.idolName.isNotBlank()) Pill("⭐ ${d.looks.idolName.take(12)}", Gold)
                        }
                    }
                }
            }
        }

        // ---------- Правила ----------
        item {
            SectionCard("Правила роста", icon = Icons.AutoMirrored.Filled.TrendingUp, accent = Green) {
                Rule(Icons.Filled.Restaurant, Green, "Белок ${t.protein} г", "каждый день — главный строительный материал")
                Rule(Icons.AutoMirrored.Filled.TrendingUp, Gold, "Прогрессия нагрузки", "каждую неделю +1 повтор или +2.5 кг")
                Rule(Icons.Filled.Bedtime, Violet, "Сон 7–9 часов", "рост мышц идёт во время восстановления")
                Rule(Icons.Filled.WaterDrop, Blue, "Вода ≈ ${t.waterL} л", "меньше отёков на лице по утрам")
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun RingLegend(color: Color, label: String, value: Int, target: Int, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextLo)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$value", style = MaterialTheme.typography.titleMedium, color = TextHi)
                Text(" / $target$unit", style = MaterialTheme.typography.bodySmall, color = TextLo)
            }
        }
    }
}

@Composable
private fun WeekStrip(trained: Set<String>, active: Set<String>, monday: LocalDate, modifier: Modifier = Modifier) {
    val names = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
    val todayD = LocalDate.now()
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardGradient)
            .border(1.dp, BorderGradient, RoundedCornerShape(22.dp)).padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (i in 0 until 7) {
            val day = monday.plusDays(i.toLong())
            val ds = day.toString()
            val isToday = day == todayD
            val did = ds in trained
            val any = ds in active
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(names[i], style = MaterialTheme.typography.labelSmall, color = if (isToday) Gold else TextLo)
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(
                            when {
                                did -> Brush.linearGradient(listOf(Gold, GoldDeep))
                                any -> Brush.linearGradient(listOf(Gold.copy(alpha = 0.16f), Gold.copy(alpha = 0.16f)))
                                else -> Brush.linearGradient(listOf(Surface1, Surface1))
                            }
                        )
                        .border(if (isToday) 2.dp else 1.dp, if (isToday) Gold else Track, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        did -> Text("💪", fontSize = 15.sp)
                        any -> Box(Modifier.size(6.dp).clip(CircleShape).background(Gold))
                        else -> Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelMedium,
                            color = if (day.isAfter(todayD)) TextLo.copy(alpha = 0.5f) else TextLo)
                    }
                }
            }
        }
    }
}

@Composable
private fun Rule(icon: ImageVector, color: Color, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, color, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextHi)
            Text(text, style = MaterialTheme.typography.bodySmall, color = TextLo)
        }
    }
}
