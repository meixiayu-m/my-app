# 热量小记（CalorieLens）

这是一个 Kotlin + Jetpack Compose 的 Android 热量记录 MVP。当前版本已经可以通过 GitHub Actions 编译 Debug APK。

## 当前进度（交接摘要）

已完成：

- 使用系统相机拍摄饭菜照片并触发本地识别流程
- 在保存前确认识别结果、修改每种食物的克数
- 展示食物、份量、热量和三大营养素
- 计算 BMR、TDEE、每日目标和剩余热量
- 对每顿饭给出 100 分制、可解释评分
- Room 本地持久化餐食、删除和个人体重/年龄调整
- `backend/` 中提供可独立部署的 FastAPI 健康检查和评分接口

当前状态：

- Android 主流程已经搭好：拍照 → 本地识别 → 确认/修改份量 → 保存餐食 → 展示营养和评分。
- 本地识别仍是演示实现，不会真正分析照片；任何照片都会返回“米饭 + 清炒时蔬”。
- 用户资料目前只在内存中保存，应用重启后会恢复默认值；餐食通过 Room 持久化。
- Retrofit 和 FastAPI 接口已经定义，但当前主界面尚未把餐食同步到后端。
- 当前 UI 已调整为浅米白背景、薄荷绿/桃粉点缀、圆角卡片、热量进度条和 Material/emoji 图标；个人资料支持性别、年龄、身高、体重和运动强度。
- 当前最新功能提交：`f5ed9ed`。它刷新了 UI、补齐个人资料和运动强度编辑，并保留了前面的 JVM/CI 修复。

## 目录和职责

```text
app/src/main/java/com/calorielens/app/
├── MainActivity.kt                 Compose 主界面、拍照入口和编辑页
├── domain/Calories.kt              BMR/TDEE、营养计算、餐食评分
├── data/RoomStore.kt               Room 实体、DAO、本地餐食存储
├── data/CalorieApi.kt              Retrofit 数据模型和接口
└── recognition/FoodRecognition.kt  识别器接口及演示实现
backend/main.py                     FastAPI 后端基础接口
.github/workflows/android.yml       GitHub Actions 构建和 APK 上传
```

## GitHub Actions 构建（推荐）

仓库中的 `.github/workflows/android.yml` 会：

1. 配置 JDK 17；
2. 安装 Android SDK 35 和 Build Tools 35.0.0；
3. 检查 `settings.gradle.kts`、`app/build.gradle.kts` 和 `:app` 模块；
4. 执行 `:app:testDebugUnitTest` 和 `:app:assembleDebug`；
5. 上传 `calorie-lens-debug-apk` Artifact。

在 GitHub 的 `Actions` 页面重新运行 `Android build`，成功后下载该 Artifact 即可得到 APK。

构建命令必须使用：

```bash
gradle :app:testDebugUnitTest :app:assembleDebug --stacktrace
```

不要改回 `./gradlew ...`，因为仓库目前没有 `gradle/wrapper/gradle-wrapper.jar`；也不要只上传工作流文件，必须保留完整的 `app/` 模块。

## 本地构建

用 Android Studio Hedgehog 或更高版本打开此目录，等待 Gradle 同步后运行 `app`。需要 Android SDK 35 和 JDK 17。

当前机器准备过的工具路径是：

```text
JDK 17:       /home/user/toolcache/jdk17/usr/lib/jvm/java-17-openjdk-amd64
Gradle 8.9:   /home/user/toolcache/gradle-8.9
Android SDK:  /home/user/toolcache/android-sdk
```

本地 Android SDK 路径写在 `local.properties`，该文件不应提交到 Git。网络受限时，Gradle 可能因无法访问 `dl.google.com` 或 `repo.maven.apache.org` 而卡住；此时优先使用 GitHub Actions，不要删除 `~/.gradle` 缓存。

## 已知坑和排查方式

### 1. `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

说明工作流执行了 `./gradlew`，但缺少 `gradle-wrapper.jar`。改用 GitHub runner 上的 Gradle 8.9：

```yaml
run: gradle :app:testDebugUnitTest :app:assembleDebug --stacktrace
```

### 2. `Task 'assembleDebug' not found`

通常是上传时漏了 `app/`，或 `settings.gradle.kts` 缺少：

