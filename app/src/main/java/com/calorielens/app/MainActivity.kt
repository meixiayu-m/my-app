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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.data.PersistentMealStore
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.calculateDailyTarget
import com.calorielens.app.domain.scoreMeal
import com.calorielens.app.recognition.LocalFoodRecognizer
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalorieLensApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun CalorieLensApp() {
    val context = LocalContext.current
    val store = remember { PersistentMealStore(context) }
    val meals by store.meals.collectAsStateWithLifecycle()
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

    Scaffold(topBar = { TopAppBar(title = { Text("热量小记") }) }, floatingActionButton = {
        FloatingActionButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) {
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
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("确认识别结果", style = MaterialTheme.typography.headlineSmall)
        Text("模型结果是估算值，请检查食物和份量后保存。")
        items.forEachIndexed { index, item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("${item.name} · ${item.nutrition().calories} kcal")
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
            Button(onClick = { onSave(items) }) { Text("保存餐食") }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomeScreen(meals: List<Meal>, target: com.calorielens.app.domain.DailyTarget, recognizing: Boolean, onDelete: (String) -> Unit, onEditProfile: () -> Unit, modifier: Modifier) {
    val consumed = meals.sumOf { it.nutrition().calories }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("今日概览", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("已摄入 $consumed kcal / 目标 ${target.targetCalories} kcal")
                Text("还可摄入 ${(target.targetCalories - consumed).coerceAtLeast(0)} kcal")
                Text("基础代谢 ${target.bmr} kcal · 活动消耗 ${target.tdee - target.bmr} kcal")
                Button(onClick = onEditProfile, modifier = Modifier.padding(top = 8.dp)) { Text("调整个人目标") }
            } }
            if (recognizing) Text("正在分析照片…", Modifier.padding(vertical = 8.dp))
            Text("今天的餐食", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }
        if (meals.isEmpty()) item { Text("还没有记录，点击右下角拍一张饭菜照片。") }
        items(meals, key = { it.id }) { meal -> MealCard(meal, target, onDelete) }
    }
}

@androidx.compose.runtime.Composable
private fun MealCard(meal: Meal, target: com.calorielens.app.domain.DailyTarget, onDelete: (String) -> Unit) {
    val n = meal.nutrition(); val score = scoreMeal(meal, target)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(meal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onDelete(meal.id) }) { Icon(Icons.Default.Delete, "删除") }
        }
        Text("${n.calories} kcal · 蛋白质 ${n.protein.toInt()}g · 碳水 ${n.carbs.toInt()}g · 脂肪 ${n.fat.toInt()}g")
        Text("本餐评分 ${score.total}/100", color = MaterialTheme.colorScheme.primary)
        meal.items.forEach { item -> Text("${item.name} ${item.grams.toInt()}g · ${item.nutrition().calories} kcal") }
        score.reasons.take(2).forEach { Text("· $it", style = MaterialTheme.typography.bodySmall) }
    } }
}

@androidx.compose.runtime.Composable
private fun ProfileEditor(current: UserProfile, onSave: (UserProfile) -> Unit, modifier: Modifier) {
    var weight by remember { mutableStateOf(current.weightKg.toString()) }
    var age by remember { mutableStateOf(current.age.toString()) }
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("个人目标", style = MaterialTheme.typography.headlineSmall)
        Text("当前使用默认女性、30岁、165cm、轻度活动参数。可先修改体重和年龄。")
        androidx.compose.material3.OutlinedTextField(weight, { weight = it }, label = { Text("体重（kg）") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(age, { age = it }, label = { Text("年龄") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(current.copy(weightKg = weight.toDoubleOrNull() ?: current.weightKg, age = age.toIntOrNull() ?: current.age)) }) { Text("保存") }
    }
}
