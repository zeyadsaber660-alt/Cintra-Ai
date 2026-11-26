package com.cintra.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalCintraPalette = staticCompositionLocalOf { ObsidianPalette }

val CintraThemePalette: CintraPalette
    @Composable get() = LocalCintraPalette.current

/**
 * Shared iOS-inspired Pop Spring motion presets for all Cintra modals, sheets and popovers.
 */
object CintraMotion {
    fun iosPopEnter(origin: TransformOrigin = TransformOrigin.Center): EnterTransition =
        fadeIn(tween(180)) + scaleIn(
            initialScale = 0.88f,
            transformOrigin = origin,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 420f,
            ),
        )

    fun iosPopExit(origin: TransformOrigin = TransformOrigin.Center): ExitTransition =
        fadeOut(tween(140)) + scaleOut(
            targetScale = 0.90f,
            transformOrigin = origin,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 500f,
            ),
        )
}

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
 * Cintra root theme with instant Obsidian/Alabaster switching and automatic
 * Arabic RTL / English LTR layout direction.
 */
@Composable
fun CintraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) ObsidianPalette else AlabasterPalette
    val colorScheme = if (darkTheme) CintraDarkColors else CintraLightColors
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalCintraPalette provides palette,
        LocalLayoutDirection provides direction,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}