package com.example.dgumobilesoftwarefinalproject

import com.example.dgumobilesoftwarefinalproject.data.MealCodec
import com.example.dgumobilesoftwarefinalproject.model.Meal
import com.example.dgumobilesoftwarefinalproject.model.MealItem
import com.example.dgumobilesoftwarefinalproject.model.MealType
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MealCodecTest {
    private val tolerance = 1e-6

    @Test
    fun decodeRejectsDuplicateMealIdsAfterEncoding() {
        val first = meal("duplicate", "2026-11-16T08:00")
        val second = meal("duplicate", "2026-11-16T12:00")

        val encoded = MealCodec.encode(listOf(first, second))

        assertEquals("인코딩 시 두 기록 유지", 2, JSONObject(encoded).getJSONArray("meals").length())
        assertThrows(IllegalArgumentException::class.java) {
            MealCodec.decode(encoded)
        }
    }

    @Test
    fun decodeSortsMealsNewestFirstAndPreservesIdsAndTimes() {
        val latest = meal("latest", "2026-11-16T20:15:30")
        val oldest = meal("oldest", "2026-11-14T07:30:00")
        val middle = meal("middle", "2026-11-15T12:45:00")

        val decoded = MealCodec.decode(MealCodec.encode(listOf(latest, oldest, middle)))

        assertEquals("복원된 식사 개수", 3, decoded.size)
        assertEquals("최신순 ID", listOf("latest", "middle", "oldest"), decoded.map { it.id })
        assertEquals(
            "최신순 식사 시각",
            listOf(
                LocalDateTime.of(2026, 11, 16, 20, 15, 30),
                LocalDateTime.of(2026, 11, 15, 12, 45, 0),
                LocalDateTime.of(2026, 11, 14, 7, 30, 0),
            ),
            decoded.map { it.eatenAt },
        )
    }

    @Test
    fun decodeRejectsMissingProteinInsteadOfReturningZeroOrEmptyRecords() {
        val original = meal("missing-protein", "2026-11-16T12:30")
        val json = JSONObject(MealCodec.encode(listOf(original)))
        val nutrients = json.getJSONArray("meals").getJSONObject(0)
            .getJSONArray("items").getJSONObject(0).getJSONObject("nutrients")

        assertTrue("제거 전 필수 단백질 필드 존재", nutrients.has("proteinG"))
        nutrients.remove("proteinG")
        assertFalse("필수 단백질 필드 제거", nutrients.has("proteinG"))

        assertThrows(JSONException::class.java) {
            MealCodec.decode(json.toString())
        }
    }

    @Test
    fun encodeAndDecodePreserveAllMealAndItemData() {
        val breakfast = meal(
            id = "breakfast",
            eatenAt = "2026-11-14T07:15:30",
            type = MealType.BREAKFAST,
            photoName = "11111111-1111-4111-8111-111111111111.jpg",
            items = listOf(
                MealItem("egg", "달걀찜", 85.75, "g", Nutrients(120.125, 2.375, 8.625, 7.875)),
            ),
        )
        val lunch = meal(
            id = "lunch",
            eatenAt = "2026-11-15T12:30:45",
            type = MealType.LUNCH,
            photoName = null,
            items = listOf(
                MealItem("rice", "현미밥", 150.5, "g", Nutrients(205.55, 42.125, 4.375, 1.625)),
                MealItem("soy-milk", "두유", 200.25, "mL", Nutrients(95.875, 8.625, 6.375, 3.125)),
            ),
        )
        val dinner = meal(
            id = "dinner",
            eatenAt = "2026-11-16T18:20:10.123456789",
            type = MealType.DINNER,
            photoName = "22222222-2222-4222-8222-222222222222.jpg",
            items = listOf(
                MealItem("chicken", "닭가슴살", 100.75, "g", Nutrients(165.125, 0.25, 31.375, 3.625)),
            ),
        )
        val snack = meal(
            id = "snack",
            eatenAt = "2026-11-16T21:05:55",
            type = MealType.SNACK,
            photoName = null,
            items = listOf(
                MealItem("yogurt", "요거트_딸기", 125.125, "mL", Nutrients(88.875, 12.625, 3.75, 2.375)),
            ),
        )

        val encoded = MealCodec.encode(listOf(lunch, breakfast, snack, dinner))
        val decoded = MealCodec.decode(encoded)

        assertEquals("복원된 식사 개수", 4, decoded.size)
        assertEquals(
            "시각 내림차순 ID",
            listOf("snack", "dinner", "lunch", "breakfast"),
            decoded.map { it.id },
        )
        val expectedMeals = listOf(snack, dinner, lunch, breakfast)
        expectedMeals.zip(decoded).forEach { (expected, actual) ->
            assertMeal(expected, actual)
        }
    }

    private fun meal(
        id: String,
        eatenAt: String,
        type: MealType = MealType.LUNCH,
        photoName: String? = null,
        items: List<MealItem> = listOf(
            MealItem("test-food", "검증 음식", 100.5, "g", Nutrients(123.456, 7.89, 4.56, 1.23)),
        ),
    ) = Meal(
        id = id,
        eatenAt = LocalDateTime.parse(eatenAt),
        type = type,
        photoName = photoName,
        items = items,
    )

    private fun assertMeal(expected: Meal, actual: Meal) {
        val label = "식사 ${expected.id}"
        assertEquals("$label: ID", expected.id, actual.id)
        assertEquals("$label: 식사 일시", expected.eatenAt, actual.eatenAt)
        assertEquals("$label: 식사 유형", expected.type, actual.type)
        assertEquals("$label: 사진명", expected.photoName, actual.photoName)
        assertEquals("$label: 음식 항목 개수", expected.items.size, actual.items.size)
        expected.items.zip(actual.items).forEachIndexed { index, (expectedItem, actualItem) ->
            val itemLabel = "$label 항목 $index"
            assertEquals("$itemLabel: 식품코드", expectedItem.foodCode, actualItem.foodCode)
            assertEquals("$itemLabel: 이름", expectedItem.name, actualItem.name)
            assertEquals("$itemLabel: 섭취량", expectedItem.amount, actualItem.amount, tolerance)
            assertEquals("$itemLabel: 단위", expectedItem.unit, actualItem.unit)
            assertNutrients(itemLabel, expectedItem.nutrients, actualItem.nutrients)
        }
    }

    private fun assertNutrients(label: String, expected: Nutrients, actual: Nutrients) {
        assertEquals("$label: 열량(kcal)", expected.energyKcal, actual.energyKcal, tolerance)
        assertEquals("$label: 탄수화물(g)", expected.carbohydrateG, actual.carbohydrateG, tolerance)
        assertEquals("$label: 단백질(g)", expected.proteinG, actual.proteinG, tolerance)
        assertEquals("$label: 지방(g)", expected.fatG, actual.fatG, tolerance)
    }
}
