package com.calorielens.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calorielens.app.domain.Meal
import kotlinx.coroutines.delay

@Composable
fun MealRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showTitle) Text("这顿饭好吃吗？", fontWeight = FontWeight.SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(5) { index ->
                RatingStar(
                    index = index,
                    selected = index < rating,
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRatingChanged(index + 1)
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = rating > 0,
            enter = fadeIn(tween(180)) + slideInVertically { it / 3 } + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(120))
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF2B544).copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 9.dp)
            ) {
                Text(
                    "$rating 星 · ${ratingLabel(rating)}",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF9B6A0A),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (rating == 0) {
            Text("点一颗星，给这次记录留个味道", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RatingStar(index: Int, selected: Boolean, onClick: () -> Unit) {
    val bounce = remember { Animatable(1f) }
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "star-glow"
    )
    LaunchedEffect(selected) {
        if (selected) {
            bounce.snapTo(0.72f)
            bounce.animateTo(1.18f, spring(dampingRatio = 0.42f, stiffness = 620f))
            bounce.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 500f))
        }
    }
    val gold = Color(0xFFF2B544)
    val empty = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Box(
        Modifier.size(47.dp)
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .clip(CircleShape)
            .semantics {
                contentDescription = "评分 ${index + 1} 星"
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (glow > 0f) {
                drawCircle(gold.copy(alpha = glow * 0.14f), radius = size.minDimension * 0.46f)
                drawCircle(gold.copy(alpha = glow * 0.10f), radius = size.minDimension * 0.38f)
            }
            val centerPoint = center
            val outer = size.minDimension * 0.35f
            val inner = outer * 0.46f
            val star = Path().apply {
                repeat(10) { point ->
                    val radius = if (point % 2 == 0) outer else inner
                    val angle = Math.toRadians(-90.0 + point * 36.0)
                    val px = centerPoint.x + kotlin.math.cos(angle).toFloat() * radius
                    val py = centerPoint.y + kotlin.math.sin(angle).toFloat() * radius
                    if (point == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(star, if (selected) gold else empty, style = Fill)
            if (!selected) drawPath(star, empty, style = Stroke(width = 1.4.dp.toPx()))
        }
    }
}

@Composable
fun RatingDialog(meal: Meal, hapticsEnabled: Boolean, onRate: (Int) -> Unit, onSkip: () -> Unit) {
    var rating by remember(meal.id) { mutableStateOf(0) }
    var submitted by remember(meal.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(submitted) {
        if (submitted) {
            delay(680)
            onRate(rating)
        }
    }
    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(dismissOnBackPress = !submitted, dismissOnClickOutside = !submitted)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(132.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFE3A8), Color(0xFFFFB59E))))
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        repeat(8) { index ->
                            val px = size.width * (0.08f + index * 0.12f)
                            val py = size.height * (if (index % 2 == 0) 0.2f else 0.34f)
                            drawCircle(
                                Color.White.copy(alpha = 0.24f),
                                radius = (2 + index % 3).dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(px, py)
                            )
                        }
                    }
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", style = MaterialTheme.typography.headlineMedium)
                        Text("这餐，合胃口吗？", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF583813))
                        Text("${meal.title} 已记进今天的小账本", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6F4D2D))
                    }
                }
                AnimatedVisibility(visible = !submitted, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MealRatingBar(
                            rating = rating,
                            onRatingChanged = { rating = it },
                            hapticsEnabled = hapticsEnabled,
                            showTitle = false
                        )
                        Text(
                            ratingMessage(rating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = onSkip, modifier = Modifier.weight(0.42f)) { Text("稍后再评") }
                            Button(
                                onClick = {
                                    if (rating == 0) {
                                        onSkip()
                                    } else {
                                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        submitted = true
                                    }
                                },
                                modifier = Modifier.weight(0.58f),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(if (rating == 0) "跳过" else "收下这顿饭的评分") }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = submitted,
                    enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.88f),
                    exit = fadeOut()
                ) {
                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(13.dp))
                        }
                        Text("谢谢你的口味反馈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$rating 星 · ${ratingLabel(rating)}，已经记下啦", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun ratingLabel(rating: Int): String = when (rating) {
    1 -> "不太合口味"
    2 -> "一般"
    3 -> "还不错"
    4 -> "很好吃"
    5 -> "超好吃"
    else -> "暂未评价"
}

private fun ratingMessage(rating: Int): String = when (rating) {
    1 -> "没关系，记录下来才知道什么更适合自己"
    2 -> "填饱肚子也很重要，下一餐换个口味吧"
    3 -> "稳稳的一餐，普通日子也有好好吃饭"
    4 -> "看起来很满足，这一餐值得记住"
    5 -> "今天的快乐有这一餐一份！"
    else -> "喜欢的话轻轻点一颗星"
}
