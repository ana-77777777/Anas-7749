package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HighDensityDarkPrimary,
    secondary = HighDensityDarkSecondary,
    tertiary = HighDensityDarkTertiary,
    background = HighDensityDarkBackground,
    surface = HighDensityDarkSurface,
    onPrimary = HighDensityDarkOnPrimary,
    onSecondary = HighDensityDarkOnSecondary,
    onTertiary = HighDensityDarkOnTertiary,
    onBackground = HighDensityDarkOnBackground,
    onSurface = HighDensityDarkOnSurface,
    outline = HighDensityDarkOutline,
    error = HighDensityDarkError
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    secondary = HighDensitySecondary,
    tertiary = HighDensityTertiary,
    background = HighDensityBackground,
    surface = HighDensitySurface,
    onPrimary = HighDensityOnPrimary,
    onSecondary = HighDensityOnSecondary,
    onTertiary = HighDensityOnTertiary,
    onBackground = HighDensityOnBackground,
    onSurface = HighDensityOnSurface,
    outline = HighDensityOutline,
    error = HighDensityError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors by default to enforce our premium brand theme
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
