package com.mesada.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesada.app.data.Category
import com.mesada.app.data.Food
import com.mesada.app.data.FoodCatalog
import com.mesada.app.data.Meal
import com.mesada.app.data.Measure
import com.mesada.app.data.pretty
import com.mesada.app.ui.RoundButton
import com.mesada.app.ui.ScreenHeader
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(selectedMeal: Meal, onSelectMeal: (Meal) -> Unit, onAdd: (Food, Double) -> Unit) {
    var category by remember { mutableStateOf<Category?>(null) }
    var query by remember { mutableStateOf("") }
    var sheetFood by remember { mutableStateOf<Food?>(null) }
    val foods = FoodCatalog.all.filter {
        (category == null || it.category == category) && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().padding(28.dp)) {
        ScreenHeader("Tocá un alimento para elegir la porción", "Agregar comida")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Meal.entries.forEach { m ->
                FilterChip(
                    selected = m == selectedMeal, onClick = { onSelectMeal(m) },
                    label = { Text(m.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp)) },
                    shape = RoundedCornerShape(32.dp), modifier = Modifier.height(64.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Todo") },
                modifier = Modifier.height(52.dp), shape = RoundedCornerShape(26.dp))
            Category.entries.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) },
                    modifier = Modifier.height(52.dp), shape = RoundedCornerShape(26.dp))
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Buscar alimento") }, shape = RoundedCornerShape(28.dp),
                modifier = Modifier.width(280.dp))
        }
        Spacer(Modifier.height(20.dp))
        if (foods.isEmpty()) {
            Text("No hay alimentos con ese nombre. Probá con otra palabra.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(GridCells.Adaptive(168.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(foods, key = { it.id }) { food -> FoodTile(food) { sheetFood = food } }
        }
    }

    sheetFood?.let { food ->
        PortionSheet(food, selectedMeal, onDismiss = { sheetFood = null }) { qty -> onAdd(food, qty); sheetFood = null }
    }
}

@Composable
private fun FoodTile(food: Food, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.heightIn(min = 150.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(food.emoji, fontSize = 44.sp)
            Spacer(Modifier.height(8.dp))
            Text(food.name, style = MaterialTheme.typography.titleMedium)
            Text(
                if (food.measure == Measure.PIECE) "${food.macros(1.0).kcal.roundToInt()} kcal por ${food.unitSingular}"
                else "${food.per100.kcal.pretty()} kcal cada 100 ${if (food.measure == Measure.ML) "ml" else "g"}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortionSheet(food: Food, meal: Meal, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var qty by remember(food.id) { mutableDoubleStateOf(food.defaultQty) }
    val m = food.macros(qty)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 28.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(food.emoji, fontSize = 56.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(food.name, style = MaterialTheme.typography.headlineMedium)
                    Text(food.unitHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton("−", "Menos", { qty = (qty - food.step).coerceAtLeast(food.step) }, 72.dp, MaterialTheme.colorScheme.surface)
                    Text(food.formatQty(qty), style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    RoundButton("+", "Más", { qty += food.step }, 72.dp, MaterialTheme.colorScheme.surface)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                food.presets.forEach { p ->
                    OutlinedButton(onClick = { qty = p }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                        Text(if (food.measure == Measure.PIECE) p.pretty() else "${p.pretty()} ${if (food.measure == Measure.ML) "ml" else "g"}")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("kcal" to m.kcal, "prot g" to m.protein, "hidr g" to m.carbs, "grasa g" to m.fat).forEach { (l, v) ->
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${v.roundToInt()}", style = MaterialTheme.typography.headlineSmall)
                            Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onConfirm(qty) }, shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
                Text("Agregar a ${meal.label.lowercase()}", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
