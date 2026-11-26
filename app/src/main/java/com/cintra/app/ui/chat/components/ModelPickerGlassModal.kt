package com.cintra.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.model.CintraModel
import com.cintra.app.ui.theme.CintraThemePalette
import com.cintra.app.ui.theme.cintraLiquidGlass
import com.kyant.backdrop.Backdrop

/**
 * Translucent liquid glass modal listing the 4 Cintra models. Centered near the
 * top, dismisses on scrim tap.
 */
@Composable
fun ModelPickerGlassModal(
    backdrop: Backdrop,
    visible: Boolean,
    models: List<CintraModel>,
    selected: CintraModel,
    onSelect: (CintraModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = CintraThemePalette
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable(onClick = onDismiss),
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = spring()),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f, animationSpec = tween(160)),
        ) {
            val shape = RoundedCornerShape(30.dp)
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 0.dp)
                    .padding(top = 76.dp)
                    .cintraLiquidGlass(
                        backdrop = backdrop,
                        palette = palette,
                        shape = shape,
                        blurRadius = 22.dp,
                        tint = palette.glassTintModal,
                    )
                    .clip(shape)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("نماذج Cintra", color = palette.textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "اختر النموذج الأنسب لمهمتك",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onDismiss)
                            .padding(4.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    models.forEach { model ->
                        val isSelected = model.id == selected.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) palette.accentSoft else Color.Transparent,
                                    RoundedCornerShape(20.dp),
                                )
                                .then(
                                    if (isSelected) Modifier.border(1.dp, palette.accent, RoundedCornerShape(20.dp))
                                    else Modifier,
                                )
                                .clickable { onSelect(model) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(model.name, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    ModelBadge(model.badge, palette)
                                }
                                Text(
                                    model.description,
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelBadge(badge: String, palette: com.cintra.app.ui.theme.CintraPalette) {
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(badge, color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Mock Google Photos picker — a translucent glass sheet with gradient tiles. */
@Composable
fun GooglePhotosSheet(
    backdrop: Backdrop,
    visible: Boolean,
    onPick: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = CintraThemePalette
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
        ) {
            Box(Modifier.fillMaxSize().background(palette.scrim).clickable(onClick = onDismiss))
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = spring()),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f, animationSpec = tween(160)),
        ) {
            val shape = RoundedCornerShape(30.dp)
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .cintraLiquidGlass(backdrop, palette, shape, blurRadius = 22.dp, tint = palette.glassTintModal)
                    .clip(shape)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("صور جوجل (Google Photos)", color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("اختر صورة لإرفاقها في المحادثة", color = palette.textSecondary, fontSize = 11.5.sp)
                    }
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onDismiss).padding(4.dp),
                    )
                }
                val tiles = listOf(
                    PhotoTile("تصميم واجهة", "UI_Design_Sketch.png", listOf(Color(0xFF0EA5E9), Color(0xFF1E293B))),
                    PhotoTile("مخطط معماري", "Architecture_Board.jpg", listOf(Color(0xFF10B981), Color(0xFF0F172A))),
                    PhotoTile("لقطة كود Kotlin", "Code_Screenshot.png", listOf(Color(0xFF334155), Color(0xFF090D16))),
                    PhotoTile("صورة منتج", "Product_Concept.jpg", listOf(Color(0xFFD97706), Color(0xFF1E293B))),
                )
                tiles.chunked(2).forEach { rowTiles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowTiles.forEach { tile ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(tile.bg))
                                    .clickable { onPick(tile.fileName) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                Text(
                                    "${tile.label} (${tile.fileName})",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        if (rowTiles.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class PhotoTile(val label: String, val fileName: String, val bg: List<Color>)