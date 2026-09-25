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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesada.app.GoalField
import com.mesada.app.data.DayState
import com.mesada.app.data.Food
import com.mesada.app.domain.RecipeUi
import com.mesada.app.domain.TimerState
import com.mesada.app.domain.macrosOf
import com.mesada.app.domain.suggestRecipes
import com.mesada.app.hardware.ScaleConnectionState
import com.mesada.app.ui.Panel
import com.mesada.app.ui.RoundButton
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.kcal
import kotlin.math.roundToInt

private data class GoalRow(val field: GoalField, val label: String, val value: String, val step: Int)

@Composable
fun KitchenScreen(
    day: DayState,
    timer: TimerState,
    recipes: List<RecipeUi>,
    foods: List<Food>,
    onTimerPreset: (Double) -> Unit,
    onTimerToggle: () -> Unit,
    onTimerReset: () -> Unit,
    onAddRecipe: (RecipeUi) -> Unit,
    onCreateRecipe: (String, List<Pair<String, Double>>) -> Unit,
    onDeleteRecipe: (String) -> Unit,
    onGoal: (GoalField, Int) -> Unit,
    onEditProfile: () -> Unit,
    scaleEnabled: Boolean,
    scaleConnection: ScaleConnectionState,
    scaleGrams: Double?,
    onScaleToggle: (Boolean) -> Unit,
) {
    val remK = day.remainingKcal
    val remP = day.remainingProtein
    var showNewRecipe by remember { mutableStateOf(false) }
    val message = if (remK < 150) {
        "Ya estás cerca de tu objetivo: llevás ${day.totals.kcal.kcal()} kcal. Si tenés hambre, elegí algo liviano."
    } else buildString {
        append("Te quedan unas ${remK.kcal()} kcal")
        if (remP > 5) append(" y ${remP.roundToInt()} g de proteína")
        append(".")
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Según lo que llevás comido hoy", "Cocina")

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // Columna 1 (40%): Recetas e Ideas
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Panel(Modifier.fillMaxSize()) {
                    Text(message, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ideas para hoy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { showNewRecipe = true }, modifier = Modifier.heightIn(min = 40.dp)) {
                            Text("+ Receta")
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val suggested = suggestRecipes(recipes, remK, remP).take(3)
                        suggested.forEach { recipe ->
                            val m = recipe.macros
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${m.kcal.kcal()} kcal · ${m.protein.roundToInt()} g prot",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (recipe.custom) TextButton(onClick = { onDeleteRecipe(recipe.id) }) { Text("X") }
                                    FilledTonalButton(
                                        onClick = { onAddRecipe(recipe) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.heightIn(min = 40.dp),
                                    ) { Text("+ Cena") }
                                }
                            }
                        }
                    }
                }
            }

            // Columna 2 (60%): Herramientas (Temporizador, Balanza, Objetivos)
            Column(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Fila Superior: Temporizador y Balanza side by side
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Temporizador
                    Panel(Modifier.weight(1.2f).fillMaxHeight()) {
                        Text("Temporizador", style = MaterialTheme.typography.titleMedium)
                        Text(timer.label, fontSize = 56.sp, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displayMedium,
                            color = if (timer.finished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(3.0, 5.0, 10.0, 15.0).forEach { min ->
                                OutlinedButton(onClick = { onTimerPreset(min) }, modifier = Modifier.weight(1f).heightIn(min = 36.dp)) {
                                    Text("${min.roundToInt()}′", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = onTimerToggle, modifier = Modifier.weight(2f).heightIn(min = 44.dp), shape = RoundedCornerShape(14.dp)) {
                                Text(when {
                                    timer.running -> "Pausar"
                                    timer.leftSec in 1 until timer.totalSec -> "Seguir"
                                    else -> "Iniciar"
                                }, style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(onClick = onTimerReset, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) { Text("Reset") }
                        }
                    }

                    // Balanza
                    Panel(Modifier.weight(1f).fillMaxHeight()) {
                        Text("Balanza Bluetooth", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                !scaleEnabled -> "Desactivada"
                                scaleConnection == ScaleConnectionState.CONNECTED && scaleGrams != null ->
                                    "${scaleGrams.roundToInt()} g en bandeja"
                                scaleConnection == ScaleConnectionState.CONNECTING -> "Buscando…"
                                else -> "Activada, conectando…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activar", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Switch(checked = scaleEnabled, onCheckedChange = onScaleToggle)
                        }
                    }
                }

                // Fila Inferior: Ajuste de Objetivos
                Panel(Modifier.fillMaxWidth().weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Objetivos diarios", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = onEditProfile, modifier = Modifier.heightIn(min = 36.dp)) {
                            Text("Recalcular")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        listOf(
                            GoalRow(GoalField.KCAL, "Calorías", "${day.goals.kcal}", 100),
                            GoalRow(GoalField.PROTEIN, "Proteína", "${day.goals.protein}g", 5),
                            GoalRow(GoalField.CARBS, "Hidratos", "${day.goals.carbs}g", 10),
                            GoalRow(GoalField.FAT, "Grasas", "${day.goals.fat}g", 5),
                        ).forEach { row ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            ) {
                                Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                                    Text(row.label, style = MaterialTheme.typography.labelMedium)
                                    Text(row.value, style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        RoundButton("−", "Bajar", { onGoal(row.field, -row.step) }, 36.dp)
                                        RoundButton("+", "Subir", { onGoal(row.field, row.step) }, 36.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewRecipe) NewRecipeDialog(
        foods = foods,
        onDismiss = { showNewRecipe = false },
        onCreate = { name, items -> onCreateRecipe(name, items); showNewRecipe = false },
    )
}

@Composable
private fun NewRecipeDialog(
    foods: List<Food>,
    onDismiss: () -> Unit,
    onCreate: (String, List<Pair<String, Double>>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Double>() }
    val foodMap = remember(foods) { foods.associateBy { it.id } }
    val items = selected.map { it.key to it.value }
    val macros = macrosOf(items, foodMap)
    val valid = name.isNotBlank() && items.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = valid, onClick = { onCreate(name.trim(), items) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nueva receta") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("Nombre de la receta") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("${macros.kcal.kcal()} kcal · ${macros.protein.roundToInt()} g proteína",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Ingredientes", style = MaterialTheme.typography.labelMedium)
                foods.take(5).forEach { food ->
                    val qty = selected[food.id]
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(food.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        if (qty == null) {
                            OutlinedButton(onClick = { selected[food.id] = food.defaultQty }) { Text("+", style = MaterialTheme.typography.labelSmall) }
                        } else {
                            RoundButton("−", "Menos", {
                                val next = qty - food.step
                                if (next < food.step) selected.remove(food.id) else selected[food.id] = next
                            }, 32.dp)
                            Text(food.formatQty(qty), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                            RoundButton("+", "Más", { selected[food.id] = qty + food.step }, 32.dp)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
    )
}
