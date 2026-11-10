package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.data.PhotoRepository
import com.example.dgumobilesoftwarefinalproject.domain.NutritionCalculator
import com.example.dgumobilesoftwarefinalproject.model.Meal
import java.time.format.DateTimeFormatter

@Composable
fun DiaryScreen(meals: List<Meal>, photos: PhotoRepository, onAdd: () -> Unit, onSelect: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.testTag("meal-grid"), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(span = { GridItemSpan(2) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("차곡차곡, 나의 식사", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("사진과 함께 한 끼씩 돌아봐요", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (meals.isEmpty()) item(span = { GridItemSpan(2) }) {
            SectionCard(title = "아직 기록한 식사가 없어요") {
                Text("오늘 먹은 음식부터 남겨 보세요. 하루와 한 주의 영양 균형을 함께 볼 수 있어요.")
                Button(onClick = onAdd) { Text("첫 식사 기록하기") }
            }
        }
        meals.sortedByDescending { it.eatenAt }.groupBy { it.eatenAt.toLocalDate() }.forEach { (day, rows) ->
            item(key = "day-$day", span = { GridItemSpan(2) }) {
                Surface(color = White, contentColor = Ink, shape = RoundedCornerShape(24.dp), modifier = Modifier.softCardShadow()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(day.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")), fontWeight = FontWeight.Bold)
                        Text("${NutritionCalculator.total(rows).energyKcal.display()} kcal · ${rows.size}끼 기록")
                    }
                }
            }
            items(rows, key = { it.id }) { meal ->
                Card(onClick = { onSelect(meal.id) }, modifier = Modifier.fillMaxWidth().softCardShadow(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = White, contentColor = Ink)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealPhoto(meal.photoName, photos, Modifier.fillMaxWidth().aspectRatio(1f))
                        Text("${meal.type.label} · ${meal.eatenAt.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            style = MaterialTheme.typography.labelLarge)
                        Text(meal.items.joinToString(", ") { it.name }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(meal.eatenAt.format(DateTimeFormatter.ofPattern("MM.dd")), style = MaterialTheme.typography.labelSmall)
                        Text("${NutritionCalculator.total(listOf(meal)).energyKcal.display()} kcal · 자세히 보기")
                    }
                }
            }
        }
        if (meals.isNotEmpty()) item(span = { GridItemSpan(2) }) { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ 식사 기록하기") } }
    }
}

@Composable
fun DetailScreen(meal: Meal, photos: PhotoRepository, busy: Boolean, onBack: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text("← 식사 목록") } }
        item {
            SectionCard(title = "기록한 식사") {
                MealPhoto(meal.photoName, photos, Modifier.fillMaxWidth().height(240.dp))
                Text(meal.eatenAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")), style = MaterialTheme.typography.titleLarge)
                Text(meal.type.label)
            }
        }
        items(meal.items.indices.toList()) { i ->
            val item = meal.items[i]
            SectionCard(title = item.name) {
                Text("섭취량 ${item.amount.display()} ${item.unit}")
                Text("탄수화물 ${item.nutrients.carbohydrateG.display()} g · 단백질 ${item.nutrients.proteinG.display()} g · 지방 ${item.nutrients.fatG.display()} g")
                Text("${item.nutrients.energyKcal.display()} kcal", fontWeight = FontWeight.Bold)
            }
        }
        item { NutritionPanel(NutritionCalculator.total(listOf(meal)), "한 끼 총 섭취량", balance = true) }
        item { OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("이 기록 삭제") } }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("식사 기록을 삭제할까요?") },
        text = { Text("삭제한 기록은 되돌릴 수 없어요.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("삭제") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } })
}
