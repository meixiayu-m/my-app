package com.calorielens.app

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.calorielens.app.data.PersistentMealStore
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import com.calorielens.app.domain.Sex
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.calculateDailyTarget
import com.calorielens.app.domain.scoreMeal
import com.calorielens.app.recognition.LocalFoodRecognizer
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalorieLensTheme { CalorieLensApp() } }
    }
}

@Composable
private fun CalorieLensTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Color(0xFF5C8D70),
        onPrimary = Color.White,
        secondary = Color(0xFFE69A83),
        tertiary = Color(0xFFF2C66D),
        background = Color(0xFFFFFBF5),
        surface = Color(0xFFFFFBF5),
        surfaceVariant = Color(0xFFF2F4EC)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun CalorieLensApp() {
    val context = LocalContext.current
    val store = remember { PersistentMealStore(context) }
    val meals by store.meals.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var recognizing by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var pendingItems by remember { mutableStateOf<List<FoodItem>?>(null) }
    var profile by remember { mutableStateOf(UserProfile()) }
    val target = calculateDailyTarget(profile)
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        pendingUri = null
        if (success && uri != null) {
            recognizing = true
            scope.launch {
                pendingItems = LocalFoodRecognizer().recognize(context, uri)
                recognizing = false
            }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("meal_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(context, "com.calorielens.app.fileprovider", file)
            pendingUri = uri
            takePicture.launch(uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocalFireDepartment, "热量", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text("热量小记", fontWeight = FontWeight.Bold)
                            Text("吃得开心，也吃得明白", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
        FloatingActionButton(
            onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "拍照记录")
        }
    }) { padding ->
        if (pendingItems != null) {
            MealEditor(
                initialItems = pendingItems!!,
                onCancel = { pendingItems = null },
                onSave = { items -> store.addMeal(items); pendingItems = null },
                modifier = Modifier.padding(padding)
            )
        } else if (showProfile) {
            ProfileEditor(profile, onSave = { profile = it; showProfile = false }, Modifier.padding(padding))
        } else {
            HomeScreen(meals, target, recognizing, onDelete = store::deleteMeal, onEditProfile = { showProfile = true }, modifier = Modifier.padding(padding))
        }
    }
}

@androidx.compose.runtime.Composable
private fun MealEditor(initialItems: List<FoodItem>, onCancel: () -> Unit, onSave: (List<FoodItem>) -> Unit, modifier: Modifier) {
    var items by remember(initialItems) { mutableStateOf(initialItems) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🍱", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Column {
                Text("确认识别结果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("模型结果是估算值，请检查食物和份量后保存。", style = MaterialTheme.typography.bodySmall)
            }
        }
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) { Column(Modifier.padding(14.dp)) {
                Text("${item.name} · ${item.nutrition().calories} kcal", fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.OutlinedTextField(
                    value = item.grams.toInt().toString(),
                    onValueChange = { value ->
                        val grams = value.toDoubleOrNull() ?: item.grams
                        items = items.toMutableList().also { it[index] = item.copy(grams = grams) }
                    },
                    label = { Text("份量（克）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("识别置信度 ${(item.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCancel) { Text("取消") }
            Button(onClick = { onSave(items) }) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("保存餐食")
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomeScreen(meals: List<Meal>, target: com.calorielens.app.domain.DailyTarget, recognizing: Boolean, onDelete: (String) -> Unit, onEditProfile: () -> Unit, modifier: Modifier) {
    val consumed = meals.sumOf { it.nutrition().calories }
    val progress = (consumed.toFloat() / target.targetCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EA))
            ) { Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("今日小目标", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("保持节奏，慢慢变好 🌿", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("已摄入 $consumed kcal / 目标 ${target.targetCalories} kcal")
                Text("还可摄入 ${(target.targetCalories - consumed).coerceAtLeast(0)} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("基础代谢 ${target.bmr} kcal · 活动消耗 ${target.tdee - target.bmr} kcal", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onEditProfile, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("调整个人目标")
                }
            } }
            if (recognizing) Text("正在分析照片…", Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.secondary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("🍽️", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(8.dp))
                Text("今天的餐食", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (meals.isEmpty()) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1E9))
            ) { Column(Modifier.padding(18.dp)) {
                Text("还没有记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("点击右下角拍一张饭菜照片，开始记录今天的第一餐。", style = MaterialTheme.typography.bodyMedium)
            } }
        }
        items(meals, key = { it.id }) { meal -> MealCard(meal, target, onDelete) }
    }
}

@androidx.compose.runtime.Composable
private fun MealCard(meal: Meal, target: com.calorielens.app.domain.DailyTarget, onDelete: (String) -> Unit) {
    val n = meal.nutrition(); val score = scoreMeal(meal, target)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text(meal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onDelete(meal.id) }) { Icon(Icons.Default.Delete, "删除") }
        }
        Spacer(Modifier.height(8.dp))
        Text("${n.calories} kcal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Text("蛋白质 ${n.protein.toInt()}g · 碳水 ${n.carbs.toInt()}g · 脂肪 ${n.fat.toInt()}g", style = MaterialTheme.typography.bodySmall)
        Text("本餐评分 ${score.total}/100", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        meal.items.forEach { item -> Text("${item.name} ${item.grams.toInt()}g · ${item.nutrition().calories} kcal") }
        score.reasons.take(2).forEach { Text("· $it", style = MaterialTheme.typography.bodySmall) }
    } }
}

@androidx.compose.runtime.Composable
private fun ProfileEditor(current: UserProfile, onSave: (UserProfile) -> Unit, modifier: Modifier) {
    var sex by remember { mutableStateOf(current.sex) }
    var weight by remember { mutableStateOf(current.weightKg.toString()) }
    var age by remember { mutableStateOf(current.age.toString()) }
    var height by remember { mutableStateOf(current.heightCm.toString()) }
    var activity by remember { mutableStateOf(current.activity) }
    var sexMenuOpen by remember { mutableStateOf(false) }
    var activityMenuOpen by remember { mutableStateOf(false) }
    val draft = current.copy(
        sex = sex,
        age = age.toIntOrNull() ?: current.age,
        heightCm = height.toDoubleOrNull() ?: current.heightCm,
        weightKg = weight.toDoubleOrNull() ?: current.weightKg,
        activity = activity
    )
    val draftTarget = calculateDailyTarget(draft)
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Text("个人目标", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text("完善资料后，应用会用你的运动强度估算基础代谢和每日目标。", style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { sexMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("性别：${sex.label()}")
            }
            DropdownMenu(expanded = sexMenuOpen, onDismissRequest = { sexMenuOpen = false }) {
                Sex.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = { sex = option; sexMenuOpen = false }
                    )
                }
            }
        }
        androidx.compose.material3.OutlinedTextField(weight, { weight = it }, label = { Text("体重（kg）") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(age, { age = it }, label = { Text("年龄") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(height, { height = it }, label = { Text("身高（cm）") }, modifier = Modifier.fillMaxWidth())
        Box {
            OutlinedButton(onClick = { activityMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("运动强度：${activity.label}")
            }
            DropdownMenu(expanded = activityMenuOpen, onDismissRequest = { activityMenuOpen = false }) {
                ActivityLevel.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { activity = option; activityMenuOpen = false }
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EA))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("你的代谢预览", fontWeight = FontWeight.Bold)
                Text("基础代谢 ${draftTarget.bmr} kcal · TDEE ${draftTarget.tdee} kcal")
                Text("建议每日目标 ${draftTarget.targetCalories} kcal · 安全缺口 ${draftTarget.maxSafeDeficit} kcal", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = { onSave(draft) }) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("保存资料")
        }
    }
}

private fun Sex.label(): String = when (this) {
    Sex.MALE -> "男性"
    Sex.FEMALE -> "女性"
}
