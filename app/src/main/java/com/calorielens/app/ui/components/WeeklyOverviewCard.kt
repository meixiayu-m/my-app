package com.calorielens.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calorielens.app.domain.DailySummary
import com.calorielens.app.domain.WeeklyStats
import java.time.LocalDate

@Composable
fun WeeklyOverviewCard(stats: WeeklyStats, summaries: List<DailySummary>, fallbackTarget: Int) {
    val today = LocalDate.now()
    val week = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
    val byDate = summaries.associateBy { it.localDate }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📊 近 7 天", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (stats.daysTracked == 0) "记录几餐后，这里会出现你的节奏" else "不是追完美，是看看自己的节奏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(
                        "${stats.daysTracked}/7 天记录",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().height(82.dp)
            ) {
                week.forEach { date ->
                    val summary = byDate[date]
                    val dayTarget = summary?.targetCalories?.takeIf { it > 0 } ?: fallbackTarget
                    val ratio = (summary?.calories?.toFloat()?.div(dayTarget.coerceAtLeast(1)) ?: 0f).coerceIn(0.08f, 1f)
                    val color = when (summary?.let { it.calories > dayTarget }) {
                        true -> Color(0xFFD46858)
                        false -> Color(0xFF4F9A68)
                        null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(ratio).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.72f)))
                        Spacer(Modifier.height(5.dp))
                        Text("${date.dayOfWeek.value}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                WeeklyMetric("日均热量", if (stats.daysTracked == 0) "—" else "${stats.averageCalories}", "kcal", Modifier.weight(1f))
                WeeklyMetric("目标内", "${stats.targetHitRate}%", "${stats.daysWithinTarget} 天", Modifier.weight(1f))
                WeeklyMetric("日均蛋白", if (stats.daysTracked == 0) "—" else "${stats.averageProtein}", "g · ${stats.totalMeals} 餐", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeeklyMetric(label: String, value: String, note: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
