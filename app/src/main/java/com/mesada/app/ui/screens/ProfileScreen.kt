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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mesada.app.data.db.GoalsEntity
import com.mesada.app.data.db.ProfileEntity
import com.mesada.app.domain.ActivityLevel
import com.mesada.app.domain.Sex
import com.mesada.app.domain.WeightGoal
import com.mesada.app.ui.Panel
import com.mesada.app.ui.ScreenHeader
import com.mesada.app.ui.thousands
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(profile: ProfileEntity?, goals: GoalsEntity, onEdit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState())) {
        ScreenHeader("Tu estado actual", "Perfil") {
            Button(onClick = onEdit, shape = RoundedCornerShape(20.dp), modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Editar mis datos")
            }
        }
        if (profile == null) {
            Text("Todavía no cargaste tus datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        val imc = if (profile.heightCm > 0) profile.weightKg / (profile.heightCm / 100.0).let { it * it } else 0.0
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Panel(Modifier.width(460.dp)) {
                Text("Tus datos", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                DataRow("Peso", "${profile.weightKg.clean()} kg")
                DataRow("Altura", "${profile.heightCm.clean()} cm")
                DataRow("Edad", "${profile.age} años")
                DataRow("Sexo", Sex.entries.firstOrNull { it.key == profile.sex }?.label ?: profile.sex)
                DataRow("Actividad", ActivityLevel.fromKey(profile.activity).label)
                DataRow("Objetivo", WeightGoal.fromKey(profile.goal).label)
                DataRow("IMC", "${imc.oneDecimal()} · ${imcLabel(imc)}", last = true)
            }
            Panel(Modifier.width(460.dp)) {
                Text("Tus objetivos diarios", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("Calculados a partir de tus datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                DataRow("Calorías", "${goals.kcal.thousands()} kcal")
                DataRow("Proteína", "${goals.protein} g")
                DataRow("Hidratos", "${goals.carbs} g")
                DataRow("Grasas", "${goals.fat} g", last = true)
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun Double.clean(): String = if (this % 1.0 == 0.0) toLong().toString() else oneDecimal()
private fun Double.oneDecimal(): String = "${(this * 10).roundToInt() / 10.0}"

private fun imcLabel(imc: Double): String = when {
    imc <= 0 -> "—"
    imc < 18.5 -> "bajo peso"
    imc < 25 -> "normal"
    imc < 30 -> "sobrepeso"
    else -> "obesidad"
}
