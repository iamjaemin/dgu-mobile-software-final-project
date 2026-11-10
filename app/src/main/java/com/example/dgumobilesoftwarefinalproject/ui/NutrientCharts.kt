package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.domain.NutritionCalculator
import com.example.dgumobilesoftwarefinalproject.model.Nutrients

@Composable
fun MacroDonut(nutrients: Nutrients) {
    val shares = NutritionCalculator.macroShares(nutrients)
    val labels = listOf("탄수화물", "단백질", "지방")
    val colors = listOf(Orange, BrightOrange, PaleOrange)
    val description = labels.indices.joinToString(", ") { "${labels[it]} ${(shares[it] * 100).display()}%" }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(170.dp).semantics { contentDescription = "탄단지 에너지 비율 그래프: $description" }) {
                val width = 26.dp.toPx()
                val margin = width / 2
                val arcSize = Size(size.width - width, size.height - width)
                drawArc(PaleOrange, 0f, 360f, false, Offset(margin, margin), arcSize, style = Stroke(width))
                var angle = -90f
                shares.forEachIndexed { i, share ->
                    val sweep = (share * 360).toFloat()
                    if (sweep > 0) drawArc(colors[i], angle, sweep, false, Offset(margin, margin), arcSize, style = Stroke(width))
                    angle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (shares.sum() == 0.0) "0%" else "탄 · 단 · 지", fontWeight = FontWeight.Bold)
                Text("에너지 비율", style = MaterialTheme.typography.labelSmall)
            }
        }
        labels.indices.forEach { i ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).background(colors[i], CircleShape))
                Text(labels[i], modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                Text("${(shares[i] * 100).display()}%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MacroBars(nutrients: Nutrients, compact: Boolean = false) {
    val values = listOf(nutrients.carbohydrateG, nutrients.proteinG, nutrients.fatG)
    val names = listOf("탄수화물", "단백질", "지방")
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)) {
        values.forEachIndexed { i, value ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${names[i]} ${value.display()} g", style = MaterialTheme.typography.labelMedium)
                Canvas(Modifier.fillMaxWidth().height(if (compact) 8.dp else 14.dp).semantics {
                    contentDescription = "${names[i]} 섭취량 그래프 ${value.display()}g"
                }) {
                    val stroke = size.height
                    val start = stroke / 2
                    val end = size.width - start
                    drawLine(PaleOrange, Offset(start, stroke / 2), Offset(end, stroke / 2), stroke, StrokeCap.Round)
                    if (value > 0) drawLine(Orange, Offset(start, stroke / 2), Offset(start + (end - start) * (value / max).toFloat(), stroke / 2), stroke, StrokeCap.Round)
                }
            }
        }
    }
}
