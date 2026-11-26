package com.cintra.app.ui.chat.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.model.AttachmentType
import com.cintra.app.ui.theme.CintraThemePalette
import com.cintra.app.ui.theme.cintraLiquidGlass
import com.kyant.backdrop.Backdrop

private enum class PrimaryMode { LIVE, SEND, STOP }

/**
 * Clean floating liquid glass composer: `+` popover, auto-expanding text field,
 * mic dictation and a morphing primary action (Live / Send / Stop).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingGlassComposer(
    backdrop: Backdrop,
    text: String,
    onTextChange: (String) -> Unit,
    activeTool: ActiveTool?,
    attachments: List<Attachment>,
    plusOpen: Boolean,
    onTogglePlus: () -> Unit,
    onPlusAction: (PlusAction) -> Unit,
    onClearTool: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    isRecordingMic: Boolean,
    onMic: () -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CintraThemePalette
    val hasContent = text.isNotBlank() || attachments.isNotEmpty()
    val mode = when {
        isGenerating -> PrimaryMode.STOP
        hasContent -> PrimaryMode.SEND
        else -> PrimaryMode.LIVE
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            PlusToolsPopover(
                backdrop = backdrop,
                visible = plusOpen,
                activeTool = activeTool,
                onAction = onPlusAction,
            )

            val composerShape = RoundedCornerShape(30.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .cintraLiquidGlass(backdrop, palette, composerShape, blurRadius = 18.dp)
                    .clip(composerShape)
                    .padding(10.dp, 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActiveStrip(
                    activeTool = activeTool,
                    attachments = attachments,
                    onClearTool = onClearTool,
                    onRemoveAttachment = onRemoveAttachment,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val plusRotation by animateFloatAsState(
                        targetValue = if (plusOpen) 45f else 0f,
                        animationSpec = tween(220),
                        label = "plusRotation",
                    )
                    CircleButton(
                        backdrop = backdrop,
                        onClick = onTogglePlus,
                        containerColor = if (plusOpen) palette.accentSolid else palette.surface2.copy(alpha = 0.5f),
                        contentColor = if (plusOpen) palette.accentSolidText else palette.textPrimary,
                        modifier = Modifier.rotate(plusRotation),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "أدوات ومرفقات", modifier = Modifier.size(20.dp))
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 132.dp),
                        textStyle = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(palette.accent),
                        maxLines = 6,
                        decorationBox = { inner ->
                            if (text.isEmpty()) {
                                Text(
                                    "اسأل Cintra أي شيء...",
                                    color = palette.textSecondary,
                                    fontSize = 15.sp,
                                )
                            }
                            inner()
                        },
                    )

                    MicButton(
                        backdrop = backdrop,
                        recording = isRecordingMic,
                        onClick = onMic,
                    )

                    PrimaryActionButton(
                        backdrop = backdrop,
                        mode = mode,
                        onClick = when (mode) {
                            PrimaryMode.LIVE -> onLive
                            PrimaryMode.SEND -> onSend
                            PrimaryMode.STOP -> onStop
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveStrip(
    activeTool: ActiveTool?,
    attachments: List<Attachment>,
    onClearTool: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
) {
    val palette = CintraThemePalette
    if (activeTool == null && attachments.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (activeTool != null) {
            val (icon, label) = when (activeTool) {
                ActiveTool.THINKING -> Icons.Filled.Psychology to "تفكير عميق"
                ActiveTool.WEB -> Icons.Filled.Public to "بحث ويب"
                ActiveTool.IMAGE -> Icons.Filled.AutoAwesome to "إنشاء صور"
            }
            ActivePill(
                icon = icon,
                label = label,
                accent = palette.accent,
                soft = palette.accentSoft,
                onRemove = onClearTool,
            )
        }
        attachments.forEachIndexed { index, att ->
            ActivePill(
                icon = if (att.type == AttachmentType.PHOTO) Icons.Filled.Image else Icons.Filled.Description,
                label = att.name,
                accent = palette.textPrimary,
                soft = palette.surface2,
                onRemove = { onRemoveAttachment(index) },
            )
        }
    }
}

@Composable
private fun ActivePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    soft: Color,
    onRemove: () -> Unit,
) {
    val palette = CintraThemePalette
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(soft)
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            Icons.Filled.Close,
            contentDescription = "إزالة",
            tint = accent,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(1.dp),
        )
    }
}

@Composable
private fun CircleButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(containerColor, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun MicButton(
    backdrop: Backdrop,
    recording: Boolean,
    onClick: () -> Unit,
) {
    val palette = CintraThemePalette
    val transition = rememberInfiniteTransition(label = "mic")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (recording) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "micScale",
    )
    val container = if (recording) palette.roseSoft else palette.surface2.copy(alpha = 0.5f)
    val content = if (recording) palette.rose else palette.textPrimary
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(container, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(if (recording) 40.dp * scale else 40.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Mic, contentDescription = "الميكروفون", tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PrimaryActionButton(
    backdrop: Backdrop,
    mode: PrimaryMode,
    onClick: () -> Unit,
) {
    val palette = CintraThemePalette
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(palette.accentSolid, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = mode, animationSpec = tween(180), label = "primary") { m ->
            when (m) {
                PrimaryMode.LIVE -> Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = "تحدث مباشر",
                    tint = palette.accentSolidText,
                    modifier = Modifier.size(20.dp),
                )
                PrimaryMode.SEND -> Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = "إرسال",
                    tint = palette.accentSolidText,
                    modifier = Modifier.size(20.dp),
                )
                PrimaryMode.STOP -> Icon(
                    Icons.Filled.Stop,
                    contentDescription = "إيقاف",
                    tint = palette.accentSolidText,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}