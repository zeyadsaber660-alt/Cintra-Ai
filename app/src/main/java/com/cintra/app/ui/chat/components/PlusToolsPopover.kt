package com.cintra.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.theme.CintraPalette
import com.cintra.app.ui.theme.CintraThemePalette
import com.cintra.app.ui.theme.cintraLiquidGlass
import com.kyant.backdrop.Backdrop

enum class PlusAction { THINKING, WEB, IMAGE, PHOTOS, FILES }

private data class PlusItem(
    val action: PlusAction,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: (CintraPalette) -> androidx.compose.ui.graphics.Color,
    val soft: (CintraPalette) -> androidx.compose.ui.graphics.Color,
)

private val PlusItems = listOf(
    PlusItem(PlusAction.THINKING, "تفكير عميق", "تحليل منطقي خطوة بخطوة", Icons.Filled.Psychology, { it.accent }, { it.accentSoft }),
    PlusItem(PlusAction.WEB, "بحث ويب مباشر", "جلب أحدث المعلومات والروابط", Icons.Filled.Public, { it.emerald }, { it.emeraldSoft }),
    PlusItem(PlusAction.IMAGE, "إنشاء صور", "توليد تصميمات ومفاهيم بصرية", Icons.Filled.AutoAwesome, { it.amber }, { it.amberSoft }),
    PlusItem(PlusAction.PHOTOS, "صور جوجل (Google Photos)", "استيراد صورة من مكتبتك", Icons.Filled.Image, { it.rose }, { it.roseSoft }),
    PlusItem(PlusAction.FILES, "استيراد ملفات ومستندات", "PDF، أكواد Kotlin، أو مستندات", Icons.Filled.FolderOpen, { it.textPrimary }, { it.surface2 }),
)

/**
 * Floating liquid glass popover revealed by the `+` button. Anchored bottom-end
 * with a springy jelly reveal (transform origin at the bottom corner).
 */
@Composable
fun PlusToolsPopover(
    backdrop: Backdrop,
    visible: Boolean,
    activeTool: com.cintra.app.ui.model.ActiveTool?,
    onAction: (PlusAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, transformOrigin = TransformOrigin(1f, 1f)),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(160)),
    ) {
        val shape = RoundedCornerShape(26.dp)
        Column(
            modifier = Modifier
                .width(300.dp)
                .cintraLiquidGlass(backdrop, palette, shape, blurRadius = 22.dp, tint = palette.glassTintModal)
                .clip(shape)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlusItems.forEachIndexed { index, item ->
                if (index == 3) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .height(1.dp)
                            .background(palette.borderSubtle),
                    )
                }
                val selected = when (item.action) {
                    PlusAction.THINKING -> activeTool == com.cintra.app.ui.model.ActiveTool.THINKING
                    PlusAction.WEB -> activeTool == com.cintra.app.ui.model.ActiveTool.WEB
                    PlusAction.IMAGE -> activeTool == com.cintra.app.ui.model.ActiveTool.IMAGE
                    else -> false
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) item.soft(palette) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onAction(item.action) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(item.soft(palette)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.accent(palette), modifier = Modifier.size(17.dp))
                    }
                    Column {
                        Text(item.title, color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.subtitle, color = palette.textSecondary, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}