package com.example.dgumobilesoftwarefinalproject

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.dgumobilesoftwarefinalproject.ui.*
import org.junit.Rule
import org.junit.Test

class MealUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun scrollTo(text: String) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    @Test fun opensInputSearchAndShowsLiveNutrition() {
        rule.waitUntil(15_000) { rule.onAllNodesWithText("음식과 기록을 불러오는 중이에요").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("음식 입력").performClick()
        scrollTo("+ 음식 추가")
        rule.onNodeWithText("+ 음식 추가").performClick()
        rule.onNodeWithText("음식 이름 검색").performTextInput("국밥 돼지머리")
        rule.onNodeWithText("국밥_돼지머리", useUnmergedTree = true).performClick()
        scrollTo("섭취량 (g)")
        rule.onNodeWithText("섭취량 (g)").performTextClearance()
        rule.onNodeWithText("섭취량 (g)").performTextInput("150")
        rule.onAllNodesWithText("205.5 kcal").onFirst().assertExists()
        rule.onNodeWithText("음식·반찬 이름").performTextClearance()
        rule.onNodeWithText("음식·반찬 이름").performTextInput("검증 식사")
        scrollTo("식사 기록 저장")
        rule.onNodeWithText("식사 기록 저장").assertIsEnabled().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("검증 식사").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("검증 식사").performClick()
        rule.onNodeWithText("섭취량 150.0 g").assertExists()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("섭취량 150.0 g").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("식사를 기록했습니다.").fetchSemanticsNodes().isEmpty() }
        scrollTo("이 기록 삭제")
        rule.onNodeWithText("이 기록 삭제").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("삭제").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("삭제", useUnmergedTree = true).performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("아직 기록한 식사가 없어요").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun showsAnalysisAndBothOfficialSources() {
        rule.waitUntil(15_000) { rule.onAllNodesWithText("음식과 기록을 불러오는 중이에요").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("식사 분석").performClick()
        rule.onNodeWithText("하루의 영양 균형").assertIsDisplayed()
        scrollTo("최근 한 달")
        rule.onNodeWithText("최근 한 달").assertIsDisplayed()
        scrollTo("간식/음료 · 0끼")
        rule.onNodeWithText("간식/음료 · 0끼").assertIsDisplayed()
        rule.onNodeWithText("데이터 출처").performClick()
        rule.onNodeWithText("식품의약품안전처 · K-FIND").assertIsDisplayed()
        scrollTo("공공데이터포털")
        rule.onNodeWithText("공공데이터포털").assertIsDisplayed()
    }
}
