package com.calorielens.app.data

import android.content.Context
import android.net.Uri
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.DailySummary
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.domain.Meal
import com.calorielens.app.domain.Sex
import com.calorielens.app.domain.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BackupPayload(
    val meals: List<Meal>,
    val summaries: List<DailySummary>,
    val profile: UserProfile,
    val appearance: AppAppearance
)

/** 可移植的 JSON 备份。故意不包含 API Key。 */
object BackupStore {
    private const val SCHEMA_VERSION = 1

    suspend fun write(
        context: Context,
        target: Uri,
        meals: List<Meal>,
        summaries: List<DailySummary>,
        profile: UserProfile,
        appearance: AppAppearance
    ) = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("profile", JSONObject()
                .put("sex", profile.sex.name)
                .put("age", profile.age)
                .put("heightCm", profile.heightCm)
                .put("weightKg", profile.weightKg)
                .put("activity", profile.activity.name)
                .put("goalDeficitPercent", profile.goalDeficitPercent))
            .put("appearance", JSONObject()
                .put("theme", appearance.theme.name)
                .put("wallpaper", appearance.wallpaper.name)
                .put("glass", appearance.glass)
                .put("haptics", appearance.haptics))
            .put("meals", JSONArray().apply {
                meals.forEach { meal ->
                    put(JSONObject()
                        .put("id", meal.id)
                        .put("timestamp", meal.timestamp)
                        .put("title", meal.title)
                        .put("rating", meal.rating)
                        .put("items", JSONArray().apply {
                            meal.items.forEach { item ->
                                put(JSONObject()
                                    .put("id", item.id)
                                    .put("name", item.name)
                                    .put("grams", item.grams)
                                    .put("caloriesPer100g", item.caloriesPer100g)
                                    .put("proteinPer100g", item.proteinPer100g)
                                    .put("fatPer100g", item.fatPer100g)
                                    .put("carbsPer100g", item.carbsPer100g)
                                    .put("fiberPer100g", item.fiberPer100g)
                                    .put("confidence", item.confidence.toDouble()))
                            }
                        }))
                }
            })
            .put("summaries", JSONArray().apply {
                summaries.forEach { summary ->
                    put(JSONObject()
                        .put("localDate", summary.localDate.toString())
                        .put("calories", summary.calories)
                        .put("protein", summary.protein)
                        .put("carbs", summary.carbs)
                        .put("fat", summary.fat)
                        .put("fiber", summary.fiber)
                        .put("targetCalories", summary.targetCalories)
                        .put("mealCount", summary.mealCount))
                }
            })
        context.contentResolver.openOutputStream(target, "wt")?.use {
            it.write(root.toString(2).toByteArray(Charsets.UTF_8))
        } ?: error("无法创建备份文件")
    }

    suspend fun read(context: Context, source: Uri): BackupPayload = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(source)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("无法读取备份文件")
        val root = JSONObject(json)
        require(root.optInt("schemaVersion", 0) == SCHEMA_VERSION) { "备份版本不受支持" }

        val meals = root.optJSONArray("meals").orEmptyObjects().map { mealJson ->
            val items = mealJson.optJSONArray("items").orEmptyObjects().map { itemJson ->
                FoodItem(
                    id = itemJson.getString("id"),
                    name = itemJson.getString("name"),
                    grams = itemJson.getDouble("grams"),
                    caloriesPer100g = itemJson.getDouble("caloriesPer100g"),
                    proteinPer100g = itemJson.getDouble("proteinPer100g"),
                    fatPer100g = itemJson.getDouble("fatPer100g"),
                    carbsPer100g = itemJson.getDouble("carbsPer100g"),
                    fiberPer100g = itemJson.getDouble("fiberPer100g"),
                    confidence = itemJson.optDouble("confidence", 1.0).toFloat()
                )
            }
            Meal(
                id = mealJson.getString("id"),
                timestamp = mealJson.getLong("timestamp"),
                title = mealJson.getString("title"),
                items = items,
                imagePath = null,
                rating = mealJson.optInt("rating", 0).coerceIn(0, 5)
            )
        }
        val summaries = root.optJSONArray("summaries").orEmptyObjects().map { summaryJson ->
            DailySummary(
                localDate = LocalDate.parse(summaryJson.getString("localDate")),
                calories = summaryJson.getInt("calories"),
                protein = summaryJson.getDouble("protein"),
                carbs = summaryJson.getDouble("carbs"),
                fat = summaryJson.getDouble("fat"),
                fiber = summaryJson.getDouble("fiber"),
                targetCalories = summaryJson.optInt("targetCalories", 0),
                mealCount = summaryJson.optInt("mealCount", 0)
            )
        }
        val profileJson = root.optJSONObject("profile")
        val appearanceJson = root.optJSONObject("appearance")
        BackupPayload(
            meals = meals,
            summaries = summaries,
            profile = UserProfile(
                sex = runCatching { Sex.valueOf(profileJson?.optString("sex").orEmpty()) }.getOrDefault(Sex.FEMALE),
                age = profileJson?.optInt("age", 30) ?: 30,
                heightCm = profileJson?.optDouble("heightCm", 165.0) ?: 165.0,
                weightKg = profileJson?.optDouble("weightKg", 60.0) ?: 60.0,
                activity = runCatching { ActivityLevel.valueOf(profileJson?.optString("activity").orEmpty()) }.getOrDefault(ActivityLevel.LIGHT),
                goalDeficitPercent = profileJson?.optInt("goalDeficitPercent", 15) ?: 15
            ),
            appearance = AppAppearance(
                theme = runCatching { AppTheme.valueOf(appearanceJson?.optString("theme").orEmpty()) }.getOrDefault(AppTheme.MINT),
                wallpaper = runCatching { AppWallpaper.valueOf(appearanceJson?.optString("wallpaper").orEmpty()) }.getOrDefault(AppWallpaper.CREAM),
                glass = appearanceJson?.optBoolean("glass", false) ?: false,
                customWallpaperUri = null,
                haptics = appearanceJson?.optBoolean("haptics", true) ?: true
            )
        )
    }

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) optJSONObject(index)?.let(::add)
        }
    }
}
