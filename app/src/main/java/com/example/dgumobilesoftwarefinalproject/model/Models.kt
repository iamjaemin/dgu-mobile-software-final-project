package com.example.dgumobilesoftwarefinalproject.model

import java.time.LocalDateTime

data class Nutrients(
    val energyKcal: Double = 0.0,
    val carbohydrateG: Double = 0.0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
) {
    init {
        require(listOf(energyKcal, carbohydrateG, proteinG, fatG).all { it.isFinite() && it >= 0 })
    }
}

data class Food(
    val code: String,
    val name: String,
    val category: String,
    val origin: String,
    val basisAmount: Double,
    val basisUnit: String,
    val nutrients: Nutrients,
)

enum class MealType(val label: String) {
    BREAKFAST("조식"), LUNCH("중식"), DINNER("석식"), SNACK("간식/음료")
}

data class MealItem(
    val foodCode: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val nutrients: Nutrients,
) {
    init {
        require(foodCode.isNotBlank() && name.isNotBlank())
        require(amount.isFinite() && amount > 0)
        require(unit == "g" || unit == "mL")
    }
}

data class Meal(
    val id: String,
    val eatenAt: LocalDateTime,
    val type: MealType,
    val photoName: String?,
    val items: List<MealItem>,
) {
    init { require(id.isNotBlank() && items.isNotEmpty()) }
}
