package com.example.dgumobilesoftwarefinalproject

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.dgumobilesoftwarefinalproject.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import com.example.dgumobilesoftwarefinalproject.data.MealRepository
import java.util.UUID

class MealUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun scrollTo(text: String) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    @Test fun opensInputSearchAndShowsLiveNutrition() {
        val testMealName = "검증 식사-${UUID.randomUUID()}"
        rule.waitUntil(15_000) { rule.onAllNodesWithText("음식과 기록을 불러오는 중이에요").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("1/4 · 식사한 시간").assertIsDisplayed()
        rule.onNodeWithText("음식 입력").assertDoesNotExist()
        rule.onNodeWithContentDescription("음식 입력").assertExists()
        rule.onNodeWithText("날짜와 시간 선택").performClick()
        rule.onNodeWithTag("date-time-sheet").assertIsDisplayed()
        rule.onNodeWithContentDescription("일요일").assertExists()
        rule.onNodeWithTag("meal-time-input").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("hour-wheel").performScrollToIndex(18)
        rule.onNodeWithTag("minute-wheel").performScrollToIndex(5)
        rule.onNodeWithText("날짜와 시간 적용").performClick()
        rule.onNodeWithText("18:05").assertExists()
        rule.onNodeWithText("다음").performClick()
        rule.onNodeWithText("2/4 · 음식과 반찬").assertIsDisplayed()
        rule.onNodeWithText("다음").assertIsNotEnabled()
        rule.onNodeWithText("음식·반찬 찾기").performClick()
        rule.onNodeWithTag("food-search-sheet").assertIsDisplayed()
        rule.onNodeWithTag("food-results-grid").performScrollToNode(hasTestTag("menu-card-밥류"))
        rule.onNodeWithTag("menu-card-밥류").performClick()
        rule.onNodeWithTag("category-밥류").assertIsSelected()
        rule.onNodeWithContentDescription("음식 이름 검색").performTextInput("검색결과없는항목")
        rule.onNodeWithText("일치하는 음식이 없어요. 검색어나 필터를 바꿔 주세요.").assertExists()
        rule.onNodeWithContentDescription("음식 이름 검색").performTextClearance()
        rule.onNodeWithContentDescription("음식 이름 검색").performTextInput("국밥 돼지머리")
        rule.onNodeWithText("국밥_돼지머리", useUnmergedTree = true).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("국밥_돼지머리 추가 완료").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("국밥_돼지머리 추가 완료").assertIsDisplayed()
        rule.onNodeWithText("선택 완료").performClick()
        rule.onNodeWithContentDescription("섭취량 (g)").performScrollTo()
        rule.onNodeWithContentDescription("섭취량 (g)").performTextClearance()
        rule.onNodeWithContentDescription("섭취량 (g)").performTextInput("150")
        rule.onAllNodesWithText("205.5 kcal").onFirst().assertExists()
        rule.onNodeWithContentDescription("음식·반찬 이름").performTextClearance()
        rule.onNodeWithContentDescription("음식·반찬 이름").performTextInput(testMealName)
        rule.onNodeWithText("다음").performClick()
        rule.onNodeWithText("3/4 · 사진 업로드").assertIsDisplayed()
        rule.onNodeWithText("사진 선택").assertExists()
        rule.onNodeWithText("다음").performClick()
        rule.onNodeWithText("4/4 · 이번 식사 미리보기").assertIsDisplayed()
        rule.onNodeWithText("식사 기록 저장").assertIsEnabled().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText(testMealName).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(testMealName).performClick()
        rule.onNodeWithText("섭취량 150.0 g").assertExists()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("섭취량 150.0 g").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("식사를 기록했습니다.").fetchSemanticsNodes().isEmpty() }
        scrollTo("이 기록 삭제")
        rule.onNodeWithText("이 기록 삭제").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("삭제").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("삭제", useUnmergedTree = true).performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("meal-grid").fetchSemanticsNodes().isNotEmpty() }
        val stored = MealRepository(InstrumentationRegistry.getInstrumentation().targetContext).load()
        assertTrue(stored.none { meal -> meal.items.any { it.name == testMealName } })
    }

    @Test fun showsAnalysisAndBothOfficialSources() {
        rule.waitUntil(15_000) { rule.onAllNodesWithText("음식과 기록을 불러오는 중이에요").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithContentDescription("식사 분석").performClick()
        rule.onNodeWithText("최근 칼로리 총량").assertIsDisplayed()
        rule.onNodeWithTag("analysis-period").assertTextContains("1달 ▾")
        rule.onNodeWithTag("analysis-period").performClick()
        rule.onNodeWithText("7일").performClick()
        rule.onNodeWithTag("analysis-period").assertTextContains("7일 ▾")
        scrollTo("시간대별 섭취 영양소")
        scrollTo("18–24시")
        rule.onNodeWithText("저녁").assertIsDisplayed()
        rule.onNodeWithText("18–24시").assertIsDisplayed()
        rule.onNodeWithText("데이터 출처").performClick()
        rule.onNodeWithText("식품의약품안전처 · K-FIND").assertIsDisplayed()
        scrollTo("공공데이터포털")
        rule.onNodeWithText("공공데이터포털").assertIsDisplayed()
    }
}