```kotlin
include(":app")
```

仓库根目录必须同时存在 `settings.gradle.kts`、`build.gradle.kts`、`gradle.properties` 和 `app/build.gradle.kts`。

### 3. `Inconsistent JVM-target compatibility ... 1.8 ... 17`

`app/build.gradle.kts` 必须保持 Java、Kotlin、KSP 都使用 17：

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin { jvmToolchain(17) }
```

### 4. `sdk.dir property in local.properties ... Directory does not exist`

这是把本机的 `local.properties` 带到了 GitHub runner。工作流会在安装 SDK 后重新生成：

```bash
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
```

该警告本身不是 JVM 编译失败的原因，但应保留工作流中的 SDK 路径步骤。

### 5. Maven/DNS 超时

这是网络问题，不是 Kotlin 代码问题。不要反复中断 Gradle，也不要使用 `--offline`（除非所有 JAR 都已缓存）。切换能访问 GitHub/Maven 的网络后重跑 Actions 最省时间。

### 6. `collectAsStateWithLifecycle` 缺少 `initialValue` 或 `lifecycle`

`PersistentMealStore.meals` 的类型是普通 `Flow<List<Meal>>`，不同版本的 lifecycle-compose 对该扩展函数参数要求不同。当前代码使用 Compose runtime 的兼容写法：

```kotlin
val meals by store.meals.collectAsState(initial = emptyList())
```

不要把它改回不带参数的 `collectAsStateWithLifecycle()`，否则可能在 CI 编译时报缺少 `initialValue`/`lifecycle`。

## 下一步开发顺序

1. 用真实的 LiteRT/ONNX/TFLite 模型替换 `LocalFoodRecognizer`。
2. 用 DataStore 持久化 `UserProfile`，补齐性别、身高、活动量和目标设置。
3. 让主界面通过 Retrofit 调用 `backend`，加入同步失败重试和离线队列。
4. 增加餐食编辑/删除测试、Room migration 和 UI 测试。
5. 完善隐私说明、照片清理策略和营养估算免责声明。

## AI 识别接入方案

目前 `FoodRecognizer` 是一个接口，`LocalFoodRecognizer` 只是演示实现。这是故意保留的替换点，后续可以在不改拍照和编辑页面的情况下更换识别方式。

推荐分三层推进：

1. **免费端侧识别（优先）**：在手机内集成 LiteRT/TFLite/ONNX 模型。照片不离开手机，不需要 API 费用，但需要自己准备食物分类模型和营养数据库，识别精度通常不如云端大模型。
2. **云端视觉模型（效果优先）**：手机把压缩后的照片发给自己的 FastAPI，再由后端调用视觉模型并返回结构化食物、份量和置信度。模型服务可能有免费额度，但长期通常按图片/token 计费；API Key 必须放在后端环境变量，不能写进 APK。
3. **混合模式（产品化推荐）**：先端侧识别；低置信度或用户主动点击“更准确识别”时，再请求云端。没有网络时仍能记录，并允许用户手动修改。

小爱同学这类系统助手通常不是可供第三方应用直接调用的通用“看图识菜 API”。可以做语音输入或系统 Intent 集成，但不能把它当作稳定的食物视觉识别后端；是否开放能力也取决于手机厂商和系统版本。接入云端视觉模型或自带端侧模型更可控。

建议的后端返回格式：

```json
{
  "items": [
    {
      "name": "米饭",
      "grams": 150,
      "calories": 174,
      "protein": 3.9,
      "fat": 0.5,
      "carbs": 38.9,
      "fiber": 0.5,
      "confidence": 0.86
    }
  ]
}
```

无论使用哪种 AI，识别结果都必须经过确认页，特别是份量、烹饪油和调味料只能视为估算值，不能当作医学建议。

## 交接检查清单

新窗口接手时先检查：

```bash
cd /home/user/CalorieLens
git status
git log --oneline -5
find app -maxdepth 3 -type f | sort
```

然后查看这三个文件：

```text
README.md
app/build.gradle.kts
.github/workflows/android.yml
```

如果 GitHub Actions 失败，先看失败任务名称：wrapper、项目结构、JVM target、SDK 路径和网络超时分别对应上面的排查章节。
