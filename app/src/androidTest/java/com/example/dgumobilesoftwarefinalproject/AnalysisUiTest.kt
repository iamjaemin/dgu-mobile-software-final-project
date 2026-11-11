package com.example.dgumobilesoftwarefinalproject

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.dgumobilesoftwarefinalproject.model.*
import com.example.dgumobilesoftwarefinalproject.ui.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AnalysisUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun periodChangesTotalsAndActualTimeControlsCards() {
        val today = LocalDate.of(2026, 11, 11)
        fun meal(id: String, day: LocalDate, hour: Int, energy: Double) = Meal(
            id, day.atTime(hour, 0), MealType.LUNCH, null,
            listOf(MealItem("test", "음식", 100.0, "g", Nutrients(energy, 20.0, 10.0, 5.0))))
        val meals = listOf(meal("old", today.minusDays(10), 8, 100.0), meal("recent", today, 20, 200.0))
        rule.setContent { MealTheme { AnalysisScreen(meals, today) } }
        rule.onNodeWithTag("analysis-total-kcal").assertTextEquals("300.0 kcal")
        rule.onNodeWithTag("analysis-grid").performScrollToNode(hasTestTag("daily-calories-chart"))
        rule.onNodeWithTag("daily-calories-chart").assertIsDisplayed()
            .assert(hasContentDescription("2026-11-01: 100.0 kcal", substring = true))
        rule.onNodeWithTag("analysis-grid").performScrollToNode(hasTestTag("timeband-MORNING"))
        rule.onNodeWithTag("timeband-MORNING").assert(hasAnyDescendant(hasText("100.0 kcal")))
        rule.onNodeWithTag("analysis-grid").performScrollToNode(hasTestTag("timeband-EVENING"))
        rule.onNodeWithTag("timeband-EVENING").assert(hasAnyDescendant(hasText("200.0 kcal")))
        rule.onNodeWithTag("analysis-grid").performScrollToIndex(0)
        rule.onNodeWithTag("analysis-period").performClick()
        rule.onNodeWithText("7일").performClick()
        rule.onNodeWithTag("analysis-total-kcal").assertTextEquals("200.0 kcal")
        rule.onNodeWithTag("analysis-grid").performScrollToNode(hasTestTag("timeband-MORNING"))
        rule.onNodeWithTag("timeband-MORNING").assert(hasAnyDescendant(hasText("0.0 kcal")))
        rule.onNodeWithTag("analysis-grid").performScrollToNode(hasTestTag("daily-calories-chart"))
        rule.onNodeWithTag("daily-calories-chart")
            .assert(hasContentDescription("2026-11-11: 200.0 kcal", substring = true))
            .assert(hasContentDescription("2026-11-01", substring = true).not())
    }
}
