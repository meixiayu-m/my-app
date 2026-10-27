package com.calorielens.app.domain

import kotlin.math.roundToInt

enum class Sex { MALE, FEMALE }
enum class ActivityLevel(val label: String, val multiplier: Double) {
    SEDENTARY("久坐少动", 1.2), LIGHT("轻度活动", 1.375), MODERATE("中度活动", 1.55), HIGH("高强度活动", 1.725)
}

data class UserProfile(
    val sex: Sex = Sex.FEMALE,
    val age: Int = 30,
    val heightCm: Double = 165.0,
    val weightKg: Double = 60.0,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val goalDeficitPercent: Int = 15
)

data class DailyTarget(val bmr: Int, val tdee: Int, val targetCalories: Int, val maxSafeDeficit: Int)

fun calculateDailyTarget(profile: UserProfile): DailyTarget {
    val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + if (profile.sex == Sex.MALE) 5 else -161
    val bmr = base.roundToInt().coerceAtLeast(900)
    val tdee = (bmr * profile.activity.multiplier).roundToInt()
    val requestedDeficit = (tdee * profile.goalDeficitPercent / 100.0).roundToInt()
    val safeDeficit = requestedDeficit.coerceIn(0, 500)
    return DailyTarget(bmr, tdee, (tdee - safeDeficit).coerceAtLeast(1200), safeDeficit)
}

data class Nutrition(val calories: Int, val protein: Double, val fat: Double, val carbs: Double, val fiber: Double)

data class FoodItem(
    val id: String,
    val name: String,
    val grams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double,
    val confidence: Float = 1f,
    val editable: Boolean = true
) {
    fun nutrition(): Nutrition {
        val ratio = grams / 100.0
        return Nutrition(
            (caloriesPer100g * ratio).roundToInt(), proteinPer100g * ratio,
            fatPer100g * ratio, carbsPer100g * ratio, fiberPer100g * ratio
        )
    }
}

data class Meal(
    val id: String,
    val timestamp: Long,
    val title: String,
    val items: List<FoodItem>,
    val imagePath: String? = null,
    /** 0 means not rated yet; user rating is independent from the nutrition score. */
    val rating: Int = 0
) {
    fun nutrition(): Nutrition = items.map(FoodItem::nutrition).fold(Nutrition(0, 0.0, 0.0, 0.0, 0.0)) { a, b ->
        Nutrition(a.calories + b.calories, a.protein + b.protein, a.fat + b.fat, a.carbs + b.carbs, a.fiber + b.fiber)
    }
}

data class DailySummary(
    val localDate: java.time.LocalDate,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val targetCalories: Int,
    val mealCount: Int
) {
    val overLimit: Boolean? get() = targetCalories.takeIf { it > 0 }?.let { calories > it }
}

data class MealScore(val total: Int, val reasons: List<String>)

fun scoreMeal(meal: Meal, dailyTarget: DailyTarget): MealScore {
    val n = meal.nutrition()
    val budget = dailyTarget.targetCalories / 3.0
    var score = 100
    val reasons = mutableListOf<String>()
    if (n.calories <= budget * 1.1) reasons += "热量符合本餐预算，+0" else { score -= 15; reasons += "本餐热量超过建议预算，-15" }
    if (n.protein >= 20) reasons += "蛋白质较充足，+0" else { score -= 15; reasons += "蛋白质偏少，-15" }
    if (n.fiber >= 5) reasons += "含有较好的膳食纤维，+0" else { score -= 10; reasons += "蔬菜或膳食纤维偏少，-10" }
    if (n.fat > 35) { score -= 10; reasons += "脂肪偏高，-10" }
    if (n.carbs > 100) { score -= 5; reasons += "碳水偏高，-5" }
    return MealScore(score.coerceIn(0, 100), reasons)
}
