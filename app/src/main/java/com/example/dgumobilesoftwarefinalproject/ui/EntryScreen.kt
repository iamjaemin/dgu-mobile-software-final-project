package com.example.dgumobilesoftwarefinalproject.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dgumobilesoftwarefinalproject.model.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DateButton(date: LocalDate, enabled: Boolean = true, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, y, m, d -> onDate(LocalDate.of(y, m + 1, d)) },
            date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, enabled = enabled) { Text(date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(state: MealUiState, vm: MealViewModel) {
    var choosingFood by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("한 끼를 기록해요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            SectionCard(title = "언제 드셨나요?") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton(state.date, !state.busy, vm::setDate)
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m -> vm.setTime(LocalTime.of(h, m)) },
                            state.time.hour, state.time.minute, true).show()
                    }, enabled = !state.busy) { Text(state.time.format(DateTimeFormatter.ofPattern("HH:mm"))) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { type -> FilterChip(selected = state.type == type,
                        onClick = { vm.setType(type) }, label = { Text(type.label) }, enabled = !state.busy) }
                }
            }
        }
        item {
            SectionCard(title = "사진으로 남기는 식사") {
                MealPhoto(state.photo, vm.photos, Modifier.fillMaxWidth().height(180.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !state.busy) { Text(if (state.photo == null) "사진 선택" else "사진 변경") }
                    if (state.photo != null) TextButton(onClick = vm::removePhoto, enabled = !state.busy) { Text("사진 제거") }
                }
                Text("사진은 선택 사항이에요. 음식과 반찬은 아래에서 직접 추가해 주세요.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("음식과 반찬 ${state.items.size}개", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { choosingFood = true }, enabled = !state.busy) { Text("+ 음식 추가") }
            }
        }
        if (state.items.isEmpty()) item {
            SectionCard {
                Text("어떤 음식을 드셨나요?")
                Text("음식 이름을 검색하고, 함께 먹은 반찬도 각각 추가해 주세요.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { choosingFood = true }, enabled = !state.busy) { Text("음식 검색하기") }
            }
        }
        items(state.items, key = { it.id }) { item ->
            SectionCard {
                Text(item.food.category + " · " + item.food.origin, style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(value = item.name, onValueChange = { vm.updateItem(item.id, name = it) },
                    label = { Text("음식·반찬 이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    enabled = !state.busy, isError = item.name.isBlank())
                OutlinedTextField(value = item.amount, onValueChange = { vm.updateItem(item.id, amount = it) },
                    label = { Text("섭취량 (${item.food.basisUnit})") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !state.busy,
                    isError = item.quantity == null, supportingText = {
                        Text(if (item.quantity == null) "0보다 크고 100,000 이하인 숫자를 입력해 주세요."
                            else if (item.food.basisUnit == "mL") "부피 기준 음료는 mL로 입력해요. g으로 환산하지 않아요." else "먹은 양을 g으로 입력해 주세요.")
                    })
                val n = item.nutrition
                Text("탄수화물 ${n.carbohydrateG.display()}g · 단백질 ${n.proteinG.display()}g · 지방 ${n.fatG.display()}g",
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${n.energyKcal.display()} kcal", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { vm.removeItem(item.id) }, enabled = !state.busy) { Text("항목 삭제") }
                }
            }
        }
        item { NutritionPanel(state.preview, "이번 식사 미리보기") }
        item {
            Button(onClick = vm::save, enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text(if (state.busy) "처리 중…" else "식사 기록 저장")
            }
        }
    }
    if (choosingFood) FoodPicker(state.foods, onDismiss = { choosingFood = false }) {
        vm.add(it)
        choosingFood = false
    }
}

@Composable
fun FoodPicker(foods: List<Food>, onDismiss: () -> Unit, onSelect: (Food) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(foods, query) {
        val terms = query.replace('_', ' ').trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        foods.filter { food -> terms.all { food.name.replace('_', ' ').contains(it, ignoreCase = true) } }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = White, border = BorderStroke(1.dp, Orange)) {
            Column(Modifier.padding(20.dp).fillMaxWidth().heightIn(max = 580.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("음식·반찬 검색", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text("음식 이름 검색") }, modifier = Modifier.fillMaxWidth())
                Text("${results.size}개 검색 결과 · 최대 40개 표시", style = MaterialTheme.typography.labelSmall)
                if (results.isEmpty()) Text("검색 결과가 없어요. 다른 이름으로 검색해 주세요.")
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(results.take(40), key = { it.code }) { food ->
                        Column(Modifier.fillMaxWidth().clickable { onSelect(food) }.padding(vertical = 12.dp)) {
                            Text(food.name, fontWeight = FontWeight.Bold)
                            Text("${food.origin} · ${food.basisAmount.display()}${food.basisUnit}당 ${food.nutrients.energyKcal.display()}kcal",
                                style = MaterialTheme.typography.bodySmall)
                            Text(food.code, style = MaterialTheme.typography.labelSmall)
                        }
                        HorizontalDivider(color = PaleOrange)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("닫기") }
            }
        }
    }
}
