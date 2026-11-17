package com.example.dgumobilesoftwarefinalproject

import com.example.dgumobilesoftwarefinalproject.domain.NutritionCalculator
import com.example.dgumobilesoftwarefinalproject.model.Food
import com.example.dgumobilesoftwarefinalproject.model.Meal
import com.example.dgumobilesoftwarefinalproject.model.MealItem
import com.example.dgumobilesoftwarefinalproject.model.MealType
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDateTime

class NutritionCalculationAccuracyTest {
    private val tolerance = 1e-6

    @Test
    fun portionAndTotalCalculateAllNutrientsAcrossTwoMeals() {
        val foodA = Food(
            code = "A", name = "음식 A", category = "테스트", origin = "테스트",
            basisAmount = 100.0, basisUnit = "g",
            nutrients = Nutrients(100.0, 10.0, 5.0, 2.0),
        )
        val foodB = Food(
            code = "B", name = "음식 B", category = "테스트", origin = "테스트",
            basisAmount = 100.0, basisUnit = "g",
            nutrients = Nutrients(50.0, 4.0, 3.0, 1.0),
        )
        val foodC = Food(
            code = "C", name = "음식 C", category = "테스트", origin = "테스트",
            basisAmount = 200.0, basisUnit = "mL",
            nutrients = Nutrients(80.0, 8.0, 2.0, 3.0),
        )

        val portionA = NutritionCalculator.portion(foodA, amount = 150.0, unit = "g")
        val portionB = NutritionCalculator.portion(foodB, amount = 50.0, unit = "g")
        val portionC = NutritionCalculator.portion(foodC, amount = 100.0, unit = "mL")

        assertNutrients("음식 A", Nutrients(150.0, 15.0, 7.5, 3.0), portionA)
        assertNutrients("음식 B", Nutrients(25.0, 2.0, 1.5, 0.5), portionB)
        assertNutrients("음식 C", Nutrients(40.0, 4.0, 1.0, 1.5), portionC)

        val meals = listOf(
            Meal(
                id = "meal-1",
                eatenAt = LocalDateTime.of(2026, 11, 17, 8, 0),
                type = MealType.BREAKFAST,
                photoName = null,
                items = listOf(
                    MealItem(foodA.code, foodA.name, 150.0, "g", portionA),
                    MealItem(foodB.code, foodB.name, 50.0, "g", portionB),
                ),
            ),
            Meal(
                id = "meal-2",
                eatenAt = LocalDateTime.of(2026, 11, 17, 12, 0),
                type = MealType.LUNCH,
                photoName = null,
                items = listOf(MealItem(foodC.code, foodC.name, 100.0, "mL", portionC)),
            ),
        )

        assertNutrients(
            "두 식사의 전체 합계",
            Nutrients(215.0, 21.0, 10.0, 5.0),
            NutritionCalculator.total(meals),
        )
    }

    @Test
    fun totalPreservesStoredNutrientsWithoutRescalingOrRecalculatingEnergy() {
        val item = MealItem(
            foodCode = "stored-food",
            name = "저장된 음식",
            amount = 250.0,
            unit = "g",
            nutrients = Nutrients(123.456, 7.89, 4.56, 1.23),
        )
        val meal = Meal(
            id = "stored-meal",
            eatenAt = LocalDateTime.of(2026, 11, 17, 18, 0),
            type = MealType.DINNER,
            photoName = null,
            items = listOf(item),
        )

        assertNutrients(
            "저장된 영양소 합계",
            Nutrients(123.456, 7.89, 4.56, 1.23),
            NutritionCalculator.total(listOf(meal)),
        )
    }

    @Test
    fun portionRejectsZeroNegativeAndNonFiniteBasisAmounts() {
        val food = Food(
            code = "invalid-basis", name = "기준량 검증 음식", category = "테스트", origin = "테스트",
            basisAmount = 100.0, basisUnit = "g",
            nutrients = Nutrients(100.0, 10.0, 5.0, 2.0),
        )
        val invalidBasisAmounts = listOf(
            0.0,
            -100.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
        )

        invalidBasisAmounts.forEach { basisAmount ->
            assertThrows("기준량: $basisAmount", IllegalArgumentException::class.java) {
                NutritionCalculator.portion(food.copy(basisAmount = basisAmount), amount = 100.0, unit = "g")
            }
        }
    }

    private fun assertNutrients(label: String, expected: Nutrients, actual: Nutrients) {
        assertEquals("$label: 열량(kcal)", expected.energyKcal, actual.energyKcal, tolerance)
        assertEquals("$label: 탄수화물(g)", expected.carbohydrateG, actual.carbohydrateG, tolerance)
        assertEquals("$label: 단백질(g)", expected.proteinG, actual.proteinG, tolerance)
        assertEquals("$label: 지방(g)", expected.fatG, actual.fatG, tolerance)
    }
}
