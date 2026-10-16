package com.calorielens.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaloriesTest {
    @Test fun femaleTargetUsesSafeDeficit() {
        val target = calculateDailyTarget(UserProfile())
        assertTrue(target.bmr > 1200)
        assertTrue(target.targetCalories <= target.tdee)
        assertTrue(target.maxSafeDeficit <= 500)
    }

    @Test fun mealNutritionScalesByGrams() {
        val item = FoodItem("rice", "米饭", 150.0, 116.0, 2.6, .3, 25.9, .3)
        assertEquals(174, item.nutrition().calories)
    }

    @Test fun mealScoreExplainsLowFiber() {
        val meal = Meal("1", 0, "午餐", listOf(FoodItem("rice", "米饭", 150.0, 116.0, 2.6, .3, 25.9, .3)))
        val score = scoreMeal(meal, calculateDailyTarget(UserProfile()))
        assertTrue(score.reasons.any { it.contains("膳食纤维") })
    }
}
