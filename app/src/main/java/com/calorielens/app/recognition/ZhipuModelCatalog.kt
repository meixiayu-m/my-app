package com.calorielens.app.recognition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A model entry returned by Zhipu's OpenAI-compatible model list endpoint. */
data class ZhipuModelInfo(
    val id: String,
    val ownedBy: String? = null,
    val contextLength: Long? = null,
    val maxOutputTokens: Long? = null,
    val rateLimit: String? = null,
    val timeLimit: String? = null,
    val description: String? = null
) {
    fun limitsLabel(): String = buildList {
        contextLength?.let { add("上下文 ${it} tokens") }
        maxOutputTokens?.let { add("输出上限 ${it} tokens") }
        rateLimit?.let { add("限流 $it") }
        timeLimit?.let { add("时间 $it") }
    }.joinToString(" · ").ifBlank { "Token/限流：接口未返回，以智谱控制台为准" }
}

object ZhipuModelCatalog {
    private const val MODELS_URL = "https://open.bigmodel.cn/api/paas/v4/models"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    suspend fun fetch(apiKey: String): List<ZhipuModelInfo> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "请先填写智谱 API Key" }
        val request = Request.Builder()
            .url(MODELS_URL)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    val json = JSONObject(body)
                    json.optString("message")
                        .ifBlank { json.optJSONObject("error")?.optString("message").orEmpty() }
                }.getOrDefault("")
                val suffix = detail.take(160).takeIf { it.isNotBlank() }?.let { "：$it" }.orEmpty()
                error("模型列表请求失败（${response.code}$suffix）")
            }
            parse(body)
        }
    }

    private fun parse(body: String): List<ZhipuModelInfo> {
        val root = JSONObject(body)
        val data = root.optJSONArray("data")
            ?: root.optJSONArray("models")
            ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val id = item.optString("id")
                    .ifBlank { item.optString("name") }
                    .trim()
                if (id.isBlank()) continue
                add(
                    ZhipuModelInfo(
                        id = id,
                        ownedBy = item.optString("owned_by").takeIf { it.isNotBlank() },
                        contextLength = firstLong(item, "context_length", "max_context_length", "context_window"),
                        maxOutputTokens = firstLong(item, "max_output_tokens", "output_token_limit", "max_tokens"),
                        rateLimit = firstString(item, "rate_limit", "requests_per_minute", "rpm", "tokens_per_minute", "tpm"),
                        timeLimit = firstString(item, "time_limit", "timeout", "request_timeout"),
                        description = item.optString("description").takeIf { it.isNotBlank() }
                    )
                )
            }
        }.distinctBy { it.id }.sortedBy { it.id }
    }

    private fun firstLong(item: JSONObject, vararg names: String): Long? = names
        .asSequence()
        .mapNotNull { name ->
            if (!item.has(name) || item.isNull(name)) null
            else item.optLong(name, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE && it > 0 }
        }
        .firstOrNull()

    private fun firstString(item: JSONObject, vararg names: String): String? = names
        .asSequence()
        .mapNotNull { name ->
            if (!item.has(name) || item.isNull(name)) null
            else item.optString(name).trim().takeIf { it.isNotBlank() }
        }
        .firstOrNull()
}
