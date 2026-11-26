package com.cintra.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.theme.CintraThemePalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Slide-in liquid glass Artifacts panel (Haze-blurred) showing full code with a
 * copy action. Slides in from the end edge with a physics spring feel.
 */
@Composable
fun ArtifactsGlassSheet(
    hazeState: HazeState,
    visible: Boolean,
    title: String,
    code: String,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val palette = CintraThemePalette
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
        ) {
            Box(Modifier.fillMaxSize().background(palette.scrim).clickable(onClick = onClose))
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(animationSpec = tween(240)) { it } + fadeOut(tween(160)),
        ) {
            val shape = RoundedCornerShape(26.dp)
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.92f)
                    .padding(12.dp)
                    .clip(shape)
                    .hazeEffect(state = hazeState, style = HazeMaterials.thin())
                    .border(1.dp, palette.borderSubtle, shape),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("معاينة الكود المباشر", color = palette.textSecondary, fontSize = 11.5.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surface2)
                                .clickable { onCopy(code) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(13.dp))
                            Text("نسخ", color = palette.textPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onClose)
                                .padding(5.dp),
                        )
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D0F14))
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp),
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        SelectionContainer {
                            Text(
                                text = code,
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.8.sp,
                                lineHeight = 21.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}