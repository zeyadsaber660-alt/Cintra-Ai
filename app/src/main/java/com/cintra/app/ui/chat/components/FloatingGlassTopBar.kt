package com.cintra.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.theme.CintraThemePalette
import com.cintra.app.ui.theme.cintraLiquidGlass
import com.kyant.backdrop.Backdrop

/**
 * Floating 3-zone liquid glass top bar: menu (start), centered model pill,
 * theme toggle + new chat (end). The center pill is strictly centered.
 */
@Composable
fun FloatingGlassTopBar(
    backdrop: Backdrop,
    modelName: String,
    darkTheme: Boolean,
    onMenu: () -> Unit,
    onModel: () -> Unit,
    onTheme: () -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
    ) {
        // Start: drawer toggle
        GlassCircleButton(
            backdrop = backdrop,
            onClick = onMenu,
            modifier = Modifier.align(Alignment.CenterStart),
            icon = { Icon(Icons.Filled.Menu, contentDescription = "القائمة", tint = palette.textPrimary) },
        )

        // Center: model pill (strictly centered)
        ModelPill(
            backdrop = backdrop,
            modelName = modelName,
            onClick = onModel,
            modifier = Modifier.align(Alignment.Center),
        )

        // End: theme + new chat
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassPillButton(
                backdrop = backdrop,
                onClick = onTheme,
                content = {
                    Icon(
                        if (darkTheme) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                        contentDescription = "المظهر",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (darkTheme) "نهاري" else "ليلي",
                        color = palette.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
            GlassCircleButton(
                backdrop = backdrop,
                onClick = onNewChat,
                icon = { Icon(Icons.Filled.Add, contentDescription = "محادثة جديدة", tint = palette.textPrimary) },
            )
        }
    }
}

@Composable
private fun ModelPill(
    backdrop: Backdrop,
    modelName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .cintraLiquidGlass(backdrop = backdrop, palette = palette, shape = shape, blurRadius = 16.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
        Text(
            text = modelName,
            color = palette.textPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun GlassCircleButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(44.dp)
            .cintraLiquidGlass(backdrop = backdrop, palette = palette, shape = shape, blurRadius = 16.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun GlassPillButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .cintraLiquidGlass(backdrop = backdrop, palette = palette, shape = shape, blurRadius = 16.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}