package com.luxmaxx.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.data.Activity
import com.luxmaxx.app.data.Experience
import com.luxmaxx.app.data.Profile
import com.luxmaxx.app.data.Sex
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.ui.components.GoldChip
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.LogoMark
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.StatTile
import com.luxmaxx.app.ui.components.ThemePicker
import com.luxmaxx.app.ui.components.Wordmark
import com.luxmaxx.app.ui.components.enter
import com.luxmaxx.app.ui.components.fmt
import com.luxmaxx.app.ui.components.luxFieldColors
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.ThemeState
import com.luxmaxx.app.ui.theme.Violet

@Composable
fun ProfileScreen(vm: AppViewModel, modifier: Modifier = Modifier, firstRun: Boolean, onDone: () -> Unit, onAchievements: () -> Unit = {}) {
    val existing = vm.data.value.profile ?: Profile()
    var name by remember { mutableStateOf(existing.name) }
    var sex by remember { mutableStateOf(existing.sex) }
    var age by remember { mutableStateOf(existing.age.toString()) }
    var height by remember { mutableStateOf(existing.heightCm.toString()) }
    var weight by remember { mutableStateOf(vm.currentWeight().fmt().replace(',', '.')) }
    var activity by remember { mutableStateOf(existing.activity) }
    var days by remember { mutableIntStateOf(existing.trainingDays) }
    var exp by remember { mutableStateOf(existing.experience) }
    var key by remember { mutableStateOf(vm.data.value.apiKey) }
    var textModel by remember { mutableStateOf(vm.data.value.textModel) }
    var visionModel by remember { mutableStateOf(vm.data.value.visionModel) }
    val ctx = LocalContext.current

    val parsed = run {
        val a = age.toIntOrNull(); val h = height.toIntOrNull(); val w = weight.toDoubleOrNull()
        if (a != null && h != null && w != null && a in 12..90 && h in 120..230 && w in 30.0..250.0)
            Profile(name.trim(), sex, a, h, w, activity, days, exp) else null
    }

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (firstRun) Column(
                Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp).enter(0),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LogoMark(110.dp, spin = true)
                Spacer(Modifier.height(18.dp))
                Wordmark(30)
                Spacer(Modifier.height(8.dp))
                Text("Масса. Лицо. Стиль. Один AI-тренер.", style = MaterialTheme.typography.bodyMedium, color = TextLo)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("💪 Программа", Gold); Pill("🥩 КБЖУ", Green); Pill("✨ Образ", Violet)
                }
                Spacer(Modifier.height(10.dp))
                Text("Заполни анкету — рассчитаю калории, белок и подберу программу.",
                    style = MaterialTheme.typography.bodySmall, color = TextLo, textAlign = TextAlign.Center)
            } else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = TextHi) }
                ScreenHeader("Профиль", overline = "LuxMaxx")
            }
        }
        if (!firstRun) item {
            val lvl = vm.levelInfo()
            Box(Modifier.clickable(onClick = onAchievements)) { LevelCard(lvl) }
        }
        if (!firstRun) item {
            GradientButton("🏆  Все достижения", onClick = onAchievements, modifier = Modifier.fillMaxWidth())
        }
        item {
            SectionCard("Оформление", icon = Icons.Filled.Palette, accent = Violet) {
                Text("Тема применяется сразу — попробуй все.", style = MaterialTheme.typography.bodySmall, color = TextLo)
                ThemePicker(ThemeState.palette.theme) { vm.setTheme(it) }
            }
        }
        item {
            SectionCard("О тебе", icon = Icons.Filled.Person) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = luxFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldChip(sex == Sex.MALE, { sex = Sex.MALE }, { Text("Мужчина") })
                    GoldChip(sex == Sex.FEMALE, { sex = Sex.FEMALE }, { Text("Женщина") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(age, { age = it }, "Возраст", Modifier.weight(1f), decimal = false)
                    NumberField(height, { height = it }, "Рост, см", Modifier.weight(1f), decimal = false)
                    NumberField(weight, { weight = it }, "Вес, кг", Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard("Активность вне зала", icon = Icons.AutoMirrored.Filled.DirectionsWalk, accent = Blue) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Activity.entries.forEach { a -> GoldChip(activity == a, { activity = a }, { Text(a.title) }) }
                }
            }
        }
        item {
            SectionCard("Тренировки", icon = Icons.Filled.FitnessCenter, accent = Green) {
                Text("Сколько дней в неделю готов ходить в зал?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { d -> GoldChip(days == d, { days = d }, { Text("$d") }) }
                }
                Text("Опыт")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Experience.entries.forEach { e -> GoldChip(exp == e, { exp = e }, { Text(e.title) }) }
                }
            }
        }
        item {
            SectionCard("DeepSeek AI (необязательно)", icon = Icons.Filled.AutoAwesome, accent = Violet) {
                Text("Ключ нужен для AI-тренера, анализа динамики лица и подсчёта калорий по описанию. " +
                    "Получить: platform.deepseek.com → API keys. Ключ хранится только на телефоне.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(key, { key = it }, label = { Text("API-ключ sk-...") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = luxFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(textModel, { textModel = it }, label = { Text("Модель для чата") },
                        singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = luxFieldColors())
                    OutlinedTextField(visionModel, { visionModel = it }, label = { Text("Модель для фото") },
                        singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = luxFieldColors())
                }
                Text("Если DeepSeek переименует модели — просто впиши новое название из их документации.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.deepseek.com/api_keys")))
                }) { Text("Открыть сайт DeepSeek") }
            }
        }
        if (parsed != null) item {
            val t = Calculator.targets(parsed)
            SectionCard("Твои цели для набора", icon = Icons.Filled.TrackChanges) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Калории", "${t.kcal}", Modifier.weight(1f), "ккал", hint = "поддержка ${t.tdee}")
                    StatTile("Белок", "${t.protein}", Modifier.weight(1f), "г", accent = Green)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Углеводы", "${t.carbs}", Modifier.weight(1f), "г", accent = Blue)
                    StatTile("Жиры", "${t.fat}", Modifier.weight(1f), "г", accent = Violet)
                }
                Text("Вода ≈ ${t.waterL} л · ИМТ ${"%.1f".format(Calculator.bmi(parsed, parsed.weightKg))}",
                    style = MaterialTheme.typography.bodySmall, color = TextLo)
            }
        }
        item {
            GradientButton(
                if (firstRun) "Начать" else "Сохранить",
                onClick = {
                    parsed?.let { vm.saveProfile(it) }
                    vm.setApiKey(key)
                    vm.setModels(textModel, visionModel)
                    onDone()
                },
                enabled = parsed != null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }
}
