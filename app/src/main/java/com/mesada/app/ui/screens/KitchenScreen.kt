package com.mesada.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
        "Ya estás cerca de tu objetivo: llevás ${day.totals.kcal.kcal()} kcal. Si tenés hambre, elegí algo liviano con verduras."
    } else buildString {
        append("Te quedan unas ${remK.kcal()} kcal")
        if (remP > 5) append(" y ${remP.roundToInt()} g de proteína")
        append(".")
        if (!day.hasProduce) append(" Hoy todavía no sumaste frutas ni verduras.")
    }

    Column(Modifier.fillMaxSize().padding(28.dp)) {
        ScreenHeader("Según lo que llevás comido hoy", "Cocina")
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1.4f).verticalScroll(rememberScrollState())) {
                Text(message, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 18.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ideas para completar el día", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { showNewRecipe = true }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text("+ Nueva receta")
                    }
                }
                suggestRecipes(recipes, remK, remP).forEach { recipe ->
                    val m = recipe.macros
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                                Text("${m.kcal.kcal()} kcal · ${m.protein.roundToInt()} g de proteína",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (recipe.custom) TextButton(onClick = { onDeleteRecipe(recipe.id) }) { Text("Eliminar") }
                            FilledTonalButton(onClick = { onAddRecipe(recipe) }, modifier = Modifier.heightIn(min = 60.dp),
                                shape = RoundedCornerShape(18.dp)) { Text("Agregar a cena") }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Panel(Modifier.fillMaxWidth()) {
                    Text("Temporizador", style = MaterialTheme.typography.headlineSmall)
                    Text(timer.label, fontSize = 84.sp, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall,
                        color = if (timer.finished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3.0, 5.0, 10.0, 15.0).forEach { min ->
                            OutlinedButton(onClick = { onTimerPreset(min) }, modifier = Modifier.weight(1f).heightIn(min = 60.dp)) {
                                Text("${min.roundToInt()}′")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onTimerToggle, modifier = Modifier.weight(2f).heightIn(min = 64.dp),
                            shape = RoundedCornerShape(20.dp)) {
                            Text(when {
                                timer.running -> "Pausar"
                                timer.leftSec in 1 until timer.totalSec -> "Seguir"
                                else -> "Iniciar"
                            }, style = MaterialTheme.typography.titleLarge)
                        }
                        OutlinedButton(onClick = onTimerReset, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) { Text("Reiniciar") }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Objetivos diarios", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = onEditProfile, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Recalcular")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        GoalRow(GoalField.KCAL, "Calorías", "${day.goals.kcal} kcal", 100),
                        GoalRow(GoalField.PROTEIN, "Proteína", "${day.goals.protein} g", 5),
                        GoalRow(GoalField.CARBS, "Hidratos", "${day.goals.carbs} g", 10),
                        GoalRow(GoalField.FAT, "Grasas", "${day.goals.fat} g", 5),
                    ).forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(row.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            RoundButton("−", "Bajar ${row.label}", { onGoal(row.field, -row.step) })
                            Text(row.value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                                modifier = Modifier.width(100.dp))
                            RoundButton("+", "Subir ${row.label}", { onGoal(row.field, row.step) })
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Balanza Bluetooth", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                when {
                                    !scaleEnabled -> "Desactivada — los gramos se estiman por voz o se cargan a mano."
                                    scaleConnection == ScaleConnectionState.CONNECTED && scaleGrams != null ->
                                        "Conectada · ${scaleGrams.roundToInt()} g en la bandeja"
                                    scaleConnection == ScaleConnectionState.CONNECTING -> "Buscando la balanza…"
                                    else -> "Activada, pero no conectada todavía."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = scaleEnabled, onCheckedChange = onScaleToggle)
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

    Column {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(enabled = valid, onClick = { onCreate(name.trim(), items) }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
            title = { Text("Nueva receta") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text("Nombre de la receta") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${macros.kcal.kcal()} kcal · ${macros.protein.roundToInt()} g proteína",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Ingredientes", style = MaterialTheme.typography.labelLarge)
                    foods.forEach { food ->
                        val qty = selected[food.id]
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.titleSmall)
                                if (qty != null) Text(food.formatQty(qty), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (qty == null) {
                                OutlinedButton(onClick = { selected[food.id] = food.defaultQty }) { Text("Agregar") }
                            } else {
                                RoundButton("−", "Menos ${food.name}", {
                                    val next = qty - food.step
                                    if (next < food.step) selected.remove(food.id) else selected[food.id] = next
                                }, 44.dp)
                                Spacer(Modifier.width(6.dp))
                                RoundButton("+", "Más ${food.name}", { selected[food.id] = qty + food.step }, 44.dp)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            },
        )
    }
}
