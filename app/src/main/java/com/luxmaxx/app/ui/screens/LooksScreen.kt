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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.ui.components.PhotoPickerRow
import com.luxmaxx.app.ui.components.PhotoPreview
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.luxFieldColors
import com.luxmaxx.app.ui.theme.Blue
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldGradient
import com.luxmaxx.app.ui.theme.Green
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo
import com.luxmaxx.app.ui.theme.Violet
import java.io.File

private data class LooksTab(val title: String, val icon: ImageVector)

private val looksTabs = listOf(
    LooksTab("Лицо", Icons.Filled.Face),
    LooksTab("Волосы", Icons.Filled.ContentCut),
    LooksTab("Стиль", Icons.Filled.Checkroom),
    LooksTab("Кумир", Icons.Filled.Star),
)

/** Раздел «Образ»: лицо, волосы, одежда и кумир. */
@Composable
fun LooksScreen(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)).background(Surface1).border(1.dp, Stroke, RoundedCornerShape(18.dp)).padding(4.dp),
        ) {
            looksTabs.forEachIndexed { i, t ->
                val sel = i == tab
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (sel) GoldGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { tab = i }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(t.icon, null, tint = if (sel) Color(0xFF1C1200) else TextLo, modifier = Modifier.size(16.dp))
                    Text(" ${t.title}", style = MaterialTheme.typography.labelMedium, color = if (sel) Color(0xFF1C1200) else TextLo, maxLines = 1)
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> FaceScreen(vm)
                1 -> HairTab(vm)
                2 -> StyleTab(vm)
                else -> IdolTab(vm)
            }
        }
    }
}

