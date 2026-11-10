package com.example.dgumobilesoftwarefinalproject.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dgumobilesoftwarefinalproject.R
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealApp(vm: MealViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var sources by rememberSaveable { mutableStateOf(false) }
    var consumedSaveVersion by rememberSaveable { mutableIntStateOf(0) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { while (true) { today = LocalDate.now(); delay(60_000) } }
    LaunchedEffect(state.message) {
        state.message?.let { message -> snackbar.showSnackbar(message); vm.clearMessage() }
    }
    LaunchedEffect(state.saveVersion) {
        if (state.saveVersion > consumedSaveVersion) {
            consumedSaveVersion = state.saveVersion
            tab = 1
            detailId = null
        }
    }
    val detail = state.meals.find { it.id == detailId }
    BackHandler(enabled = sources || detailId != null) { if (sources) sources = false else detailId = null }
    Scaffold(containerColor = White, contentColor = Orange,
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("오늘을 담다", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("MEAL DIARY", style = MaterialTheme.typography.labelSmall)
                }
            }, actions = { TextButton(onClick = { sources = true }) { Text("데이터 출처") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = Orange))
        }, bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(40.dp), color = White, contentColor = Orange,
                    border = BorderStroke(1.dp, PaleOrange)) {
                    Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val icons = listOf(R.drawable.ic_nav_entry, R.drawable.ic_nav_diary, R.drawable.ic_nav_analysis)
                        listOf("음식 입력", "식사 보기", "식사 분석").forEachIndexed { index, label ->
                            FilledIconToggleButton(checked = tab == index && !sources,
                                onCheckedChange = { tab = index; detailId = null; sources = false },
                                modifier = Modifier.size(60.dp), shape = RoundedCornerShape(30.dp),
                                colors = IconButtonDefaults.filledIconToggleButtonColors(
                                    containerColor = White, contentColor = Orange,
                                    checkedContainerColor = Orange, checkedContentColor = White)) {
                                Icon(painterResource(icons[index]), contentDescription = label, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }
        }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            when {
                sources -> SourcesScreen { sources = false }
                state.loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("음식과 기록을 불러오는 중이에요", modifier = Modifier.padding(16.dp))
                }
                state.loadFailed -> Column(Modifier.padding(20.dp).align(Alignment.Center)) {
                    Text("데이터를 불러오지 못했어요. 기존 기록은 그대로 보관되어 있어요.")
                    Button(onClick = vm::load) { Text("다시 불러오기") }
                }
                detail != null -> DetailScreen(detail, vm.photos, state.busy,
                    onBack = { detailId = null }, onDelete = { vm.delete(detail.id) })
                tab == 0 -> EntryScreen(state, vm)
                tab == 1 -> DiaryScreen(state.meals, vm.photos, onAdd = { tab = 0 }, onSelect = { detailId = it })
                else -> AnalysisScreen(state.meals, today)
            }
        }
    }
}
