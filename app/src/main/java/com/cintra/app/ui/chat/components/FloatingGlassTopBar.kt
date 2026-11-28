package com.cintra.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraThemePalette
import com.cintra.app.ui.theme.GroupedGlassCapsule
import com.cintra.app.ui.theme.cintraLiquidGlass
import com.kyant.backdrop.Backdrop

/**
 * Floating 3-zone Frosted Liquid Glass top bar (v26):
 * - Theme toggle moved strictly to Settings.
 * - End capsule shows [New Chat | Temporary Chat] on Home, or [New Chat | Chat Options ⋮] inside an active chat.
 */
@Composable
fun FloatingGlassTopBar(
    backdrop: Backdrop,
    modelName: String,
    hasMessages: Boolean,
    isTemporaryChat: Boolean,
    onMenu: () -> Unit,
    onModel: () -> Unit,
    onNewChat: () -> Unit,
    onToggleTemporary: () -> Unit,
    onChatOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.canvas,
                        palette.canvas.copy(alpha = 0.88f),
                        Color.Transparent,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // Start: Modern 2-line menu icon
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .cintraLiquidGlass(backdrop = backdrop, palette = palette, shape = CircleShape)
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CintraIcons.MenuTwoLines,
                contentDescription = "القائمة",
                tint = palette.textPrimary,
                modifier = Modifier.size(21.dp),
            )
        }

        // Center: Cintra Model Selector Pill (or Temporary indicator)
        val pillShape = RoundedCornerShape(999.dp)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .height(44.dp)
                .cintraLiquidGlass(backdrop = backdrop, palette = palette, shape = pillShape)
                .clickable(onClick = onModel)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isTemporaryChat) Color(0xFFF59E0B) else palette.accent),
            )
            Text(
                text = if (isTemporaryChat) "$modelName • مؤقتة" else modelName,
                color = palette.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = CintraIcons.ChevronDown,
                contentDescription = "تغيير النموذج",
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }

        // End: Unified Grouped Capsule [New Chat | Temp Chat or Chat Options ⋮]
        GroupedGlassCapsule(
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalPadding = 5.dp,
            spacing = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNewChat),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CintraIcons.NewChatSquare,
                    contentDescription = "محادثة جديدة",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(0.9.dp)
                    .height(17.dp)
                    .background(palette.borderSubtle),
            )

            if (hasMessages) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onChatOptions),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "خيارات المحادثة",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isTemporaryChat) palette.accent.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            width = if (isTemporaryChat) 1.dp else 0.dp,
                            color = if (isTemporaryChat) palette.accent.copy(alpha = 0.45f) else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable(onClick = onToggleTemporary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = "محادثة مؤقتة",
                        tint = if (isTemporaryChat) palette.accent else palette.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}