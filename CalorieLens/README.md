# 热量小记（CalorieLens）

这是一个 Kotlin + Jetpack Compose 的 Android 热量记录 MVP。当前版本包含：

- 使用系统相机拍摄饭菜照片并触发本地识别流程
- 在保存前确认识别结果、修改每种食物的克数
- 展示食物、份量、热量和三大营养素
- 计算 BMR、TDEE、每日目标和剩余热量
- 对每顿饭给出 100 分制、可解释评分
- Room 本地持久化餐食、删除和个人体重/年龄调整
- `backend/` 中提供可独立部署的 FastAPI 健康检查和评分接口

## 打开和运行

用 Android Studio Hedgehog 或更高版本打开此目录，等待 Gradle 同步后运行 `app`。需要 Android SDK 35 和 JDK 17。

当前容器没有 JDK/Gradle，因此没有在容器内执行 APK 编译。端侧识别器 `LocalFoodRecognizer` 是可替换适配层；当前返回演示识别结果，接入 LiteRT/ONNX 模型后即可替换为真实模型。

## 后续接入

1. 在 `LocalFoodRecognizer` 中加载量化模型，输出检测框、食物类别和置信度。
2. 使用 Retrofit 调用 `backend`，把餐食、用户资料与评分同步到服务端。
3. 增加 DataStore 保存用户资料，并完善活动量、性别、身高和目标设置。
