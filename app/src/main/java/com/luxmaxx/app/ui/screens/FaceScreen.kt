package com.luxmaxx.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.ai.FaceAiResult
import com.luxmaxx.app.data.FaceCheck
import com.luxmaxx.app.data.FaceMetrics
import com.luxmaxx.app.face.FaceResult
import com.luxmaxx.app.today
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.LabeledSlider
import com.luxmaxx.app.ui.components.LineChart
import com.luxmaxx.app.ui.components.Pill
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.StatTile
import com.luxmaxx.app.ui.components.luxFieldColors
import com.luxmaxx.app.ui.theme.Amber
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Red
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.Violet
import java.io.File
import kotlin.math.roundToInt

private val routine = listOf(
    "Утро: умыться мягким гелем",
    "Утро: SPF 30+ (даже в пасмурно)",
    "Вечер: умыться, снять грязь и пот после зала",
    "Вечер: увлажняющий крем",
    "Выпил норму воды",
    "Без солёного и сладкого после 19:00",
    "Сон 7–9 часов",
    "Сменил наволочку (раз в 3–4 дня)",
)

@Composable
fun FaceScreen(vm: AppViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val busy by vm.aiBusy.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var metrics by remember { mutableStateOf<FaceMetrics?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }

    var skin by remember { mutableFloatStateOf(6f) }
    var acne by remember { mutableFloatStateOf(2f) }
    var puff by remember { mutableFloatStateOf(2f) }
    var sleep by remember { mutableFloatStateOf(8f) }
    var water by remember { mutableFloatStateOf(2f) }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<Long?>(null) }
    var aiResult by remember { mutableStateOf<FaceAiResult?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoPath = pendingPath else pendingPath?.let { File(it).delete() }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importFacePhoto(it)?.let { f -> photoPath = f.absolutePath } }
    }

    LaunchedEffect(photoPath) {
        val p = photoPath ?: return@LaunchedEffect
        analyzing = true; metrics = null; warning = null; aiResult = null
        when (val r = vm.analyzeFace(File(p))) {
            is FaceResult.Ok -> { metrics = r.metrics; warning = r.warning }
            is FaceResult.Error -> warning = r.message
        }
        analyzing = false
    }

    val checks = d.faceChecks.sortedBy { it.id }
    val doneToday = d.faceRoutineDone[today()].orEmpty()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Состояние лица", overline = "Ежедневный чек-ин",
                subtitle = "Замеры делаются на телефоне. Фото уходит в DeepSeek только по кнопке «AI-анализ».")
        }

        item {
            SectionCard("Новая отметка", icon = Icons.Filled.CameraAlt, accent = Violet) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("Камера", icon = Icons.Filled.CameraAlt, modifier = Modifier.weight(1f), onClick = {
                        val f = vm.newFacePhotoFile()
                        pendingPath = f.absolutePath
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                        takePicture.launch(uri)
                    })
                    Row(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(Surface1)
                            .border(1.dp, Stroke, RoundedCornerShape(16.dp))
                            .clickable { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = TextHi, modifier = Modifier.size(20.dp))
                        Text("  Галерея", color = TextHi, style = MaterialTheme.typography.labelLarge)
                    }
                }
                photoPath?.let { p ->
                    AsyncImage(
                        model = File(p), contentDescription = "Фото лица",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                            .border(1.dp, BorderGradient, RoundedCornerShape(20.dp)),
                    )
                }
                if (analyzing) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp)); Text("  Анализирую лицо…")
                }
                metrics?.let { m ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Симметрия", "${m.symmetry}", Modifier.weight(1f), "%", accent = Violet)
                        StatTile("Индекс ширины", "${m.widthIndex}", Modifier.weight(1f), accent = Blue)
                    }
                    Text("Индекс ширины снижается, когда уходят отёки и лишний жир. Сравнивай только фото в одинаковых условиях.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                warning?.let { Text("⚠️ $it", color = Amber, style = MaterialTheme.typography.bodySmall) }

                photoPath?.let { p ->
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Violet, Blue)))
                            .clickable(enabled = !busy && !analyzing) {
                                if (d.apiKey.isBlank()) vm.message.value = "Добавь ключ DeepSeek в Профиле"
                                else vm.analyzeFacePhotoAi(File(p), notes) { r ->
                                    aiResult = r
                                    r.skin?.let { skin = it.toFloat() }
                                    r.acne?.let { acne = it.toFloat() }
                                    r.puffiness?.let { puff = it.toFloat() }
                                }
                            },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (busy) { CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp); Text("  DeepSeek смотрит фото…", color = Color.White, fontWeight = FontWeight.Bold) }
                        else { Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp)); Text("  AI-анализ фото", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
                aiResult?.let { r ->
                    if (r.skin != null) Text("AI выставил оценки ниже — поправь, если не согласен.",
                        style = MaterialTheme.typography.bodySmall, color = Amber)
                    Text(r.asText(), style = MaterialTheme.typography.bodyMedium, color = TextHi,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Violet.copy(alpha = 0.10f)).padding(14.dp))
                }

                LabeledSlider("Кожа в целом", skin, 1f..10f, 8, { skin = it }, "/10")
                LabeledSlider("Высыпания", acne, 0f..10f, 9, { acne = it }, "/10")
                LabeledSlider("Отёчность", puff, 0f..10f, 9, { puff = it }, "/10")
                LabeledSlider("Сон прошлой ночью", sleep, 3f..12f, 17, { sleep = it }, " ч")
                LabeledSlider("Вода вчера", water, 0f..5f, 9, { water = it }, " л")
                OutlinedTextField(notes, { notes = it }, label = { Text("Заметки (новый крем, пицца вечером…)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = luxFieldColors())
                GradientButton(
                    "Сохранить отметку", icon = Icons.Filled.Check,
                    enabled = !analyzing,
                    onClick = {
                        vm.saveFaceCheck(
                            FaceCheck(
                                id = System.currentTimeMillis(), date = today(), photoPath = photoPath,
                                skin = skin.roundToInt(), acne = acne.roundToInt(), puffiness = puff.roundToInt(),
                                sleepHours = (sleep * 2).roundToInt() / 2.0, waterLiters = (water * 2).roundToInt() / 2.0,
                                notes = notes.trim(), metrics = metrics,
                                aiAdvice = aiResult?.asText()?.takeIf { it.isNotBlank() },
                            )
                        )
                        photoPath = null; pendingPath = null; metrics = null; warning = null; notes = ""; aiResult = null
                        vm.message.value = "Отметка сохранена"
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            SectionCard("Уход сегодня · ${doneToday.size}/${routine.size}", icon = Icons.Filled.Spa, accent = Green) {
                routine.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vm.toggleRoutine(r) }) {
                        Checkbox(checked = r in doneToday, onCheckedChange = { vm.toggleRoutine(r) },
                            colors = CheckboxDefaults.colors(checkedColor = Green, uncheckedColor = TextLo, checkmarkColor = Color(0xFF06140D)))
                        Text(r, style = MaterialTheme.typography.bodyMedium, color = if (r in doneToday) TextLo else TextHi)
                    }
                }
            }
        }

        if (checks.size >= 2) item {
            SectionCard("Динамика", icon = Icons.Filled.ShowChart, accent = Blue) {
                Text("Кожа (выше = лучше)", style = MaterialTheme.typography.labelLarge)
                LineChart(checks.takeLast(30).map { it.skin.toDouble() }, color = Green)
                Text("Отёчность (ниже = лучше)", style = MaterialTheme.typography.labelLarge)
                LineChart(checks.takeLast(30).map { it.puffiness.toDouble() }, color = Red)
                val widths = checks.mapNotNull { it.metrics?.widthIndex }.takeLast(30)
                if (widths.size >= 2) {
                    Text("Индекс ширины лица", style = MaterialTheme.typography.labelLarge)
                    LineChart(widths, color = Blue)
                }
            }
        }

        val withPhotos = checks.filter { it.photoPath != null && File(it.photoPath).exists() }
        if (withPhotos.size >= 2) item {
            SectionCard("Было → Стало", icon = Icons.Filled.CompareArrows, accent = Gold) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(withPhotos.first(), withPhotos.last()).forEach { c ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = File(c.photoPath!!), contentDescription = c.date,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(10.dp)),
                            )
                            Text(c.date, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (checks.isNotEmpty()) item {
            Text("История", style = MaterialTheme.typography.titleLarge, color = TextHi, modifier = Modifier.padding(top = 6.dp))
        }
        items(checks.reversed(), key = { it.id }) { c ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    c.photoPath?.takeIf { File(it).exists() }?.let {
                        AsyncImage(File(it), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(c.date, style = MaterialTheme.typography.titleSmall, color = TextHi)
                        Text("Кожа ${c.skin} · Высыпания ${c.acne} · Отёки ${c.puffiness}", style = MaterialTheme.typography.bodySmall)
                        Text("Сон ${c.sleepHours} ч · Вода ${c.waterLiters} л" +
                            (c.metrics?.let { " · Сим. ${it.symmetry}%" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        if (c.notes.isNotBlank()) Text(c.notes, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.deleteFaceCheck(c) }) { Icon(Icons.Filled.Delete, "Удалить") }
                }
                if (c.aiAdvice != null) {
                    Text(if (expanded == c.id) "Скрыть совет AI ▲" else "Показать совет AI ▼", color = Amber,
                        modifier = Modifier.clickable { expanded = if (expanded == c.id) null else c.id })
                    if (expanded == c.id) Text(c.aiAdvice, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Pill(if (busy) "AI думает…" else "✨ Получить советы AI", Violet,
                        Modifier.clickable(enabled = !busy) { vm.faceAdvice(c); expanded = c.id })
                }
            }
        }

        item {
            SectionCard("Что реально улучшает лицо", icon = Icons.Filled.Lightbulb, accent = Blue) {
                Text("• Снижение % жира (до ~12–15% у парней) — скулы и челюсть видны лучше всего от этого.")
                Text("• Сон и меньше соли/алкоголя — главные враги утренних отёков.")
                Text("• SPF каждый день — защищает от пигментации и раннего старения.")
                Text("• Высыпания: умывание после зала, не трогать лицо руками, при сильном акне — к дерматологу (ретиноиды, азелаиновая кислота).")
                Text("• Осанка и тренировка шеи/трапеций визуально делают фигуру и лицо мощнее.")
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }
}
