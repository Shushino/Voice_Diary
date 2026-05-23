package com.george.voicediary.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.george.voicediary.data.FontSize
import com.george.voicediary.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun VoiceDiaryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontSize: FontSize = FontSize.MEDIUM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.let { scheme ->
        if (themeMode == ThemeMode.AMOLED) {
            scheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF121212) // Slightly lighter for contrast
            )
        } else {
            scheme
        }
    }

    val scale = when (fontSize) {
        FontSize.SMALL -> 0.85f
        FontSize.MEDIUM -> 1f
        FontSize.LARGE -> 1.2f
    }

    val scaledTypography = Typography(
        displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * scale),
        displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * scale),
        displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * scale),
        headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * scale),
        headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * scale),
        headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * scale),
        titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * scale),
        titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * scale),
        titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * scale),
        bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * scale),
        bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * scale),
        bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * scale),
        labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * scale),
        labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * scale),
        labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * scale)
    )

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
        typography = scaledTypography,
        content = content
    )
}
