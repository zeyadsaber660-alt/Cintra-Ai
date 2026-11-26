package com.cintra.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Calibrated Frosted Liquid Glass modifier powered by Kyant0/AndroidLiquidGlass.
 *
 * Uses strong frosted blur (`26.dp`+) combined with subtle optical edge refraction
 * and a calibrated frosted tint so underlying chat text diffuses smoothly without
 * visual clutter or overlapping characters.
 */
fun Modifier.cintraLiquidGlass(
    backdrop: Backdrop,
    palette: CintraPalette,
    shape: Shape,
    blurRadius: Dp = 26.dp,
    tint: Color = palette.glassTint,
    refraction: Boolean = true,
    highlight: Highlight = Highlight.Default,
    shadow: Shadow? = Shadow.Default,
): Modifier = this
    .clip(shape)
    .drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (refraction) {
                lens(
                    refractionHeight = 6f.dp.toPx(),
                    refractionAmount = 14f.dp.toPx(),
                    chromaticAberration = false,
                )
            }
        },
        highlight = { highlight },
        shadow = { shadow },
        onDrawSurface = { drawRect(tint) },
    )
    .border(
        width = 0.85.dp,
        brush = Brush.verticalGradient(
            colors = listOf(palette.rimTop, palette.glassBorder),
        ),
        shape = shape,
    )

/**
 * Unified Frosted Glass Pill Capsule that cleanly groups 2+ adjacent action icons
 * inside a single bordered pill frame (matching the user's reference capsule design).
 */
@Composable
fun GroupedGlassCapsule(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    horizontalPadding: Dp = 6.dp,
    spacing: Dp = 2.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = CintraThemePalette
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .height(height)
            .cintraLiquidGlass(
                backdrop = backdrop,
                palette = palette,
                shape = shape,
                blurRadius = 26.dp,
                tint = palette.glassTint,
            )
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}