package com.mesada.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import com.mesada.app.data.DayState
import com.mesada.app.data.Meal
import com.mesada.app.data.displayName
import com.mesada.app.data.macros
import com.mesada.app.data.qtyLabel
import com.mesada.app.data.db.EntryEntity
import com.mesada.app.ui.CalorieRing
import com.mesada.app.ui.MacroBar
import com.mesada.app.ui.Panel
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.kcal
import com.mesada.app.ui.theme.Palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    day: DayState,
    onRemove: (Long) -> Unit,
    onAddTo: (Meal) -> Unit,
    onReset: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "AR")))
        .replaceFirstChar { it.uppercase() }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader(dateLabel, "Tu día") {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Empezar día nuevo")
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Panel(Modifier.width(310.dp).fillMaxHeight()) {
                CalorieRing(day.totals.kcal, day.goals.kcal, Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(12.dp))
                val t = day.totals
                MacroBar("Proteína", t.protein, day.goals.protein, MaterialTheme.colorScheme.primary)
                MacroBar("Hidratos", t.carbs, day.goals.carbs, Palette.Saffron)
                MacroBar("Grasas", t.fat, day.goals.fat, MaterialTheme.colorScheme.tertiary)
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MealCard(Meal.BREAKFAST, day.meal(Meal.BREAKFAST), onRemove, { onAddTo(Meal.BREAKFAST) }, Modifier.weight(1f).fillMaxHeight())
                    MealCard(Meal.LUNCH, day.meal(Meal.LUNCH), onRemove, { onAddTo(Meal.LUNCH) }, Modifier.weight(1f).fillMaxHeight())
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MealCard(Meal.SNACK, day.meal(Meal.SNACK), onRemove, { onAddTo(Meal.SNACK) }, Modifier.weight(1f).fillMaxHeight())
                    MealCard(Meal.DINNER, day.meal(Meal.DINNER), onRemove, { onAddTo(Meal.DINNER) }, Modifier.weight(1f).fillMaxHeight())
                }
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
private fun MealCard(meal: Meal, entries: List<EntryEntity>, onRemove: (Long) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(meal.label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${entries.sumOf { it.kcal }.kcal()} kcal", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f)) {
                if (entries.isEmpty()) {
                    Text("Todavía no registraste nada.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    val visible = entries.take(3)
                    visible.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.displayName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${e.qtyLabel} · ${e.macros.protein.roundToInt()} g prot",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(e.kcal.kcal(), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onRemove(e.id) }, modifier = Modifier.padding(start = 2.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar ${e.displayName}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (entries.size > 3) {
                        Text("+ ${entries.size - 3} más", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            FilledTonalButton(
                onClick = onAdd, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            ) {
                Text("+ Agregar a ${meal.label.lowercase()}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
