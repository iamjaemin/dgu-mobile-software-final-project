package com.example.dgumobilesoftwarefinalproject

import android.content.ContextWrapper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.dgumobilesoftwarefinalproject.data.PhotoRepository
import com.example.dgumobilesoftwarefinalproject.model.Meal
import com.example.dgumobilesoftwarefinalproject.model.MealItem
import com.example.dgumobilesoftwarefinalproject.model.MealType
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import com.example.dgumobilesoftwarefinalproject.ui.AnalysisScreen
import com.example.dgumobilesoftwarefinalproject.ui.DetailScreen
import com.example.dgumobilesoftwarefinalproject.ui.DiaryScreen
import com.example.dgumobilesoftwarefinalproject.ui.MealTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MealScreensUiTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var testDirectory: File
    private lateinit var photos: PhotoRepository

    @Before
    fun createIsolatedPhotoRepository() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDirectory = File(context.cacheDir, "meal-screen-ui-${UUID.randomUUID()}")
        check(testDirectory.mkdirs()) { "테스트 전용 저장 공간을 만들지 못했습니다." }
        val directory = testDirectory
        val isolatedContext = object : ContextWrapper(context) {
            override fun getFilesDir() = directory
        }
        photos = PhotoRepository(isolatedContext)
    }

    @After
    fun removeIsolatedPhotoRepository() {
        if (::testDirectory.isInitialized) {
            assertTrue("테스트 전용 저장 공간 정리", testDirectory.deleteRecursively())
        }
    }

    @Test
    fun diaryGroupsMealsByDateAndSelectsExactMealId() {
        val breakfast = meal(
            "diary-100", "2026-11-16T08:00", "검증 아침식사",
            Nutrients(100.0, 10.0, 5.0, 2.0), type = MealType.BREAKFAST,
        )
        val lunch = meal(
            "diary-200", "2026-11-16T12:00", "검증 점심식사",
            Nutrients(200.0, 20.0, 10.0, 4.0),
        )
        val previous = meal(
            "diary-80", "2026-11-15T16:00", "검증 전날식사",
            Nutrients(80.0, 8.0, 4.0, 1.6), type = MealType.SNACK,
        )
        val selectedIds = mutableListOf<String>()
        rule.setContent {
            MealTheme {
                DiaryScreen(
                    meals = listOf(previous, breakfast, lunch),
                    photos = photos,
                    onAdd = {},
                    onSelect = { selectedIds.add(it) },
                )
            }
        }

        val grid = rule.onNodeWithTag("meal-grid")
        listOf(
            "2026.11.16",
            "300.0 kcal · 2끼 기록",
            "검증 점심식사",
            "200.0 kcal · 자세히 보기",
            "검증 아침식사",
            "100.0 kcal · 자세히 보기",
            "2026.11.15",
            "80.0 kcal · 1끼 기록",
            "검증 전날식사",
            "80.0 kcal · 자세히 보기",
        ).forEach { text ->
            grid.performScrollToNode(hasText(text))
            rule.onNodeWithText(text).assertIsDisplayed()
        }

        grid.performScrollToNode(hasText("검증 점심식사"))
        rule.onNodeWithText("검증 점심식사").performClick()
        rule.runOnIdle { assertEquals(listOf("diary-200"), selectedIds) }
    }

    @Test
    fun detailCancelKeepsMealAndConfirmCallsDeleteOnce() {
        val detail = meal(
            "detail-meal", "2026-11-16T12:30", "상세 검증 식사",
            Nutrients(205.5, 23.91, 10.05, 7.74), amount = 150.5,
        )
        var deleteCount = 0
        rule.setContent {
            MealTheme {
                DetailScreen(
                    meal = detail,
                    photos = photos,
                    busy = false,
                    onBack = {},
                    onDelete = { deleteCount++ },
                )
            }
        }

        rule.onNodeWithText("2026.11.16 12:30").assertIsDisplayed()
        val detailTexts = listOf(
            "상세 검증 식사",
            "섭취량 150.5 g",
            "탄수화물 23.9 g · 단백질 10.1 g · 지방 7.7 g",
        )
        detailTexts.forEach { text ->
            scrollDetailTo(text)
            rule.onNodeWithText(text).assertIsDisplayed()
        }
        rule.onNodeWithText("상세 검증 식사").onParent()
            .assert(hasAnyDescendant(hasText("205.5 kcal")))

        scrollDetailTo("이 기록 삭제")
        rule.onNodeWithText("이 기록 삭제").assertIsEnabled().performClick()
        rule.onNodeWithText("식사 기록을 삭제할까요?").assertIsDisplayed()
        rule.onNodeWithText("취소").performClick()

        rule.onNodeWithText("식사 기록을 삭제할까요?").assertDoesNotExist()
        rule.runOnIdle { assertEquals("취소 후 삭제 호출 횟수", 0, deleteCount) }
        detailTexts.forEach { text ->
            scrollDetailTo(text)
            rule.onNodeWithText(text).assertIsDisplayed()
        }

        scrollDetailTo("이 기록 삭제")
        rule.onNodeWithText("이 기록 삭제").performClick()
        rule.onNodeWithText("식사 기록을 삭제할까요?").assertIsDisplayed()
        rule.onNodeWithText("삭제").performClick()

        rule.runOnIdle { assertEquals("확정 후 삭제 호출 횟수", 1, deleteCount) }
        rule.onNodeWithText("식사 기록을 삭제할까요?").assertDoesNotExist()
    }

    @Test
    fun analysisFourteenDaysUpdatesTotalsNutrientsAndChart() {
        val today = LocalDate.of(2026, 11, 16)
        val older = meal(
            "analysis-old", "2026-11-02T12:00", "기간 밖 식사",
            Nutrients(200.0, 20.0, 10.0, 4.0),
        )
        val firstDay = meal(
            "analysis-first", "2026-11-03T08:00", "기간 시작일 식사",
            Nutrients(100.0, 10.0, 5.0, 2.0), type = MealType.BREAKFAST,
        )
        val lastDay = meal(
            "analysis-last", "2026-11-16T20:00", "기간 종료일 식사",
            Nutrients(300.0, 30.0, 15.0, 6.0), type = MealType.DINNER,
        )
        val future = meal(
            "analysis-future", "2026-11-17T10:00", "미래 식사",
            Nutrients(900.0, 90.0, 45.0, 18.0), type = MealType.SNACK,
        )
        rule.setContent {
            MealTheme { AnalysisScreen(listOf(future, lastDay, older, firstDay), today) }
        }

        rule.onNodeWithTag("analysis-period").assertTextContains("1달 ▾")
        rule.onNodeWithTag("analysis-total-kcal").assertTextEquals("600.0 kcal")
        val grid = rule.onNodeWithTag("analysis-grid")
        grid.performScrollToNode(hasTestTag("daily-calories-chart"))
        rule.onNodeWithTag("daily-calories-chart")
            .assert(hasContentDescription("2026-11-17", substring = true).not())

        grid.performScrollToIndex(0)
        rule.onNodeWithTag("analysis-period").performClick()
        rule.onNodeWithText("14일").performClick()

        rule.onNodeWithTag("analysis-period").assertTextContains("14일 ▾")
        rule.onNodeWithTag("analysis-total-kcal").assertTextEquals("400.0 kcal")
        rule.onNodeWithText("2026-11-03 ~ 2026-11-16").assertIsDisplayed()

        grid.performScrollToNode(hasText("기록한 식사"))
        rule.onNodeWithText("기록한 식사").onParent()
            .assert(hasAnyDescendant(hasText("2끼")))
            .assert(hasAnyDescendant(hasText("2일 기록")))
        grid.performScrollToNode(hasText("하루 평균"))
        rule.onNodeWithText("하루 평균").onParent()
            .assert(hasAnyDescendant(hasText("28.6")))
            .assert(hasAnyDescendant(hasText("kcal · 빈 날짜 포함")))

        listOf("탄수화물 40.0 g", "단백질 20.0 g", "지방 8.0 g").forEach { text ->
            grid.performScrollToNode(hasText(text))
            rule.onNodeWithText(text).assertIsDisplayed()
        }

        grid.performScrollToNode(hasTestTag("daily-calories-chart"))
        rule.onNodeWithTag("daily-calories-chart").assertIsDisplayed()
            .assert(hasContentDescription("2026-11-03: 100.0 kcal", substring = true))
            .assert(hasContentDescription("2026-11-16: 300.0 kcal", substring = true))
            .assert(hasContentDescription("2026-11-04: 0.0 kcal", substring = true))
            .assert(hasContentDescription("2026-11-02", substring = true).not())
            .assert(hasContentDescription("2026-11-17", substring = true).not())
    }

    private fun scrollDetailTo(text: String) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    private fun meal(
        id: String,
        eatenAt: String,
        name: String,
        nutrients: Nutrients,
        amount: Double = 100.0,
        type: MealType = MealType.LUNCH,
    ) = Meal(
        id = id,
        eatenAt = LocalDateTime.parse(eatenAt),
        type = type,
        photoName = null,
        items = listOf(MealItem("food-$id", name, amount, "g", nutrients)),
    )
}
