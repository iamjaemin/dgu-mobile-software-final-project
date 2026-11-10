package com.example.dgumobilesoftwarefinalproject.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSheet(date: LocalDate, time: LocalTime, onDismiss: () -> Unit,
                  includeTime: Boolean = true, onConfirm: (LocalDate, LocalTime) -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val koreanConfiguration = remember(configuration) { Configuration(configuration).apply { setLocale(Locale.KOREA) } }
    val koreanContext = remember(context, koreanConfiguration) { context.createConfigurationContext(koreanConfiguration) }
    var selectedMillis by rememberSaveable { mutableLongStateOf(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) }
    var hour by rememberSaveable { mutableIntStateOf(time.hour) }
    var minute by rememberSaveable { mutableIntStateOf(time.minute) }
    val calendar = remember { DatePickerState(locale = Locale.KOREA, initialSelectedDateMillis = selectedMillis) }
    LaunchedEffect(calendar.selectedDateMillis) { calendar.selectedDateMillis?.let { selectedMillis = it } }
    CompositionLocalProvider(LocalContext provides koreanContext, LocalConfiguration provides koreanConfiguration,
        LocalResources provides koreanContext.resources) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = White) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.95f).imePadding().testTag("date-time-sheet")) {
                Text(if (includeTime) "식사한 날짜와 시간" else "날짜 선택", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DatePicker(state = calendar, title = null, headline = null, showModeToggle = false,
                        modifier = Modifier.fillMaxWidth().testTag("korean-calendar"))
                    if (includeTime) {
                        HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = PaleOrange)
                        Text("식사 시간 · 24시간", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).testTag("meal-time-input"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeDropdown(hour, 0..23, "시", Modifier.weight(1f)) { hour = it }
                            TimeDropdown(minute, 0..59, "분", Modifier.weight(1f)) { minute = it }
                        }
                    }
                }
                Button(onClick = {
                    val appliedMillis = calendar.selectedDateMillis ?: return@Button
                    onConfirm(Instant.ofEpochMilli(appliedMillis).atZone(ZoneOffset.UTC).toLocalDate(), LocalTime.of(hour, minute))
                }, enabled = calendar.selectedDateMillis != null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).heightIn(min = 52.dp)) {
                    Text(if (includeTime) "날짜와 시간 적용" else "날짜 적용")
                }
            }
        }
    }
}

@Composable
private fun TimeDropdown(value: Int, range: IntRange, suffix: String, modifier: Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("${value.toString().padStart(2, '0')}$suffix ▾", style = MaterialTheme.typography.titleLarge)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 280.dp)) {
            range.forEach { number -> DropdownMenuItem(text = { Text("${number.toString().padStart(2, '0')}$suffix") },
                onClick = { onSelect(number); open = false }) }
        }
    }
}

@Composable
fun DateButton(date: LocalDate, enabled: Boolean = true, onDate: (LocalDate) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled) { Text(date.toString()) }
    if (open) DateTimeSheet(date, LocalTime.NOON, onDismiss = { open = false }, includeTime = false) { selected, _ ->
        onDate(selected); open = false
    }
}
