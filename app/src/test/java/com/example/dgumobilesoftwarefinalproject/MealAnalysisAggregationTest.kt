package com.example.dgumobilesoftwarefinalproject

import com.example.dgumobilesoftwarefinalproject.domain.MealAnalysis
import com.example.dgumobilesoftwarefinalproject.domain.MealSummary
import com.example.dgumobilesoftwarefinalproject.domain.TimeBand
import com.example.dgumobilesoftwarefinalproject.model.Meal
import com.example.dgumobilesoftwarefinalproject.model.MealItem
import com.example.dgumobilesoftwarefinalproject.model.MealType
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MealAnalysisAggregationTest {
    private val tolerance = 1e-6

    @Test
    fun periodRejectsStartDateAfterEndDate() {
        val start = LocalDate.of(2026, 11, 16)
        val end = LocalDate.of(2026, 11, 15)

        assertThrows(IllegalArgumentException::class.java) {
            MealAnalysis.period(emptyList(), start, end)
        }
    }

    @Test
    fun emptyPeriodIncludesEveryDateTypeAndTimeBandWithZeroTotals() {
        val start = LocalDate.of(2026, 11, 14)
        val end = LocalDate.of(2026, 11, 16)

        val summary = MealAnalysis.period(emptyList(), start, end)

        assertEquals(start, summary.start)
        assertEquals(end, summary.end)
        assertEquals("날짜 개수", 3, summary.byDate.size)
        assertEquals(
            "모든 날짜 포함",
            setOf(
                LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 16),
            ),
            summary.byDate.keys,
        )
        assertEquals("식사 유형 개수", 4, summary.byType.size)
        assertEquals(
            "모든 식사 유형 포함",
            setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK),
            summary.byType.keys,
        )
        assertEquals("시간대 개수", 4, summary.byTimeBand.size)
        assertEquals(
            "모든 시간대 포함",
            setOf(TimeBand.DAWN, TimeBand.MORNING, TimeBand.AFTERNOON, TimeBand.EVENING),
            summary.byTimeBand.keys,
        )

        val zeroNutrients = Nutrients(0.0, 0.0, 0.0, 0.0)
        assertSummary("빈 기간 전체", 0, zeroNutrients, summary.total)
        summary.byDate.forEach { (date, value) ->
            assertSummary("날짜 $date", 0, zeroNutrients, value)
        }
        summary.byType.forEach { (type, value) ->
            assertSummary("식사 유형 $type", 0, zeroNutrients, value)
        }
        summary.byTimeBand.forEach { (band, value) ->
            assertSummary("시간대 $band", 0, zeroNutrients, value)
        }
    }

    @Test
    fun dayIncludesBoundaryMealsAndCountsMealsInsteadOfItems() {
        val day = LocalDate.of(2026, 11, 16)
        val meals = listOf(
            meal(
                "before-day", "2026-11-15T23:59", MealType.SNACK,
                listOf(item("before-item", Nutrients(900.0, 90.0, 45.0, 18.0))),
            ),
            meal(
                "day-start", "2026-11-16T00:00", MealType.BREAKFAST,
                listOf(
                    item("first-item", Nutrients(100.0, 10.0, 5.0, 2.0)),
                    item("second-item", Nutrients(100.0, 10.0, 5.0, 2.0)),
                ),
            ),
            meal(
                "day-end", "2026-11-16T23:59:59", MealType.DINNER,
                listOf(item("third-item", Nutrients(100.0, 10.0, 5.0, 2.0))),
            ),
            meal(
                "after-day", "2026-11-17T00:00", MealType.BREAKFAST,
                listOf(item("after-item", Nutrients(800.0, 80.0, 40.0, 16.0))),
            ),
        )

        val summary = MealAnalysis.day(meals, day)

        assertEquals(day, summary.start)
        assertEquals(day, summary.end)
        assertEquals(setOf(LocalDate.of(2026, 11, 16)), summary.byDate.keys)
        assertSummary("당일 전체", 2, Nutrients(300.0, 30.0, 15.0, 6.0), summary.total)
        assertSummary(
            "당일 날짜별 집계",
            2,
            Nutrients(300.0, 30.0, 15.0, 6.0),
            summary.byDate.getValue(day),
        )
    }

    @Test
    fun periodAggregatesAllNutrientsByMealType() {
        val meals = listOf(
            meal(
                "breakfast", "2026-11-14T08:00", MealType.BREAKFAST,
                listOf(item("breakfast-item", Nutrients(100.0, 10.0, 5.0, 2.0))),
            ),
            meal(
                "lunch-1", "2026-11-14T12:00", MealType.LUNCH,
                listOf(item("lunch-item-1", Nutrients(50.0, 4.0, 3.0, 1.0))),
            ),
            meal(
                "lunch-2", "2026-11-16T12:30", MealType.LUNCH,
                listOf(item("lunch-item-2", Nutrients(80.0, 8.0, 2.0, 3.0))),
            ),
            meal(
                "snack", "2026-11-15T15:00", MealType.SNACK,
                listOf(item("snack-item", Nutrients(40.0, 1.0, 1.0, 2.0))),
            ),
        )

        val summary = MealAnalysis.period(
            meals,
            LocalDate.of(2026, 11, 14),
            LocalDate.of(2026, 11, 16),
        )

        assertEquals("식사 유형 개수", 4, summary.byType.size)
        assertSummary(
            "조식", 1, Nutrients(100.0, 10.0, 5.0, 2.0),
            summary.byType.getValue(MealType.BREAKFAST),
        )
        assertSummary(
            "중식", 2, Nutrients(130.0, 12.0, 5.0, 4.0),
            summary.byType.getValue(MealType.LUNCH),
        )
        assertSummary(
            "석식", 0, Nutrients(0.0, 0.0, 0.0, 0.0),
            summary.byType.getValue(MealType.DINNER),
        )
        assertSummary(
            "간식", 1, Nutrients(40.0, 1.0, 1.0, 2.0),
            summary.byType.getValue(MealType.SNACK),
        )
        assertSummary("기간 전체", 4, Nutrients(270.0, 23.0, 11.0, 8.0), summary.total)
    }

    private fun item(code: String, nutrients: Nutrients) = MealItem(
        foodCode = code,
        name = "검증 음식 $code",
        amount = 100.0,
        unit = "g",
        nutrients = nutrients,
    )

    private fun meal(id: String, eatenAt: String, type: MealType, items: List<MealItem>) = Meal(
        id = id,
        eatenAt = LocalDateTime.parse(eatenAt),
        type = type,
        photoName = null,
        items = items,
    )

    private fun assertSummary(
        label: String,
        expectedCount: Int,
        expectedNutrients: Nutrients,
        actual: MealSummary,
    ) {
        assertEquals("$label: 식사 횟수", expectedCount, actual.count)
        assertEquals("$label: 열량(kcal)", expectedNutrients.energyKcal, actual.nutrients.energyKcal, tolerance)
        assertEquals("$label: 탄수화물(g)", expectedNutrients.carbohydrateG, actual.nutrients.carbohydrateG, tolerance)
        assertEquals("$label: 단백질(g)", expectedNutrients.proteinG, actual.nutrients.proteinG, tolerance)
        assertEquals("$label: 지방(g)", expectedNutrients.fatG, actual.nutrients.fatG, tolerance)
    }
}