// ---------------- Общие элементы ----------------

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color(0xFF1C1200) else TextHi,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) GoldGradient else Brush.linearGradient(listOf(Surface1, Surface1)))
            .border(1.dp, if (selected) Color.Transparent else Stroke, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun AiButton(text: String, busy: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(if (enabled) listOf(Violet, Blue) else listOf(Stroke, Stroke)))
            .clickable(enabled = enabled && !busy, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(if (busy) "  AI думает…" else "  $text", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AiResult(text: String?) {
    if (text.isNullOrBlank()) return
    SelectionContainer {
        Text(
            text, style = MaterialTheme.typography.bodyMedium, color = TextHi,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Violet.copy(alpha = 0.10f))
                .border(1.dp, Violet.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(14.dp),
        )
    }
}

@Composable
private fun Tips(title: String, color: Color, tips: List<String>) {
    SectionCard(title, icon = Icons.Filled.Lightbulb, accent = color) {
        tips.forEach { Row { Text("→  ", color = color); Text(it, style = MaterialTheme.typography.bodyMedium, color = TextHi) } }
    }
}

@Composable
private fun NeedKeyHint(hasKey: Boolean) {
    if (!hasKey) Text("Для AI-советов добавь ключ DeepSeek в Профиле", style = MaterialTheme.typography.bodySmall, color = Gold)
}

// ---------------- Волосы ----------------

private val hairTypes = listOf("Прямые", "Волнистые", "Кудрявые", "Афро")
private val hairLengths = listOf("Очень короткие", "Короткие", "Средние", "Длинные")
private val hairIssueList = listOf("Жирнеют", "Сухие", "Перхоть", "Выпадают", "Секутся", "Пушатся", "Не держат укладку", "Редкая борода")

@Composable
private fun HairTab(vm: AppViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val busy by vm.aiBusy.collectAsStateWithLifecycle()
    val l = d.looks
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Волосы", overline = "Стрижка и уход", subtitle = "AI подберёт стрижку под форму лица и уход под твой тип волос") }
        item {
            SectionCard("Твои волосы", icon = Icons.Filled.ContentCut) {
                Text("Тип", style = MaterialTheme.typography.labelLarge, color = TextLo)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hairTypes.forEach { t -> SelectChip(t, l.hairType == t) { vm.updateLooks { it.copy(hairType = t) } } }
                }
                Text("Длина", style = MaterialTheme.typography.labelLarge, color = TextLo)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hairLengths.forEach { t -> SelectChip(t, l.hairLength == t) { vm.updateLooks { it.copy(hairLength = t) } } }
                }
                Text("Что беспокоит", style = MaterialTheme.typography.labelLarge, color = TextLo)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hairIssueList.forEach { t ->
                        SelectChip(t, t in l.hairIssues) {
                            vm.updateLooks { it.copy(hairIssues = if (t in it.hairIssues) it.hairIssues - t else it.hairIssues + t) }
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Фото для подбора стрижки", icon = Icons.Filled.Face, accent = Violet) {
                Text("Анфас, волосы видно целиком, лоб открыт. Без фото возьму последнее фото лица.",
                    style = MaterialTheme.typography.bodySmall, color = TextLo)
                PhotoPreview(l.hairPhotoPath)
                PhotoPickerRow(vm, "hair", onPicked = { p ->
                    vm.setLooksPhoto(l.hairPhotoPath, p) { lk, np -> lk.copy(hairPhotoPath = np) }
                })
                NeedKeyHint(d.apiKey.isNotBlank())
                AiButton("Подобрать стрижку и уход", busy, enabled = d.apiKey.isNotBlank()) { vm.analyzeHair() }
                AiResult(l.hairAdvice)
            }
        }
        item {
            Tips("Базовый уход", Blue, listOf(
                "Мой голову по мере загрязнения: жирные — каждый день мягким шампунем, нормальные — через 1–2 дня",
                "Кондиционер — только на длину, не на корни; смывай прохладной водой",
                "Перед феном — термозащита, фен не ближе 15 см и не на максимум",
                "Стригись каждые 3–6 недель — короткие стрижки быстро теряют форму",
                "Укладка: матовая паста/глина для коротких, крем или солевой спрей для средних",
                "Перхоть: шампунь с кетоконазолом или пиритионом цинка 2 раза в неделю",
                "Сон, белок, цинк, железо и витамин D влияют на густоту волос",
                "Выпадение, залысины, редеет макушка — не тяни, иди к трихологу: чем раньше, тем больше можно сохранить",
            ))
        }
        item {
            Tips("Борода и брови", Green, listOf(
                "Выбери одну длину щетины и держи её триммером — неряшливая борода хуже никакой",
                "Чёткая линия на шее (2 пальца над кадыком) и на щеках сразу делает лицо собраннее",
                "Масло для бороды против зуда и сухой кожи под ней",
                "Брови: убери только волоски между бровями и торчащие — не делай их тонкими",
            ))
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ---------------- Стиль и одежда ----------------

private val styleGoals = listOf("Casual", "Old money", "Streetwear", "Спорт-шик", "Классика", "Минимализм")
private val budgets = listOf("Низкий", "Средний", "Высокий")

@Composable
private fun StyleTab(vm: AppViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val busy by vm.aiBusy.collectAsStateWithLifecycle()
    val l = d.looks
    val p = d.profile
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Стиль", overline = "Одежда", subtitle = "Как одеваться, чтобы фигура выглядела мощнее") }
        item {
            SectionCard("Твой стиль", icon = Icons.Filled.Checkroom) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    styleGoals.forEach { g -> SelectChip(g, l.styleGoal == g) { vm.updateLooks { it.copy(styleGoal = g) } } }
                }
                Text("Бюджет", style = MaterialTheme.typography.labelLarge, color = TextLo)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    budgets.forEach { b -> SelectChip(b, l.budget == b) { vm.updateLooks { it.copy(budget = b) } } }
                }
                NeedKeyHint(d.apiKey.isNotBlank())
                AiButton("Собрать капсульный гардероб", busy, enabled = d.apiKey.isNotBlank()) { vm.wardrobePlan() }
                AiResult(l.wardrobeAdvice)
            }
        }
        item {
            SectionCard("Разбор образа по фото", icon = Icons.Filled.Person, accent = Violet) {
                Text("Сфоткайся в полный рост в зеркале — AI оценит посадку, цвета и подскажет, что поменять.",
                    style = MaterialTheme.typography.bodySmall, color = TextLo)
                PhotoPreview(l.outfitPhotoPath, ratio = 0.75f)
                PhotoPickerRow(vm, "outfit", onPicked = { np ->
                    vm.setLooksPhoto(l.outfitPhotoPath, np) { lk, x -> lk.copy(outfitPhotoPath = x, outfitAdvice = null) }
                })
                AiButton("Разобрать образ", busy, enabled = d.apiKey.isNotBlank() && l.outfitPhotoPath != null) { vm.analyzeOutfit() }
                AiResult(l.outfitAdvice)
            }
        }
        item {
            Tips("Посадка решает всё", Gold, listOf(
                "Шов плеча футболки/рубашки — ровно на краю плеча, не ниже",
                "Рукав футболки — на середине бицепса и чуть облегает его",
                "Футболка заканчивается на середине ширинки, не ниже",
                "Брюки — зауженные к низу (tapered), без лишней ткани на бёдрах и с минимальным заломом внизу",
                "Ищи «athletic fit» — крой под широкие плечи и узкую талию",
                "Плотные ткани (футболки от 200 г/м²) держат форму и подчёркивают рельеф",
            ))
        }
        item {
            Tips("Визуально мощнее и выше", Blue, listOf(
                "V-силуэт: структурированные плечи сверху, узкий низ",
                "Однотонный образ (тёмный верх и низ) вытягивает фигуру",
                "Горизонтальные полосы и светлый верх — для тех, кто хочет казаться шире в плечах",
                "Henley, поло и рубашки с закатанными рукавами показывают предплечья",
                if ((p?.heightCm ?: 180) < 175) "При росте ниже 175: короче куртки, брюки без заломов, обувь на толстой подошве"
                else "Высоким: многослойность и куртки средней длины сбалансируют пропорции",
            ))
        }
        item {
            Tips("База и мелочи", Green, listOf(
                "Капсула: 3 белые/чёрные/серые футболки, тёмные джинсы, чиносы, овершот, бомбер, белые кеды",
                "Нейтральные цвета (чёрный, белый, серый, тёмно-синий, оливковый, бежевый) сочетаются со всем",
                "Обувь всегда чистая — на неё смотрят первыми",
                "Минимум аксессуаров: часы и, может быть, одно кольцо или цепочка",
                "Хороший парфюм — 1–2 пшика, не облако",
            ))
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ---------------- Кумир ----------------

@Composable
private fun IdolTab(vm: AppViewModel) {
    val d by vm.data.collectAsStateWithLifecycle()
    val busy by vm.aiBusy.collectAsStateWithLifecycle()
    val l = d.looks
    var name by remember { mutableStateOf(l.idolName) }
    val myPhoto = d.faceChecks.sortedByDescending { it.id }.firstNotNullOfOrNull { c -> c.photoPath?.takeIf { File(it).exists() } }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ScreenHeader("Кумир", overline = "Твой ориентир",
                subtitle = "Добавь фото того, на кого хочешь быть похож. AI сравнит и скажет, что реально перенять.")
        }
        item {
            SectionCard("Кто твой ориентир", icon = Icons.Filled.Star) {
                OutlinedTextField(
                    name, { name = it; vm.updateLooks { lk -> lk.copy(idolName = it) } },
                    label = { Text("Имя (актёр, спортсмен, блогер…)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = luxFieldColors(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PhotoSlot("Кумир", l.idolPhotoPath, Modifier.weight(1f))
                    PhotoSlot("Ты сейчас", myPhoto, Modifier.weight(1f))
                }
                if (myPhoto == null) Text("Своё фото добавь во вкладке «Лицо» — без него AI сравнит с твоим профилем.",
                    style = MaterialTheme.typography.bodySmall, color = TextLo)
                PhotoPickerRow(vm, "idol", cameraLabel = "Снять", onPicked = { np ->
                    vm.setLooksPhoto(l.idolPhotoPath, np) { lk, x -> lk.copy(idolPhotoPath = x, idolAnalysis = null) }
                })
                NeedKeyHint(d.apiKey.isNotBlank())
                AiButton("Сравнить и составить план", busy, enabled = d.apiKey.isNotBlank() && (l.idolPhotoPath != null || l.idolName.isNotBlank())) {
                    vm.analyzeIdol()
                }
                AiResult(l.idolAnalysis)
                if (l.idolAnalysis != null) Text("AI-тренер теперь учитывает твой ориентир во всех советах.",
                    style = MaterialTheme.typography.bodySmall, color = Green)
            }
        }
        item {
            Tips("Как равняться правильно", Gold, listOf(
                "Выбирай ориентир с похожим ростом и типом фигуры — так цель будет достижимой",
                "Фото знаменитостей — это свет, ракурс, памп и ретушь. Сравнивай тренды, а не один кадр",
                "Самое быстрое сходство дают стрижка, ухоженность и стиль — это можно сделать за неделю",
                "Телосложение: 1–3 года стабильных тренировок и питания, без химии",
                "Цель — лучшая версия тебя, а не копия. Сравнивай себя с собой месяц назад",
            ))
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PhotoSlot(label: String, path: String?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val f = path?.let { File(it) }?.takeIf { it.exists() }
        if (f != null) AsyncImage(
            f, label, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderGradient, RoundedCornerShape(18.dp)),
        ) else Box(
            Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(18.dp)).background(Surface1)
                .border(1.dp, Stroke, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Person, null, tint = TextLo, modifier = Modifier.size(40.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextLo, modifier = Modifier.padding(top = 6.dp))
    }
}
