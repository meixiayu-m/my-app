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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.calorielens.app.data.AppAppearance
import com.calorielens.app.data.AppTheme
import com.calorielens.app.data.AppWallpaper
import com.calorielens.app.data.AppearanceStore
import com.calorielens.app.data.ProfileStore
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
import com.calorielens.app.recognition.ZhipuModelCatalog
import com.calorielens.app.recognition.ZhipuModelInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalorieLensApp() }
    }
}

@Composable
private fun CalorieLensTheme(appearance: AppAppearance, content: @Composable () -> Unit) {
    val dark = appearance.theme == AppTheme.NIGHT
    val base = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFA8D9BD), secondary = Color(0xFFFFB39E), tertiary = Color(0xFFFFD47A),
            background = Color(0xFF171A18), surface = Color(0xFF202522), surfaceVariant = Color(0xFF303832)
        )
    } else {
        val primary = when (appearance.theme) {
            AppTheme.PEACH -> Color(0xFFB86D58)
            AppTheme.LAVENDER -> Color(0xFF7766A8)
            else -> Color(0xFF5C8D70)
        }
        val secondary = when (appearance.theme) {
            AppTheme.LAVENDER -> Color(0xFFE5A7D0)
            AppTheme.PEACH -> Color(0xFFE69A83)
            else -> Color(0xFFE69A83)
        }
        lightColorScheme(
            primary = primary, onPrimary = Color.White, secondary = secondary, tertiary = Color(0xFFF2C66D),
            background = when (appearance.wallpaper) {
                AppWallpaper.GREEN -> Color(0xFFF3FAF3)
                AppWallpaper.PINK -> Color(0xFFFFF5F1)
                AppWallpaper.BLUE -> Color(0xFFF3F7FB)
                AppWallpaper.CREAM -> Color(0xFFFFFBF5)
            },
            surface = when (appearance.wallpaper) {
                AppWallpaper.GREEN -> Color(0xFFF7FCF7)
                AppWallpaper.PINK -> Color(0xFFFFFAF7)
                AppWallpaper.BLUE -> Color(0xFFF8FBFF)
                AppWallpaper.CREAM -> Color(0xFFFFFBF5)
            },
            surfaceVariant = Color(0xFFF2F4EC)
        )
    }
    MaterialTheme(colorScheme = base, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalorieLensApp() {
    val context = LocalContext.current
    var appearance by remember { mutableStateOf(AppearanceStore.read(context)) }
    CalorieLensTheme(appearance) {
        CalorieLensContent(
            appearance = appearance,
            onAppearanceChanged = {
                AppearanceStore.save(context, it)
                appearance = it
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalorieLensContent(appearance: AppAppearance, onAppearanceChanged: (AppAppearance) -> Unit) {
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
    var showAppearance by remember { mutableStateOf(false) }
    var showManualMeal by remember { mutableStateOf(false) }
    var pendingItems by remember { mutableStateOf<List<FoodItem>?>(null) }
    var profile by remember { mutableStateOf(ProfileStore.read(context)) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    val target = calculateDailyTarget(profile)

    val recognizeImage: (Uri) -> Unit = { uri ->
        if (!recognizing) {
            recognitionError = null
            recognizing = true
            fabExpanded = false
            scope.launch {
                try {
                    pendingItems = recognizer.recognize(context, uri)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    recognitionError = error.message ?: "识别失败，请检查手机网络和智谱设置"
                } finally {
                    recognizing = false
                }
            }
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
        showAppearance = false
        showManualMeal = false
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (pendingItems != null || showProfile || showHistory || showApiKey || showAppearance || showManualMeal) {
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
                    if (pendingItems == null && !showProfile && !showHistory && !showApiKey && !showAppearance && !showManualMeal) {
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
            if (pendingItems == null && !showProfile && !showHistory && !showApiKey && !showAppearance && !showManualMeal && !recognizing) {
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
                        SmallFloatingActionButton(onClick = {
                            fabExpanded = false
                            showManualMeal = true
                        }) {
                            Icon(Icons.Default.Restaurant, contentDescription = "手动添加")
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
                onSave = { items, title, rating -> store.addMeal(items, title = title, rating = rating); pendingItems = null },
                modifier = Modifier.padding(padding)
            )
            showManualMeal -> ManualMealEditor(
                onCancel = { showManualMeal = false },
                onSave = { items, title, rating -> store.addMeal(items, title = title, rating = rating); showManualMeal = false },
                modifier = Modifier.padding(padding)
            )
            showProfile -> ProfileEditor(profile, onSave = {
                ProfileStore.save(context, it)
                profile = it
                showProfile = false
            }, Modifier.padding(padding))
            showHistory -> HistoryScreen(meals, target, Modifier.padding(padding))
            showApiKey -> ApiKeyEditor(
                currentKey = ApiKeyStore.read(context).orEmpty(),
                currentModel = ApiKeyStore.readModel(context).orEmpty(),
                onSave = { key, model ->
                    ApiKeyStore.save(context, key)
                    ApiKeyStore.saveModel(context, model)
                    showApiKey = false
                },
                onClear = { ApiKeyStore.clear(context); showApiKey = false },
                onAppearance = { showApiKey = false; showAppearance = true },
                modifier = Modifier.padding(padding)
            )
            showAppearance -> AppearanceEditor(
                current = appearance,
                onSave = { onAppearanceChanged(it); showAppearance = false },
                modifier = Modifier.padding(padding)
            )
            else -> HomeScreen(
                meals = meals,
                target = target,
                recognizing = recognizing,
                errorMessage = recognitionError,
                onDismissError = { recognitionError = null },
                onDelete = { meal -> store.deleteMeal(meal) },
                onRestore = store::restoreMeal,
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
    onSave: (List<FoodItem>, String, Int) -> Unit,
    modifier: Modifier
) {
    var items by remember(initialItems) { mutableStateOf(initialItems) }
    var title by remember { mutableStateOf(defaultMealTitle()) }
    var rating by remember { mutableStateOf(0) }
    var mealMenuOpen by remember { mutableStateOf(false) }
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
        Box {
            OutlinedButton(onClick = { mealMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("餐次：$title（可修改）") }
            DropdownMenu(expanded = mealMenuOpen, onDismissRequest = { mealMenuOpen = false }) {
                MEAL_TYPES.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { title = option; mealMenuOpen = false })
                }
            }
        }
        MealRatingBar(rating = rating, onRatingChanged = { rating = it })
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("${item.name} · 本份约 ${item.nutrition().calories} kcal", fontWeight = FontWeight.SemiBold)
                    Text("参考密度：${item.caloriesPer100g.toInt()} kcal / 100g · 识别份量：${item.grams.toInt()}g", style = MaterialTheme.typography.bodySmall)
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
            Button(onClick = { onSave(items, title, rating) }) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("保存餐食")
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun MealRatingBar(rating: Int, onRatingChanged: (Int) -> Unit) {
    Column {
        Text("这顿饭好吃吗？", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                Text(
                    text = if (index < rating) "★" else "☆",
                    color = if (index < rating) Color(0xFFF2B544) else MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .clickable { onRatingChanged(index + 1) }
                        .padding(end = 3.dp)
                )
            }
            Text(if (rating == 0) "暂不评价" else "$rating/5 · ${ratingLabel(rating)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun ratingLabel(rating: Int): String = when (rating) {
    1 -> "不太合口味"
    2 -> "一般"
    3 -> "还不错"
    4 -> "很好吃"
    else -> "超好吃"
}

private val MEAL_TYPES = listOf("早餐", "午餐", "晚餐", "下午茶", "夜宵", "加餐", "其他")

private fun defaultMealTitle(time: LocalTime = LocalTime.now()): String = when {
    time < LocalTime.of(10, 30) -> "早餐"
    time >= LocalTime.of(11, 0) && time < LocalTime.of(14, 0) -> "午餐"
    time >= LocalTime.of(17, 0) -> "晚餐"
    else -> "下午茶"
}

@Composable
private fun ManualMealEditor(
    onCancel: () -> Unit,
    onSave: (List<FoodItem>, String, Int) -> Unit,
    modifier: Modifier
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var calories by remember { mutableStateOf("0") }
    var protein by remember { mutableStateOf("0") }
    var fat by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var title by remember { mutableStateOf("下午茶") }
    var rating by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && (grams.toDoubleOrNull() ?: 0.0) > 0 && (calories.toDoubleOrNull() ?: -1.0) >= 0
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("手动添加餐食", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("适合下午茶、零食或 AI 没识别到的小吃。营养值按每 100 克填写。", style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("餐次：$title") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                MEAL_TYPES.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { title = option; menuOpen = false }) }
            }
        }
        androidx.compose.material3.OutlinedTextField(name, { name = it }, label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(grams, { grams = it }, label = { Text("份量（克）") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(calories, { calories = it }, label = { Text("热量（每 100 克 kcal）") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(protein, { protein = it }, label = { Text("蛋白质 g") }, modifier = Modifier.weight(1f))
            androidx.compose.material3.OutlinedTextField(fat, { fat = it }, label = { Text("脂肪 g") }, modifier = Modifier.weight(1f))
            androidx.compose.material3.OutlinedTextField(carbs, { carbs = it }, label = { Text("碳水 g") }, modifier = Modifier.weight(1f))
        }
        MealRatingBar(rating, { rating = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("取消") }
            Button(onClick = {
                onSave(listOf(FoodItem(
                    id = "manual-${System.currentTimeMillis()}", name = name.trim(), grams = grams.toDouble(),
                    caloriesPer100g = calories.toDouble(), proteinPer100g = protein.toDoubleOrNull() ?: 0.0,
                    fatPer100g = fat.toDoubleOrNull() ?: 0.0, carbsPer100g = carbs.toDoubleOrNull() ?: 0.0,
                    fiberPer100g = 0.0, confidence = 1f
                )), title, rating)
            }, enabled = valid) { Text("保存手动记录") }
        }
    }
}

@Composable
private fun HomeScreen(
    meals: List<Meal>,
    target: com.calorielens.app.domain.DailyTarget,
    recognizing: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onDelete: (Meal) -> Unit,
    onRestore: (Meal) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier
) {
    val todayMeals = meals.filter { it.localDate() == LocalDate.now() }
    var deleteCandidate by remember { mutableStateOf<Meal?>(null) }
    var undoMeal by remember { mutableStateOf<Meal?>(null) }
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
            undoMeal?.let { deleted ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("已删除「${deleted.title}」", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { undoMeal = null; onRestore(deleted) }) { Text("撤回") }
                    }
                }
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
        items(todayMeals, key = { it.id }) { meal -> MealCard(meal, target, onDelete = { deleteCandidate = meal }) }
    }
    deleteCandidate?.let { meal ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("确认删除？") },
            text = { Text("删除「${meal.title}」后会从本机日志移除，但你可以在提示出现后撤回。") },
            confirmButton = { TextButton(onClick = { deleteCandidate = null; undoMeal = meal; onDelete(meal) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("取消") } }
        )
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
private fun MealCard(meal: Meal, target: com.calorielens.app.domain.DailyTarget, onDelete: (() -> Unit)?) {
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
                if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
            }
            Spacer(Modifier.height(8.dp))
            Text("${n.calories} kcal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text("蛋白质 ${n.protein.toInt()}g · 碳水 ${n.carbs.toInt()}g · 脂肪 ${n.fat.toInt()}g", style = MaterialTheme.typography.bodySmall)
            Text("本餐评分 ${score.total}/100", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            if (meal.rating > 0) Text("我的口味评分：${"★".repeat(meal.rating)}${"☆".repeat(5 - meal.rating)}", color = Color(0xFFF2B544))
            meal.items.forEach { item -> Text("${item.name} ${item.grams.toInt()}g · ${item.caloriesPer100g.toInt()} kcal/100g · 本份 ${item.nutrition().calories} kcal") }
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
    onAppearance: () -> Unit,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()
    var key by remember(currentKey) { mutableStateOf(currentKey) }
    var model by remember(currentModel) { mutableStateOf(currentModel) }
    var modelOptions by remember { mutableStateOf<List<ZhipuModelInfo>>(emptyList()) }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var loadingModels by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf<String?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("智谱识别设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("个人模式会从本机直接调用 GLM-4.6V-Flash。Key 使用 Android Keystore 加密保存，不会写入项目或 GitHub。", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.OutlinedTextField(
            value = key,
            onValueChange = {
                key = it
                modelOptions = emptyList()
                modelError = null
            },
            label = { Text("智谱 API Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Box {
            OutlinedButton(
                onClick = { modelMenuOpen = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = modelOptions.isNotEmpty()
            ) {
                Text(if (modelOptions.isEmpty()) "模型列表为空（可手动填写）" else "选择模型：$model")
            }
            DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                modelOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.id)
                                Text(option.limitsLabel(), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        onClick = {
                            model = option.id
                            modelMenuOpen = false
                        }
                    )
                }
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("模型名称（接口 ID）") },
            supportingText = { Text("可从上方列表选择；留空会使用 ${ApiKeyStore.DEFAULT_ZHIPU_MODEL}，也可以手动填写视觉模型 ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    modelError = null
                    loadingModels = true
                    scope.launch {
                        try {
                            modelOptions = ZhipuModelCatalog.fetch(key)
                            if (modelOptions.isEmpty()) modelError = "接口没有返回可用模型，请检查 Key 权限或手动填写模型 ID"
                        } catch (error: Exception) {
                            modelError = error.message ?: "模型列表获取失败，请检查网络和 API Key"
                        } finally {
                            loadingModels = false
                        }
                    }
                },
                enabled = key.isNotBlank() && !loadingModels
            ) {
                Text(if (loadingModels) "正在获取…" else "刷新模型列表")
            }
            if (loadingModels) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        modelError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text("模型列表可能只返回模型 ID；每分钟限流、剩余 Token 和免费额度通常不由此接口提供，请以智谱控制台为准。", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { onSave(key, model) }, enabled = key.isNotBlank()) { Text("保存设置") }
        OutlinedButton(onClick = onAppearance) { Text("主题与壁纸") }
        OutlinedButton(onClick = onClear, enabled = currentKey.isNotBlank()) { Text("清除本机 Key") }
        Text("提示：个人使用可以这样配置，但如果 APK 分享给别人，Key 仍可能被提取。公开版本应改回后端代理模式。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppearanceEditor(current: AppAppearance, onSave: (AppAppearance) -> Unit, modifier: Modifier) {
    var theme by remember(current) { mutableStateOf(current.theme) }
    var wallpaper by remember(current) { mutableStateOf(current.wallpaper) }
    var themeMenuOpen by remember { mutableStateOf(false) }
    var wallpaperMenuOpen by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("主题与壁纸", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("识别结果页、首页卡片和按钮会跟随主题颜色变化；壁纸选项使用轻量纯色背景，不会增加图片体积。", style = MaterialTheme.typography.bodySmall)
        Box {
            OutlinedButton(onClick = { themeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("主题：${theme.label}") }
            DropdownMenu(expanded = themeMenuOpen, onDismissRequest = { themeMenuOpen = false }) {
                AppTheme.values().forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { theme = option; themeMenuOpen = false })
                }
            }
        }
        Box {
            OutlinedButton(onClick = { wallpaperMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("壁纸：${wallpaper.label}") }
            DropdownMenu(expanded = wallpaperMenuOpen, onDismissRequest = { wallpaperMenuOpen = false }) {
                AppWallpaper.values().forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { wallpaper = option; wallpaperMenuOpen = false })
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("预览", fontWeight = FontWeight.Bold)
                Text("识别成功后会先进入确认页，你可以改餐次、星级和份量，再保存。")
            }
        }
        Button(onClick = { onSave(AppAppearance(theme, wallpaper)) }) { Text("应用外观") }
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
