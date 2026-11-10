package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.domain.*
import com.example.dgumobilesoftwarefinalproject.model.Meal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class AnalysisPeriod(val label: String) { MONTH("1달"), WEEK("7일"), FORTNIGHT("14일"), DAY("하루") }

@Composable
fun AnalysisScreen(meals: List<Meal>, today: LocalDate) {
    var periodName by rememberSaveable { mutableStateOf(AnalysisPeriod.MONTH.name) }
    var dayText by rememberSaveable { mutableStateOf(today.toString()) }
    var expanded by remember { mutableStateOf(false) }
    val period = AnalysisPeriod.valueOf(periodName)
    val selectedDay = LocalDate.parse(dayText)
    val summary = remember(meals, period, today, selectedDay) {
        when (period) {
            AnalysisPeriod.MONTH -> MealAnalysis.month(meals, today)
            AnalysisPeriod.WEEK -> MealAnalysis.week(meals, today)
            AnalysisPeriod.FORTNIGHT -> MealAnalysis.period(meals, today.minusDays(13), today)
            AnalysisPeriod.DAY -> MealAnalysis.day(meals, selectedDay)
        }
    }
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.testTag("analysis-grid"), contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(span = { GridItemSpan(2) }) {
            SectionCard(title = "최근 칼로리 총량") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("식사 분석", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("analysis-period")) { Text("${period.label} ▾") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            AnalysisPeriod.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) },
                                onClick = { periodName = option.name; expanded = false }) }
                        }
                    }
                }
                Text("${summary.total.nutrients.energyKcal.display()} kcal", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("analysis-total-kcal"))
                Text("${summary.start} ~ ${summary.end}", style = MaterialTheme.typography.bodySmall)
                if (period == AnalysisPeriod.DAY) DateButton(selectedDay) { dayText = it.toString() }
            }
        }
        item { SectionCard(title = "기록한 식사") {
            Text("${summary.total.count}끼", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${summary.byDate.count { it.value.count > 0 }}일 기록", style = MaterialTheme.typography.bodySmall)
        } }
        item { SectionCard(title = "하루 평균") {
            Text("${(summary.total.nutrients.energyKcal / summary.byDate.size).display()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("kcal · 빈 날짜 포함", style = MaterialTheme.typography.bodySmall)
        } }
        item(span = { GridItemSpan(2) }) {
            SectionCard(title = "영양 균형") {
                MacroDonut(summary.total.nutrients)
                Text("탄수화물·단백질 4kcal/g, 지방 9kcal/g 기준의 에너지 비율이에요.", style = MaterialTheme.typography.bodySmall)
                MacroBars(summary.total.nutrients)
            }
        }
        item(span = { GridItemSpan(2) }) { Text("시간대별 섭취 영양소", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(TimeBand.entries, key = { it.name }) { band ->
            val entry = summary.byTimeBand.getValue(band)
            SectionCard(title = band.label, modifier = Modifier.testTag("timeband-${band.name}")) {
                Text(band.hours, style = MaterialTheme.typography.labelMedium)
                Text("${entry.nutrients.energyKcal.display()} kcal", fontWeight = FontWeight.Bold)
                Text("${entry.count}끼", style = MaterialTheme.typography.bodySmall)
                MacroBars(entry.nutrients, compact = true)
            }
        }
        item(span = { GridItemSpan(2) }) {
            SectionCard(title = "날짜별 열량") {
                val max = summary.byDate.values.maxOfOrNull { it.nutrients.energyKcal }?.takeIf { it > 0 } ?: 1.0
                summary.byDate.forEach { (day, value) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(day.format(DateTimeFormatter.ofPattern("MM.dd")), style = MaterialTheme.typography.labelSmall)
                            Text("${value.nutrients.energyKcal.display()} kcal", style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(progress = { (value.nutrients.energyKcal / max).toFloat() },
                            modifier = Modifier.fillMaxWidth(), color = Orange, trackColor = PaleOrange, gapSize = 0.dp, drawStopIndicator = {})
                    }
                }
            }
        }
        item(span = { GridItemSpan(2) }) { Text("실제로 기록한 시각으로 시간대를 나눠요. 막대 길이는 각 카드 안의 최댓값 기준이며, 총 열량은 DB kcal를 합산해요.", style = MaterialTheme.typography.bodySmall) }
    }
}
