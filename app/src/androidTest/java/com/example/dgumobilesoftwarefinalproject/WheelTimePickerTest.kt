package com.example.dgumobilesoftwarefinalproject

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.dgumobilesoftwarefinalproject.ui.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class WheelTimePickerTest {
    @get:Rule val rule = createComposeRule()

    @Test fun wheelsSnapAfterSwipeAndSupportBothRangeEnds() {
        val hour = mutableIntStateOf(0)
        val minute = mutableIntStateOf(0)
        rule.setContent {
            MealTheme { Column {
                WheelTimePicker(hour.intValue, minute.intValue, { hour.intValue = it }, { minute.intValue = it }, {}, {})
                Text("선택 ${hour.intValue}:${minute.intValue}")
            } }
        }
        rule.onNodeWithTag("hour-wheel").performScrollToIndex(23)
        rule.onNodeWithTag("minute-wheel").performScrollToIndex(59)
        rule.onNodeWithText("선택 23:59").assertExists()
        rule.onNodeWithTag("hour-wheel").performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(hour.intValue in 0..22); assertEquals(59, minute.intValue) }
        val selectedHour = hour.intValue.toString().padStart(2, '0')
        val selectedBounds = rule.onNodeWithText(selectedHour).fetchSemanticsNode().boundsInRoot
        val wheelBounds = rule.onNodeWithTag("hour-wheel").fetchSemanticsNode().boundsInRoot
        assertTrue("스냅 후 선택한 값은 휠 중앙에 있어야 합니다.", abs(selectedBounds.center.y - wheelBounds.center.y) < 2f)
        rule.onNodeWithTag("hour-wheel").performScrollToIndex(0)
        rule.onNodeWithTag("minute-wheel").performScrollToIndex(0)
        rule.onNodeWithText("선택 0:0").assertExists()
    }
}
