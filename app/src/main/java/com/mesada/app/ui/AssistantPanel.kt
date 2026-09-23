package com.mesada.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mesada.app.AssistantUi
import com.mesada.app.Role
import com.mesada.app.VoiceMode

private val HINTS = listOf(
    "Sumá dos huevos al desayuno",
    "En la cena comí una empanada de carne y una ensalada",
    "¿Cuánta proteína me falta hoy?",
    "Poné un temporizador de 12 minutos",
)

@Composable
fun AssistantPanel(
    ui: AssistantUi,
    onMic: () -> Unit,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
    onToggleSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var typed by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(ui.messages.size) { if (ui.messages.isNotEmpty()) list.animateScrollToItem(ui.messages.size) }

    Surface(modifier.fillMaxHeight().widthIn(max = 460.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp, tonalElevation = 0.dp) {
        Column {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Asistente", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = onToggleSpeak, modifier = Modifier.size(56.dp)) {
                    Icon(if (ui.speak) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (ui.speak) "Silenciar respuestas" else "Leer respuestas en voz alta")
                }
                Spacer(Modifier.size(8.dp))
                FilledTonalIconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar asistente")
                }
            }
            LazyColumn(state = list, modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (ui.messages.isEmpty()) {
                    item { Bubble("Decime qué comiste o qué necesitás. Por ejemplo:", Role.ASSISTANT) }
                    items(HINTS) { h ->
                        OutlinedButton(onClick = { onSend(h) }, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(h, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                items(ui.messages, key = { it.id }) { Bubble(it.text, it.role) }
                if (ui.mode == VoiceMode.LISTENING && ui.partial.isNotBlank()) item { Bubble(ui.partial + "…", Role.USER) }
                if (ui.mode == VoiceMode.THINKING) item { Bubble("Pensando…", Role.ASSISTANT) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Column(Modifier.padding(20.dp)) {
                val (label, color) = when (ui.mode) {
                    VoiceMode.LISTENING -> "Escuchando… tocá para terminar" to MaterialTheme.colorScheme.error
                    VoiceMode.THINKING -> "Pensando… tocá para detener" to MaterialTheme.colorScheme.onSurfaceVariant
                    VoiceMode.IDLE -> "Tocá y hablá" to MaterialTheme.colorScheme.primary
                }
                Button(onClick = onMic, shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp)) {
                    Icon(if (ui.mode == VoiceMode.IDLE) Icons.Filled.Mic else Icons.Filled.Stop, null, Modifier.size(32.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(label, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.size(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = typed, onValueChange = { typed = it }, singleLine = true,
                        placeholder = { Text("O escribí tu pedido") }, shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend(typed); typed = "" }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    FilledTonalIconButton(onClick = { onSend(typed); typed = "" }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}

@Composable
private fun Bubble(text: String, role: Role) {
    val cs = MaterialTheme.colorScheme
    when (role) {
        Role.ACTION -> Text(text, color = cs.primary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Role.ERROR -> Text(text, color = cs.error, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth())
        else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (role == Role.USER) Arrangement.End else Arrangement.Start) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (role == Role.USER) cs.inverseSurface else cs.surfaceVariant,
                contentColor = if (role == Role.USER) cs.inverseOnSurface else cs.onSurface,
                modifier = Modifier.widthIn(max = 360.dp),
            ) { Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) }
        }
    }
}
