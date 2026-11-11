package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dgumobilesoftwarefinalproject.domain.MealSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** One date-aligned chart, including zero-intake dates, rather than a row per date. */
@Composable
fun DailyCaloriesChart(byDate: Map<LocalDate, MealSummary>) {
    val points = remember(byDate) { byDate.toSortedMap().entries.toList() }
    val max = points.maxOfOrNull { it.value.nutrients.energyKcal }?.takeIf { it > 0 } ?: 100.0
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Pretendard, fontSize = 10.sp, color = Muted)
    val description = remember(points) {
        "날짜별 열량 막대 그래프. " + points.joinToString("; ") { "${it.key}: ${it.value.nutrients.energyKcal.display()} kcal" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("kcal", style = MaterialTheme.typography.labelSmall, color = Muted)
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("daily-calories-chart").semantics { contentDescription = description }) {
            val left = 48.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 32.dp.toPx()
            val height = bottom - top
            listOf(0.0, .5, 1.0).forEach { fraction ->
                val y = bottom - height * fraction.toFloat()
                drawLine(Divider, Offset(left, y), Offset(right, y), 1.dp.toPx())
                val label = textMeasurer.measure((max * fraction).display(), labelStyle)
                drawText(label, topLeft = Offset((left - label.size.width - 8.dp.toPx()).coerceAtLeast(0f), y - label.size.height / 2))
            }
            if (points.isNotEmpty()) {
                val slot = (right - left) / points.size
                val width = minOf(24.dp.toPx(), slot * .65f)
                points.forEachIndexed { index, point ->
                    val barHeight = height * (point.value.nutrients.energyKcal / max).toFloat()
                    if (barHeight > 0) drawRoundRect(Primary,
                        topLeft = Offset(left + slot * index + (slot - width) / 2, bottom - barHeight),
                        size = Size(width, barHeight), cornerRadius = CornerRadius(minOf(width / 3, 4.dp.toPx())))
                }
                val marks = if (points.size <= 7) points.indices.toList() else (0..4).map { it * (points.size - 1) / 4 }.distinct()
                marks.forEach { index ->
                    val label = textMeasurer.measure(points[index].key.format(DateTimeFormatter.ofPattern("MM.dd")), labelStyle)
                    val x = (left + slot * (index + .5f) - label.size.width / 2).coerceIn(0f, size.width - label.size.width)
                    drawText(label, topLeft = Offset(x, bottom + 10.dp.toPx()))
                }
            }
        }
        if (points.none { it.value.count > 0 }) Text("선택한 기간에는 기록한 식사가 없어요.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Text("가로축은 날짜, 세로축은 하루 총 열량이에요. 기록이 없는 날짜는 0kcal로 표시해요.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
