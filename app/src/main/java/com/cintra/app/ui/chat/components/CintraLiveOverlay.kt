package com.cintra.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.theme.CintraThemePalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Full-screen Cintra Live voice overlay: Haze-blurred backdrop, a breathing
 * liquid glass orb reacting to voice state, active model label, mute toggle and
 * a red end-call button.
 */
@Composable
fun CintraLiveOverlay(
    hazeState: HazeState,
    visible: Boolean,
    modelName: String,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onEnd: () -> Unit,
    onOpenFlutterGlass: () -> Unit,
) {
    val palette = CintraThemePalette
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(240)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.glassTintModal)
                .hazeEffect(state = hazeState, style = HazeMaterials.thin())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(palette.accent))
                Text(
                    "Cintra Live — $modelName",
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LiveOrb(muted = muted, onClick = onOpenFlutterGlass)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (muted) "الميكروفون مكتوم" else "أستمع إليك الآن...",
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "تحدث بشكل طبيعي بالعربية أو الإنجليزية",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LiveControlButton(
                    background = palette.surface2,
                    onClick = onToggleMute,
                ) {
                    Icon(
                        if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (muted) "إلغاء الكتم" else "كتم الميكروفون",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                LiveControlButton(
                    background = Color(0xFFEF4444),
                    onClick = onEnd,
                ) {
                    Icon(
                        Icons.Filled.CallEnd,
                        contentDescription = "إنهاء التحدث المباشر",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveOrb(muted: Boolean, onClick: () -> Unit) {
    val palette = CintraThemePalette
    val transition = rememberInfiniteTransition(label = "orb")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "breathe",
    )
    val scale = if (muted) 0.92f else breathe
    Box(
        modifier = Modifier
            .size(170.dp)
            .scale(scale)
            .alpha(if (muted) 0.45f else 1f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f),
                        palette.accent.copy(alpha = 0.32f),
                        palette.canvas.copy(alpha = 0.15f),
                    ),
                ),
            )
            .border(1.5.dp, palette.rimTop, CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun LiveControlButton(
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}