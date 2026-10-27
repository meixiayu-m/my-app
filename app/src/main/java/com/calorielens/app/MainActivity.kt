package com.calorielens.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.calorielens.app.data.ApiKeyStore
import com.calorielens.app.data.AppAppearance
import com.calorielens.app.data.AppTheme
import com.calorielens.app.data.AppWallpaper
import com.calorielens.app.data.AppearanceStore
import com.calorielens.app.data.BackupStore
import com.calorielens.app.data.ProfileStore
import com.calorielens.app.data.PersistentMealStore
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.DailySummary
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import com.calorielens.app.domain.Nutrition
import com.calorielens.app.domain.Sex
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.calculateDailyTarget
import com.calorielens.app.domain.calculateWeeklyStats
import com.calorielens.app.domain.scoreMeal
import com.calorielens.app.recognition.DirectZhipuFoodRecognizer
import com.calorielens.app.recognition.ZhipuModelCatalog
import com.calorielens.app.recognition.ZhipuModelInfo
import com.calorielens.app.ui.components.MealRatingBar
import com.calorielens.app.ui.components.MealNutritionSummary
import com.calorielens.app.ui.components.MacroBar
import com.calorielens.app.ui.components.NutritionRing
import com.calorielens.app.ui.components.RatingDialog
import com.calorielens.app.ui.components.WeeklyOverviewCard
import com.calorielens.app.ui.components.macroTargets
import com.calorielens.app.ui.theme.CalorieLensTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalorieLensApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalorieLensApp() {
    val context = LocalContext.current
    var appearance by remember { mutableStateOf(AppearanceStore.read(context)) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { ApiKeyStore.read(context) }
    }
    CalorieLensTheme(appearance) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CustomWallpaper(appearance.customWallpaperUri)
            BackgroundAccents()
            CalorieLensContent(
                appearance = appearance,
                onAppearanceChanged = {
                    AppearanceStore.save(context, it)
                    appearance = it
                }
            )
        }
    }
}

@Composable
private fun BackgroundAccents() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            color = primary.copy(alpha = 0.055f),
            radius = size.width * 0.46f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.12f)
        )
        drawCircle(
            color = secondary.copy(alpha = 0.045f),
            radius = size.width * 0.34f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.72f)
        )
    }
}

