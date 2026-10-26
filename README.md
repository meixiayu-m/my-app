# 热量小记（CalorieLens）

这是一个 Kotlin + Jetpack Compose 的 Android 热量记录应用。当前版本支持相机/相册选图，并可在个人模式下直接调用智谱视觉模型识别餐食。`0.5.0` 开始改用带热量环、营养占比、动态问候和触感反馈的饮食手账界面。

## 当前进度

- 相机拍照或从相册选择图片。
- 个人模式在 App 设置页填写智谱 API Key，Key 使用 Android Keystore 加密后保存在本机；图片直接发给智谱并返回结构化食物、重量、热量和营养估算。
- 如果智谱返回 429，App 会等待 2.5 秒自动重试，最多额外重试 2 次。
- 上传前会把相机/相册图片缩放到最长边 1024px 并压缩为不超过 768 KiB 的 JPEG，减少大图上传超时；没有强制缩短模型输出长度，避免部分视觉模型返回空内容。
- 识别请求明确设置连接 15 秒、上传 30 秒、等待/读取 90 秒、单次请求总计 120 秒；错误会标出 DNS、TLS、上传、等待模型或接收响应阶段。
- 设置页可以用 API Key 刷新智谱模型列表并直接选择模型，也保留手动输入模型 ID。若接口返回上下文 Token、输出上限、限流或请求时间字段，列表会显示；账号剩余 Token、每分钟限流和免费额度通常不由此接口提供，这些以智谱控制台为准。
- `backend/` 仍保留后端代理模式，适合以后公开发布或多人使用，后端从环境变量读取 API Key。
- 保存前有确认页，可以修改食物份量；不再返回固定的“米饭 + 清炒时蔬”。
- 识别结果会同时展示“每 100g 参考值”和“按识别克数计算的本份具体值”；水果、零食、饮料和下午茶也按独立食物项估算。
- 识别确认页可以直接编辑食物名称和克数；勾选“识别不准”后输入自然语言补充，App 会把原图、当前列表和文字一起交给视觉模型重新修正。
- Room 本地保存餐食，首页只统计今天的摄入。
- “饮食日志”按日期统计热量、蛋白质、碳水、脂肪、纤维和餐数。
- 个人资料支持性别、年龄、身高、体重和运动强度，并计算 BMR/TDEE/每日目标。
- 识别确认页会按时间默认建议餐次：10:30 前早餐、11:00–14:00 午餐、17:00 后晚餐，其余时间下午茶；用户可以改成夜宵、加餐或其他。
- 已加入薄荷绿、蜜桃、薰衣草、夜间主题，以及奶油/薄荷/桃粉/蓝灰纯色壁纸；识别确认页也跟随主题配色。
- 识别或手动添加保存后会弹出口味星级评价；支持删除二次确认和删除后撤回、手动添加下午茶/零食/水果等餐食。
- 外观设置支持玻璃半透明卡片、夜间深色配色和从相册选择自定义壁纸；系统状态栏/导航栏图标会随明暗主题自动切换。
- 首页会根据时间及当天记录显示早餐、午餐、晚餐等动态问候；使用环形图展示今日热量，并展示蛋白质、碳水、脂肪完成比例。
- 识图结果页使用原图头图、本餐热量环和占全天目标的营养占比；食物名称与克数都可在展开卡片中修改。
- 识别等待时会显示照片缩略图、阶段文案、旋转与线性进度反馈，以及代码绘制的小猫奔跑动画。
- 修改名称或克数后，点击“AI 校正并重算”会把当前完整清单、用户文字和原图一起提交，避免只改名称却继续使用旧食物热量。
- 视觉识别请求会同时返回 `items` 和一句简短的 `comment` 餐食点评，不额外增加第二次网络请求；没有返回点评时使用本地备用文案。
- 主要交互加入轻量进入、展开、加载、星级弹跳动画；选择餐次、评分、AI 修正、保存和删除等操作可提供触感反馈，并可在“主题与壁纸”中关闭。
- 已加入薄荷绿/桃粉卡通风 UI、Material 图标和标准 Android 自适应应用图标。
- GitHub Actions 可编译并上传 Debug APK。

## 目录和职责

```text
app/src/main/java/com/calorielens/app/
├── MainActivity.kt                 Compose 主界面、相机/相册、日志和资料页
├── domain/Calories.kt              BMR/TDEE、营养计算、餐食评分
├── data/RoomStore.kt               Room 实体、DAO、本地餐食存储（含评分字段迁移）
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
12. AI 返回的热量是估算值：每项会显示模型估算的可食重量、每 100g 参考值和按该重量计算的本份热量；油、酱料、果核和隐藏配料仍可能造成误差。
13. 数据库已从 v1 升到 v2，仅增加餐食评分字段并带迁移；正常升级安装会保留原有餐食。
14. 如果出现 `End of input character 0`，通常是视觉模型返回了空 content，不是“没有食物”；当前版本会显示更具体的接口提示，并已移除可能导致部分模型空返回的输出长度限制。
15. 直接改食物名称或克数时，热量暂时仍按原营养密度显示；确认页会提示这一点，点击“AI 校正并重算”后才按新食物重新估算。AI 不可用时仍可直接保存当前数值。
16. 删除餐食后的撤回提示现在同时提供“撤回”和关闭按钮；没有 API Key 等错误卡片的“关闭”文字和叉号都可以关闭提示。
17. AI 餐食点评和营养数值都只用于日常记录参考，不构成医疗建议。模型无法可靠判断隐藏油、酱汁、果核和精确重量。

## 交接检查清单

新窗口接手先执行：

```bash
cd /home/user/CalorieLens
git status
git log --oneline -5
```

再查看 `README.md`、`app/build.gradle.kts`、`.github/workflows/android.yml`、`backend/main.py` 和 `app/src/main/java/com/calorielens/app/MainActivity.kt`。

无论使用哪种 AI，识别结果都必须经过确认页；视觉模型无法可靠判断隐藏的油、酱料和准确克数，结果不能当作医学建议。
