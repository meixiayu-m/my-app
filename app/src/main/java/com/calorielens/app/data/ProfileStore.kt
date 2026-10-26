package com.calorielens.app.data

import android.content.Context
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.Sex
import com.calorielens.app.domain.UserProfile

/** 保存个人目标设置，避免重启 App 后恢复默认值。 */
object ProfileStore {
    private const val PREFS = "private_settings"
    private const val SEX = "profile_sex"
    private const val AGE = "profile_age"
    private const val HEIGHT = "profile_height_cm"
    private const val WEIGHT = "profile_weight_kg"
    private const val ACTIVITY = "profile_activity"
    private const val DEFICIT = "profile_deficit_percent"

    fun read(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sex = runCatching { Sex.valueOf(prefs.getString(SEX, null).orEmpty()) }.getOrDefault(Sex.FEMALE)
        val activity = runCatching {
            ActivityLevel.valueOf(prefs.getString(ACTIVITY, null).orEmpty())
        }.getOrDefault(ActivityLevel.LIGHT)
        return UserProfile(
            sex = sex,
            age = prefs.getInt(AGE, 30).coerceIn(13, 120),
            heightCm = prefs.getFloat(HEIGHT, 165f).toDouble().coerceIn(100.0, 250.0),
            weightKg = prefs.getFloat(WEIGHT, 60f).toDouble().coerceIn(25.0, 400.0),
            activity = activity,
            goalDeficitPercent = prefs.getInt(DEFICIT, 15).coerceIn(0, 30)
        )
    }

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SEX, profile.sex.name)
            .putInt(AGE, profile.age.coerceIn(13, 120))
            .putFloat(HEIGHT, profile.heightCm.toFloat().coerceIn(100f, 250f))
            .putFloat(WEIGHT, profile.weightKg.toFloat().coerceIn(25f, 400f))
            .putString(ACTIVITY, profile.activity.name)
            .putInt(DEFICIT, profile.goalDeficitPercent.coerceIn(0, 30))
            .apply()
    }
}
