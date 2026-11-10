package com.example.dgumobilesoftwarefinalproject.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.data.PhotoRepository
import com.example.dgumobilesoftwarefinalproject.domain.NutritionCalculator
import com.example.dgumobilesoftwarefinalproject.model.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

fun Double.display(): String = String.format(Locale.KOREA, "%,.1f", this)

@Composable
fun SectionCard(modifier: Modifier = Modifier, title: String? = null,
                content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, PaleOrange),
        colors = CardDefaults.cardColors(containerColor = White, contentColor = Orange)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun NutritionPanel(nutrients: Nutrients, title: String = "섭취 영양소", balance: Boolean = false) {
    SectionCard(title = title) {
        Text("${nutrients.energyKcal.display()} kcal", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("탄수화물" to nutrients.carbohydrateG, "단백질" to nutrients.proteinG, "지방" to nutrients.fatG).forEach { (label, value) ->
                Column { Text(label, style = MaterialTheme.typography.labelMedium)
                    Text("${value.display()} g", fontWeight = FontWeight.Bold) }
            }
        }
        if (balance) {
            val shares = NutritionCalculator.macroShares(nutrients)
            listOf("탄수화물", "단백질", "지방").forEachIndexed { i, label ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$label 에너지 비율 ${(shares[i] * 100).display()}%", style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(progress = { shares[i].toFloat() }, modifier = Modifier.fillMaxWidth(),
                        color = Orange, trackColor = PaleOrange)
                }
            }
            Text("비율은 탄수화물·단백질 4kcal/g, 지방 9kcal/g 기준입니다. 총 열량은 식품 DB 값을 사용합니다.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MealPhoto(name: String?, photos: PhotoRepository, modifier: Modifier = Modifier) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, name) {
        value = if (name == null) null else withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(photos.file(name).path)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(PaleOrange), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, contentDescription = "식사 사진", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(if (name == null) "식사 사진" else "사진을 불러올 수 없어요", color = Orange)
    }
}
