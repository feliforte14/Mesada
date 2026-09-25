package com.mesada.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.mesada.app.ui.Panel
import com.mesada.app.ui.RoundButton
import com.mesada.app.ui.ScreenHeader
import kotlin.math.ceil
import kotlin.math.roundToInt

private fun Double.fieldText(): String = if (this == 0.0) "" else pretty()
private const val PAGE_SIZE = 6

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
    var selectedFood by remember { mutableStateOf<Food?>(foods.firstOrNull()) }
    var showNewFood by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var page by remember { mutableIntStateOf(0) }

    val filtered = foods.filter {
        (category == null || it.category == category) && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
    }
    val totalPages = ceil(filtered.size.toDouble() / PAGE_SIZE).toInt().coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val pagedFoods = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

    if (selectedFood == null || selectedFood !in foods) {
        selectedFood = filtered.firstOrNull() ?: foods.firstOrNull()
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Seleccioná un alimento y ajustá la porción", "Agregar comida") {
            Button(onClick = { showNewFood = true }, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text("+ Nuevo alimento")
            }
        }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // Columna Izquierda (60%): Filtros, Búsqueda, Grilla Fija con Paginación
            Column(Modifier.weight(1.3f).fillMaxHeight()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Meal.entries.forEach { m ->
                        FilterChip(
                            selected = m == selectedMeal, onClick = { onSelectMeal(m) },
                            label = { Text(m.label, style = MaterialTheme.typography.labelLarge) },
                            shape = RoundedCornerShape(20.dp), modifier = Modifier.height(44.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilterChip(selected = category == null, onClick = { category = null; page = 0 }, label = { Text("Todo") },
                        modifier = Modifier.height(40.dp), shape = RoundedCornerShape(20.dp))
                    Category.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c; page = 0 }, label = { Text(c.label) },
                            modifier = Modifier.height(40.dp), shape = RoundedCornerShape(20.dp))
                    }
                    OutlinedTextField(
                        value = query, onValueChange = { query = it; page = 0 }, singleLine = true,
                        placeholder = { Text("Buscar") }, shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Contenido principal de alimentos (Grilla 2x3 o lista paginada)
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    if (pagedFoods.isEmpty()) {
                        Text("No hay alimentos con ese criterio.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp))
                    } else {
                        // Mostrar máximo 6 ítems en 2 columnas x 3 filas
                        val rows = pagedFoods.chunked(2)
                        rows.forEach { rowFoods ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                                rowFoods.forEach { food ->
                                    val isSelected = food.id == selectedFood?.id
                                    FoodTile(
                                        food = food,
                                        selected = isSelected,
                                        onClick = { selectedFood = food },
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                                if (rowFoods.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                // Control de Paginación
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { page = (currentPage - 1).coerceAtLeast(0) },
                        enabled = currentPage > 0,
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("◄ Anterior") }

                    Text("Página ${currentPage + 1} de $totalPages", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedButton(
                        onClick = { page = (currentPage + 1).coerceAtMost(totalPages - 1) },
                        enabled = currentPage < totalPages - 1,
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Siguiente ►") }
                }
            }

            // Columna Derecha (40%): Calculador de Porciones en vivo (Master-Detail)
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                val food = selectedFood
                if (food != null) {
                    PortionPanel(
                        food = food,
                        selectedMeal = selectedMeal,
                        onEdit = { editingFood = food },
                        onDelete = { onDeleteFood(food); selectedFood = foods.firstOrNull { it.id != food.id } },
                        onConfirm = { qty -> onAdd(food, qty) },
                    )
                } else {
                    Text("Seleccioná o creá un alimento para agregar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
private fun FoodTile(food: Food, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(food.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                if (food.measure == Measure.PIECE) "${food.macros(1.0).kcal.roundToInt()} kcal / ${food.unitSingular}"
                else "${food.per100.kcal.pretty()} kcal / 100 ${if (food.measure == Measure.ML) "ml" else "g"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PortionPanel(
    food: Food, selectedMeal: Meal, onEdit: () -> Unit, onDelete: () -> Unit, onConfirm: (Double) -> Unit,
) {
    var qty by remember(food.id) { mutableDoubleStateOf(food.defaultQty) }
    val m = food.macros(qty)

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.headlineSmall)
                Text(food.unitHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text("Editar") }
            TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.height(14.dp))

        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton("−", "Menos", { qty = (qty - food.step).coerceAtLeast(food.step) }, 56.dp, MaterialTheme.colorScheme.surface)
                Text(food.formatQty(qty), style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                RoundButton("+", "Más", { qty += food.step }, 56.dp, MaterialTheme.colorScheme.surface)
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            food.presets.forEach { p ->
                OutlinedButton(onClick = { qty = p }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) {
                    Text(if (food.measure == Measure.PIECE) p.pretty() else "${p.pretty()} ${if (food.measure == Measure.ML) "ml" else "g"}",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("kcal" to m.kcal, "prot g" to m.protein, "hidr g" to m.carbs, "grasa g" to m.fat).forEach { (l, v) ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${v.roundToInt()}", style = MaterialTheme.typography.titleLarge)
                        Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onConfirm(qty) }, shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text("Agregar a ${selectedMeal.label.lowercase()}", style = MaterialTheme.typography.titleMedium)
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
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Categoría", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Category.entries.forEach { c ->
                                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Se mide en", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val measures = listOf(Measure.GRAM to "Gramos", Measure.ML to "ML", Measure.PIECE to "Unidad")
                            measures.forEach { (mm, lbl) ->
                                FilterChip(selected = measure == mm, onClick = { measure = mm }, label = { Text(lbl) })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroField("kcal/100g", kcal, { kcal = it }, Modifier.weight(1f))
                    MacroField("Prot g", protein, { protein = it }, Modifier.weight(1f))
                    MacroField("Hidr g", carbs, { carbs = it }, Modifier.weight(1f))
                    MacroField("Grasa g", fat, { fat = it }, Modifier.weight(1f))
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
