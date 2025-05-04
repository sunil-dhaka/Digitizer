package com.example.digitizer.ui.theme

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
    primary = BluePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BluePrimaryVariant,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = TealSecondary,
    onSecondary = Color(0xFF000000),
    secondaryContainer = TealSecondaryVariant,
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = AccentPurple,
    onTertiary = Color(0xFFFFFFFF),
    background = BackgroundDark,
    onBackground = Color(0xFFFFFFFF),
    surface = SurfaceDark,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFFEEEEEE),
    error = AccentRed,
    onError = Color(0xFFFFFFFF),
    outline = DividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BluePrimaryLight,
    onPrimaryContainer = Color(0xFF00174C),
    secondary = TealSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = TealSecondaryLight,
    onSecondaryContainer = Color(0xFF002018),
    tertiary = AccentPurple,
    onTertiary = Color(0xFFFFFFFF),
    background = BackgroundLight,
    onBackground = Color(0xFF121212),
    surface = SurfaceLight,
    onSurface = Color(0xFF121212),
    surfaceVariant = CardLight,
    onSurfaceVariant = Color(0xFF424242),
    error = AccentRed,
    onError = Color(0xFFFFFFFF),
    outline = DividerLight
)

@Composable
fun DigitizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to use our custom theme
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
    
    // Set status bar and navigation bar colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}