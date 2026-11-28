package com.cintra.app.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Global Frosted Liquid Glass Toast controller — replaces default grey Android Toasts
 * with a sleek, theme-matched glass pill across the entire app.
 */
object CintraToastManager {
    var currentMessage by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    var triggerNonce by mutableStateOf(0)
        private set

    fun show(message: String, error: Boolean = false) {
        currentMessage = message
        isError = error
        triggerNonce++
    }

    fun dismiss() {
        currentMessage = null
    }
}

@Composable
fun CintraGlassToastHost(modifier: Modifier = Modifier) {
    val palette = CintraThemePalette
    val msg = CintraToastManager.currentMessage
    val isErr = CintraToastManager.isError
    val nonce = CintraToastManager.triggerNonce

    LaunchedEffect(nonce, msg) {
        if (msg != null) {
            delay(2800L)
            CintraToastManager.dismiss()
        }
    }

    AnimatedVisibility(
        visible = msg != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier,
    ) {
        val accent = if (isErr) Color(0xFFEF4444) else palette.accent
        val pillShape = RoundedCornerShape(999.dp)
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .shadow(16.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(pillShape)
                .background(if (palette.isDark) Color(0xEC161B26) else Color(0xF2FFFFFF))
                .border(1.dp, accent.copy(alpha = 0.45f), pillShape)
                .clickable { CintraToastManager.dismiss() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                text = msg.orEmpty(),
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Unified Frosted Liquid Glass Modal Dialog for confirmations, previews, and alerts.
 */
@Composable
fun CintraGlassDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String = "إغلاق",
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = CintraThemePalette
    val shape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(28.dp, shape, spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(shape)
                .background(if (palette.isDark) Color(0xF2141923) else Color(0xF7FFFFFF))
                .border(1.dp, palette.borderSubtle, shape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = CintraIcons.Sparkle,
                        contentDescription = null,
                        tint = if (destructive) Color(0xFFEF4444) else palette.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 22.sp,
                )
            }

            if (content != null) {
                content()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface)
                        .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(dismissLabel, color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
                if (confirmLabel != null && onConfirm != null) {
                    val btnColor = if (destructive) Color(0xFFEF4444) else palette.accent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(btnColor)
                            .clickable { onConfirm() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(confirmLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
