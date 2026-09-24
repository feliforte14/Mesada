package com.mesada.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.mesada.app.data.DayState
import com.mesada.app.data.DaySummary
import com.mesada.app.domain.Reward
import com.mesada.app.domain.Rewards
import com.mesada.app.ui.ScreenHeader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewardsScreen(day: DayState, history: List<DaySummary>) {
    val groups = Rewards.compute(day, history)
    val all = groups.flatMap { it.rewards }
    val doneCount = all.count { it.done }

    Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState())) {
        ScreenHeader("Cumplí objetivos y sumá logros", "Recompensas")
        Text("Llevás $doneCount de ${all.size} logros conseguidos.",
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        groups.forEach { group ->
            Text(group.scope.eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(group.scope.label, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                group.rewards.forEach { RewardCard(it) }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RewardCard(reward: Reward) {
    val done = reward.done
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (done) accent else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(330.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (done) accent else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (done) Icons.Filled.Check else Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(reward.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (done) "¡Conseguido!" else "${reward.current} / ${reward.target}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (done) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { reward.progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = accent, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round, drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Text(reward.detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
