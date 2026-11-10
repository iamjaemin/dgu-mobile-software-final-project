package com.example.dgumobilesoftwarefinalproject.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.model.MealType
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(state: MealUiState, vm: MealViewModel) {
    var step by rememberSaveable(state.saveVersion) { mutableIntStateOf(0) }
    var choosingFood by rememberSaveable { mutableStateOf(false) }
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val titles = listOf("식사한 시간", "음식과 반찬", "사진 업로드", "이번 식사 미리보기")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }
    BackHandler(enabled = step > 0 && !choosingFood && !choosingTime && !state.busy) { step-- }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("한 끼를 기록해요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                titles.indices.forEach { index ->
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (index <= step) Orange else PaleOrange), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = if (index <= step) White else Orange, style = MaterialTheme.typography.labelMedium)
                    }
                    if (index < 3) HorizontalDivider(Modifier.weight(1f), color = if (index < step) Orange else PaleOrange)
                }
            }
            Text("${step + 1}/4 · ${titles[step]}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        key(step) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (step) {
                    0 -> item {
                        SectionCard(title = "언제 드셨나요?") {
                            Text(state.date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")), style = MaterialTheme.typography.titleLarge)
                            Text(state.time.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { choosingTime = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("날짜와 시간 선택") }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MealType.entries.forEach { type -> FilterChip(selected = state.type == type,
                                    onClick = { vm.setType(type) }, label = { Text(type.label) }, enabled = !state.busy) }
                            }
                        }
                    }
                    1 -> {
                        item {
                            SectionCard(title = "음식과 반찬 ${state.items.size}개") {
                                Text("종류별로 찾아 추가한 뒤, 실제로 먹은 양을 입력해 주세요.")
                                Button(onClick = { choosingFood = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("음식·반찬 찾기") }
                            }
                        }
                        items(state.items, key = { it.id }) { item ->
                            SectionCard {
                                Text(item.food.category + " · " + item.food.origin, style = MaterialTheme.typography.labelSmall)
                                OutlinedTextField(value = item.name, onValueChange = { vm.updateItem(item.id, name = it) },
                                    label = { Text("음식·반찬 이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy, isError = item.name.isBlank())
                                OutlinedTextField(value = item.amount, onValueChange = { vm.updateItem(item.id, amount = it) },
                                    label = { Text("섭취량 (${item.food.basisUnit})") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !state.busy,
                                    isError = item.quantity == null, supportingText = {
                                        Text(if (item.quantity == null) "0보다 크고 100,000 이하인 양을 입력해 주세요."
                                        else if (item.food.basisUnit == "mL") "부피 기준 음료는 mL로 입력해요." else "실제로 먹은 양을 g으로 입력해 주세요.")
                                    })
                                val n = item.nutrition
                                Text("탄수화물 ${n.carbohydrateG.display()}g · 단백질 ${n.proteinG.display()}g · 지방 ${n.fatG.display()}g", style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${n.energyKcal.display()} kcal", fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { vm.removeItem(item.id) }, enabled = !state.busy) { Text("항목 삭제") }
                                }
                            }
                        }
                    }
                    2 -> item {
                        SectionCard(title = "사진으로 남기는 식사") {
                            MealPhoto(state.photo, vm.photos, Modifier.fillMaxWidth().height(220.dp))
                            Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = White)) { Text(if (state.photo == null) "사진 선택" else "사진 변경") }
                            if (state.photo != null) TextButton(onClick = vm::removePhoto, enabled = !state.busy) { Text("사진 제거") }
                            Text("사진은 선택 사항이에요. 사진 없이도 다음 단계로 이동할 수 있어요.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    3 -> {
                        item { SectionCard(title = "기록 내용 확인") {
                            Text("${state.date} ${state.time.format(DateTimeFormatter.ofPattern("HH:mm"))} · ${state.type.label}")
                            state.items.forEach { Text("${it.name} · ${it.quantity?.display()} ${it.food.basisUnit}") }
                            if (state.photo != null) MealPhoto(state.photo, vm.photos, Modifier.fillMaxWidth().height(160.dp))
                        } }
                        item { NutritionPanel(state.preview, "이번 식사 미리보기", balance = true) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) OutlinedButton(onClick = { focusManager.clearFocus(); keyboard?.hide(); step-- }, enabled = !state.busy, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("이전") }
            val valid = step != 1 || (state.items.isNotEmpty() && state.items.all { it.valid })
            Button(onClick = { focusManager.clearFocus(); keyboard?.hide(); if (step < 3) step++ else vm.save() }, enabled = !state.busy && valid && (step < 3 || state.canSave),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text(if (state.busy) "처리 중…" else if (step < 3) "다음" else "식사 기록 저장")
            }
        }
    }
    if (choosingTime) DateTimeSheet(state.date, state.time, onDismiss = { choosingTime = false }) { date, time ->
        vm.setDate(date); vm.setTime(time); choosingTime = false
    }
    if (choosingFood) FoodPicker(state.foods, onDismiss = { choosingFood = false }) { vm.add(it) }
}
