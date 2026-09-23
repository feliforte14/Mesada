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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mesada.app.data.db.ProfileEntity
import com.mesada.app.domain.ActivityLevel
import com.mesada.app.domain.Sex
import com.mesada.app.domain.WeightGoal
import com.mesada.app.ui.Panel
import com.mesada.app.ui.ScreenHeader

/**
 * Onboarding: pide lo mínimo (peso, altura, edad, sexo biológico, actividad, objetivo) para
 * calcular kcal/macros con GoalsCalculator. Se muestra al primer uso; también se reabre desde
 * "Cocina" (RoundButton de editar) para recalcular si cambió el peso o el objetivo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initial: ProfileEntity?,
    onCancel: (() -> Unit)?,
    onSave: (ProfileEntity) -> Unit,
) {
    var weight by remember { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(initial?.heightCm?.toString() ?: "") }
    var age by remember { mutableStateOf(initial?.age?.toString() ?: "") }
    var sex by remember { mutableStateOf(initial?.sex?.let { s -> Sex.entries.firstOrNull { it.key == s } } ?: Sex.MALE) }
    var activity by remember { mutableStateOf(ActivityLevel.fromKey(initial?.activity ?: ActivityLevel.MODERATE.key)) }
    var goal by remember { mutableStateOf(WeightGoal.fromKey(initial?.goal ?: WeightGoal.MAINTAIN.key)) }

    val weightNum = weight.toDoubleOrNull()
    val heightNum = height.toDoubleOrNull()
    val ageNum = age.toIntOrNull()
    val valid = (weightNum?.let { it > 0 } ?: false) && (heightNum?.let { it > 0 } ?: false) && (ageNum?.let { it > 0 } ?: false)

    Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState())) {
        ScreenHeader("Para calcular tus objetivos", "Contame de vos")

        Panel(Modifier.widthIn(max = 640.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Peso (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = height, onValueChange = { height = it.filter { c -> c.isDigit() } },
                    label = { Text("Altura (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = age, onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = { Text("Edad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(22.dp))
            Text("Sexo biológico", style = MaterialTheme.typography.titleMedium)
            Text("Se usa solo para el cálculo de metabolismo basal (fórmula Mifflin-St Jeor).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Sex.entries.forEach { s ->
                    FilterChip(selected = sex == s, onClick = { sex = s }, label = { Text(s.label) },
                        modifier = Modifier.heightIn(min = 52.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Nivel de actividad", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityLevel.entries.forEach { a ->
                    FilterChip(selected = activity == a, onClick = { activity = a }, label = { Text(a.label) },
                        modifier = Modifier.heightIn(min = 52.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Objetivo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WeightGoal.entries.forEach { g ->
                    FilterChip(selected = goal == g, onClick = { goal = g }, label = { Text(g.label) },
                        modifier = Modifier.heightIn(min = 52.dp))
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onCancel != null) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                        Text("Cancelar")
                    }
                }
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(ProfileEntity(
                            weightKg = weightNum!!, heightCm = heightNum!!, age = ageNum!!,
                            sex = sex.key, activity = activity.key, goal = goal.key,
                        ))
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                ) { Text("Calcular mis objetivos", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
