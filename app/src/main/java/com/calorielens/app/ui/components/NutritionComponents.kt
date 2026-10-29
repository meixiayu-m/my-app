package com.calorielens.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calorielens.app.domain.DailyTarget
import com.calorielens.app.domain.Nutrition

data class MacroTargets(val protein: Double, val carbs: Double, val fat: Double)

fun macroTargets(calories: Int): MacroTargets = MacroTargets(
    protein = calories * 0.20 / 4.0,
    carbs = calories * 0.50 / 4.0,
    fat = calories * 0.30 / 9.0
)

@Composable
fun MealNutritionSummary(nutrition: Nutrition, target: DailyTarget) {
    val targets = macroTargets(target.targetCalories)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("这餐营养", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("占今日目标", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniNutrientRing("🔥 热量", nutrition.calories.toFloat(), target.targetCalories.toFloat(), "${nutrition.calories}", "kcal", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MiniNutrientRing("🥩 蛋白", nutrition.protein.toFloat(), targets.protein.toFloat(), "${nutrition.protein.toInt()}g", "${(nutrition.protein / targets.protein * 100).toInt()}%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MiniNutrientRing("🍚 碳水", nutrition.carbs.toFloat(), targets.carbs.toFloat(), "${nutrition.carbs.toInt()}g", "${(nutrition.carbs / targets.carbs * 100).toInt()}%", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                MiniNutrientRing("🥑 脂肪", nutrition.fat.toFloat(), targets.fat.toFloat(), "${nutrition.fat.toInt()}g", "${(nutrition.fat / targets.fat * 100).toInt()}%", Color(0xFFE1B64B), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniNutrientRing(
    emojiLabel: String,
    value: Float,
    target: Float,
    center: String,
    unit: String,
    color: Color,
    modifier: Modifier
) {
    val progress = (value / target.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    LaunchedEffect(progress) { animated.animateTo(progress, tween(850, easing = FastOutSlowInEasing)) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(track, -90f, 360f, false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                drawArc(color, -90f, 360f * animated.value, false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(center, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(emojiLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
fun MacroBar(label: String, value: Double, target: Double, color: Color) {
    val progress by animateFloatAsState(
        targetValue = (value / target.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "$label-progress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("${value.toInt()}g · ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = color)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(color.copy(alpha = 0.14f))) {
            Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
fun NutritionRing(
    value: Float,
    target: Float,
    label: String,
    center: String,
    unit: String,
    modifier: Modifier,
    strokeWidth: Dp
) {
    val progress = (value / target.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(progress) { animated.animateTo(progress, tween(850, easing = FastOutSlowInEasing)) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animated.value,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(center, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
