package com.mesada.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesada.app.data.DayState
import com.mesada.app.data.Meal
import com.mesada.app.data.displayName
import com.mesada.app.data.emoji
import com.mesada.app.data.macros
import com.mesada.app.data.qtyLabel
import com.mesada.app.data.db.EntryEntity
import com.mesada.app.ui.CalorieRing
import com.mesada.app.ui.MacroBar
import com.mesada.app.ui.Panel
import com.mesada.app.ui.RoundButton
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.kcal
import com.mesada.app.ui.theme.Palette
import com.mesada.app.ui.thousands
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    day: DayState,
    onRemove: (Long) -> Unit,
    onAddTo: (Meal) -> Unit,
    onSteps: (Int) -> Unit,
    onReset: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "AR")))
        .replaceFirstChar { it.uppercase() }

    Column(Modifier.fillMaxSize().padding(28.dp)) {
        ScreenHeader(dateLabel, "Tu día") {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Empezar día nuevo")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Panel(Modifier.width(340.dp).verticalScroll(rememberScrollState())) {
                CalorieRing(day.totals.kcal, day.goals.kcal, Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.padding(8.dp))
                val t = day.totals
                MacroBar("Proteína", t.protein, day.goals.protein, MaterialTheme.colorScheme.primary)
                MacroBar("Hidratos", t.carbs, day.goals.carbs, Palette.Saffron)
                MacroBar("Grasas", t.fat, day.goals.fat, MaterialTheme.colorScheme.tertiary)
                HorizontalDivider(Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(day.steps.thousands(), style = MaterialTheme.typography.headlineMedium)
                        Text("pasos · ≈${(day.steps * 0.04).roundToInt()} kcal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RoundButton("−", "Restar 1000 pasos", { onSteps(-1000) })
                    Spacer(Modifier.width(8.dp))
                    RoundButton("+", "Sumar 1000 pasos", { onSteps(1000) })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(Meal.entries) { meal -> MealCard(meal, day.meal(meal), onRemove) { onAddTo(meal) } }
            }
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("¿Empezar un día nuevo?") },
        text = { Text("Se borra lo registrado hoy y los pasos vuelven a cero.") },
        confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("Borrar y empezar") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun MealCard(meal: Meal, entries: List<EntryEntity>, onRemove: (Long) -> Unit, onAdd: () -> Unit) {
    Panel(Modifier.heightIn(min = 220.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(meal.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${entries.sumOf { it.kcal }.kcal()} kcal", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.padding(4.dp))
        if (entries.isEmpty()) {
            Text("Todavía no registraste nada.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 14.dp))
        }
        entries.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e.emoji, fontSize = 26.sp, modifier = Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${e.qtyLabel} · ${e.macros.protein.roundToInt()} g prot",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(e.kcal.kcal(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onRemove(e.id) }, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar ${e.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.padding(4.dp))
        FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
            Text("+ Agregar a ${meal.label.lowercase()}")
        }
    }
}
