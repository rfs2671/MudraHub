package com.mudrahub.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White

private val Dark = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFF42A5F5),
    background = Color(0xFF0F1020),
    surface = Color(0xFF0F1020),
    onPrimary = White, onSecondary = White, onBackground = Color(0xFFEAE7FF), onSurface = Color(0xFFEAE7FF)
)

@Composable fun gradientBrush() = Brush.horizontalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF42A5F5)))

@Composable fun MudraHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}
