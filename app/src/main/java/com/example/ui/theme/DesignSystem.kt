package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Responsive Window Size Classification
enum class WindowSizeClass {
    COMPACT, // Small / normal phones (< 600dp width)
    MEDIUM,  // Large phones / small tablets (600dp - 840dp width)
    EXPANDED // Large tablets / desktop screens (> 840dp width)
}

object AppSpacing {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val normal: Dp = 16.dp
    val large: Dp = 20.dp
    val extraLarge: Dp = 24.dp
    val huge: Dp = 32.dp
}

object AppShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val normal = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(20.dp)
    val extraLarge = RoundedCornerShape(24.dp)
}

object AppDimensions {
    val minTouchTarget: Dp = 48.dp
    val buttonHeight: Dp = 48.dp
    val cardElevation: Dp = 2.dp
    val iconSizeSmall: Dp = 18.dp
    val iconSizeNormal: Dp = 24.dp
    val iconSizeLarge: Dp = 32.dp
}

object AppColors {
    val SuccessGreen = Color(0xFF10B981)
    val SuccessGreenLight = Color(0xFFD1FAE5)
    val DangerRed = Color(0xFFEF4444)
    val DangerRedLight = Color(0xFFFEE2E2)
    val WarningAmber = Color(0xFFF59E0B)
    val WarningAmberLight = Color(0xFFFEF3C7)
    val InfoBlue = Color(0xFF3B82F6)
    val InfoBlueLight = Color(0xFFDBEAFE)

    // Dark variants
    val DarkSuccessGreen = Color(0xFF34D399)
    val DarkDangerRed = Color(0xFFF87171)
    val DarkWarningAmber = Color(0xFFFBBF24)
    val DarkInfoBlue = Color(0xFF60A5FA)
}

val LocalWindowSizeClass = staticCompositionLocalOf { WindowSizeClass.COMPACT }

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 600 -> WindowSizeClass.COMPACT
        screenWidth < 840 -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

@Composable
fun ProvideWindowSizeClass(
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}
