package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SourcesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text("← 돌아가기") } }
        item { Text("음식 데이터 출처", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            SectionCard(title = "식품의약품안전처 · K-FIND") {
                Text("식품영양성분 데이터베이스 음식 DB")
                Text("2025.12.29 파일 · 원본 19,495건", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { uriHandler.openUri("https://various.foodsafetykorea.go.kr/nutrient/general/down/historyList.do") }) { Text("K-FIND 원본 출처 열기") }
            }
        }
        item {
            SectionCard(title = "공공데이터포털") {
                Text("전국통합식품영양성분정보(음식)표준데이터")
                Text("데이터 ID 15100070 · 원본 19,738건\n선택 자료 기준일 2026.09.29", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { uriHandler.openUri("https://www.data.go.kr/data/15100070/standard.do") }) { Text("공공데이터포털 출처 열기") }
            }
        }
        item {
            SectionCard(title = "이 앱의 데이터 이용 기준") {
                Text("공통 필수 항목과 9종 영양성분에 결측치가 없는 음식 2,784개를 사용해요.")
                Text("같은 식품코드는 최신 기준일의 완전한 행을 선택했어요. 이번 자료에서는 JSON의 값이 선택되고 Excel 출처 참조도 보존되어 있어요.")
                Text("100g 기준 음식 2,493개 · 100mL 기준 음료 291개")
                Text("g과 mL는 서로 환산하지 않아요. 실제 섭취량에 비례해 계산하며, 조리법과 제품에 따라 실제 영양성분은 달라질 수 있어요.")
            }
        }
    }
}
