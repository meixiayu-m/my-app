package com.calorielens.app.recognition

import android.content.Context
import android.net.Uri
import com.calorielens.app.domain.FoodItem
import java.util.UUID

interface FoodRecognizer {
    suspend fun recognize(context: Context, imageUri: Uri): List<FoodItem>
}

/** 可替换为 LiteRT/ONNX 模型的识别器。当前提供可运行的本地演示结果，便于先验证完整交互。 */
class LocalFoodRecognizer : FoodRecognizer {
    override suspend fun recognize(context: Context, imageUri: Uri): List<FoodItem> = listOf(
        FoodItem(UUID.randomUUID().toString(), "米饭", 150.0, 116.0, 2.6, 0.3, 25.9, 0.3, 0.82f),
        FoodItem(UUID.randomUUID().toString(), "清炒时蔬", 180.0, 55.0, 2.2, 2.4, 5.4, 2.8, 0.71f)
    )
}
