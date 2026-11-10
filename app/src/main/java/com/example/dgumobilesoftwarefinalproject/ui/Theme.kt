package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dgumobilesoftwarefinalproject.R

val Primary = Color(0xFFFF6F0F)
val AppBackground = Color(0xFFFAFAFA)
val FieldBackground = Color(0xFFF8F8F8)
val White = Color.White
val Ink = Color(0xFF242424)
val Muted = Color(0xFF777777)
val Divider = Color(0xFFEAEAEA)

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

private val typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Pretendard), displayMedium = base.displayMedium.copy(fontFamily = Pretendard),
        displaySmall = base.displaySmall.copy(fontFamily = Pretendard), headlineLarge = base.headlineLarge.copy(fontFamily = Pretendard),
        headlineMedium = base.headlineMedium.copy(fontFamily = Pretendard), headlineSmall = base.headlineSmall.copy(fontFamily = Pretendard),
        titleLarge = base.titleLarge.copy(fontFamily = Pretendard), titleMedium = base.titleMedium.copy(fontFamily = Pretendard),
        titleSmall = base.titleSmall.copy(fontFamily = Pretendard), bodyLarge = base.bodyLarge.copy(fontFamily = Pretendard),
        bodyMedium = base.bodyMedium.copy(fontFamily = Pretendard), bodySmall = base.bodySmall.copy(fontFamily = Pretendard),
        labelLarge = base.labelLarge.copy(fontFamily = Pretendard), labelMedium = base.labelMedium.copy(fontFamily = Pretendard),
        labelSmall = base.labelSmall.copy(fontFamily = Pretendard),
    )
}

@Composable
fun MealTheme(content: @Composable () -> Unit) {
    MaterialTheme(typography = typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(20.dp), small = RoundedCornerShape(24.dp),
            medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)),
        colorScheme = lightColorScheme(
            primary = Primary, onPrimary = White, primaryContainer = FieldBackground, onPrimaryContainer = Ink,
            secondary = Primary, onSecondary = White, secondaryContainer = FieldBackground, onSecondaryContainer = Ink,
            tertiary = Primary, onTertiary = White, tertiaryContainer = FieldBackground, onTertiaryContainer = Ink,
            background = AppBackground, onBackground = Ink, surface = White, onSurface = Ink,
            surfaceVariant = FieldBackground, onSurfaceVariant = Muted, surfaceTint = White,
            inverseSurface = Ink, inverseOnSurface = White, inversePrimary = Primary,
            error = Primary, onError = White, errorContainer = FieldBackground, onErrorContainer = Ink,
            outline = Divider, outlineVariant = Divider, scrim = Color.Black,
            surfaceBright = White, surfaceDim = AppBackground, surfaceContainer = White,
            surfaceContainerHigh = White, surfaceContainerHighest = White,
            surfaceContainerLow = White, surfaceContainerLowest = White,
        ), content = content)
}
