package com.luxmaxx.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.ui.components.AiOrb
import com.luxmaxx.app.ui.components.GradientButton
import com.luxmaxx.app.ui.components.LogoMark
import com.luxmaxx.app.ui.components.MarkdownText
import com.luxmaxx.app.ui.components.ScreenHeader
import com.luxmaxx.app.ui.components.SectionCard
import com.luxmaxx.app.ui.components.TypingDots
import com.luxmaxx.app.ui.components.enter
import com.luxmaxx.app.ui.components.luxFieldColors
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.CardGradient
import com.luxmaxx.app.ui.theme.Gold
import com.luxmaxx.app.ui.theme.GoldDeep
import com.luxmaxx.app.ui.theme.OnAccent
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface2
import com.luxmaxx.app.ui.theme.TextHi
import com.luxmaxx.app.ui.theme.TextLo

private val suggestions = listOf(
    "Разбери мои последние тренировки",
    "Составь меню на день под мои калории",
    "Почему у меня не растёт вес?",
    "Как убрать отёки с лица по утрам?",
    "Какие добавки реально работают?",
    "Как улучшить кожу после зала?",
)

@Composable
fun CoachScreen(vm: AppViewModel, onProfile: () -> Unit) {
    val d by vm.data.collectAsStateWithLifecycle()
    val busy by vm.aiBusy.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    val proposedProgram by vm.proposedProgram.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importChatPhoto(it)?.let { file -> selectedImagePath = file.absolutePath } }
    }

    LaunchedEffect(d.chat.size, busy) {
        if (d.chat.isNotEmpty()) listState.animateScrollToItem(d.chat.size - 1 + if (busy) 1 else 0)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            ScreenHeader("AI-тренер", overline = "DeepSeek",
                subtitle = "Работает со всеми данными приложения и анализирует фото") {
                if (d.chat.isNotEmpty()) IconButton(onClick = { vm.clearChat() }) { Icon(Icons.Filled.DeleteSweep, "Очистить чат", tint = TextLo) }
            }
        }

        if (d.apiKey.isBlank()) {
            Box(Modifier.padding(16.dp)) {
                SectionCard("Нужен API-ключ DeepSeek") {
                    Text("Зарегистрируйся на platform.deepseek.com, пополни баланс (запросы стоят копейки) и вставь ключ в профиле.")
                    GradientButton("Открыть профиль", onClick = onProfile)
                }
            }
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (d.chat.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AiOrb(170.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Чем помочь?", style = MaterialTheme.typography.headlineMedium, color = TextHi)
                    Spacer(Modifier.height(4.dp))
                    Text("Я вижу твой профиль, тренировки, питание и образ — спрашивай что угодно",
                        style = MaterialTheme.typography.bodyMedium, color = TextLo, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            items(d.chat) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth().enter(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom) {
                    val shape = if (mine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                    if (!mine) { LogoMark(30.dp, glow = false); Spacer(Modifier.width(6.dp)) }
                    SelectionContainer {
                        if (mine) Column(
                            modifier = Modifier
                                .widthIn(max = 290.dp)
                                .clip(shape)
                                .background(Brush.linearGradient(listOf(Gold, GoldDeep)))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            m.imagePath?.let { path ->
                                val photo = File(path).takeIf { it.exists() }
                                if (photo != null) AsyncImage(
                                    model = photo, contentDescription = "Прикреплённое фото",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)),
                                )
                            }
                            Text(m.content, color = OnAccent, style = MaterialTheme.typography.bodyMedium)
                        } else MarkdownText(
                            m.content,
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(shape)
                                .background(CardGradient)
                                .border(1.dp, BorderGradient, shape)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            if (busy) item {
                Row(verticalAlignment = Alignment.Bottom) {
                    LogoMark(30.dp, glow = false, spin = true); Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(CardGradient)
                            .border(1.dp, BorderGradient, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                    ) { TypingDots() }
                }
            }
        }

        if (proposedProgram != null) {
            SectionCard("Предложение новой программы") {
                val plan = proposedProgram!!
                Text(plan.title, color = TextHi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(plan.description, color = TextLo, style = MaterialTheme.typography.bodySmall)
                plan.days.forEach { day ->
                    Text(day.name, color = TextHi, fontWeight = FontWeight.SemiBold)
                    day.exercises.forEach { ex ->
                        Text("• ${ex.name} — ${ex.sets}×${ex.repMin}–${ex.repMax}", color = TextLo, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton("Применить программу", onClick = { vm.applyProposedProgram() }, modifier = Modifier.weight(1f))
                    Button(onClick = { vm.discardProposedProgram() }) { Text("Отмена") }
                }
            }
        }

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(suggestions) { s ->
                Text(s, color = TextHi, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape).background(Surface2).border(1.dp, Stroke, CircleShape)
                        .clickable(enabled = !busy) { vm.sendChat(s) }.padding(horizontal = 14.dp, vertical = 9.dp))
            }
        }
        selectedImagePath?.let { path ->
            val photo = File(path).takeIf { it.exists() }
            if (photo != null) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = photo, contentDescription = "Фото для отправки", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(8.dp))
                Text("Фото прикреплено", color = TextLo, modifier = Modifier.weight(1f))
                IconButton(onClick = { File(path).delete(); selectedImagePath = null }) {
                    Icon(Icons.Filled.Close, "Убрать фото", tint = TextLo)
                }
            }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
                Icon(Icons.Filled.AddPhotoAlternate, "Прикрепить фото", tint = Gold)
            }
            OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Сообщение…") }, maxLines = 4, shape = RoundedCornerShape(22.dp), colors = luxFieldColors())
            Spacer(Modifier.width(8.dp))
            val canSend = !busy && (input.isNotBlank() || selectedImagePath != null)
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(if (canSend) Brush.linearGradient(listOf(Gold, GoldDeep)) else Brush.linearGradient(listOf(Surface2, Surface2)))
                    .clickable(enabled = canSend) { vm.sendChat(input, selectedImagePath); input = ""; selectedImagePath = null },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = if (canSend) OnAccent else TextLo) }
        }
    }
}
