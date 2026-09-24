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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.mesada.app.data.DaySummary
import com.mesada.app.export.HistoryPdf
import com.mesada.app.data.Meal
import com.mesada.app.data.displayName
import com.mesada.app.data.macros
import com.mesada.app.data.qtyLabel
import com.mesada.app.ui.Panel
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.kcal
import com.mesada.app.ui.thousands
import androidx.compose.ui.unit.dp
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
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        ScreenHeader("Tu registro día a día", "Evolución") {
            Button(
                onClick = { HistoryPdf.email(context, history) },
                enabled = history.isNotEmpty(),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.heightIn(min = 56.dp),
            ) { Text("Enviar PDF por mail") }
        }
        if (history.isEmpty()) {
            Text("Todavía no hay días registrados. Lo que cargues va a aparecer acá para ver tu evolución.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ActivityPanel(history, Modifier.width(360.dp).fillMaxHeight())
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(history, key = { it.date }) { DayCard(it) }
            }
        }
    }
}

@Composable
private fun ActivityPanel(history: List<DaySummary>, modifier: Modifier) {
    val recent = history.take(14).reversed() // más viejo → más nuevo
    val maxSteps = (history.maxOfOrNull { it.steps } ?: 0).coerceAtLeast(1)
    val daysWithData = history.filter { it.steps > 0 || it.totals.kcal > 0 }
    val avgSteps = if (daysWithData.isEmpty()) 0 else daysWithData.sumOf { it.steps } / daysWithData.size
    val avgKcal = if (daysWithData.isEmpty()) 0.0 else daysWithData.sumOf { it.totals.kcal } / daysWithData.size

    Panel(modifier) {
        Text("Pasos y actividad", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text("Promedio ${avgSteps.thousands()} pasos/día · ${avgKcal.kcal()} kcal/día",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        recent.forEach { d ->
            val frac = d.steps.toFloat() / maxSteps
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dayShort(d.date), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                Box(Modifier.weight(1f).height(20.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(20.dp)) {}
                    if (frac > 0f) Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(frac).height(20.dp)) {}
                }
                Text(d.steps.thousands(), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

private fun dayShort(iso: String): String = runCatching {
    val d = LocalDate.parse(iso)
    "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, AR).replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}"
}.getOrDefault(iso)

@Composable
private fun DayCard(day: DaySummary) {
    var expanded by remember { mutableStateOf(false) }
    Surface(onClick = { expanded = !expanded }, shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (day.date == today) "Hoy" else prettyDate(day.date),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    )
                    Text("${day.steps.thousands()} pasos · ≈${day.burnedKcal} kcal quemadas",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${day.totals.kcal.kcal()} kcal", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(8.dp))
            val t = day.totals
            Text("P ${t.protein.roundToInt()} g · H ${t.carbs.roundToInt()} g · G ${t.fat.roundToInt()} g",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
                if (day.entries.isEmpty()) {
                    Text("No se registró comida este día.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                Meal.entries.forEach { meal ->
                    val es = day.meal(meal)
                    if (es.isNotEmpty()) {
                        Text(meal.label, style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        es.forEach { e ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(e.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("${e.qtyLabel} · ${e.macros.kcal.kcal()} kcal",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Text(if (day.entries.isEmpty()) "" else "Tocá para ver el detalle",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
