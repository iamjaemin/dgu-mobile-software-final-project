package com.example.dgumobilesoftwarefinalproject.domain

import com.example.dgumobilesoftwarefinalproject.model.*
import java.time.LocalDate

operator fun Nutrients.plus(other: Nutrients) = Nutrients(
    energyKcal + other.energyKcal, carbohydrateG + other.carbohydrateG,
    proteinG + other.proteinG, fatG + other.fatG
)

object NutritionCalculator {
    fun portion(food: Food, amount: Double, unit: String = food.basisUnit): Nutrients {
        require(amount.isFinite() && amount >= 0) { "섭취량은 0 이상이어야 합니다." }
        require(food.basisAmount.isFinite() && food.basisAmount > 0)
        require(unit == food.basisUnit) { "음식과 섭취량의 단위가 다릅니다." }
        val ratio = amount / food.basisAmount
        return Nutrients(food.nutrients.energyKcal * ratio, food.nutrients.carbohydrateG * ratio,
            food.nutrients.proteinG * ratio, food.nutrients.fatG * ratio)
    }

    fun total(meals: List<Meal>): Nutrients = meals.flatMap { it.items }
        .fold(Nutrients()) { sum, item -> sum + item.nutrients }

    fun macroShares(nutrients: Nutrients): List<Double> {
        val energy = listOf(nutrients.carbohydrateG * 4, nutrients.proteinG * 4, nutrients.fatG * 9)
        val sum = energy.sum()
        return if (sum == 0.0) listOf(0.0, 0.0, 0.0) else energy.map { it / sum }
    }
}

data class MealSummary(val count: Int, val nutrients: Nutrients)

enum class TimeBand(val label: String, val hours: String, val startHour: Int) {
    DAWN("새벽", "00–06시", 0), MORNING("오전", "06–12시", 6),
    AFTERNOON("오후", "12–18시", 12), EVENING("저녁", "18–24시", 18);

    companion object {
        fun at(hour: Int): TimeBand {
            require(hour in 0..23)
            return entries.last { hour >= it.startHour }
        }
    }
}

data class PeriodSummary(
    val start: LocalDate,
    val end: LocalDate,
    val total: MealSummary,
    val byType: Map<MealType, MealSummary>,
    val byDate: Map<LocalDate, MealSummary>,
    val byTimeBand: Map<TimeBand, MealSummary>,
)

object MealAnalysis {
    fun period(meals: List<Meal>, start: LocalDate, end: LocalDate): PeriodSummary {
        require(!end.isBefore(start))
        val selected = meals.filter { it.eatenAt.toLocalDate() in start..end }
        fun summary(rows: List<Meal>) = MealSummary(rows.size, NutritionCalculator.total(rows))
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }
            .associateWith { day -> summary(selected.filter { it.eatenAt.toLocalDate() == day }) }
        return PeriodSummary(start, end, summary(selected),
            MealType.entries.associateWith { type -> summary(selected.filter { it.type == type }) }, dates,
            TimeBand.entries.associateWith { band -> summary(selected.filter { TimeBand.at(it.eatenAt.hour) == band }) })
    }

    fun day(meals: List<Meal>, day: LocalDate) = period(meals, day, day)
    fun week(meals: List<Meal>, today: LocalDate) = period(meals, today.minusDays(6), today)
    fun month(meals: List<Meal>, today: LocalDate) = period(meals, today.minusMonths(1).plusDays(1), today)
}
