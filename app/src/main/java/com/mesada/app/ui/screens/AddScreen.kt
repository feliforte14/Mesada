package com.mesada.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mesada.app.data.Category
import com.mesada.app.data.Food
import com.mesada.app.data.Macros
import com.mesada.app.data.Meal
import com.mesada.app.data.Measure
import com.mesada.app.data.pretty
import com.mesada.app.ui.RoundButton
import com.mesada.app.ui.ScreenHeader
import kotlin.math.roundToInt

private fun Double.fieldText(): String = if (this == 0.0) "" else pretty()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    foods: List<Food>,
    selectedMeal: Meal,
    onSelectMeal: (Meal) -> Unit,
    onAdd: (Food, Double) -> Unit,
    onCreateFood: (name: String, category: Category, measure: Measure, per100: Macros,
                   gramsPerPiece: Double, unitSingular: String, unitPlural: String) -> Unit,
    onUpdateFood: (id: String, name: String, category: Category, measure: Measure, per100: Macros,
                   gramsPerPiece: Double, unitSingular: String, unitPlural: String) -> Unit,
    onDeleteFood: (Food) -> Unit,
) {
    var category by remember { mutableStateOf<Category?>(null) }
    var query by remember { mutableStateOf("") }
    var sheetFood by remember { mutableStateOf<Food?>(null) }
    var showNewFood by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    val shown = foods.filter {
        (category == null || it.category == category) && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().padding(28.dp)) {
        ScreenHeader("Tocá un alimento para elegir la porción", "Agregar comida") {
            Button(onClick = { showNewFood = true }, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.heightIn(min = 56.dp)) {
                Text("+ Nuevo alimento")
            }
        }
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
        if (shown.isEmpty()) {
            Text("No hay alimentos con ese nombre. Probá con otra palabra o creá uno nuevo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(GridCells.Adaptive(168.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(shown, key = { it.id }) { food -> FoodTile(food) { sheetFood = food } }
        }
    }

    sheetFood?.let { food ->
        PortionSheet(
            food, selectedMeal,
            onDismiss = { sheetFood = null },
            onEdit = { editingFood = food; sheetFood = null },
            onDelete = { onDeleteFood(food); sheetFood = null },
            onConfirm = { qty -> onAdd(food, qty); sheetFood = null },
        )
    }

    if (showNewFood) FoodFormDialog(
        initial = null,
        onDismiss = { showNewFood = false },
        onSubmit = { name, cat, measure, per100, gpp, sing, plur ->
            onCreateFood(name, cat, measure, per100, gpp, sing, plur); showNewFood = false
        },
    )

    editingFood?.let { food ->
        FoodFormDialog(
            initial = food,
            onDismiss = { editingFood = null },
            onSubmit = { name, cat, measure, per100, gpp, sing, plur ->
                onUpdateFood(food.id, name, cat, measure, per100, gpp, sing, plur); editingFood = null
            },
        )
    }
}

@Composable
private fun FoodTile(food: Food, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.heightIn(min = 120.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(food.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
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
private fun PortionSheet(
    food: Food, meal: Meal, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onConfirm: (Double) -> Unit,
) {
    var qty by remember(food.id) { mutableDoubleStateOf(food.defaultQty) }
    val m = food.macros(qty)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 28.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(food.name, style = MaterialTheme.typography.headlineMedium)
                    Text(food.unitHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodFormDialog(
    initial: Food?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, category: Category, measure: Measure, per100: Macros,
               gramsPerPiece: Double, unitSingular: String, unitPlural: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Category.PROTEIN) }
    var measure by remember { mutableStateOf(initial?.measure ?: Measure.GRAM) }
    var kcal by remember { mutableStateOf(initial?.per100?.kcal?.fieldText() ?: "") }
    var protein by remember { mutableStateOf(initial?.per100?.protein?.fieldText() ?: "") }
    var carbs by remember { mutableStateOf(initial?.per100?.carbs?.fieldText() ?: "") }
    var fat by remember { mutableStateOf(initial?.per100?.fat?.fieldText() ?: "") }
    var gramsPerPiece by remember { mutableStateOf(initial?.gramsPerPiece?.fieldText() ?: "") }
    var unitSingular by remember { mutableStateOf(initial?.unitSingular?.ifBlank { "unidad" } ?: "unidad") }
    var unitPlural by remember { mutableStateOf(initial?.unitPlural?.ifBlank { "unidades" } ?: "unidades") }

    val perLabel = when (measure) {
        Measure.GRAM, Measure.PIECE -> "cada 100 g"
        Measure.ML -> "cada 100 ml"
    }
    val valid = name.isNotBlank() && kcal.toDoubleOrNull() != null &&
        (measure != Measure.PIECE || (gramsPerPiece.toDoubleOrNull()?.let { it > 0 } == true))

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSubmit(
                        name.trim(), category, measure,
                        Macros(
                            kcal.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0,
                        ),
                        gramsPerPiece.toDoubleOrNull() ?: 0.0,
                        unitSingular.trim().ifBlank { "unidad" }, unitPlural.trim().ifBlank { "unidades" },
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (initial == null) "Nuevo alimento" else "Editar alimento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Se mide en", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val measures = listOf(Measure.GRAM to "Gramos", Measure.ML to "Mililitros", Measure.PIECE to "Por unidad")
                    measures.forEach { (mm, lbl) ->
                        FilterChip(selected = measure == mm, onClick = { measure = mm }, label = { Text(lbl) })
                    }
                }
                if (measure == Measure.PIECE) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = gramsPerPiece, onValueChange = { gramsPerPiece = it }, singleLine = true,
                        label = { Text("Gramos por unidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = unitSingular, onValueChange = { unitSingular = it }, singleLine = true,
                            label = { Text("Unidad") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unitPlural, onValueChange = { unitPlural = it }, singleLine = true,
                            label = { Text("Plural") }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Valores nutricionales ($perLabel)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroField("Calorías", kcal, { kcal = it }, Modifier.weight(1f))
                    MacroField("Proteína", protein, { protein = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroField("Hidratos", carbs, { carbs = it }, Modifier.weight(1f))
                    MacroField("Grasas", fat, { fat = it }, Modifier.weight(1f))
                }
            }
        },
    )
}

@Composable
private fun MacroField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, singleLine = true, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
    )
}
