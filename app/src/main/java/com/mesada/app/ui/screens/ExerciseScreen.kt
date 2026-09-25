package com.mesada.app.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mesada.app.data.DayState
import com.mesada.app.ui.MacroBar
import com.mesada.app.ui.Panel
import com.mesada.app.ui.RoundButton
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.thousands
import kotlin.math.roundToInt

private const val STEP_GOAL = 10_000

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(day: DayState, onSteps: (Int) -> Unit) {
    val burned = (day.steps * 0.04).roundToInt()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Sumá el movimiento de tu día", "Ejercicio")

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Text("Pasos de hoy", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(day.steps.thousands(), style = MaterialTheme.typography.headlineLarge)
                Text("≈ $burned kcal quemadas", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                MacroBar("Meta ${STEP_GOAL.thousands()} pasos", day.steps.toDouble(), STEP_GOAL, MaterialTheme.colorScheme.primary, unit = "")
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundButton("−", "Restar 1000 pasos", { onSteps(-1000) }, 60.dp)
                    Text("1000 pasos", style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    RoundButton("+", "Sumar 1000 pasos", { onSteps(1000) }, 60.dp)
                }
            }

            Panel(Modifier.weight(1.2f).fillMaxHeight()) {
                Text("Agregar rápido", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("Tocá para sumar pasos a lo de hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(500, 1000, 2000, 5000).forEach { n ->
                        OutlinedButton(onClick = { onSteps(n) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.heightIn(min = 52.dp)) {
                            Text("+${n.thousands()}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onSteps(-day.steps) }, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text("Poner en cero", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.weight(1f))
                Text("Los pasos son informativos: te ayudan a ver tu actividad, no cambian tus objetivos de comida. " +
                    "La estimación usa ≈0,04 kcal por paso.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
