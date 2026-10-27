package com.calorielens.app.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import com.calorielens.app.data.ApiKeyStore
import com.calorielens.app.domain.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RecognitionResult(
    val items: List<FoodItem>,
    val comment: String = ""
)

interface FoodRecognizer {
    suspend fun recognize(context: Context, imageUri: Uri): RecognitionResult
    suspend fun refine(context: Context, imageUri: Uri, currentItems: List<FoodItem>, userCorrection: String): RecognitionResult
}

/**
 * 个人模式：直接从 App 调用智谱视觉接口。
 * Key 从 Android Keystore 加密存储读取，不会写进源码、Gradle 或 APK 常量。
 */
class DirectZhipuFoodRecognizer : FoodRecognizer {
    private var cachedImageUri: String? = null
    private var cachedImageBytes: ByteArray? = null

    override suspend fun recognize(context: Context, imageUri: Uri): RecognitionResult = request(
        context = context,
        imageUri = imageUri,
        systemPrompt = PROMPT,
        userText = "请识别这张餐食图片并严格按要求返回 JSON。"
    )

    override suspend fun refine(
        context: Context,
        imageUri: Uri,
        currentItems: List<FoodItem>,
        userCorrection: String
    ): RecognitionResult {
        val currentJson = JSONArray().apply {
            currentItems.forEach { item ->
                put(JSONObject()
                    .put("name", item.name)
                    .put("grams", item.grams)
                    .put("calories", item.nutrition().calories)
                    .put("protein", item.nutrition().protein)
                    .put("fat", item.nutrition().fat)
                    .put("carbs", item.nutrition().carbs)
                    .put("fiber", item.nutrition().fiber))
            }
        }
        return request(
            context = context,
            imageUri = imageUri,
            systemPrompt = CORRECTION_PROMPT,
            userText = """
用户补充/纠正：${userCorrection.trim()}
当前识别列表：$currentJson
请结合原图和用户补充，修正不准确的食物；确认正确的项目请保留。只返回完整 JSON。
""".trimIndent()
        )
    }

    private suspend fun request(
        context: Context,
        imageUri: Uri,
        systemPrompt: String,
        userText: String
    ): RecognitionResult = withContext(Dispatchers.IO) {
        val startedAt = SystemClock.elapsedRealtime()
        val apiKey = ApiKeyStore.read(context)?.trim().orEmpty()
        require(apiKey.isNotEmpty()) { "请先在设置中填写智谱 API Key" }
        val model = ApiKeyStore.readModel(context)?.trim()
            .orEmpty()
            .ifBlank { DEFAULT_MODEL }
        val resolver = context.contentResolver
        val bytes = synchronized(this@DirectZhipuFoodRecognizer) {
            val key = imageUri.toString()
            cachedImageBytes?.takeIf { cachedImageUri == key } ?: compressImage(resolver, imageUri).also {
                cachedImageUri = key
                cachedImageBytes = it
            }
        }
        val preparedAt = SystemClock.elapsedRealtime()

        val imageData = "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0.1)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject()
                    .put("role", "user")
                    .put("content", JSONArray()
                        .put(JSONObject().put("type", "text").put("text", userText))
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
        val body = executeWithRateLimitRetry(request)
        val responseAt = SystemClock.elapsedRealtime()
        parseResponse(body).also {
            Log.i(
                TAG,
                "recognition timing: prepare=${preparedAt - startedAt}ms network=${responseAt - preparedAt}ms parse=${SystemClock.elapsedRealtime() - responseAt}ms bytes=${bytes.size}"
            )
        }
    }

    private suspend fun executeWithRateLimitRetry(request: Request): String {
        var retryCount = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val (statusCode, body) = executeOnce(request)
            if (statusCode in 200..299) return body

            if (RecognitionNetworkPolicy.shouldRetry(statusCode, retryCount)) {
                retryCount += 1
                delay(RecognitionNetworkPolicy.RETRY_DELAY_MS)
                continue
            }

            val detail = runCatching {
                val json = JSONObject(body)
                json.optString("message")
                    .ifBlank { json.optJSONObject("error")?.optString("message").orEmpty() }
                    .ifBlank { json.optString("msg") }
            }.getOrDefault("")
            val suffix = detail.take(180).takeIf { it.isNotBlank() }?.let { "：$it" }.orEmpty()
            error("智谱接口错误（$statusCode）$suffix")
        }
    }

