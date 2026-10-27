package com.calorielens.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test fun dailySummaryMarksOnlyKnownTargets() {
        val summary = DailySummary(
            localDate = java.time.LocalDate.of(2026, 10, 27),
            calories = 1800,
            protein = 70.0,
            carbs = 220.0,
            fat = 60.0,
            fiber = 18.0,
            targetCalories = 1700,
            mealCount = 3
        )
        assertEquals(true, summary.overLimit)
        assertNull(summary.copy(targetCalories = 0).overLimit)
    }

    @Test fun weeklyStatsUseOnlyLastSevenDays() {
        val end = java.time.LocalDate.of(2026, 10, 27)
        val summaries = listOf(
            DailySummary(end, 1800, 70.0, 200.0, 60.0, 20.0, 1700, 3),
            DailySummary(end.minusDays(1), 1500, 50.0, 180.0, 50.0, 16.0, 1700, 2),
            DailySummary(end.minusDays(7), 3000, 20.0, 300.0, 100.0, 2.0, 1700, 4)
        )
        val stats = calculateWeeklyStats(summaries, end)
        assertEquals(2, stats.daysTracked)
        assertEquals(1, stats.daysWithinTarget)
        assertEquals(1650, stats.averageCalories)
        assertEquals(60, stats.averageProtein)
        assertEquals(5, stats.totalMeals)
    }
}
