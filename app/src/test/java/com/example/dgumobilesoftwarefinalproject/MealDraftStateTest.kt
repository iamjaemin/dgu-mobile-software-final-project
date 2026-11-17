package com.example.dgumobilesoftwarefinalproject

import com.example.dgumobilesoftwarefinalproject.model.Food
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import com.example.dgumobilesoftwarefinalproject.ui.DraftItem
import com.example.dgumobilesoftwarefinalproject.ui.MealUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MealDraftStateTest {
    private val tolerance = 1e-6

    @Test
    fun quantityAndValidityAcceptOnlyAllowedAmounts() {
        val food = food("A", Nutrients(100.0, 10.0, 5.0, 2.0))
        val validInputs = listOf("0.5" to 0.5, "100000" to 100000.0)

        validInputs.forEach { (input, expectedQuantity) ->
            val item = DraftItem(id = "valid-$input", food = food, amount = input)

            assertEquals(
                "정상 섭취량 변환: $input",
                expectedQuantity,
                requireNotNull(item.quantity) { "정상 섭취량이 null입니다: $input" },
                tolerance,
            )
            assertTrue("정상 입력의 유효성: $input", item.valid)
        }

        val invalidInputs = listOf(
            "", "abc", "0", "-1", "100000.1", "NaN", "Infinity", "-Infinity",
        )
        invalidInputs.forEach { input ->
            val item = DraftItem(id = "invalid-$input", food = food, amount = input)

            assertNull("잘못된 입력의 섭취량: '$input'", item.quantity)
            assertFalse("잘못된 입력의 유효성: '$input'", item.valid)
        }
    }

    @Test
    fun nutritionAndPreviewTreatInvalidAmountAsZero() {
        val itemA = DraftItem(
            id = "draft-a",
            food = food("A", Nutrients(100.0, 10.0, 5.0, 2.0)),
            amount = "150",
        )
        val itemB = DraftItem(
            id = "draft-b",
            food = food("B", Nutrients(50.0, 4.0, 3.0, 1.0)),
            amount = "50",
        )
        val itemC = DraftItem(
            id = "draft-c",
            food = food("C", Nutrients(80.0, 8.0, 2.0, 3.0)),
            amount = "abc",
        )
        val state = MealUiState(
            loading = false,
            loadFailed = false,
            busy = false,
            date = LocalDate.of(2026, 11, 17),
            time = LocalTime.NOON,
            photo = null,
            items = listOf(itemA, itemB, itemC),
        )

        assertNutrients("음식 A", Nutrients(150.0, 15.0, 7.5, 3.0), itemA.nutrition)
        assertNutrients("음식 B", Nutrients(25.0, 2.0, 1.5, 0.5), itemB.nutrition)
        assertNutrients("잘못된 섭취량의 음식 C", Nutrients(0.0, 0.0, 0.0, 0.0), itemC.nutrition)
        assertNutrients("전체 미리보기", Nutrients(175.0, 17.0, 9.0, 3.5), state.preview)
    }

    @Test
    fun canSaveRequiresReadyStateAndAllValidItemsWithoutPhoto() {
        val validItem = DraftItem(
            id = "valid-1",
            food = food("A", Nutrients(100.0, 10.0, 5.0, 2.0)),
            amount = "100",
        )
        val secondValidItem = validItem.copy(id = "valid-2", amount = "50")
        val readyState = MealUiState(
            loading = false,
            loadFailed = false,
            busy = false,
            date = LocalDate.of(2026, 11, 17),
            time = LocalTime.NOON,
            photo = null,
            items = listOf(validItem, secondValidItem),
        )

        assertNull("사진 없는 정상 상태", readyState.photo)
        assertTrue("사진 없이도 유효한 식사는 저장 가능", readyState.canSave)

        val blockedStates = listOf(
            "로딩 중" to readyState.copy(loading = true),
            "불러오기 실패" to readyState.copy(loadFailed = true),
            "처리 중" to readyState.copy(busy = true),
            "빈 항목 목록" to readyState.copy(items = emptyList()),
            "공백 이름" to readyState.copy(
                items = listOf(validItem.copy(name = "   "), secondValidItem),
            ),
            "잘못된 섭취량" to readyState.copy(
                items = listOf(validItem.copy(amount = "abc"), secondValidItem),
            ),
        )
        blockedStates.forEach { (condition, state) ->
            assertFalse("저장 차단 조건: $condition", state.canSave)
        }
    }

    private fun food(code: String, nutrients: Nutrients) = Food(
        code = code,
        name = "음식 $code",
        category = "테스트",
        origin = "테스트",
        basisAmount = 100.0,
        basisUnit = "g",
        nutrients = nutrients,
    )

    private fun assertNutrients(label: String, expected: Nutrients, actual: Nutrients) {
        assertEquals("$label: 열량(kcal)", expected.energyKcal, actual.energyKcal, tolerance)
        assertEquals("$label: 탄수화물(g)", expected.carbohydrateG, actual.carbohydrateG, tolerance)
        assertEquals("$label: 단백질(g)", expected.proteinG, actual.proteinG, tolerance)
        assertEquals("$label: 지방(g)", expected.fatG, actual.fatG, tolerance)
    }
}
