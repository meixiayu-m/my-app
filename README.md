# 热量小记（CalorieLens）

这是一个 Kotlin + Jetpack Compose 的 Android 热量记录 MVP。当前版本支持相机/相册选图，并可在个人模式下直接调用智谱 GLM-4.6V-Flash 识别餐食。

## 当前进度

- 相机拍照或从相册选择图片。
- 个人模式在 App 设置页填写智谱 API Key，Key 使用 Android Keystore 加密后保存在本机；图片直接发给智谱并返回结构化食物、重量、热量和营养估算。
- 如果智谱返回 429，App 会等待 2.5 秒自动重试，最多额外重试 2 次。
- 上传前会把相机/相册图片缩放到最长边 1280px 并压缩为不超过 1 MiB 的 JPEG，减少大图上传超时。
- 识别请求明确设置连接 15 秒、上传 30 秒、等待/读取 90 秒、单次请求总计 120 秒；错误会标出 DNS、TLS、上传、等待模型或接收响应阶段。
- 设置页可以用 API Key 刷新智谱模型列表并直接选择模型，也保留手动输入模型 ID。若接口返回上下文 Token、输出上限、限流或请求时间字段，列表会显示；账号剩余 Token、每分钟限流和免费额度通常不由此接口提供，这些以智谱控制台为准。
- `backend/` 仍保留后端代理模式，适合以后公开发布或多人使用，后端从环境变量读取 API Key。
- 保存前有确认页，可以修改食物份量；不再返回固定的“米饭 + 清炒时蔬”。
- Room 本地保存餐食，首页只统计今天的摄入。
- “饮食日志”按日期统计热量、蛋白质、碳水、脂肪、纤维和餐数。
- 个人资料支持性别、年龄、身高、体重和运动强度，并计算 BMR/TDEE/每日目标。
- 已加入薄荷绿/桃粉卡通风 UI、Material 图标和标准 Android 自适应应用图标。
- GitHub Actions 可编译并上传 Debug APK。

## 目录和职责

```text
app/src/main/java/com/calorielens/app/
├── MainActivity.kt                 Compose 主界面、相机/相册、日志和资料页
├── domain/Calories.kt              BMR/TDEE、营养计算、餐食评分
├── data/RoomStore.kt               Room 实体、DAO、本地餐食存储
├── data/CalorieApi.kt              Retrofit 数据模型和后端接口（可选）
├── data/ApiKeyStore.kt              Android Keystore 加密保存个人模式 Key
└── recognition/FoodRecognition.kt  个人模式直连 GLM-4.6V-Flash
    recognition/RecognitionNetworkPolicy.kt  超时、429 重试和安全错误提示
    recognition/ZhipuModelCatalog.kt         API Key 对应的模型列表查询
backend/main.py                     FastAPI + GLM-4.6V-Flash + 餐食接口
backend/.env.example                后端密钥配置模板
.github/workflows/android.yml       GitHub Actions 构建和 APK 上传
```

## 个人模式：直接在 App 填 Key（当前推荐给你）

GitHub Actions 编译 APK 后，打开 App，点击右上角设置图标，在“智谱识别设置”中填写 API Key 和模型名称，点击“保存设置”。Key 只保存在这台手机的 Android Keystore 加密存储中，源码、GitHub 和 APK 都不会包含你的真实 Key。模型名称默认是 `glm-4.6v-flash`，可以改成智谱控制台显示且账号有权限的其他视觉模型接口 ID。

个人模式不需要启动 `backend/`，手机需要能访问智谱接口。删除 App 或点击“清除本机 Key”后，保存的 Key 会失效。这个方案适合个人自用；如果把 APK 分享给别人，运行中的 Key 仍可能被提取。

## 后端代理模式（以后公开发布时使用）

API Key 只放在后端机器的环境变量中，不能放进 Android Kotlin、`BuildConfig`、`gradle.properties`、GitHub 仓库、APK 或 Retrofit 请求头。`backend/.env` 已被 `.gitignore` 忽略。

```bash
cd /home/user/CalorieLens/backend
cp .env.example .env
# 编辑 .env，只填写自己的 Key：
# ZHIPUAI_API_KEY=你的智谱Key
# ZHIPUAI_MODEL=glm-4.6v-flash

python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

模型名配置为 `glm-4.6v-flash`（接口参数通常使用小写）；显示名称是 GLM-4.6V-Flash。智谱的免费额度、限流和计费规则以控制台当前规则为准，不要把免费额度当成无限量生产服务。

接口：

```text
GET  /health
POST /recognize   multipart/form-data，字段名 image
```

后端会把图片转换为 base64 data URL，调用 `https://open.bigmodel.cn/api/paas/v4/chat/completions`，并校验模型返回的 JSON。图片、份量、油量和营养值都是估算，用户仍需在确认页检查。

