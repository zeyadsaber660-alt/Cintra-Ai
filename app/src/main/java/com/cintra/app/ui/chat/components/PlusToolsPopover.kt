package com.cintra.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraMotion
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
    val accent: (CintraPalette) -> Color,
    val soft: (CintraPalette) -> Color,
)

private val PlusItems = listOf(
    PlusItem(PlusAction.THINKING, "تفكير عميق", "تحليل منطقي خطوة بخطوة", CintraIcons.Brain, { it.accent }, { it.accentSoft }),
    PlusItem(PlusAction.WEB, "بحث ويب مباشر", "جلب أحدث المعلومات والروابط", CintraIcons.Globe, { it.emerald }, { it.emeraldSoft }),
    PlusItem(PlusAction.IMAGE, "إنشاء صور", "توليد تصميمات ومفاهيم بصرية", CintraIcons.Sparkle, { it.amber }, { it.amberSoft }),
    PlusItem(PlusAction.PHOTOS, "معرض الصور / صور جوجل", "فتح منتقي صور الجهاز الأصلي", CintraIcons.Images, { it.rose }, { it.roseSoft }),
    PlusItem(PlusAction.FILES, "استيراد ملفات ومستندات", "PDF، أكواد، أو مستندات من الجهاز", CintraIcons.Folder, { it.textPrimary }, { it.surface2 }),
)

@Composable
fun PlusToolsPopover(
    backdrop: Backdrop,
    visible: Boolean,
    activeTool: ActiveTool?,
    onAction: (PlusAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = CintraMotion.iosPopEnter(TransformOrigin(0.9f, 1f)),
        exit = CintraMotion.iosPopExit(TransformOrigin(0.9f, 1f)),
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .width(296.dp)
                .cintraLiquidGlass(
                    backdrop = backdrop,
                    palette = palette,
                    shape = shape,
                    blurRadius = 34.dp,
                    tint = palette.glassTintModal,
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlusItems.forEachIndexed { index, item ->
                if (index == 3) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .height(0.8.dp)
                            .background(palette.borderSubtle),
                    )
                }
                val selected = when (item.action) {
                    PlusAction.THINKING -> activeTool == ActiveTool.THINKING
                    PlusAction.WEB -> activeTool == ActiveTool.WEB
                    PlusAction.IMAGE -> activeTool == ActiveTool.IMAGE
                    else -> false
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) item.soft(palette) else Color.Transparent)
                        .clickable { onAction(item.action) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(item.soft(palette)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.accent(palette), modifier = Modifier.size(18.dp))
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