package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.R
import com.example.dgumobilesoftwarefinalproject.data.PhotoRepository

@Composable
fun PhotoUploadArea(name: String?, photos: PhotoRepository, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().heightIn(min = 220.dp).clip(RoundedCornerShape(20.dp)).background(White)
        .drawBehind {
            val line = 1.5.dp.toPx()
            drawRoundRect(color = Divider, topLeft = Offset(line / 2, line / 2), size = Size(size.width - line, size.height - line),
                cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
        }.clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag("photo-upload-area").padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        if (name != null) MealPhoto(name, photos, Modifier.fillMaxWidth().height(150.dp))
        Icon(painterResource(R.drawable.ic_camera_add), contentDescription = "사진 추가", tint = Primary, modifier = Modifier.size(44.dp))
        Text(if (name == null) "사진 선택" else "사진 변경", style = MaterialTheme.typography.titleMedium)
        Text("눌러서 식사 사진을 업로드해 주세요", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
