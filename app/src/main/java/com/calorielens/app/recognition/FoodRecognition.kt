package com.calorielens.app.recognition

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.calorielens.app.data.ApiKeyStore
import com.calorielens.app.domain.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

interface FoodRecognizer {
    suspend fun recognize(context: Context, imageUri: Uri): List<FoodItem>
}

/**
 * 个人模式：直接从 App 调用智谱视觉接口。
 * Key 从 Android Keystore 加密存储读取，不会写进源码、Gradle 或 APK 常量。
 */
class DirectZhipuFoodRecognizer : FoodRecognizer {
    override suspend fun recognize(context: Context, imageUri: Uri): List<FoodItem> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.read(context)?.trim().orEmpty()
        require(apiKey.isNotEmpty()) { "请先在设置中填写智谱 API Key" }
        val resolver = context.contentResolver
        val mimeType = resolver.getType(imageUri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val bytes = resolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: error("无法读取图片")
        require(bytes.isNotEmpty()) { "图片为空" }
        require(bytes.size <= MAX_IMAGE_BYTES) { "图片太大，请选择 12MB 以内的图片" }

        val imageData = "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        val payload = JSONObject()
            .put("model", MODEL)
            .put("temperature", 0.1)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", PROMPT))
                .put(JSONObject()
                    .put("role", "user")
                    .put("content", JSONArray()
                        .put(JSONObject().put("type", "text").put("text", "请识别这张餐食图片并严格按要求返回 JSON。"))
                        .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageData)))
                    )
                )
            )

        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        HTTP_CLIENT.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("智谱接口错误（${response.code}）")
            parseResponse(body)
        }
    }

    private fun parseResponse(body: String): List<FoodItem> {
        val response = JSONObject(body)
        val content = response.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .get("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    append(content.optJSONObject(index)?.optString("text").orEmpty())
                }
            }
            else -> content.toString()
        }
        val cleaned = text.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val items = JSONObject(cleaned).optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                add(item.toFoodItem())
            }
        }.ifEmpty { listOf(unknownFood()) }
    }

    private fun JSONObject.toFoodItem(): FoodItem {
        val grams = optDouble("grams", 100.0).coerceIn(1.0, 2000.0)
        return FoodItem(
            id = UUID.randomUUID().toString(),
            name = optString("name").trim().ifBlank { "待确认食物" },
            grams = grams,
            caloriesPer100g = (optDouble("calories", 0.0) * 100.0 / grams).coerceIn(0.0, 1200.0),
            proteinPer100g = (optDouble("protein", 0.0) * 100.0 / grams).coerceIn(0.0, 100.0),
            fatPer100g = (optDouble("fat", 0.0) * 100.0 / grams).coerceIn(0.0, 100.0),
            carbsPer100g = (optDouble("carbs", 0.0) * 100.0 / grams).coerceIn(0.0, 100.0),
            fiberPer100g = (optDouble("fiber", 0.0) * 100.0 / grams).coerceIn(0.0, 100.0),
            confidence = optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f)
        )
    }

    private fun unknownFood() = FoodItem(
        id = UUID.randomUUID().toString(), name = "待确认食物", grams = 100.0,
        caloriesPer100g = 150.0, proteinPer100g = 6.0, fatPer100g = 5.0,
        carbsPer100g = 20.0, fiberPer100g = 2.0, confidence = 0.1f
    )

    private companion object {
        const val API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        const val MODEL = "glm-4.6v-flash"
        const val MAX_IMAGE_BYTES = 12 * 1024 * 1024
        const val PROMPT = """
你是一个严谨的食物营养估算助手。识别图片中能看见的食物，按独立食物项返回。
请估算每项可食部分重量（克）和整份对应的热量、蛋白质、脂肪、碳水、膳食纤维。
只能返回 JSON，不要 Markdown，不要解释文字，格式必须是：
{"items":[{"name":"食物名称","grams":150,"calories":174,"protein":3.9,"fat":0.5,"carbs":38.9,"fiber":0.5,"confidence":0.86}]}
看不清时可以返回空 items，不要编造固定的菜名；所有数值都必须是非负数字，confidence 在 0 到 1 之间。
"""
        val HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
