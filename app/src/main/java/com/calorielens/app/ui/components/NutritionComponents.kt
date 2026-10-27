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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            NutritionRing(
                value = nutrition.calories.toFloat(),
                target = target.targetCalories.toFloat(),
                label = "本餐热量",
                center = "${nutrition.calories}",
                unit = "kcal",
                modifier = Modifier.size(128.dp),
                strokeWidth = 11.dp
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("占今日目标", fontWeight = FontWeight.Bold)
                MacroBar("蛋白质", nutrition.protein, targets.protein, MaterialTheme.colorScheme.primary)
                MacroBar("碳水", nutrition.carbs, targets.carbs, MaterialTheme.colorScheme.secondary)
                MacroBar("脂肪", nutrition.fat, targets.fat, Color(0xFFE1B64B))
            }
        }
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