后端代理模式下，Android 才需要访问 `http://10.0.2.2:8000/`（模拟器）或电脑局域网 IP（真机）。生产环境必须使用 HTTPS；当前开发版 Manifest 为了本地调试开启了明文 HTTP。

如果 Key 曾经被提交到 Git、发到聊天或打印进日志，应立即在智谱控制台撤销并重新生成。

## GitHub Actions 构建（推荐）

工作流会配置 JDK 17、Android SDK 35 和 Gradle 8.9，然后执行测试和打包。构建命令必须是：

```bash
gradle :app:testDebugUnitTest :app:assembleDebug --stacktrace
```

在 GitHub 的 Actions 页面重新运行 `Android build`，成功后下载 `calorie-lens-debug-apk` Artifact。

不要改回 `./gradlew ...`，因为仓库目前没有 `gradle/wrapper/gradle-wrapper.jar`。也不要只上传工作流文件，必须保留完整的 `app/`、`settings.gradle.kts`、根 `build.gradle.kts` 和 `gradle.properties`。

## 本地构建

用 Android Studio Hedgehog 或更高版本打开项目，需要 Android SDK 35 和 JDK 17。当前机器准备过的工具路径：

```text
JDK 17:       /home/user/toolcache/jdk17/usr/lib/jvm/java-17-openjdk-amd64
Gradle 8.9:   /home/user/toolcache/gradle-8.9
Android SDK:  /home/user/toolcache/android-sdk
```

网络受限时 Gradle 可能因无法访问 `dl.google.com` 或 `repo.maven.apache.org` 卡住；优先使用 GitHub Actions，不要删除 `~/.gradle` 缓存。

## 已知坑

1. `GradleWrapperMain` 缺失：不要用 `./gradlew`，改用 Actions 安装的 `gradle`。
2. `Task 'assembleDebug' not found`：检查 `app/` 是否完整上传，以及 `settings.gradle.kts` 是否有 `include(":app")`。
3. JVM target 报 `1.8 ... 17`：`app/build.gradle.kts` 的 Java compileOptions 和 `kotlin { jvmToolchain(17) }` 都必须保留。
4. `sdk.dir` 警告：不要提交本机 `local.properties`；Actions 会自动生成 runner 的 SDK 路径。
5. `stripDebugDebugSymbols` 警告：通常不是失败原因。
6. 个人模式识别失败：先检查 App 中 Key 是否保存、手机是否能访问智谱接口和 Key 是否有额度；失败时不会伪造固定菜名，可重试或手动补录。
7. 用户资料、模型选择和餐食会保存在本机设置/Room 中，重启 App 后会恢复；清除 App 数据或卸载 App 会清除这些设置。安装新 APK 时先直接覆盖安装，不要为了换图标先卸载。
8. 餐食目前只保存在本机 Room；后端 `/meals` 是独立示例接口，尚未做账号和跨设备同步。
9. 后端当前是单进程内存存储示例，生产环境要增加数据库、鉴权、限流和 HTTPS。
10. 如果识别提示“等待模型响应超时”，它表示手机已完成/正在完成连接但在等待智谱返回，不等同于模型名称错误；请记录错误中的阶段和秒数。若提示 DNS、TLS 或上传，则分别检查网络/DNS、系统时间/证书和图片大小。
11. 小米桌面可能缓存旧的活动图标：先覆盖安装并重启桌面/手机，或只删除桌面上的旧快捷方式后从应用抽屉重新添加；不要清除桌面应用数据，也不要卸载 App，以免丢失本地记录。当前 APK 已同时声明 application/activity 图标、roundIcon、自适应图标和 Android 13+ 单色图标，但仍需在实际机型上确认主题/启动器表现。

## 交接检查清单

新窗口接手先执行：

```bash
cd /home/user/CalorieLens
git status
git log --oneline -5
```

再查看 `README.md`、`app/build.gradle.kts`、`.github/workflows/android.yml`、`backend/main.py` 和 `app/src/main/java/com/calorielens/app/MainActivity.kt`。

无论使用哪种 AI，识别结果都必须经过确认页；视觉模型无法可靠判断隐藏的油、酱料和准确克数，结果不能当作医学建议。