@Composable
private fun CustomWallpaper(uri: String?) {
    if (uri.isNullOrBlank()) return
    val context = LocalContext.current
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    image?.let {
        androidx.compose.foundation.Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.22f
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalorieLensContent(appearance: AppAppearance, onAppearanceChanged: (AppAppearance) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val store = remember { PersistentMealStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var todayDate by remember { mutableStateOf(LocalDate.now()) }
    val todayMealsFlow = remember(todayDate) { store.mealsForDate(todayDate) }
    val todayMeals by todayMealsFlow.collectAsState(initial = emptyList())
    val dailySummaries by store.dailySummaries.collectAsState(initial = emptyList())
    val recentFoods by store.recentFoods.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val recognizer = remember { DirectZhipuFoodRecognizer() }
    var recognizing by remember { mutableStateOf(false) }
    var recognitionError by remember { mutableStateOf<String?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showManualMeal by remember { mutableStateOf(false) }
    var ratingTarget by remember { mutableStateOf<Meal?>(null) }
    var editingMeal by remember { mutableStateOf<Meal?>(null) }
    var editingComment by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingItems by remember { mutableStateOf<List<FoodItem>?>(null) }
    var pendingComment by remember { mutableStateOf("") }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var cropCandidateUri by remember { mutableStateOf<Uri?>(null) }
    var cropLaunchSource by remember { mutableStateOf<Uri?>(null) }
    var cropOutputUri by remember { mutableStateOf<Uri?>(null) }
    var refining by remember { mutableStateOf(false) }
    var refinementError by remember { mutableStateOf<String?>(null) }
    var recognitionStage by remember { mutableStateOf("正在准备照片…") }
    var profile by remember { mutableStateOf(ProfileStore.read(context)) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    val target = calculateDailyTarget(profile)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        todayDate = LocalDate.now()
        scope.launch { store.performMaintenance() }
    }
    LaunchedEffect(recognizing) {
        if (recognizing) {
            var index = kotlin.random.Random.nextInt(RECOGNITION_WAIT_LINES.size)
            while (true) {
                recognitionStage = RECOGNITION_WAIT_LINES[index % RECOGNITION_WAIT_LINES.size]
                index++
                delay(2300)
            }
        }
    }

    val recognizeImage: (Uri) -> Unit = { uri ->
        if (!recognizing) {
            recognitionError = null
            recognizing = true
            fabExpanded = false
            pendingImageUri = uri
            recognitionStage = RECOGNITION_WAIT_LINES.first()
            scope.launch {
                try {
                    recognitionStage = "正在理解画面并估算营养…"
                    val result = recognizer.recognize(context, uri)
                    pendingItems = result.items
                    pendingComment = result.comment
                    pendingImageUri = uri
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    pendingImageUri = null
                    recognitionError = error.message ?: "识别失败，请检查手机网络和智谱设置"
                } finally {
                    recognizing = false
                }
            }
        }
    }
    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val output = cropOutputUri
        cropLaunchSource = null
        cropOutputUri = null
        val wroteOutput = output != null && runCatching {
            context.contentResolver.openAssetFileDescriptor(output, "r")?.use { it.length > 0 } == true
        }.getOrDefault(false)
        if ((result.resultCode == Activity.RESULT_OK || wroteOutput) && output != null) {
            recognizeImage(output)
        }
    }
    val launchCrop: (Uri) -> Unit = { source ->
        runCatching {
            val outputFile = File.createTempFile("crop_", ".jpg", File(context.cacheDir, "images").apply { mkdirs() })
            val output = FileProvider.getUriForFile(context, "com.calorielens.app.fileprovider", outputFile)
            cropLaunchSource = source
            cropOutputUri = output
            val intent = Intent("com.android.camera.action.CROP").apply {
                setDataAndType(source, "image/*")
                clipData = ClipData.newRawUri("source", source)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra("crop", "true")
                putExtra("aspectX", 4)
                putExtra("aspectY", 3)
                putExtra("scale", true)
                putExtra("return-data", false)
                putExtra("output", output)
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, output)
            }
            cropLauncher.launch(intent)
        }.onFailure {
            cropLaunchSource?.let(recognizeImage)
            cropLaunchSource = null
            cropOutputUri = null
            scope.launch { snackbarHostState.showSnackbar("此手机不支持系统裁剪，已使用原图识别") }
        }
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        pendingUri = null
        if (success && uri != null) cropCandidateUri = uri
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
        if (uri != null) cropCandidateUri = uri
    }
    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    BackupStore.write(
                        context = context,
                        target = uri,
                        meals = store.allMealsSnapshot(),
                        summaries = store.summariesSnapshot(),
                        profile = profile,
                        appearance = appearance
                    )
                }.onSuccess {
                    backupMessage = "备份已导出；API Key 未包含在文件中。"
                }.onFailure {
                    backupMessage = "备份失败：${it.message ?: "无法写入文件"}"
                }
            }
        }
    }
    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val payload = BackupStore.read(context, uri)
                    store.restoreBackup(payload.meals, payload.summaries)
                    ProfileStore.save(context, payload.profile)
                    AppearanceStore.save(context, payload.appearance)
                    profile = payload.profile
                    onAppearanceChanged(payload.appearance)
                }.onSuccess {
                    backupMessage = "备份已导入。API Key 不在备份中；新安装后需重新填写。"
                }.onFailure {
                    backupMessage = "导入失败：${it.message ?: "文件格式不正确"}"
                }
            }
        }
    }

    val goHome = {
        pendingItems = null
        pendingImageUri = null
        pendingComment = ""
        refinementError = null
        showProfile = false
        showHistory = false
        showApiKey = false
        showAppearance = false
        showManualMeal = false
        editingMeal = null
        editingComment = ""
        cropCandidateUri = null
    }
    Scaffold(
        containerColor = if (appearance.customWallpaperUri != null || appearance.glass) Color.Transparent else MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (pendingItems != null || editingMeal != null || showProfile || showHistory || showApiKey || showAppearance || showManualMeal) {
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
                    if (pendingItems == null && editingMeal == null && !showProfile && !showHistory && !showApiKey && !showAppearance && !showManualMeal) {
                        IconButton(onClick = { showHistory = true }) {
                            Icon(Icons.Default.History, contentDescription = "饮食日志")
                        }
                        IconButton(onClick = { showApiKey = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置智谱 Key")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (pendingItems == null && editingMeal == null && !showProfile && !showHistory && !showApiKey && !showAppearance && !showManualMeal && !recognizing) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.7f) + slideInVertically { it / 2 },
                        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f) + slideOutVertically { it / 2 }
                    ) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionBubble("拍照识别", Icons.Default.CameraAlt, appearance.haptics) {
                                fabExpanded = false
                                cameraPermission.launch(Manifest.permission.CAMERA)
                            }
                            ActionBubble("相册选择", Icons.Default.PhotoLibrary, appearance.haptics) {
                                fabExpanded = false
                                gallery.launch("image/*")
                            }
                            ActionBubble("手动添加", Icons.Default.Restaurant, appearance.haptics) {
                                fabExpanded = false
                                showManualMeal = true
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            if (appearance.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            fabExpanded = !fabExpanded
                        },
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
                imageUri = pendingImageUri,
                target = target,
                aiComment = pendingComment,
                refinementError = refinementError,
                refining = refining,
                hapticsEnabled = appearance.haptics,
                onCancel = { pendingItems = null; pendingImageUri = null; pendingComment = "" },
                onRefine = { items, note ->
                    val imageUri = pendingImageUri
                    if (imageUri != null && !refining && note.isNotBlank()) {
                        refinementError = null
                        refining = true
                        scope.launch {
                            try {
                                val result = recognizer.refine(context, imageUri, items, note)
                                pendingItems = result.items
                                pendingComment = result.comment
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                refinementError = error.message ?: "AI 修正失败，请稍后重试"
                            } finally {
                                refining = false
                            }
                        }
                    }
                },
                onSave = { items, title, _ ->
                    scope.launch {
                        ratingTarget = store.addMealAndReturn(
                            items = items,
                            title = title,
                            targetCalories = target.targetCalories,
                            imageSourceUri = pendingImageUri
                        )
                        pendingItems = null
                        pendingImageUri = null
                        pendingComment = ""
                    }
                },
                modifier = Modifier.padding(padding)
            )
            editingMeal != null -> {
                val meal = editingMeal!!
                val existingImage = meal.imagePath?.let(::File)?.takeIf(File::exists)?.let(Uri::fromFile)
                MealEditor(
                    initialItems = meal.items,
                    imageUri = existingImage,
                    target = target,
                    aiComment = editingComment,
                    refinementError = refinementError,
                    refining = refining,
                    hapticsEnabled = appearance.haptics,
                    initialTitle = meal.title,
                    initialRating = meal.rating,
                    allowRatingEdit = true,
                    onCancel = { editingMeal = null; editingComment = "" },
                    onRefine = { items, note ->
                        if (existingImage != null && !refining && note.isNotBlank()) {
                            refining = true
                            refinementError = null
                            scope.launch {
                                try {
                                    val result = recognizer.refine(context, existingImage, items, note)
                                    editingMeal = meal.copy(items = result.items)
                                    editingComment = result.comment
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    refinementError = error.message ?: "AI 修正失败，请稍后重试"
                                } finally {
                                    refining = false
                                }
                            }
                        }
                    },
                    onSave = { items, title, rating ->
                        scope.launch {
                            store.updateMealAndReturn(meal, items, title, rating, target.targetCalories)
                            editingMeal = null
                            editingComment = ""
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            showManualMeal -> ManualMealEditor(
                recentFoods = recentFoods,
                onCancel = { showManualMeal = false },
                onSave = { items, title ->
                    scope.launch {
                        ratingTarget = store.addMealAndReturn(
                            items = items,
                            title = title,
                            targetCalories = target.targetCalories
                        )
                        showManualMeal = false
                    }
                },
                modifier = Modifier.padding(padding)
            )
            showProfile -> ProfileEditor(profile, onSave = {
                ProfileStore.save(context, it)
                profile = it
                showProfile = false
            }, Modifier.padding(padding))
            showHistory -> HistoryScreen(
                store = store,
                summaries = dailySummaries,
                target = target,
                hapticsEnabled = appearance.haptics,
                onEdit = { editingMeal = it; showHistory = false },
                onCopy = { meal ->
                    scope.launch {
                        store.copyMealToToday(meal, target.targetCalories)
                        snackbarHostState.showSnackbar("已复制「${meal.title}」到今天")
                    }
                },
                modifier = Modifier.padding(padding)
            )
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
                onExportBackup = { backupExporter.launch("calorie-lens-backup-${LocalDate.now()}.json") },
                onImportBackup = { backupImporter.launch(arrayOf("application/json", "text/plain")) },
                backupMessage = backupMessage,
                modifier = Modifier.padding(padding)
            )
            showAppearance -> AppearanceEditor(
                current = appearance,
                onSave = { onAppearanceChanged(it); showAppearance = false },
                modifier = Modifier.padding(padding)
            )
            else -> HomeScreen(
                meals = todayMeals,
                target = target,
                recognizing = recognizing,
                recognizingImageUri = pendingImageUri.takeIf { recognizing },
                recognitionStage = recognitionStage,
                errorMessage = recognitionError,
                hapticsEnabled = appearance.haptics,
                onDismissError = { recognitionError = null },
                onDelete = { meal -> store.deleteMeal(meal) },
                onRestore = { meal -> store.restoreMeal(meal, target.targetCalories) },
                onEdit = { editingMeal = it },
                onEditProfile = { showProfile = true },
                modifier = Modifier.padding(padding)
            )
        }
    }
    cropCandidateUri?.let { source ->
        AlertDialog(
            onDismissRequest = { cropCandidateUri = null },
            title = { Text("先裁一下再识别吗？") },
            text = { Text("裁掉桌面和无关背景通常能提高识别速度与准确率；也可以直接使用原图。") },
            confirmButton = {
                TextButton(onClick = {
                    cropCandidateUri = null
                    launchCrop(source)
                }) { Text("裁剪照片") }
            },
            dismissButton = {
                TextButton(onClick = {
                    cropCandidateUri = null
                    recognizeImage(source)
                }) { Text("直接识别") }
            }
        )
    }
    ratingTarget?.let { meal ->
        RatingDialog(
            meal = meal,
            hapticsEnabled = appearance.haptics,
            onRate = { rating ->
                store.updateMeal(meal, meal.items, meal.title, rating, target.targetCalories)
                ratingTarget = null
            },
            onSkip = { ratingTarget = null }
        )
    }
}

@Composable
private fun MealEditor(
    initialItems: List<FoodItem>,
    imageUri: Uri?,
    target: com.calorielens.app.domain.DailyTarget,
    aiComment: String,
    refinementError: String?,
    refining: Boolean,
    hapticsEnabled: Boolean,
    onCancel: () -> Unit,
    onRefine: (List<FoodItem>, String) -> Unit,
    onSave: (List<FoodItem>, String, Int) -> Unit,
    initialTitle: String = defaultMealTitle(),
    initialRating: Int = 0,
    allowRatingEdit: Boolean = false,
    modifier: Modifier
) {
    val haptic = LocalHapticFeedback.current
    var items by remember(initialItems) { mutableStateOf(initialItems) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var rating by remember(initialRating) { mutableStateOf(initialRating) }
    var mealMenuOpen by remember { mutableStateOf(false) }
    var correctionEnabled by remember(initialItems) { mutableStateOf(false) }
    var correctionText by remember(initialItems) { mutableStateOf("") }
    val mealNutrition = items.nutritionTotal()
    val itemValuesChanged = items != initialItems
    val canRefine = itemValuesChanged || correctionText.isNotBlank()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("这餐，猜得怎么样？", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("可以改餐次、食物名称和份量，再让 AI 一起重新估算。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MealPreviewHeader(imageUri = imageUri, title = title, nutrition = mealNutrition)
        Box {
            OutlinedButton(
                onClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    mealMenuOpen = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🍽  $title  ·  点击更换餐次") }
            DropdownMenu(expanded = mealMenuOpen, onDismissRequest = { mealMenuOpen = false }) {
                MEAL_TYPES.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        title = option
                        mealMenuOpen = false
                    })
                }
            }
        }
        MealNutritionSummary(nutrition = mealNutrition, target = target)
        AiMealComment(comment = aiComment.ifBlank { localMealRemark(mealNutrition, target) })

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("识别清单", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Text("${items.size} 项", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        items.forEachIndexed { index, item ->
            EditableFoodCard(
                item = item,
                expandedByDefault = index == 0,
                hapticsEnabled = hapticsEnabled,
                onChanged = { updated -> items = items.toMutableList().also { it[index] = updated } }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("还是不太像？", fontWeight = FontWeight.Bold)
                        Text(
                            if (imageUri != null) "改完名称后也可以直接用一句话告诉 AI。" else "原图只保留当天；现在仍可手动修改名称和克数。",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Checkbox(checked = correctionEnabled && imageUri != null, enabled = imageUri != null, onCheckedChange = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        correctionEnabled = it
                    })
                }
                AnimatedVisibility(
                    visible = correctionEnabled,
                    enter = fadeIn() + slideInVertically { -it / 3 } + scaleIn(initialScale = 0.96f),
                    exit = fadeOut() + slideOutVertically { -it / 3 } + scaleOut(targetScale = 0.96f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = correctionText,
                            onValueChange = { correctionText = it },
                            label = { Text("例如：蒸鸡蛋糕，不是冻豆腐；另外还有半个苹果") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        )
                        if (itemValuesChanged) {
                            Text("已检测到名称或克数变化；修正时会把当前清单一起发给 AI 重算。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (refinementError != null) Text(refinementError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRefine(items, buildRefinementInstruction(correctionText, itemValuesChanged))
                    },
                    enabled = imageUri != null && canRefine && !refining,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (refining) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.size(8.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(if (refining) "AI 正在看图重新估算…" else "AI 校正并重算")
                }
            }
        }
        if (itemValuesChanged) {
            Text("当前热量仍按旧营养密度计算；点击上方 AI 校正后会按新名称和克数更新。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        if (allowRatingEdit) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                MealRatingBar(
                    rating = rating,
                    onRatingChanged = { rating = it },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(0.38f), shape = RoundedCornerShape(18.dp)) { Text("取消") }
            Button(
                onClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(items, title, rating)
                },
                modifier = Modifier.weight(0.62f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("就这样，记下来")
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

private fun buildRefinementInstruction(note: String, itemValuesChanged: Boolean): String = buildString {
    if (itemValuesChanged) append("用户已经修改了清单中的食物名称或克数，请保留这些修改，并按新名称和克数重新估算营养。")
    if (note.isNotBlank()) {
        if (isNotEmpty()) append("\n")
        append("用户补充：${note.trim()}")
    }
}

@Composable
private fun MealPreviewHeader(imageUri: Uri?, title: String, nutrition: Nutrition) {
    Box(
        Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
    ) {
        imageUri?.let { LocalUriImage(it, Modifier.fillMaxSize(), alpha = 0.88f) }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                Text(title, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text("${nutrition.calories} kcal", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("AI 结合图片估算了这一份", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LocalUriImage(uri: Uri, modifier: Modifier, alpha: Float = 1f) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri.toString()) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(bitmap = it, contentDescription = "餐食照片", modifier = modifier, contentScale = ContentScale.Crop, alpha = alpha)
    }
}

@Composable
private fun AiMealComment(comment: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text("饭搭子说", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EditableFoodCard(item: FoodItem, expandedByDefault: Boolean, hapticsEnabled: Boolean, onChanged: (FoodItem) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember(item.id) { mutableStateOf(expandedByDefault) }
    var portionMenuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Icon(Icons.Default.EmojiFoodBeverage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(20.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name.ifBlank { "待确认食物" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.grams.toInt()}g · 本份 ${item.nutrition().calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expanded = !expanded
                }) { Text(if (expanded) "收起" else "修改") }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically { -it / 4 } + scaleIn(initialScale = 0.97f),
                exit = fadeOut() + slideOutVertically { -it / 4 } + scaleOut(targetScale = 0.97f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = item.name,
                        onValueChange = { onChanged(item.copy(name = it)) },
                        label = { Text("食物种类") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Box {
                        OutlinedButton(
                            onClick = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                portionMenuOpen = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("常用份量：一小份 / 半碗 / 一份 / 一大份") }
                        DropdownMenu(expanded = portionMenuOpen, onDismissRequest = { portionMenuOpen = false }) {
                            PORTION_PRESETS.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text("${preset.label} · ${preset.grams.toInt()}g") },
                                    onClick = {
                                        onChanged(item.copy(grams = preset.grams))
                                        portionMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = item.grams.toInt().toString(),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let { onChanged(item.copy(grams = it.coerceAtLeast(1.0))) }
                        },
                        label = { Text("份量（克）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Text(
                        "参考 ${item.caloriesPer100g.toInt()} kcal / 100g · 识别把握 ${(item.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun localMealRemark(nutrition: Nutrition, target: com.calorielens.app.domain.DailyTarget): String {
    val ratio = nutrition.calories.toDouble() / target.targetCalories.coerceAtLeast(1)
    return when {
        ratio > 0.48 -> "这一餐份量比较扎实，下一餐可以清淡一点，给全天留些空间。"
        nutrition.protein < 15 -> "看起来挺香，蛋白质还可以再补一点；数值只是图片估算。"
        nutrition.fiber < 4 -> "主食和蛋白质都有了，再添点蔬菜会更均衡。"
        else -> "搭配看起来不错，记得这只是根据图片做出的粗略估算。"
    }
}

private val MEAL_TYPES = listOf("早餐", "午餐", "晚餐", "下午茶", "夜宵", "加餐", "其他")
private data class PortionPreset(val label: String, val grams: Double)
private val PORTION_PRESETS = listOf(
    PortionPreset("一小份", 60.0),
    PortionPreset("半碗", 100.0),
    PortionPreset("一份", 150.0),
    PortionPreset("一大份", 220.0),
    PortionPreset("一杯", 250.0)
)
private val RECOGNITION_WAIT_LINES = listOf(
    "正在仔细看看这盘菜是什么…",
    "让我猜猜这顿吃了多少卡路里 🔍",
    "正在努力辨认食材，马上就好~",
    "AI 营养师正在疯狂计算中…",
    "先别急，我正在看看酱汁和配菜 👀",
    "把每一口都认真估一估，很快就有结果啦",
    "这盘看起来不错，我正在拆解食材",
    "正在帮你整理这顿饭的营养小账本 📒"
)

private fun defaultMealTitle(time: LocalTime = LocalTime.now()): String = when {
    time < LocalTime.of(10, 30) -> "早餐"
    time >= LocalTime.of(11, 0) && time < LocalTime.of(14, 0) -> "午餐"
    time >= LocalTime.of(17, 0) -> "晚餐"
    else -> "下午茶"
}

@Composable
private fun ManualMealEditor(
    recentFoods: List<FoodItem>,
    onCancel: () -> Unit,
    onSave: (List<FoodItem>, String) -> Unit,
    modifier: Modifier
) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var calories by remember { mutableStateOf("0") }
    var protein by remember { mutableStateOf("0") }
    var fat by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var title by remember { mutableStateOf("下午茶") }
    var menuOpen by remember { mutableStateOf(false) }
    var portionMenuOpen by remember { mutableStateOf(false) }
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
        if (recentFoods.isNotEmpty()) {
            Text("最近吃过", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentFoods, key = { it.id }) { food ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                        modifier = Modifier.clickable {
                            name = food.name
                            grams = food.grams.toInt().toString()
                            calories = food.caloriesPer100g.toInt().toString()
                            protein = food.proteinPer100g.toString()
                            fat = food.fatPer100g.toString()
                            carbs = food.carbsPer100g.toString()
                        }
                    ) {
                        Text(
                            "${food.name} · ${food.grams.toInt()}g",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        androidx.compose.material3.OutlinedTextField(name, { name = it }, label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(grams, { grams = it }, label = { Text("份量（克）") }, modifier = Modifier.fillMaxWidth())
        Box {
            OutlinedButton(onClick = { portionMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("填入常用份量") }
            DropdownMenu(expanded = portionMenuOpen, onDismissRequest = { portionMenuOpen = false }) {
                PORTION_PRESETS.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text("${preset.label} · ${preset.grams.toInt()}g") },
                        onClick = { grams = preset.grams.toInt().toString(); portionMenuOpen = false }
                    )
                }
            }
        }
        androidx.compose.material3.OutlinedTextField(calories, { calories = it }, label = { Text("热量（每 100 克 kcal）") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(protein, { protein = it }, label = { Text("蛋白质 g") }, modifier = Modifier.weight(1f))
            androidx.compose.material3.OutlinedTextField(fat, { fat = it }, label = { Text("脂肪 g") }, modifier = Modifier.weight(1f))
            androidx.compose.material3.OutlinedTextField(carbs, { carbs = it }, label = { Text("碳水 g") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("取消") }
            Button(onClick = {
                onSave(listOf(FoodItem(
                    id = "manual-${System.currentTimeMillis()}", name = name.trim(), grams = grams.toDouble(),
                    caloriesPer100g = calories.toDouble(), proteinPer100g = protein.toDoubleOrNull() ?: 0.0,
                    fatPer100g = fat.toDoubleOrNull() ?: 0.0, carbsPer100g = carbs.toDoubleOrNull() ?: 0.0,
                    fiberPer100g = 0.0, confidence = 1f
                )), title)
            }, enabled = valid) { Text("保存手动记录") }
        }
    }
}

@Composable
private fun HomeScreen(
    meals: List<Meal>,
    target: com.calorielens.app.domain.DailyTarget,
    recognizing: Boolean,
    recognizingImageUri: Uri?,
    recognitionStage: String,
    errorMessage: String?,
    hapticsEnabled: Boolean,
    onDismissError: () -> Unit,
    onDelete: (Meal) -> Unit,
    onRestore: (Meal) -> Unit,
    onEdit: (Meal) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier
) {
    val haptic = LocalHapticFeedback.current
    val todayMeals = meals.filter { it.localDate() == LocalDate.now() }
    var deleteCandidate by remember { mutableStateOf<Meal?>(null) }
    var undoMeal by remember { mutableStateOf<Meal?>(null) }
    val consumed = todayMeals.sumOf { it.nutrition().calories }
    val todayNutrition = todayMeals.totalNutrition()
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            GreetingHero(
                message = homeGreeting(todayMeals, consumed, target.targetCalories),
                mealCount = todayMeals.size
            )
            Spacer(Modifier.height(14.dp))
            DailyNutritionCard(
                nutrition = todayNutrition,
                target = target,
                onEditProfile = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEditProfile()
                }
            )
            AnimatedVisibility(
                visible = recognizing,
                enter = fadeIn() + slideInVertically { -it / 3 } + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + slideOutVertically { -it / 3 } + scaleOut(targetScale = 0.96f)
            ) {
                RecognitionLoadingCard(imageUri = recognizingImageUri, stage = recognitionStage, consumed = consumed)
            }
            if (errorMessage != null) {
                val message = errorMessage
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDismissError) { Text("关闭") }
                        IconButton(onClick = onDismissError) { Icon(Icons.Default.Close, "关闭提示") }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("今天的餐食", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (todayMeals.isEmpty()) "第一口，也值得认真记下来" else "今天已经记下 ${todayMeals.size} 餐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text("$consumed kcal", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            undoMeal?.let { deleted ->
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { -it }) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("已删除「${deleted.title}」", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                undoMeal = null
                                onRestore(deleted)
                            }) { Text("撤回") }
                            IconButton(onClick = { undoMeal = null }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭撤回提示")
                            }
                        }
                    }
                }
            }
        }
        if (todayMeals.isEmpty()) item {
            EmptyMealCard {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        items(todayMeals, key = { it.id }) { meal ->
            MealCard(
                meal = meal,
                target = target,
                onEdit = { onEdit(meal) },
                onCopy = null,
                onDelete = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    deleteCandidate = meal
                }
            )
        }
    }
    deleteCandidate?.let { meal ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("确认删除？") },
            text = { Text("删除「${meal.title}」后会从本机日志移除，但你可以在提示出现后撤回。") },
            confirmButton = { TextButton(onClick = {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                deleteCandidate = null
                undoMeal = meal
                onDelete(meal)
            }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun GreetingHero(message: Pair<String, String>, mealCount: Int) {
    val visible = remember { Animatable(0f) }
    LaunchedEffect(Unit) { visible.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    Row(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = visible.value
            translationY = (1f - visible.value) * 24f
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(message.first, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(message.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$mealCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("今日餐数", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DailyNutritionCard(
    nutrition: Nutrition,
    target: com.calorielens.app.domain.DailyTarget,
    onEditProfile: () -> Unit
) {
    val macroTarget = macroTargets(target.targetCalories)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NutritionRing(
                    value = nutrition.calories.toFloat(),
                    target = target.targetCalories.toFloat(),
                    label = "已摄入",
                    center = "${nutrition.calories}",
                    unit = "kcal",
                    modifier = Modifier.size(142.dp),
                    strokeWidth = 12.dp
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("今日节奏", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (nutrition.calories <= target.targetCalories) "还可摄入 ${target.targetCalories - nutrition.calories} kcal" else "超出目标 ${nutrition.calories - target.targetCalories} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    MacroBar("蛋白质", nutrition.protein, macroTarget.protein, MaterialTheme.colorScheme.primary)
                    MacroBar("碳水", nutrition.carbs, macroTarget.carbs, MaterialTheme.colorScheme.secondary)
                    MacroBar("脂肪", nutrition.fat, macroTarget.fat, Color(0xFFE1B64B))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("基础代谢 ${target.bmr} · 活动消耗 ${target.tdee - target.bmr} kcal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = onEditProfile) { Text("调整目标") }
            }
        }
    }
}

@Composable
private fun RecognitionLoadingCard(imageUri: Uri?, stage: String, consumed: Int) {
    val pulse by rememberInfiniteTransition(label = "recognition-loading").animateFloat(
        initialValue = 0.08f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "recognition-pulse"
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    imageUri?.let { LocalUriImage(it, Modifier.fillMaxSize(), alpha = 0.78f) }
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = pulse)))
                    CircularProgressIndicator(modifier = Modifier.size(26.dp).align(Alignment.Center), strokeWidth = 3.dp, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("正在替你看这顿饭", fontWeight = FontWeight.Bold)
                    Text(stage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("今天已记录 $consumed kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            RunningCat(
                modifier = Modifier.fillMaxWidth().height(62.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun RunningCat(modifier: Modifier, color: Color) {
    val motion by rememberInfiniteTransition(label = "running-cat").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1450, easing = FastOutSlowInEasing)),
        label = "running-cat-progress"
    )
    Canvas(modifier) {
        val x = size.width * 0.08f + size.width * 0.7f * motion
        val groundY = size.height * 0.82f
        val bob = kotlin.math.sin(motion.toDouble() * Math.PI * 8.0).toFloat() * 1.8f
        val stride = kotlin.math.sin(motion.toDouble() * Math.PI * 10.0).toFloat() * 4.5f
        val outline = 1.7.dp.toPx()
        val ink = Color(0xFF384039)

        drawLine(Color(0xFF8AA493).copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(0f, groundY + 6.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width, groundY + 6.dp.toPx()), strokeWidth = 1.dp.toPx())
        drawLine(color.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(x - 38.dp.toPx(), groundY - 18.dp.toPx()), androidx.compose.ui.geometry.Offset(x - 29.dp.toPx(), groundY - 18.dp.toPx()), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color.copy(alpha = 0.25f), androidx.compose.ui.geometry.Offset(x - 43.dp.toPx(), groundY - 11.dp.toPx()), androidx.compose.ui.geometry.Offset(x - 32.dp.toPx(), groundY - 11.dp.toPx()), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
        val tail = Path().apply {
            moveTo(x - 22.dp.toPx(), groundY - 17.dp.toPx() + bob)
            cubicTo(
                x - 40.dp.toPx(), groundY - 30.dp.toPx() + bob,
                x - 42.dp.toPx(), groundY - 8.dp.toPx(),
                x - 32.dp.toPx(), groundY - 3.dp.toPx()
            )
        }
        drawPath(tail, color.copy(alpha = 0.75f), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        drawOval(color, androidx.compose.ui.geometry.Offset(x - 25.dp.toPx(), groundY - 21.dp.toPx() + bob), androidx.compose.ui.geometry.Size(38.dp.toPx(), 20.dp.toPx()))
        drawLine(ink, androidx.compose.ui.geometry.Offset(x - 15.dp.toPx(), groundY - 3.dp.toPx() + bob), androidx.compose.ui.geometry.Offset(x - 12.dp.toPx() + stride, groundY + 4.dp.toPx()), strokeWidth = outline, cap = StrokeCap.Round)
        drawLine(ink, androidx.compose.ui.geometry.Offset(x + 7.dp.toPx(), groundY - 3.dp.toPx() + bob), androidx.compose.ui.geometry.Offset(x + 4.dp.toPx() - stride, groundY + 4.dp.toPx()), strokeWidth = outline, cap = StrokeCap.Round)
        val headCenter = androidx.compose.ui.geometry.Offset(x + 20.dp.toPx(), groundY - 24.dp.toPx() + bob)
        drawCircle(color, radius = 13.dp.toPx(), center = headCenter)
        val ears = Path().apply {
            moveTo(x + 9.dp.toPx(), groundY - 32.dp.toPx() + bob)
            lineTo(x + 12.dp.toPx(), groundY - 42.dp.toPx() + bob)
            lineTo(x + 18.dp.toPx(), groundY - 34.dp.toPx() + bob)
            moveTo(x + 23.dp.toPx(), groundY - 34.dp.toPx() + bob)
            lineTo(x + 29.dp.toPx(), groundY - 41.dp.toPx() + bob)
            lineTo(x + 32.dp.toPx(), groundY - 30.dp.toPx() + bob)
        }
        drawPath(ears, color)
        drawCircle(ink, 1.3.dp.toPx(), androidx.compose.ui.geometry.Offset(x + 16.dp.toPx(), groundY - 26.dp.toPx() + bob))
        drawCircle(ink, 1.3.dp.toPx(), androidx.compose.ui.geometry.Offset(x + 25.dp.toPx(), groundY - 26.dp.toPx() + bob))
        drawCircle(Color(0xFFFF9B9B).copy(alpha = 0.8f), 1.8.dp.toPx(), androidx.compose.ui.geometry.Offset(x + 13.dp.toPx(), groundY - 21.dp.toPx() + bob))
        drawCircle(Color(0xFFFF9B9B).copy(alpha = 0.8f), 1.8.dp.toPx(), androidx.compose.ui.geometry.Offset(x + 29.dp.toPx(), groundY - 21.dp.toPx() + bob))
        drawPath(Path().apply {
            moveTo(x + 19.dp.toPx(), groundY - 22.dp.toPx() + bob)
            lineTo(x + 22.dp.toPx(), groundY - 22.dp.toPx() + bob)
            lineTo(x + 20.5.dp.toPx(), groundY - 20.dp.toPx() + bob)
        }, Color(0xFFD86E72))
    }
}

@Composable
private fun EmptyMealCard(onTouch: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))))
            .clickable(onClick = onTouch)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🥑", style = MaterialTheme.typography.displaySmall)
            Text("今天还是空白页", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("拍下第一餐，让这一页慢慢丰富起来", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActionBubble(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hapticsEnabled: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun homeGreeting(meals: List<Meal>, consumed: Int, target: Int): Pair<String, String> {
    val hour = LocalTime.now().hour
    val prefix = when {
        hour < 5 -> "夜深了"
        hour < 11 -> "早上好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
    val todayHas = { meal: String -> meals.any { it.title == meal } }
    return when {
        hour < 10 && !todayHas("早餐") -> prefix to "今天早餐吃的什么呀？拍下来我帮你看看。"
        hour in 11..13 && !todayHas("午餐") -> prefix to "午饭时间到，今天想记录哪一道？"
        hour in 17..20 && !todayHas("晚餐") -> prefix to "忙了一天，晚饭别忘了记一笔。"
        consumed > target -> prefix to "今天的热量已经超目标一点，接下来喝点水、慢慢走走吧。"
        meals.isEmpty() -> prefix to "今天想吃什么？第一口也值得认真记下来。"
        else -> prefix to "已经记下 ${meals.size} 餐，继续保持这份小小的记录感。"
    }
}

@Composable
private fun HistoryScreen(
    store: PersistentMealStore,
    summaries: List<DailySummary>,
    target: com.calorielens.app.domain.DailyTarget,
    hapticsEnabled: Boolean,
    onEdit: (Meal) -> Unit,
    onCopy: (Meal) -> Unit,
    modifier: Modifier
) {
    val haptic = LocalHapticFeedback.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var copyCandidate by remember { mutableStateOf<Meal?>(null) }
    val selectedMealsFlow = remember(selectedDate) { store.mealsForDate(selectedDate) }
    val selectedMeals by selectedMealsFlow.collectAsState(initial = emptyList())
    val byDate = summaries.associateBy { it.localDate }
    val leadingDays = month.atDay(1).dayOfWeek.value - 1
    val cells: List<LocalDate?> = buildList {
        repeat(leadingDays) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    val selectedSummary = byDate[selectedDate]

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("饮食日历", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("每天只看一眼热量和是否超标；近 30 天仍可查看并编辑餐食。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        WeeklyOverviewCard(
            stats = calculateWeeklyStats(summaries, LocalDate.now()),
            summaries = summaries,
            fallbackTarget = target.targetCalories
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        month = month.minusMonths(1)
                        if (selectedDate.year != month.year || selectedDate.monthValue != month.monthValue) {
                            selectedDate = month.atDay(1)
                        }
                    }) { Icon(Icons.Default.ArrowBack, "上个月") }
                    Text("${month.year} 年 ${month.monthValue} 月", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ArrowForward, "下个月") }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { date ->
                            CalendarDayCell(
                                date = date,
                                summary = date?.let(byDate::get),
                                fallbackTarget = target.targetCalories,
                                selected = date == selectedDate,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (date != null) {
                                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedDate = date
                                    }
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CalendarLegend(Color(0xFF4F9A68), "未超标")
                    CalendarLegend(Color(0xFFD46858), "超标")
                    CalendarLegend(MaterialTheme.colorScheme.outline, "无记录")
                }
            }
        }
        SelectedDaySummary(selectedDate, selectedSummary, target)
        Text("${selectedDate.monthValue} 月 ${selectedDate.dayOfMonth} 日餐食", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (selectedMeals.isEmpty()) {
            Text(
                if (selectedSummary != null) "这一天的详细餐食已经归档，只保留热量汇总。" else "这一天还没有记录。",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)).padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            selectedMeals.sortedByDescending { it.timestamp }.forEach { meal ->
                MealCard(
                    meal = meal,
                    target = target,
                    onEdit = { onEdit(meal) },
                    onCopy = if (selectedDate == LocalDate.now()) null else ({ copyCandidate = meal }),
                    onDelete = null
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    copyCandidate?.let { meal ->
        AlertDialog(
            onDismissRequest = { copyCandidate = null },
            title = { Text("复制到今天的哪一餐？") },
            text = { Text("会新增一条「${meal.title}」，原记录不会改变。如果你只是想查看或修改历史，请选择编辑。") },
            confirmButton = {
                TextButton(onClick = {
                    copyCandidate = null
                    onCopy(meal)
                }) { Text("新增到「${meal.title}」") }
            },
            dismissButton = { TextButton(onClick = { copyCandidate = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    summary: DailySummary?,
    fallbackTarget: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (date == null) {
        Spacer(modifier.height(54.dp))
        return
    }
    val over = summary?.let { it.calories > (it.targetCalories.takeIf { target -> target > 0 } ?: fallbackTarget) }
    val statusColor = when (over) {
        true -> Color(0xFFD46858)
        false -> Color(0xFF4F9A68)
        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    }
    Column(
        modifier.height(54.dp).clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                else statusColor.copy(alpha = 0.07f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("${date.dayOfMonth}", fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelLarge)
        if (summary != null) {
            Text("${summary.calories}", style = MaterialTheme.typography.labelSmall, color = statusColor, maxLines = 1)
            Text(if (over == true) "!" else "✓", style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
        } else {
            Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SelectedDaySummary(
    date: LocalDate,
    summary: DailySummary?,
    target: com.calorielens.app.domain.DailyTarget
) {
    val calories = summary?.calories ?: 0
    val targetCalories = summary?.targetCalories?.takeIf { it > 0 } ?: target.targetCalories
    val over = summary?.let { calories > targetCalories }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            NutritionRing(
                value = calories.toFloat(),
                target = targetCalories.toFloat(),
                label = "${date.monthValue}/${date.dayOfMonth}",
                center = "$calories",
                unit = "kcal",
                modifier = Modifier.size(112.dp),
                strokeWidth = 10.dp
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    when (over) {
                        true -> "这天超过目标"
                        false -> "这天在目标内"
                        null -> "这天暂无汇总"
                    },
                    fontWeight = FontWeight.Bold,
                    color = when (over) {
                        true -> Color(0xFFD46858)
                        false -> Color(0xFF4F9A68)
                        null -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text("目标 $targetCalories kcal · ${summary?.mealCount ?: 0} 餐", style = MaterialTheme.typography.bodySmall)
                if (summary != null) {
                    Text("蛋白质 ${summary.protein.toInt()}g · 碳水 ${summary.carbs.toInt()}g · 脂肪 ${summary.fat.toInt()}g", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    target: com.calorielens.app.domain.DailyTarget,
    onEdit: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val n = meal.nutrition()
    val score = scoreMeal(meal, target)
    val appeared = remember(meal.id) { Animatable(0f) }
    LaunchedEffect(meal.id) { appeared.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            alpha = appeared.value
            translationY = (1f - appeared.value) * 20f
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.size(10.dp))
                Text(meal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onCopy != null) IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "复制到今天") }
                if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑餐食") }
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
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backupMessage: String?,
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
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("记录备份", fontWeight = FontWeight.Bold)
                Text("导出餐食、每日汇总、个人资料和外观设置。为了安全，API Key 不会写入备份。", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("导出备份") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("导入备份") }
                }
                backupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        OutlinedButton(onClick = onClear, enabled = currentKey.isNotBlank()) { Text("清除本机 Key") }
        Text("提示：个人使用可以这样配置，但如果 APK 分享给别人，Key 仍可能被提取。公开版本应改回后端代理模式。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppearanceEditor(current: AppAppearance, onSave: (AppAppearance) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var theme by remember(current) { mutableStateOf(current.theme) }
    var wallpaper by remember(current) { mutableStateOf(current.wallpaper) }
    var glass by remember(current) { mutableStateOf(current.glass) }
    var haptics by remember(current) { mutableStateOf(current.haptics) }
    var customWallpaperUri by remember(current) { mutableStateOf(current.customWallpaperUri) }
    var themeMenuOpen by remember { mutableStateOf(false) }
    var wallpaperMenuOpen by remember { mutableStateOf(false) }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            customWallpaperUri = uri.toString()
        }
    }
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("玻璃卡片效果", fontWeight = FontWeight.SemiBold)
                Text("让卡片和模块半透明，适合搭配自定义壁纸", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = glass, onCheckedChange = { glass = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("触感反馈", fontWeight = FontWeight.SemiBold)
                Text("选择餐次、评分和保存时给出轻触反馈", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = haptics, onCheckedChange = { haptics = it })
        }
        OutlinedButton(onClick = { wallpaperPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (customWallpaperUri == null) "从相册选择自定义壁纸" else "更换自定义壁纸")
        }
        if (customWallpaperUri != null) {
            TextButton(onClick = { customWallpaperUri = null }) { Text("恢复纯色壁纸") }
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
                Text("识别成功后会先进入确认页，你可以改餐次、食物名称和份量；保存成功后再给口味打星。")
            }
        }
        Button(onClick = { onSave(AppAppearance(theme, wallpaper, glass, customWallpaperUri, haptics)) }) { Text("应用外观") }
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
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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

private fun List<FoodItem>.nutritionTotal(): Nutrition = fold(Nutrition(0, 0.0, 0.0, 0.0, 0.0)) { total, item ->
    val n = item.nutrition()
    Nutrition(total.calories + n.calories, total.protein + n.protein, total.fat + n.fat, total.carbs + n.carbs, total.fiber + n.fiber)
}
