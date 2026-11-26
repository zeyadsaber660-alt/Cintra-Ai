package com.cintra.app.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay

/**
 * Reusable option model for [HyperOsGlassSelectionMenu].
 */
data class HyperOsMenuOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
)

/**
 * HyperOS 4 "Liquid Glass" Reusable Selection Modal / Pop-up Menu.
 *
 * Implements all 4 exact specifications:
 * 1. Frosted Liquid Glass Surface: 26.dp rounded container + backdrop blur + 1px luminous
 *    top-left specular border gradient fading out + diffused ambient shadow.
 * 2. Contextual Origin & Spring Physics: expands from [transformOrigin] scaling 0.85 -> 1.0
 *    with spring(stiffness = 380f, dampingRatio = 0.78f) for brisk open with micro-bounce.
 * 3. Sliding Selection Indicator (Selection Morphing): smooth gliding highlight pill that
 *    morphs/slides between rows using spring physics + light haptic feedback on change.
 * 4. Staggered Content & Micro-interactions: ~30ms staggered fade-in + 4.dp upward slide
 *    for title and each row, plus scale(0.97f) press state on tap.
 */
@Composable
fun <T> HyperOsGlassSelectionMenu(
    backdrop: Backdrop,
    hazeState: HazeState,
    visible: Boolean,
    title: String,
    subtitle: String? = null,
    options: List<HyperOsMenuOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.TopCenter,
    transformOrigin: TransformOrigin = TransformOrigin(0.5f, 0f),
    topPadding: Dp = 74.dp,
    itemHeight: Dp = 62.dp,
) {
    val palette = CintraThemePalette
    val haptics = LocalHapticFeedback.current

    // Spring curve matching stiffness: 380, damping ~28, mass ~0.8 (dampingRatio ≈ 0.78f)
    val openSpring = remember {
        spring<Float>(dampingRatio = 0.78f, stiffness = 380f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Frosted blurred scrim so background text is cleanly diffused
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(state = hazeState, style = HazeMaterials.thick())
                    .background(palette.scrim)
                    .clickable(onClick = onDismiss),
            )
        }

        // Contextual expanding Liquid Glass Menu Container
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(alignment)
                .padding(top = topPadding, start = 16.dp, end = 16.dp),
            enter = fadeIn(tween(160)) + scaleIn(
                initialScale = 0.85f,
                transformOrigin = transformOrigin,
                animationSpec = openSpring,
            ),
            exit = fadeOut(tween(140)) + scaleOut(
                targetScale = 0.85f,
                transformOrigin = transformOrigin,
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 460f),
            ),
        ) {
            val containerShape = RoundedCornerShape(26.dp)
            // 1px luminous top-left border gradient fading out (refractive glass edge)
            val luminousBorder = Brush.linearGradient(
                colors = if (palette.isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.03f),
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFF0F172A).copy(alpha = 0.10f),
                    )
                },
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .cintraLiquidGlass(
                        backdrop = backdrop,
                        palette = palette,
                        shape = containerShape,
                        blurRadius = 34.dp,
                        tint = palette.glassTintModal,
                    )
                    .border(width = 1.dp, brush = luminousBorder, shape = containerShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Staggered Header (index 0 -> 0ms delay)
                StaggeredReveal(visible = visible, delayMs = 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (!subtitle.isNullOrBlank()) {
                                Text(subtitle, color = palette.textSecondary, fontSize = 12.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(palette.surface2.copy(alpha = 0.85f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(CintraIcons.Close, contentDescription = "إغلاق", tint = palette.textPrimary, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                // Option List with Sliding Selection Indicator (Selection Morphing)
                val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
                val rowSpacing = 6.dp
                val targetOffsetY = (itemHeight + rowSpacing) * selectedIndex
                val indicatorOffsetY by animateDpAsState(
                    targetValue = targetOffsetY,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                    label = "slidingPillOffset",
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    // Gliding Selection Pill Background
                    Box(
                        modifier = Modifier
                            .offset(y = indicatorOffsetY)
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.accentSoft)
                            .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
                    )

                    // Staggered Option Rows (~30ms each)
                    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
                        options.forEachIndexed { index, option ->
                            val isSelected = option.value == selectedValue
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val pressScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.97f else 1f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                                label = "pressScale",
                            )

                            StaggeredReveal(visible = visible, delayMs = (index + 1) * 30) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(itemHeight)
                                        .graphicsLayer {
                                            scaleX = pressScale
                                            scaleY = pressScale
                                        }
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                        ) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelect(option.value)
                                        }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = option.title,
                                                color = palette.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            )
                                            if (!option.badge.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 6.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(palette.surface2)
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                ) {
                                                    Text(option.badge, color = palette.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                        if (!option.subtitle.isNullOrBlank()) {
                                            Text(
                                                text = option.subtitle,
                                                color = palette.textSecondary,
                                                fontSize = 11.5.sp,
                                                maxLines = 1,
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = CintraIcons.Check,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredReveal(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            if (delayMs > 0) delay(delayMs.toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
            )
        } else {
            progress.snapTo(0f)
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 4.dp.toPx()
        },
    ) {
        content()
    }
}
