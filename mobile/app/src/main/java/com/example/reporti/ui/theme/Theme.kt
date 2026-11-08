
package com.example.reporti.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ================================
// DARK COLOR SCHEME
// ================================

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccentRed,
    onPrimary = DarkBackground,

    secondary = DarkNav,
    onSecondary = DarkText,

    tertiary = DarkMuted,
    onTertiary = DarkBackground,

    background = DarkBackground,
    onBackground = DarkText,

    surface = DarkSurface,
    onSurface = DarkText,

    surfaceVariant = DarkInteractive,
    onSurfaceVariant = DarkMuted,

    outline = DarkBorder,
    error = DarkAccentRed,
    onError = DarkBackground
)

// ================================
// LIGHT COLOR SCHEME
// ================================

private val LightColorScheme = lightColorScheme(
    primary = AccentRed,
    onPrimary = White,

    secondary = BeigeNav,
    onSecondary = MainText,

    tertiary = MutedText,
    onTertiary = White,

    background = Cream,
    onBackground = MainText,

    surface = SurfaceLight,
    onSurface = MainText,

    surfaceVariant = ButtonPill,
    onSurfaceVariant = MutedText,

    outline = BorderLight,
    error = AccentRed,
    onError = White
)

// ================================
// REPORTI THEME
// ================================

@Composable
fun ReportiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
