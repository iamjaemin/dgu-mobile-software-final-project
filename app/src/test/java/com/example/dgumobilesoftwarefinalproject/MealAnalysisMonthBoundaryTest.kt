package com.example.dgumobilesoftwarefinalproject

import com.example.dgumobilesoftwarefinalproject.domain.MealAnalysis
import com.example.dgumobilesoftwarefinalproject.model.Meal
import com.example.dgumobilesoftwarefinalproject.model.MealItem
import com.example.dgumobilesoftwarefinalproject.model.MealType
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class MealAnalysisMonthBoundaryTest {
    private val tolerance = 1e-6

    @Test
    fun monthIncludesLeapDayAndExcludesMealsOutsideCalendarRange() {
        val meals = listOf(
            meal("2024-01-29"),
            meal("2024-01-30"),
            meal("2024-02-29"),
            meal("2024-03-01"),
        )

        val summary = MealAnalysis.month(meals, LocalDate.of(2024, 2, 29))

        assertEquals("분석 시작일", LocalDate.of(2024, 1, 30), summary.start)
        assertEquals("분석 종료일", LocalDate.of(2024, 2, 29), summary.end)
        assertEquals("날짜별 집계 개수", 31, summary.byDate.size)
        assertEquals("포함된 식사 횟수", 2, summary.total.count)
        assertEquals("총칼로리", 200.0, summary.total.nutrients.energyKcal, tolerance)

        val firstDay = summary.byDate.getValue(LocalDate.of(2024, 1, 30))
        val lastDay = summary.byDate.getValue(LocalDate.of(2024, 2, 29))
        assertEquals("시작일 식사 횟수", 1, firstDay.count)
        assertEquals("시작일 칼로리", 100.0, firstDay.nutrients.energyKcal, tolerance)
        assertEquals("윤일 식사 횟수", 1, lastDay.count)
        assertEquals("윤일 칼로리", 100.0, lastDay.nutrients.energyKcal, tolerance)
        assertFalse("시작일 이전 제외", summary.byDate.containsKey(LocalDate.of(2024, 1, 29)))
        assertFalse("오늘 이후 제외", summary.byDate.containsKey(LocalDate.of(2024, 3, 1)))
    }

    @Test
    fun monthCrossesYearBoundaryAndExcludesMealsOutsideCalendarRange() {
        val meals = listOf(
            meal("2025-12-15"),
            meal("2025-12-16"),
            meal("2026-01-15"),
            meal("2026-01-16"),
        )

        val summary = MealAnalysis.month(meals, LocalDate.of(2026, 1, 15))

        assertEquals("분석 시작일", LocalDate.of(2025, 12, 16), summary.start)
        assertEquals("분석 종료일", LocalDate.of(2026, 1, 15), summary.end)
        assertEquals("날짜별 집계 개수", 31, summary.byDate.size)
        assertEquals("포함된 식사 횟수", 2, summary.total.count)
        assertEquals("총칼로리", 200.0, summary.total.nutrients.energyKcal, tolerance)

        val firstDay = summary.byDate.getValue(LocalDate.of(2025, 12, 16))
        val lastDay = summary.byDate.getValue(LocalDate.of(2026, 1, 15))
        assertEquals("작년 시작일 식사 횟수", 1, firstDay.count)
        assertEquals("작년 시작일 칼로리", 100.0, firstDay.nutrients.energyKcal, tolerance)
        assertEquals("올해 종료일 식사 횟수", 1, lastDay.count)
        assertEquals("올해 종료일 칼로리", 100.0, lastDay.nutrients.energyKcal, tolerance)
        assertFalse("시작일 이전 제외", summary.byDate.containsKey(LocalDate.of(2025, 12, 15)))
        assertFalse("오늘 이후 제외", summary.byDate.containsKey(LocalDate.of(2026, 1, 16)))
    }

    private fun meal(date: String) = Meal(
        id = "meal-$date",
        eatenAt = LocalDate.parse(date).atTime(12, 0),
        type = MealType.LUNCH,
        photoName = null,
        items = listOf(
            MealItem(
                foodCode = "test-food",
                name = "검증 음식",
                amount = 100.0,
                unit = "g",
                nutrients = Nutrients(100.0, 10.0, 5.0, 2.0),
            ),
        ),
    )
}
