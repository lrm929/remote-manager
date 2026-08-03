package com.remotemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = TechBlack,
    primaryContainer = NeonBlue.copy(alpha = 0.22f),
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = TechBlack,
    secondaryContainer = NeonPurple.copy(alpha = 0.18f),
    onSecondaryContainer = NeonPurple,
    tertiary = NeonPink,
    onTertiary = TechBlack,
    tertiaryContainer = NeonPink.copy(alpha = 0.15f),
    onTertiaryContainer = NeonPink,
    background = TechBlack,
    onBackground = TextPrimary,
    surface = TechSurface,
    onSurface = TextPrimary,
    surfaceVariant = TechPanel,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonCyan,
    outline = TechBorder,
    error = TerminalError,
    onError = TechBlack,
    errorContainer = TerminalError.copy(alpha = 0.15f),
    onErrorContainer = TerminalError
)

// Keep a light scheme available for system-driven switches, but force tech-dark by default.
private val LightColorScheme = lightColorScheme(
    primary = NeonBlue,
    onPrimary = Color.White,
    primaryContainer = NeonBlue.copy(alpha = 0.15f),
    onPrimaryContainer = NeonBlue,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonPurple.copy(alpha = 0.12f),
    onSecondaryContainer = NeonPurple,
    tertiary = NeonPink,
    onTertiary = Color.White,
    tertiaryContainer = NeonPink.copy(alpha = 0.10f),
    onTertiaryContainer = NeonPink,
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF1A1C23),
    surface = Color.White,
    onSurface = Color(0xFF1A1C23),
    surfaceVariant = Color(0xFFE8EBF5),
    onSurfaceVariant = Color(0xFF5C6278),
    surfaceTint = NeonBlue,
    outline = Color(0xFFD1D5E6),
    error = TerminalError,
    onError = Color.White,
    errorContainer = TerminalError.copy(alpha = 0.10f),
    onErrorContainer = TerminalError
)

@Composable
fun RemoteManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TechPanel.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
