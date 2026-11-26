package com.cintra.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCintraPalette = staticCompositionLocalOf { ObsidianPalette }

val CintraThemePalette: CintraPalette
    @Composable get() = LocalCintraPalette.current

private val CintraDarkColors = darkColorScheme(
    primary = ObsidianPalette.accent,
    onPrimary = ObsidianPalette.accentSolidText,
    secondary = ObsidianPalette.accent,
    background = ObsidianPalette.canvas,
    onBackground = ObsidianPalette.textPrimary,
    surface = ObsidianPalette.surface,
    onSurface = ObsidianPalette.textPrimary,
    surfaceVariant = ObsidianPalette.surface2,
    onSurfaceVariant = ObsidianPalette.textSecondary,
    outline = ObsidianPalette.borderSubtle,
    error = ObsidianPalette.rose,
)

private val CintraLightColors = lightColorScheme(
    primary = AlabasterPalette.accent,
    onPrimary = AlabasterPalette.accentSolidText,
    secondary = AlabasterPalette.accent,
    background = AlabasterPalette.canvas,
    onBackground = AlabasterPalette.textPrimary,
    surface = AlabasterPalette.surface,
    onSurface = AlabasterPalette.textPrimary,
    surfaceVariant = AlabasterPalette.surface2,
    onSurfaceVariant = AlabasterPalette.textSecondary,
    outline = AlabasterPalette.borderSubtle,
    error = AlabasterPalette.rose,
)

/**
 * Cintra root theme. [darkTheme] toggles Obsidian / Alabaster instantly across
 * every surface, text token and glass rim.
 */
@Composable
fun CintraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) ObsidianPalette else AlabasterPalette
    val colorScheme = if (darkTheme) CintraDarkColors else CintraLightColors

    CompositionLocalProvider(LocalCintraPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}