    private suspend fun executeOnce(request: Request): Pair<Int, String> = suspendCancellableCoroutine { continuation ->
        val progress = RequestProgress()
        val call = HTTP_CLIENT.newCall(request.newBuilder().tag(RequestProgress::class.java, progress).build())
        val start = System.nanoTime()
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            private fun fail(error: IOException) {
                if (!continuation.isActive) return
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                continuation.resumeWithException(IllegalStateException(
                    RecognitionNetworkPolicy.failureMessage(error, progress.stage, elapsedMs), error
                ))
            }

            override fun onFailure(call: Call, e: IOException) = fail(e)

            override fun onResponse(call: Call, response: okhttp3.Response) {
                try {
                    // 即使读取响应体超时也关闭资源，并保留具体的失败阶段。
                    val result = response.use { it.code to it.body?.string().orEmpty() }
                    if (continuation.isActive) continuation.resume(result)
                } catch (error: IOException) {
                    fail(error)
                }
            }
        })
    }

    private class RequestProgress : EventListener() {
        @Volatile var stage = RequestStage.CONNECTING
        override fun dnsStart(call: Call, domainName: String) { stage = RequestStage.DNS }
        override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) { stage = RequestStage.CONNECTING }
        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) { stage = RequestStage.CONNECTING }
        override fun secureConnectStart(call: Call) { stage = RequestStage.TLS }
        override fun secureConnectEnd(call: Call, handshake: Handshake?) { stage = RequestStage.UPLOADING }
        override fun connectionAcquired(call: Call, connection: Connection) { stage = RequestStage.UPLOADING }
        override fun requestBodyStart(call: Call) { stage = RequestStage.UPLOADING }
        override fun requestBodyEnd(call: Call, byteCount: Long) { stage = RequestStage.WAITING }
        override fun responseHeadersEnd(call: Call, response: okhttp3.Response) { stage = RequestStage.READING }
    }

    private fun compressImage(resolver: android.content.ContentResolver, imageUri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片" }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("无法读取图片")

        val bitmaps = mutableListOf(bitmap)
        return try {
            val orientation = runCatching {
                resolver.openInputStream(imageUri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull()
            val transform = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                }
            }
            val oriented = if (transform.isIdentity) bitmap else {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true).also(bitmaps::add)
            }
            val scaled = if (maxOf(oriented.width, oriented.height) > MAX_IMAGE_DIMENSION) {
                val ratio = MAX_IMAGE_DIMENSION.toDouble() / maxOf(oriented.width, oriented.height)
                Bitmap.createScaledBitmap(oriented, (oriented.width * ratio).toInt().coerceAtLeast(1),
                    (oriented.height * ratio).toInt().coerceAtLeast(1), true).also(bitmaps::add)
            } else oriented
            var quality = 84
            var result: ByteArray
            do {
                val output = ByteArrayOutputStream()
                check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "图片压缩失败" }
                result = output.toByteArray()
                quality -= 8
            } while (result.size > MAX_IMAGE_BYTES && quality >= MIN_JPEG_QUALITY)
            require(result.size <= MAX_IMAGE_BYTES) { "图片太大，请选择更小的图片" }
            result
        } finally {
            bitmaps.distinct().forEach { it.recycle() }
        }
    }

    private fun parseResponse(body: String): RecognitionResult {
        val response = JSONObject(body)
        val choices = response.optJSONArray("choices")
            ?: error("智谱返回缺少 choices，可能是模型或接口版本不匹配")
        if (choices.length() == 0) error("智谱返回空 choices，没有生成识别结果")
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: error("智谱返回缺少 message")
        val content = message.opt("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    append(content.optJSONObject(index)?.optString("text").orEmpty())
                }
            }
            null, JSONObject.NULL -> ""
            else -> content.toString()
        }
        if (text.isBlank()) {
            val finishReason = choices.optJSONObject(0)?.optString("finish_reason").orEmpty()
            val suffix = finishReason.takeIf { it.isNotBlank() }?.let { "（finish_reason=$it）" }.orEmpty()
            error("智谱返回了空内容$suffix；请确认模型支持图片识别，或稍后重试")
        }
        val cleaned = text.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        val jsonText = if (jsonStart >= 0 && jsonEnd > jsonStart) cleaned.substring(jsonStart, jsonEnd + 1) else cleaned
        val payload = runCatching { JSONObject(jsonText) }
            .getOrElse { error("智谱返回内容不是有效 JSON，请重试或更换支持图片的模型") }
        val items = payload.optJSONArray("items") ?: JSONArray()
        val parsedItems = buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                add(item.toFoodItem())
            }
        }.ifEmpty { listOf(unknownFood()) }
        return RecognitionResult(
            items = parsedItems,
            comment = payload.optString("comment").trim()
        )
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
        const val DEFAULT_MODEL = "glm-4.6v-flash"
        const val TAG = "CalorieLensRecognition"
        const val MAX_IMAGE_BYTES = 640 * 1024
        const val MAX_IMAGE_DIMENSION = 960
        const val MIN_JPEG_QUALITY = 62
        const val PROMPT = """
你是一个严谨的食物营养估算助手。识别图片中能看见的食物，按独立食物项返回。
请估算每项可食部分重量（克）和整份对应的热量、蛋白质、脂肪、碳水、膳食纤维；零食、水果、饮料和餐后三餐之外的加餐也要单独识别，不要忽略。
返回的 calories/protein/fat/carbs/fiber 必须是“识别出的这一份”的具体数值，App 会同时换算并展示每 100g 的参考值。
只能返回 JSON，不要 Markdown，不要解释文字，格式必须是：
{"items":[{"name":"食物名称","grams":150,"calories":174,"protein":3.9,"fat":0.5,"carbs":38.9,"fiber":0.5,"confidence":0.86}],"comment":"一句自然、克制的餐食点评"}
看不清时可以返回空 items，不要编造固定的菜名；所有数值都必须是非负数字，confidence 在 0 到 1 之间。
comment 最多 45 个汉字，结合这餐最明显的营养特点给出一句友好建议；不做疾病诊断，不制造身材焦虑，不确定油量时明确是估算。
"""
        const val CORRECTION_PROMPT = """
你是一个严谨的食物营养估算助手。用户正在纠正之前的图片识别结果。
你会收到原始图片、当前食物列表和用户的文字补充。用户可能只纠正某一道菜，也可能补充漏掉的食物。
以用户明确说明的食物种类为优先，并结合原图估算份量；未被纠正且仍合理的项目要保留，不要随意删除或全部重做。
如果用户修改了名称，要重新估算该食物的热量及营养密度；不要把旧食物的营养值照搬到新名称上。
返回完整的食物列表，每项为本份可食重量（克）与本份的具体营养值，不是每 100g 的值。
只能返回 JSON，不要 Markdown 或解释。格式：
{"items":[{"name":"食物名称","grams":150,"calories":174,"protein":3.9,"fat":0.5,"carbs":38.9,"fiber":0.5,"confidence":0.86}],"comment":"一句自然、克制的餐食点评"}
所有数值必须是非负数字，confidence 在 0 到 1 之间。
comment 最多 45 个汉字，要针对修正后的完整列表，语气像朋友，不做疾病诊断。
"""
        val HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(RecognitionNetworkPolicy.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(RecognitionNetworkPolicy.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(RecognitionNetworkPolicy.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(RecognitionNetworkPolicy.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .eventListenerFactory { call -> call.request().tag(RequestProgress::class.java) ?: EventListener.NONE }
            .build()
    }
}
