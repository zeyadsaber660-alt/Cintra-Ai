package com.cintra.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Liquid glass is provided entirely by the libraries — no hand-drawn effects:
 *  - **Kyant0/AndroidLiquidGlass** (`drawBackdrop` + `vibrancy`/`blur`/`lens`
 *    effects and the built-in `Highlight`/`Shadow` specular rim & depth), and
 *  - **chrisbanes/haze** (`hazeEffect`/`hazeSource`) applied separately to the
 *    drawer, artifacts panel and Cintra Live overlay.
 *
 * This modifier is a thin preset that only forwards to those library effects and
 * a single translucent surface tint (the same pattern the library docs use). It
 * must be applied ONLY to floating chrome — never to the reading surface, tables
 * or code blocks.
 */
fun Modifier.cintraLiquidGlass(
    backdrop: Backdrop,
    palette: CintraPalette,
    shape: Shape,
    blurRadius: Dp = 14.dp,
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
                    refractionHeight = 10f.dp.toPx(),
                    refractionAmount = 22f.dp.toPx(),
                    chromaticAberration = true,
                )
            }
        },
        highlight = { highlight },
        shadow = { shadow },
        onDrawSurface = { drawRect(tint) },
    )