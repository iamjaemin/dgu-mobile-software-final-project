package com.example.dgumobilesoftwarefinalproject

import com.example.dgumobilesoftwarefinalproject.domain.*
import com.example.dgumobilesoftwarefinalproject.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NutritionCalculatorTest {
    private val food = Food("A", "밥", "밥류", "가정식", 100.0, "g", Nutrients(137.0, 15.94, 6.7, 5.16))
    private fun meal(date: String, type: MealType = MealType.LUNCH, id: String = date) = Meal(id,
        LocalDateTime.parse(date), type, null, listOf(MealItem("A", "밥", 100.0, "g", food.nutrients)))

    @Test fun portionScalesWithoutRounding() {
        val result = NutritionCalculator.portion(food, 150.0)
        assertEquals(205.5, result.energyKcal, 0.000001)
        assertEquals(23.91, result.carbohydrateG, 0.000001)
        assertEquals(0.685, NutritionCalculator.portion(food, 0.5).energyKcal, 0.000001)
        assertEquals(Nutrients(), NutritionCalculator.portion(food, 0.0))
    }

    @Test fun arbitraryBasisAndVolumeAreRespected() {
        val drink = food.copy(basisAmount = 200.0, basisUnit = "mL")
        assertEquals(68.5, NutritionCalculator.portion(drink, 100.0).energyKcal, 0.000001)
        assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.portion(drink, 100.0, "g") }
    }

    @Test fun invalidAmountIsRejected() {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { amount ->
            assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.portion(food, amount) }
        }
    }

    @Test fun monthUsesInclusiveCalendarBoundaryAndExcludesFuture() {
        val today = LocalDate.of(2026, 3, 31)
        val rows = listOf(meal("2026-02-28T23:59"), meal("2026-03-01T00:00"),
            meal("2026-03-31T23:59", MealType.SNACK), meal("2026-04-01T00:00"))
        val result = MealAnalysis.month(rows, today)
        assertEquals(LocalDate.of(2026, 3, 1), result.start)
        assertEquals(2, result.total.count)
        assertEquals(274.0, result.total.nutrients.energyKcal, 0.0)
        assertEquals(1, result.byType.getValue(MealType.SNACK).count)
        assertEquals(0, result.byType.getValue(MealType.BREAKFAST).count)
    }

    @Test fun weekIncludesEmptyDaysAndSeparateMealsOfSameType() {
        val rows = listOf(meal("2026-11-05T00:00"), meal("2026-11-11T08:00"),
            meal("2026-11-11T12:00"), meal("2026-11-04T23:59"))
        val summary = MealAnalysis.week(rows, LocalDate.of(2026, 11, 11))
        assertEquals(7, summary.byDate.size)
        assertEquals(3, summary.total.count)
        assertEquals(0, summary.byDate.getValue(LocalDate.of(2026, 11, 6)).count)
        assertEquals(2, MealAnalysis.day(rows, LocalDate.of(2026, 11, 11)).total.count)
    }

    @Test fun macroEnergySharesAndEmptyData() {
        val shares = NutritionCalculator.macroShares(Nutrients(500.0, 10.0, 10.0, 10.0))
        assertEquals(40.0 / 170.0, shares[0], 0.000001)
        assertEquals(90.0 / 170.0, shares[2], 0.000001)
        assertEquals(listOf(0.0, 0.0, 0.0), NutritionCalculator.macroShares(Nutrients()))
        assertEquals(Nutrients(), NutritionCalculator.total(emptyList()))
    }
}
