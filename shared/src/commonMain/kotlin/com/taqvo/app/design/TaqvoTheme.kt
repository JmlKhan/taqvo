package com.taqvo.app.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalTaqvoColors = staticCompositionLocalOf { LightColors }

/**
 * Whether the theme in force is the dark one. Every colour a screen needs is already in
 * [LocalTaqvoColors]; this exists for the one thing that is *not* a theme role — a reciter's
 * monogram hue (`com.taqvo.app.recitation.ReciterHue`), which is per-reciter data from the
 * manifest and carries its own light and dark pair. Comparing the palette instance against
 * `DarkColors` would work and would break the day a third palette exists.
 */
val LocalTaqvoDark = staticCompositionLocalOf { false }

@Composable
fun TaqvoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    SystemBarsAppearance(mode, dark)
    CompositionLocalProvider(LocalTaqvoColors provides colors, LocalTaqvoDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    background = colors.background,
                    surface = colors.surface,
                    onBackground = colors.textPrimary,
                    onSurface = colors.textPrimary,
                    primary = colors.accent,
                )
            } else {
                lightColorScheme(
                    background = colors.background,
                    surface = colors.surface,
                    onBackground = colors.textPrimary,
                    onSurface = colors.textPrimary,
                    primary = colors.accent,
                )
            },
            typography = TaqvoTypography(),
            content = content,
        )
    }
}
