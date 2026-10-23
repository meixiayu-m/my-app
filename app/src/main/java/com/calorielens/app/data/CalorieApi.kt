package com.calorielens.app.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Path
import okhttp3.MultipartBody

data class ApiProfile(val sex: String, val age: Int, val height_cm: Double, val weight_kg: Double, val activity_multiplier: Double = 1.375, val deficit_percent: Int = 15)
data class ApiTarget(val bmr: Int, val tdee: Int, val target_calories: Int, val max_safe_deficit: Int)

interface CalorieApi {
    @Multipart
    @POST("recognize")
    suspend fun recognize(@Part image: MultipartBody.Part): RecognitionResponse

    @GET("profile") suspend fun profile(): ApiProfile
    @PUT("profile") suspend fun updateProfile(@Body profile: ApiProfile): ApiProfile
    @GET("target") suspend fun target(): ApiTarget
    @GET("meals") suspend fun meals(): List<MealPayload>
    @POST("meals") suspend fun createMeal(@Body meal: MealPayload): MealPayload
    @DELETE("meals/{id}") suspend fun deleteMeal(@Path("id") id: String)
}

data class RecognitionResponse(val items: List<RecognitionItem> = emptyList())
data class RecognitionItem(
    val name: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val confidence: Float = 0.5f
)

data class MealPayload(val id: String? = null, val title: String, val eaten_on: String, val items: List<MealPayloadItem>)
data class MealPayloadItem(val name: String, val grams: Double, val calories: Int, val protein: Double, val fat: Double, val carbs: Double, val fiber: Double)

object CalorieApiFactory {
    fun create(baseUrl: String): CalorieApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(CalorieApi::class.java)
}

/** 模拟器访问开发机本地后端；真机请改成电脑的局域网 IP，生产环境使用 HTTPS。 */
const val DEFAULT_RECOGNITION_BASE_URL = "http://10.0.2.2:8000/"
