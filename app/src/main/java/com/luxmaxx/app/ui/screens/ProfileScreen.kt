package com.luxmaxx.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.data.Activity
import com.luxmaxx.app.data.Experience
import com.luxmaxx.app.data.Profile
import com.luxmaxx.app.data.Sex
import com.luxmaxx.app.logic.Calculator
import com.luxmaxx.app.ui.components.NumberField
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.fmt

@Composable
fun ProfileScreen(vm: AppViewModel, modifier: Modifier = Modifier, firstRun: Boolean, onDone: () -> Unit) {
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                if (!firstRun) IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                Text(if (firstRun) "Добро пожаловать в LuxMaxx 👋" else "Профиль",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (firstRun) Text("Заполни анкету — я рассчитаю калории, белок и подберу программу тренировок.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionCard("О тебе") {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(sex == Sex.MALE, { sex = Sex.MALE }, { Text("Мужчина") })
                    FilterChip(sex == Sex.FEMALE, { sex = Sex.FEMALE }, { Text("Женщина") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(age, { age = it }, "Возраст", Modifier.weight(1f), decimal = false)
                    NumberField(height, { height = it }, "Рост, см", Modifier.weight(1f), decimal = false)
                    NumberField(weight, { weight = it }, "Вес, кг", Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard("Активность вне зала") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Activity.entries.forEach { a -> FilterChip(activity == a, { activity = a }, { Text(a.title) }) }
                }
            }
        }
        item {
            SectionCard("Тренировки") {
                Text("Сколько дней в неделю готов ходить в зал?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { d -> FilterChip(days == d, { days = d }, { Text("$d") }) }
                }
                Text("Опыт")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Experience.entries.forEach { e -> FilterChip(exp == e, { exp = e }, { Text(e.title) }) }
                }
            }
        }
        item {
            SectionCard("DeepSeek AI (необязательно)") {
                Text("Ключ нужен для AI-тренера, анализа динамики лица и подсчёта калорий по описанию. " +
                    "Получить: platform.deepseek.com → API keys. Ключ хранится только на телефоне.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(key, { key = it }, label = { Text("sk-...") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(textModel, { textModel = it }, label = { Text("Модель для чата") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(visionModel, { visionModel = it }, label = { Text("Модель для фото") },
                        singleLine = true, modifier = Modifier.weight(1f))
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
            SectionCard("Твои цели для набора массы") {
                Text("Калории: ${t.kcal} ккал (поддержка ≈ ${t.tdee})")
                Text("Белок: ${t.protein} г · Жиры: ${t.fat} г · Углеводы: ${t.carbs} г")
                Text("Вода: ≈ ${t.waterL} л · ИМТ: ${"%.1f".format(Calculator.bmi(parsed, parsed.weightKg))}")
            }
        }
        item {
            Button(
                onClick = {
                    parsed?.let { vm.saveProfile(it) }
                    vm.setApiKey(key)
                    vm.setModels(textModel, visionModel)
                    onDone()
                },
                enabled = parsed != null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) { Text(if (firstRun) "Начать" else "Сохранить") }
        }
    }
}
