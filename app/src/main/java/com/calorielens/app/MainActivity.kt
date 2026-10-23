package com.calorielens.app

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.calorielens.app.data.ApiKeyStore
import com.calorielens.app.data.PersistentMealStore
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import com.calorielens.app.domain.Nutrition
import com.calorielens.app.domain.Sex
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.calculateDailyTarget
import com.calorielens.app.domain.scoreMeal
import com.calorielens.app.recognition.DirectZhipuFoodRecognizer
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
@Composable
private fun CalorieLensApp() {
    val context = LocalContext.current
    val store = remember { PersistentMealStore(context) }
    val meals by store.meals.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val recognizer = remember { DirectZhipuFoodRecognizer() }
    var recognizing by remember { mutableStateOf(false) }
    var recognitionError by remember { mutableStateOf<String?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var pendingItems by remember { mutableStateOf<List<FoodItem>?>(null) }
    var profile by remember { mutableStateOf(UserProfile()) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    val target = calculateDailyTarget(profile)

    val recognizeImage: (Uri) -> Unit = { uri ->
        recognitionError = null
        recognizing = true
        scope.launch {
            runCatching { recognizer.recognize(context, uri) }
                .onSuccess { pendingItems = it }
                .onFailure { recognitionError = it.message ?: "识别失败，请检查后端和网络" }
            recognizing = false
        }
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        pendingUri = null
        if (success && uri != null) recognizeImage(uri)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("meal_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(context, "com.calorielens.app.fileprovider", file)
            pendingUri = uri
            takePicture.launch(uri)
        } else {
            recognitionError = "需要相机权限才能拍照识别，也可以直接从相册选择图片"
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) recognizeImage(uri)
    }

    val goHome = {
        pendingItems = null
        showProfile = false
        showHistory = false
        showApiKey = false
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (pendingItems != null || showProfile || showHistory || showApiKey) {
                        IconButton(onClick = goHome) { Icon(Icons.Default.ArrowBack, "返回") }
                    }
                },
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
                actions = {
                    if (pendingItems == null && !showProfile && !showHistory && !showApiKey) {
                        IconButton(onClick = { showHistory = true }) {
                            Icon(Icons.Default.History, contentDescription = "饮食日志")
                        }
                        IconButton(onClick = { showApiKey = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置智谱 Key")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (pendingItems == null && !showProfile && !showHistory && !showApiKey) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (fabExpanded) {
                        SmallFloatingActionButton(onClick = {
                            fabExpanded = false
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "拍照识别")
                        }
                        SmallFloatingActionButton(onClick = {
                            fabExpanded = false
                            gallery.launch("image/*")
                        }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "从相册选择")
                        }
                    }
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ) {
                        Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.AddAPhoto, contentDescription = "添加餐食")
                    }
                }
            }
        }
    ) { padding ->
        when {
            pendingItems != null -> MealEditor(
                initialItems = pendingItems!!,
                onCancel = { pendingItems = null },
                onSave = { items -> store.addMeal(items); pendingItems = null },
                modifier = Modifier.padding(padding)
            )
            showProfile -> ProfileEditor(profile, onSave = { profile = it; showProfile = false }, Modifier.padding(padding))
            showHistory -> HistoryScreen(meals, target, Modifier.padding(padding))
            showApiKey -> ApiKeyEditor(
                currentKey = ApiKeyStore.read(context).orEmpty(),
                currentModel = ApiKeyStore.readModel(context) ?: ApiKeyStore.DEFAULT_ZHIPU_MODEL,
                onSave = { key, model ->
                    ApiKeyStore.save(context, key)
                    ApiKeyStore.saveModel(context, model)
                    showApiKey = false
                },
                onClear = { ApiKeyStore.clear(context); showApiKey = false },
                modifier = Modifier.padding(padding)
            )
            else -> HomeScreen(
                meals = meals,
                target = target,
                recognizing = recognizing,
                errorMessage = recognitionError,
                onDismissError = { recognitionError = null },
                onDelete = store::deleteMeal,
                onEditProfile = { showProfile = true },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun MealEditor(
    initialItems: List<FoodItem>,
    onCancel: () -> Unit,
    onSave: (List<FoodItem>) -> Unit,
    modifier: Modifier
) {
    var items by remember(initialItems) { mutableStateOf(initialItems) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🍱", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Column {
                Text("确认识别结果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("云端模型结果是估算值，请检查食物和份量后保存。", style = MaterialTheme.typography.bodySmall)
            }
        }
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("${item.name} · ${item.nutrition().calories} kcal", fontWeight = FontWeight.SemiBold)
                    androidx.compose.material3.OutlinedTextField(
                        value = item.grams.toInt().toString(),
                        onValueChange = { value ->
                            val grams = value.toDoubleOrNull() ?: item.grams
                            items = items.toMutableList().also { it[index] = item.copy(grams = grams.coerceAtLeast(1.0)) }
                        },
                        label = { Text("份量（克）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("识别置信度 ${(item.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCancel) { Text("取消") }
            Button(onClick = { onSave(items) }) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("保存餐食")
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun HomeScreen(
    meals: List<Meal>,
    target: com.calorielens.app.domain.DailyTarget,
    recognizing: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onDelete: (String) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier
) {
    val todayMeals = meals.filter { it.localDate() == LocalDate.now() }
    val consumed = todayMeals.sumOf { it.nutrition().calories }
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
            ) {
                Column(Modifier.padding(18.dp)) {
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
                }
            }
            if (recognizing) Text("正在上传图片并分析…", Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.secondary)
            if (errorMessage != null) {
                val message = errorMessage
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8E0)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("关闭", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = onDismissError) { Icon(Icons.Default.Close, "关闭提示") }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("🍽️", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(8.dp))
                Text("今天的餐食", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (todayMeals.isEmpty()) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1E9))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("还没有记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("点击右下角，选择拍照或从相册挑一张饭菜图片。", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(todayMeals, key = { it.id }) { meal -> MealCard(meal, target, onDelete) }
    }
}

@Composable
private fun HistoryScreen(meals: List<Meal>, target: com.calorielens.app.domain.DailyTarget, modifier: Modifier) {
    val grouped = meals.groupBy { it.localDate() }.entries.sortedByDescending { it.key }
    val todayMeals = grouped.firstOrNull { it.key == LocalDate.now() }?.value.orEmpty()
    val todayNutrition = todayMeals.totalNutrition()
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("饮食日志", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("按日期查看每一天的摄入和餐食记录。", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EA)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("今天概览", fontWeight = FontWeight.Bold)
                    Text("${todayNutrition.calories} / ${target.targetCalories} kcal · ${todayMeals.size} 餐")
                    Text("蛋白质 ${todayNutrition.protein.toInt()}g · 碳水 ${todayNutrition.carbs.toInt()}g · 脂肪 ${todayNutrition.fat.toInt()}g · 纤维 ${todayNutrition.fiber.toInt()}g", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (grouped.isEmpty()) item {
            Text("还没有历史记录。保存第一餐后，这里会自动生成每日统计。", modifier = Modifier.padding(vertical = 16.dp))
        }
        grouped.forEach { (day, dayMeals) ->
            item(key = day.toString()) {
                val n = dayMeals.totalNutrition()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${day.year}年${day.monthValue}月${day.dayOfMonth}日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${n.calories} kcal · 蛋白质 ${n.protein.toInt()}g · ${dayMeals.size} 餐", style = MaterialTheme.typography.bodySmall)
                    dayMeals.sortedByDescending { it.timestamp }.forEach { meal -> MealCard(meal, target, onDelete = null) }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: Meal, target: com.calorielens.app.domain.DailyTarget, onDelete: ((String) -> Unit)?) {
    val n = meal.nutrition()
    val score = scoreMeal(meal, target)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.size(10.dp))
                Text(meal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onDelete != null) IconButton(onClick = { onDelete(meal.id) }) { Icon(Icons.Default.Delete, "删除") }
            }
            Spacer(Modifier.height(8.dp))
            Text("${n.calories} kcal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text("蛋白质 ${n.protein.toInt()}g · 碳水 ${n.carbs.toInt()}g · 脂肪 ${n.fat.toInt()}g", style = MaterialTheme.typography.bodySmall)
            Text("本餐评分 ${score.total}/100", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            meal.items.forEach { item -> Text("${item.name} ${item.grams.toInt()}g · ${item.nutrition().calories} kcal") }
            score.reasons.take(2).forEach { Text("· $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ApiKeyEditor(
    currentKey: String,
    currentModel: String,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier
) {
    var key by remember(currentKey) { mutableStateOf(currentKey) }
    var model by remember(currentModel) { mutableStateOf(currentModel) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("智谱识别设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("个人模式会从本机直接调用 GLM-4.6V-Flash。Key 使用 Android Keystore 加密保存，不会写入项目或 GitHub。", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("智谱 API Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("模型名称（接口 ID）") },
            supportingText = { Text("默认：glm-4.6v-flash；也可填写你账号有权限的其他视觉模型") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onSave(key, model) }, enabled = key.isNotBlank() && model.isNotBlank()) { Text("保存设置") }
        OutlinedButton(onClick = onClear, enabled = currentKey.isNotBlank()) { Text("清除本机 Key") }
        Text("提示：个人使用可以这样配置，但如果 APK 分享给别人，Key 仍可能被提取。公开版本应改回后端代理模式。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Text("个人目标", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text("完善资料后，应用会用你的运动强度估算基础代谢和每日目标。", style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { sexMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("性别：${sex.label()}") }
            DropdownMenu(expanded = sexMenuOpen, onDismissRequest = { sexMenuOpen = false }) {
                Sex.values().forEach { option -> DropdownMenuItem(text = { Text(option.label()) }, onClick = { sex = option; sexMenuOpen = false }) }
            }
        }
        androidx.compose.material3.OutlinedTextField(weight, { weight = it }, label = { Text("体重（kg）") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(age, { age = it }, label = { Text("年龄") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(height, { height = it }, label = { Text("身高（cm）") }, modifier = Modifier.fillMaxWidth())
        Box {
            OutlinedButton(onClick = { activityMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("运动强度：${activity.label}") }
            DropdownMenu(expanded = activityMenuOpen, onDismissRequest = { activityMenuOpen = false }) {
                ActivityLevel.values().forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { activity = option; activityMenuOpen = false }) }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EA))) {
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
        Spacer(Modifier.height(40.dp))
    }
}

private fun Sex.label(): String = when (this) {
    Sex.MALE -> "男性"
    Sex.FEMALE -> "女性"
}

private fun Meal.localDate(): LocalDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

private fun List<Meal>.totalNutrition(): Nutrition = fold(Nutrition(0, 0.0, 0.0, 0.0, 0.0)) { total, meal ->
    val n = meal.nutrition()
    Nutrition(total.calories + n.calories, total.protein + n.protein, total.fat + n.fat, total.carbs + n.carbs, total.fiber + n.fiber)
}
