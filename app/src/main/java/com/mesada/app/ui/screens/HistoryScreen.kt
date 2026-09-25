package com.mesada.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mesada.app.data.DaySummary
import com.mesada.app.data.Meal
import com.mesada.app.data.displayName
import com.mesada.app.data.macros
import com.mesada.app.data.qtyLabel
import com.mesada.app.export.HistoryPdf
import com.mesada.app.ui.Panel
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.kcal
import com.mesada.app.ui.thousands
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val AR = Locale("es", "AR")
private val today: String get() = LocalDate.now().toString()

private fun prettyDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", AR)).replaceFirstChar { it.uppercase() }
}.getOrDefault(iso)

@Composable
fun HistoryScreen(history: List<DaySummary>) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Tu registro día a día", "Evolución") {
            Button(
                onClick = { HistoryPdf.email(context, history) },
                enabled = history.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Enviar PDF por mail") }
        }

        if (history.isEmpty()) {
            Text("Todavía no hay días registrados. Lo que cargues va a aparecer acá para ver tu evolución.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        val currentDay = history.getOrNull(selectedIndex) ?: history.first()

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // Columna Izquierda (35%): Gráfico de Actividad de 7 Días
            ActivityPanel(history, Modifier.weight(1f).fillMaxHeight())

            // Columna Derecha (65%): Selector de Día + Detalle Fijo de Comidas (2x2)
            Panel(Modifier.weight(1.8f).fillMaxHeight()) {
                // Selector de Día
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedButton(
                        onClick = { selectedIndex = (selectedIndex + 1).coerceAtMost(history.size - 1) },
                        enabled = selectedIndex < history.size - 1,
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("◄ Anterior") }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (currentDay.date == today) "Hoy" else prettyDate(currentDay.date),
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        )
                        Text("${currentDay.totals.kcal.kcal()} kcal · ${currentDay.steps.thousands()} pasos",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedButton(
                        onClick = { selectedIndex = (selectedIndex - 1).coerceAtLeast(0) },
                        enabled = selectedIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Siguiente ►") }
                }
                Spacer(Modifier.height(12.dp))

                // Resumen de Macros del Día
                val t = currentDay.totals
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Proteínas", style = MaterialTheme.typography.labelSmall)
                            Text("${t.protein.roundToInt()} g", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hidratos", style = MaterialTheme.typography.labelSmall)
                            Text("${t.carbs.roundToInt()} g", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Grasas", style = MaterialTheme.typography.labelSmall)
                            Text("${t.fat.roundToInt()} g", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Desglose de Comidas en Grilla 2x2
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealDetailBox(Meal.BREAKFAST, currentDay, Modifier.weight(1f).fillMaxHeight())
                        MealDetailBox(Meal.LUNCH, currentDay, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealDetailBox(Meal.SNACK, currentDay, Modifier.weight(1f).fillMaxHeight())
                        MealDetailBox(Meal.DINNER, currentDay, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityPanel(history: List<DaySummary>, modifier: Modifier) {
    val recent = history.take(7).reversed() // últimos 7 días
    val maxSteps = (history.maxOfOrNull { it.steps } ?: 0).coerceAtLeast(1)
    val daysWithData = history.filter { it.steps > 0 || it.totals.kcal > 0 }
    val avgSteps = if (daysWithData.isEmpty()) 0 else daysWithData.sumOf { it.steps } / daysWithData.size
    val avgKcal = if (daysWithData.isEmpty()) 0.0 else daysWithData.sumOf { it.totals.kcal } / daysWithData.size

    Panel(modifier) {
        Text("Actividad reciente", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Text("Promedio ${avgSteps.thousands()} pasos · ${avgKcal.kcal()} kcal",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            recent.forEach { d ->
                val frac = d.steps.toFloat() / maxSteps
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(dayShort(d.date), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
                    Box(Modifier.weight(1f).height(16.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(16.dp)) {}
                        if (frac > 0f) Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(frac).height(16.dp)) {}
                    }
                    Text(d.steps.thousands(), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun MealDetailBox(meal: Meal, day: DaySummary, modifier: Modifier = Modifier) {
    val es = day.meal(meal)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(meal.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            if (es.isEmpty()) {
                Text("Sin registros", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                es.take(2).forEach { e ->
                    Text("${e.displayName} (${e.kcal.kcal()} kcal)", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                if (es.size > 2) {
                    Text("+ ${es.size - 2} más", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun dayShort(iso: String): String = runCatching {
    val d = LocalDate.parse(iso)
    "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, AR).replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}"
}.getOrDefault(iso)
