package com.mesada.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.mesada.app.data.DayState
import com.mesada.app.data.DaySummary
import com.mesada.app.domain.Reward
import com.mesada.app.domain.RewardScope
import com.mesada.app.domain.Rewards
import com.mesada.app.ui.ScreenHeader

@Composable
fun RewardsScreen(day: DayState, history: List<DaySummary>) {
    val groups = Rewards.compute(day, history)
    val all = groups.flatMap { it.rewards }
    val doneCount = all.count { it.done }
    var selectedScope by remember { mutableStateOf<RewardScope?>(null) } // null = Todos

    val displayedRewards = if (selectedScope == null) all else groups.firstOrNull { it.scope == selectedScope }?.rewards ?: emptyList()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Cumplí objetivos y sumá logros", "Recompensas")

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Llevás $doneCount de ${all.size} logros conseguidos.",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedScope == null,
                    onClick = { selectedScope = null },
                    label = { Text("Todos") },
                    shape = RoundedCornerShape(16.dp),
                )
                RewardScope.entries.forEach { scope ->
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = { selectedScope = scope },
                        label = { Text(scope.label) },
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Grilla Fija 3 Columnas x 2 Filas sin Scroll
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val chunked = displayedRewards.take(6).chunked(3)
            chunked.forEach { rowRewards ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowRewards.forEach { reward ->
                        RewardCard(reward, Modifier.weight(1f).fillMaxHeight())
                    }
                    if (rowRewards.size < 3) {
                        repeat(3 - rowRewards.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardCard(reward: Reward, modifier: Modifier = Modifier) {
    val done = reward.done
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (done) accent else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (done) accent else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (done) Icons.Filled.Check else Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(reward.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(if (done) "¡Conseguido!" else "${reward.current} / ${reward.target}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (done) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { reward.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = accent, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round, drawStopIndicator = {},
            )
            Spacer(Modifier.height(6.dp))
            Text(reward.detail, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
