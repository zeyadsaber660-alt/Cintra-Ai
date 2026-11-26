package com.cintra.app.ui.chat.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.model.AttachmentType
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraThemePalette
import com.kyant.backdrop.Backdrop

private enum class PrimaryMode { LIVE, SEND, STOP }

/**
 * Zero-lag Frosted Composer with soft bottom fade gradient (matching ChatGPT Mobile):
 * avoids expensive per-frame backdrop lens shaders during IME keyboard animations
 * while keeping crisp frosted opacity and zero text bleed-through.
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
    isArabic: Boolean,
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
            .background(palette.canvas.copy(alpha = 0.94f))
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlusToolsPopover(
                backdrop = backdrop,
                visible = plusOpen,
                activeTool = activeTool,
                onAction = onPlusAction,
            )

            val composerShape = RoundedCornerShape(28.dp)
            val frostedFill = if (palette.isDark) Color(0xF0171A22) else Color(0xFAFFFFFF)
            val topRim = Brush.verticalGradient(
                colors = listOf(palette.rimTop, palette.borderSubtle),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(composerShape)
                    .background(frostedFill)
                    .border(0.9.dp, topRim, composerShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val plusRotation by animateFloatAsState(
                        targetValue = if (plusOpen) 45f else 0f,
                        animationSpec = tween(200),
                        label = "plusRotation",
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (plusOpen) palette.accentSolid else palette.surface2)
                            .clickable(onClick = onTogglePlus),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = CintraIcons.Plus,
                            contentDescription = "أدوات ومرفقات",
                            tint = if (plusOpen) palette.accentSolidText else palette.textPrimary,
                            modifier = Modifier
                                .size(19.dp)
                                .rotate(plusRotation),
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp, max = 124.dp)
                            .padding(vertical = 6.dp),
                        textStyle = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(palette.accent),
                        maxLines = 5,
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (text.isEmpty()) {
                                    Text(
                                        text = if (isArabic) "اسأل Cintra أي شيء..." else "Ask Cintra anything...",
                                        color = palette.textMuted,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                inner()
                            }
                        },
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(palette.surface2)
                            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MicButton(recording = isRecordingMic, onClick = onMic)
                        PrimaryActionButton(
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
}

@OptIn(ExperimentalLayoutApi::class)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (activeTool != null) {
            val (icon, label) = when (activeTool) {
                ActiveTool.THINKING -> CintraIcons.Brain to "تفكير عميق"
                ActiveTool.WEB -> CintraIcons.Globe to "بحث ويب"
                ActiveTool.IMAGE -> CintraIcons.Sparkle to "إنشاء صور"
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.accentSoft)
                    .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
                Text(label, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    CintraIcons.Close,
                    contentDescription = "إزالة",
                    tint = palette.accent,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClearTool),
                )
            }
        }

        attachments.forEachIndexed { index, att ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface2)
                    .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                    .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (att.type == AttachmentType.PHOTO && !att.uri.isNullOrBlank()) {
                    AsyncImage(
                        model = att.uri,
                        contentDescription = att.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    if (att.isLoading) {
                        CircularProgressIndicator(
                            color = palette.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(15.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (att.type == AttachmentType.PHOTO) CintraIcons.Images else CintraIcons.Folder,
                            contentDescription = null,
                            tint = if (att.name.endsWith(".pdf", true)) Color(0xFFEF4444) else palette.accent,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                Text(
                    text = if (att.isLoading) "جارٍ قراءة ${att.name}..." else att.name,
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 165.dp),
                )
                Icon(
                    imageVector = CintraIcons.Close,
                    contentDescription = "حذف المرفق",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .clickable { onRemoveAttachment(index) },
                )
            }
        }
    }
}

@Composable
private fun MicButton(recording: Boolean, onClick: () -> Unit) {
    val palette = CintraThemePalette
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (recording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "micPulse",
    )
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (recording) palette.roseSoft else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CintraIcons.Mic,
            contentDescription = "إملاء صوتي",
            tint = if (recording) palette.rose else palette.textPrimary,
            modifier = Modifier.size(if (recording) (18.dp * pulse) else 18.dp),
        )
    }
}

@Composable
private fun PrimaryActionButton(mode: PrimaryMode, onClick: () -> Unit) {
    val palette = CintraThemePalette
    val stopPulse = rememberInfiniteTransition(label = "stopRing")
    val ringAlpha by stopPulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "stopAlpha",
    )
    val bgColor = when (mode) {
        PrimaryMode.LIVE -> Color(0xFF00B894)
        PrimaryMode.STOP -> Color(0xFFEF4444)
        PrimaryMode.SEND -> palette.accentSolid
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (mode == PrimaryMode.STOP) {
                    Modifier.border(2.dp, Color(0xFFEF4444).copy(alpha = ringAlpha), CircleShape)
                } else Modifier
            )
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = mode, animationSpec = tween(150), label = "primaryMode") { m ->
            when (m) {
                PrimaryMode.LIVE -> Icon(
                    imageVector = CintraIcons.Waveform,
                    contentDescription = "تحدث مباشر",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
                PrimaryMode.SEND -> Icon(
                    imageVector = CintraIcons.ArrowUp,
                    contentDescription = "إرسال",
                    tint = palette.accentSolidText,
                    modifier = Modifier.size(18.dp),
                )
                PrimaryMode.STOP -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color.White),
                )
            }
        }
    }
}