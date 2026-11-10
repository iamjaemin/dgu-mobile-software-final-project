package com.example.dgumobilesoftwarefinalproject.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Orange = Color(0xFFAE4100)
val BrightOrange = Color(0xFFF87820)
val PaleOrange = Color(0xFFFFE5D1)
val White = Color.White

@Composable
fun MealTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Orange, onPrimary = White, primaryContainer = PaleOrange, onPrimaryContainer = Orange,
        secondary = Orange, onSecondary = White, secondaryContainer = PaleOrange, onSecondaryContainer = Orange,
        tertiary = Orange, onTertiary = White, tertiaryContainer = PaleOrange, onTertiaryContainer = Orange,
        background = White, onBackground = Orange, surface = White, onSurface = Orange,
        surfaceVariant = PaleOrange, onSurfaceVariant = Orange, surfaceTint = White,
        inverseSurface = Orange, inverseOnSurface = White, inversePrimary = White,
        error = Orange, onError = White, errorContainer = PaleOrange, onErrorContainer = Orange,
        outline = Orange, outlineVariant = PaleOrange, scrim = Orange,
        surfaceBright = White, surfaceDim = White, surfaceContainer = White,
        surfaceContainerHigh = White, surfaceContainerHighest = White,
        surfaceContainerLow = White, surfaceContainerLowest = White,
    ), content = content)
}
