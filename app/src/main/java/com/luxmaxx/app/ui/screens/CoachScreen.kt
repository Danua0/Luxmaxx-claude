package com.luxmaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.luxmaxx.app.ui.components.SectionCard

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
    val listState = rememberLazyListState()

    LaunchedEffect(d.chat.size, busy) {
        if (d.chat.isNotEmpty()) listState.animateScrollToItem(d.chat.size - 1 + if (busy) 1 else 0)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AI-тренер", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("DeepSeek видит твой профиль, питание, тренировки и состояние лица",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (d.chat.isNotEmpty()) IconButton(onClick = { vm.clearChat() }) { Icon(Icons.Filled.DeleteSweep, "Очистить чат") }
        }

        if (d.apiKey.isBlank()) {
            Box(Modifier.padding(16.dp)) {
                SectionCard("Нужен API-ключ DeepSeek") {
                    Text("Зарегистрируйся на platform.deepseek.com, пополни баланс (запросы стоят копейки) и вставь ключ в профиле.")
                    Button(onClick = onProfile) { Text("Открыть профиль") }
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
                Text("Спроси что угодно про тренировки, питание или уход за лицом 👇",
                    modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(d.chat) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    SelectionContainer {
                        Text(
                            m.content,
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .background(
                                    if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(12.dp),
                            color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (busy) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp)); Text("  Думаю…")
                }
            }
        }

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(suggestions) { s -> AssistChip(onClick = { vm.sendChat(s) }, label = { Text(s) }, enabled = !busy) }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Сообщение…") }, maxLines = 4)
            IconButton(enabled = !busy && input.isNotBlank(), onClick = { vm.sendChat(input); input = "" }) {
                Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
            }
        }
    }
}
