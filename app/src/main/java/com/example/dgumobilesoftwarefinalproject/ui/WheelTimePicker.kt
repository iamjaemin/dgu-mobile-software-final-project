package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WheelTimePicker(hour: Int, minute: Int, onHour: (Int) -> Unit, onMinute: (Int) -> Unit,
                    onHourScrolling: (Boolean) -> Unit, onMinuteScrolling: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("meal-time-input"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WheelColumn(hour, 24, "시", Modifier.weight(1f), "hour-wheel", onHour, onHourScrolling)
        WheelColumn(minute, 60, "분", Modifier.weight(1f), "minute-wheel", onMinute, onMinuteScrolling)
    }
}

@Composable
private fun WheelColumn(value: Int, count: Int, suffix: String, modifier: Modifier, tag: String,
                        onValue: (Int) -> Unit, onScrolling: (Boolean) -> Unit) {
    val list = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val scope = rememberCoroutineScope()
    val currentOnValue by rememberUpdatedState(onValue)
    val currentOnScrolling by rememberUpdatedState(onScrolling)
    val centered by remember {
        derivedStateOf {
            val layout = list.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: value
        }
    }
    LaunchedEffect(list) {
        snapshotFlow { list.isScrollInProgress to centered }.collect { (scrolling, index) ->
            currentOnScrolling(scrolling)
            currentOnValue(index.coerceIn(0, count - 1))
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(suffix, style = MaterialTheme.typography.labelMedium, color = Muted)
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(44.dp).background(FieldBackground, RoundedCornerShape(16.dp)))
            LazyColumn(state = list, flingBehavior = rememberSnapFlingBehavior(list),
                contentPadding = PaddingValues(vertical = 88.dp), horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().testTag(tag).semantics {
                    contentDescription = "$suffix 선택 휠"
                    stateDescription = "${value.toString().padStart(2, '0')}$suffix"
                }.drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(listOf(White, White.copy(alpha = 0f), White.copy(alpha = 0f), White)))
                }) {
                items((0 until count).toList(), key = { it }) { number ->
                    Box(Modifier.fillMaxWidth().height(44.dp).clickable { scope.launch { list.animateScrollToItem(number) } }, contentAlignment = Alignment.Center) {
                        Text(number.toString().padStart(2, '0'), color = if (number == centered) Primary else Muted,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = if (number == centered) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
