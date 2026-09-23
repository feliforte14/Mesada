package com.mesada.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Composable
fun ScreenHeader(eyebrow: String, title: String, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Text(title, style = MaterialTheme.typography.displayMedium)
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
            Text(symbol, fontSize = (size.value * 0.45f).sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CalorieRing(consumed: Double, goal: Int, modifier: Modifier = Modifier) {
    val over = consumed > goal
    val progress by animateFloatAsState((consumed / goal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(fill, -90f, 360f * progress, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(consumed.kcal(), style = MaterialTheme.typography.displaySmall)
            val left = goal - consumed
            Text(
                if (left >= 0) "de ${goal.thousands()} kcal · restan ${left.kcal()}" else "${(-left).kcal()} kcal de más",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 28.dp),
            )
        }
    }
}

@Composable
fun MacroBar(label: String, value: Double, goal: Int, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("${value.roundToInt()} / $goal g", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (value / goal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round, drawStopIndicator = {},
        )
    }
}
