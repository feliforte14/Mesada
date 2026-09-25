package com.mesada.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val arNumber = NumberFormat.getIntegerInstance(Locale("es", "AR"))
fun Double.kcal(): String = arNumber.format(roundToInt())
fun Int.thousands(): String = arNumber.format(this)

/** Tarjeta base: superficie clara con borde suave en vez de sombra dura, para un look más liviano. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun ScreenHeader(eyebrow: String, title: String, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                eyebrow.uppercase(Locale("es", "AR")),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
        }
        action()
    }
}

/** Botón redondo grande, pensado para dedos (mínimo 56 dp). */
@Composable
fun RoundButton(symbol: String, description: String, onClick: () -> Unit, size: Dp = 56.dp,
                color: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Surface(onClick = onClick, shape = CircleShape, color = color,
        modifier = Modifier.size(size).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CalorieRing(consumed: Double, goal: Int, modifier: Modifier = Modifier) {
    val over = consumed > goal
    val progress by animateFloatAsState(
        (consumed / goal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600), label = "ring",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(fill, -90f, 360f * progress, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(consumed.kcal(), style = MaterialTheme.typography.headlineLarge)
            val left = goal - consumed
            Text("kcal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                if (left >= 0) "restan ${left.kcal()} de ${goal.thousands()}" else "${(-left).kcal()} de más",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
fun MacroBar(label: String, value: Double, goal: Int, color: Color, unit: String = "g") {
    val progress by animateFloatAsState(
        (value / goal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600), label = "macro",
    )
    val goalText = if (unit.isBlank()) goal.thousands() else "$goal $unit"
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("${value.roundToInt()} / $goalText", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round, drawStopIndicator = {},
        )
    }
}
