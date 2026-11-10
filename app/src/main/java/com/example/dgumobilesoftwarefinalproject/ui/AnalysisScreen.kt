package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.domain.MealAnalysis
import com.example.dgumobilesoftwarefinalproject.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AnalysisScreen(meals: List<Meal>, today: LocalDate) {
    var dayText by rememberSaveable { mutableStateOf(today.toString()) }
    val selectedDay = LocalDate.parse(dayText)
    val daily = remember(meals, selectedDay) { MealAnalysis.day(meals, selectedDay) }
    val week = remember(meals, today) { MealAnalysis.week(meals, today) }
    val month = remember(meals, today) { MealAnalysis.month(meals, today) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("나의 식사 균형", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("하루부터 한 달까지, 기록으로 살펴봐요")
        }
        item {
            SectionCard(title = "하루의 영양 균형") {
                DateButton(selectedDay) { dayText = it.toString() }
                Text("${daily.total.count}끼 기록")
                if (daily.total.count == 0) Text("이 날짜에는 기록한 식사가 없어요.")
            }
        }
        item { NutritionPanel(daily.total.nutrients, "하루 총 섭취량", balance = true) }
        item {
            SectionCard(title = "최근 7일") {
                Text("${week.start} ~ ${week.end}", style = MaterialTheme.typography.bodySmall)
                Text("${week.total.count}끼 · ${week.total.nutrients.energyKcal.display()} kcal", fontWeight = FontWeight.Bold)
                week.byDate.forEach { (day, summary) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(day.format(DateTimeFormatter.ofPattern("MM.dd")))
                        Text("${summary.count}끼 · ${summary.nutrients.energyKcal.display()} kcal")
                    }
                }
            }
        }
        item { NutritionPanel(week.total.nutrients, "한 주의 영양 균형", balance = true) }
        item {
            SectionCard(title = "최근 한 달") {
                Text("${month.start} ~ ${month.end}", style = MaterialTheme.typography.bodySmall)
                Text("${month.total.nutrients.energyKcal.display()} kcal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${month.total.count}끼 기록 · 하루 평균 ${(month.total.nutrients.energyKcal / month.byDate.size).display()} kcal")
                Text("평균은 기록이 없는 날짜도 포함해 계산해요.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item { Text("한 달 동안, 끼니별 섭취", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        MealType.entries.forEach { type ->
            item {
                val summary = month.byType.getValue(type)
                SectionCard(title = "${type.label} · ${summary.count}끼") {
                    Text("${summary.nutrients.energyKcal.display()} kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("탄수화물 ${summary.nutrients.carbohydrateG.display()} g")
                    Text("단백질 ${summary.nutrients.proteinG.display()} g")
                    Text("지방 ${summary.nutrients.fatG.display()} g")
                }
            }
        }
        item { Text("표시된 값은 기록한 음식의 합계예요. 미래 날짜의 식사는 오늘 기준 주간·월간 분석에 포함하지 않아요.",
            style = MaterialTheme.typography.bodySmall) }
    }
}
