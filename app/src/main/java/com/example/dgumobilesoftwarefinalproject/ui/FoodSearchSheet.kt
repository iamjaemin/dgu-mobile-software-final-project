package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.model.Food
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPicker(foods: List<Food>, onDismiss: () -> Unit, onSelect: (Food) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("전체") }
    var origin by rememberSaveable { mutableStateOf("전체") }
    var unit by rememberSaveable { mutableStateOf("전체") }
    var added by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var notification by remember { mutableStateOf<Job?>(null) }
    val categories = remember(foods) { listOf("전체") + foods.map { it.category }.distinct().sorted() }
    val origins = remember(foods) { listOf("전체") + foods.map { it.origin }.distinct().sorted() }
    val results = remember(foods, query, category, origin, unit) {
        val terms = query.replace('_', ' ').trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        foods.filter { food -> (category == "전체" || food.category == category) &&
            (origin == "전체" || food.origin == origin) && (unit == "전체" || food.basisUnit == unit) &&
            terms.all { food.name.replace('_', ' ').contains(it, ignoreCase = true) } }
    }
    val grouped = remember(results) { results.groupBy { it.category }.toSortedMap() }
    val showMenus = category == "전체" && query.isBlank() && origin == "전체" && unit == "전체"
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = White) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(.95f).imePadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("food-search-sheet"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("음식과 반찬 찾기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            MealInput(query, onValueChange = { query = it }, hint = "음식 이름 검색")
            FilterRow("카테고리", categories, category) { category = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown("식품 기원", origins, origin, Modifier.weight(1f)) { origin = it }
                FilterDropdown("단위", listOf("전체", "g", "mL"), unit, Modifier.weight(1f)) { unit = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${results.size}개 결과 · 이번에 ${added}개 추가", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { category = "전체"; origin = "전체"; unit = "전체"; query = "" }, contentPadding = PaddingValues(0.dp)) { Text("필터 초기화") }
            }
            if (showMenus) Text("메뉴 분류를 선택해 음식을 찾아보세요.", style = MaterialTheme.typography.bodySmall, color = Muted)
            if (results.isEmpty()) Text("일치하는 음식이 없어요. 검색어나 필터를 바꿔 주세요.")
            key(query, category, origin, unit) {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).testTag("food-results-grid"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)) {
                    if (showMenus) {
                        items(grouped.keys.toList(), key = { "menu-$it" }) { menu ->
                            Card(onClick = { category = menu }, modifier = Modifier.fillMaxWidth().softCardShadow().testTag("menu-card-$menu"),
                                shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White, contentColor = Ink)) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(menu, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${grouped.getValue(menu).size}개 음식", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    Text("메뉴 보기 →", color = Primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else grouped.forEach { (menu, menuFoods) ->
                        item(key = "heading-$menu", span = { GridItemSpan(2) }) {
                            SectionCard(title = menu) { Text("${menuFoods.size}개 음식 · 카드를 누르면 추가돼요.", style = MaterialTheme.typography.bodySmall, color = Muted) }
                        }
                        items(menuFoods, key = { it.code }) { food ->
                        Card(onClick = {
                            onSelect(food)
                            added++
                            notification?.cancel()
                            notification = scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar("${food.name} 추가 완료", withDismissAction = true, duration = SnackbarDuration.Short)
                            }
                        }, modifier = Modifier.fillMaxWidth().softCardShadow(), shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = White, contentColor = Ink)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(food.name, fontWeight = FontWeight.Bold)
                                Text(food.origin, style = MaterialTheme.typography.bodySmall)
                                Text("${food.basisAmount.display()}${food.basisUnit}당 ${food.nutrients.energyKcal.display()}kcal", style = MaterialTheme.typography.labelSmall)
                                Text("+ 추가", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        }
                    }
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 52.dp)) { Text("선택 완료") }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).padding(bottom = 80.dp))
        }
    }
}

@Composable
private fun FilterRow(label: String, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value -> RoundedFilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(value) }, modifier = Modifier.testTag("category-$value")) }
    }
}

@Composable
private fun FilterDropdown(label: String, values: List<String>, selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $selected ▾", maxLines = 1) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 300.dp)) {
            values.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { onSelect(value); open = false }) }
        }
    }
}
