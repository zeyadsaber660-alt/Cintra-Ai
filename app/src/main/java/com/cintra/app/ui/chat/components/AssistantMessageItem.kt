package com.cintra.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.model.AttachmentType
import com.cintra.app.ui.model.ChatMessage
import com.cintra.app.ui.theme.CintraThemePalette
import com.mikepenz.markdown.m3.Markdown

private val CodeBg = Color(0xFF0D0F14)
private val CodeHeaderBg = Color(0xFF171A22)
private val CodeText = Color(0xFFE2E8F0)
private val CodeMuted = Color(0xFF9CA3AF)
private val TokKeyword = Color(0xFF7DD3FC)
private val TokString = Color(0xFF86EFAC)
private val TokNumber = Color(0xFFFCD34D)
private val TokComment = Color(0xFF64748B)
private val TokFn = Color(0xFFF9A8D4)

/**
 * Renders a single chat message. User messages become a bubble; assistant
 * messages render the collapsible Thinking card, always-on Markdown (with
 * syntax-highlighted code blocks + Copy / Open-in-Artifacts), embedded
 * Generative UI and message actions.
 */
@Composable
fun MessageItem(
    message: ChatMessage,
    onToggleReasoning: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
    onRegenerate: () -> Unit,
    onTab: (String) -> Unit,
    onToggleCheck: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (message.isUser) {
        UserBubble(message, modifier)
    } else {
        AssistantRow(
            message = message,
            onToggleReasoning = onToggleReasoning,
            onCopy = onCopy,
            onOpenArtifacts = onOpenArtifacts,
            onRegenerate = onRegenerate,
            onTab = onTab,
            onToggleCheck = onToggleCheck,
            modifier = modifier,
        )
    }
}

@Composable
private fun UserBubble(message: ChatMessage, modifier: Modifier) {
    val palette = CintraThemePalette
    val bubbleShape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .background(palette.userBubble)
                .border(1.dp, palette.borderSubtle, bubbleShape)
                .padding(horizontal = 17.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            message.attachments.forEach { att ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (att.type == AttachmentType.PHOTO) Icons.Filled.Image else Icons.Filled.Description,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(att.name, color = palette.textSecondary, fontSize = 12.sp)
                }
            }
            if (message.content.isNotEmpty()) {
                Text(message.content, color = palette.textPrimary, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun AssistantRow(
    message: ChatMessage,
    onToggleReasoning: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
    onRegenerate: () -> Unit,
    onTab: (String) -> Unit,
    onToggleCheck: (Int) -> Unit,
    modifier: Modifier,
) {
    val palette = CintraThemePalette
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(13.dp))
            }
            Text(message.modelName ?: "Cintra", color = palette.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        if (message.reasoning != null) {
            ThinkingCard(
                reasoning = message.reasoning,
                expanded = message.reasoningExpanded,
                streaming = message.isStreaming && message.content.isEmpty(),
                onToggle = onToggleReasoning,
            )
        }

        MarkdownBody(
            content = message.content,
            streaming = message.isStreaming,
            onCopy = onCopy,
            onOpenArtifacts = onOpenArtifacts,
        )

        if (!message.isStreaming && message.genUi != null) {
            GenUiView(genUi = message.genUi, onTab = onTab, onToggleCheck = onToggleCheck)
        }

        if (!message.isStreaming) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MessageAction(Icons.Filled.ContentCopy, "نسخ") { onCopy(message.content) }
                MessageAction(Icons.Filled.Refresh, "إعادة", onRegenerate)
            }
        }
    }
}

@Composable
private fun ThinkingCard(
    reasoning: String,
    expanded: Boolean,
    streaming: Boolean,
    onToggle: () -> Unit,
) {
    val palette = CintraThemePalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Psychology, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
            Text(
                if (streaming) "جارٍ التفكير..." else "عملية التفكير",
                color = palette.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(palette.accent),
                )
                Text(
                    reasoning,
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val palette = CintraThemePalette
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = palette.textMuted, modifier = Modifier.size(14.dp))
        Text(label, color = palette.textMuted, fontSize = 12.sp)
    }
}

// ---- Markdown (always on) -------------------------------------------------

private sealed interface MdSegment {
    data class TextSegment(val md: String) : MdSegment
    data class CodeSegment(val lang: String, val code: String) : MdSegment
}

private fun splitMarkdown(md: String): List<MdSegment> {
    val segments = mutableListOf<MdSegment>()
    val lines = md.split("\n")
    var i = 0
    val textBuf = StringBuilder()
    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim().ifEmpty { "code" }
            if (textBuf.isNotEmpty()) {
                segments.add(MdSegment.TextSegment(textBuf.toString()))
                textBuf.clear()
            }
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.append(lines[i]).append("\n")
                i++
            }
            segments.add(MdSegment.CodeSegment(lang, code.toString().trimEnd()))
        } else {
            textBuf.append(line).append("\n")
        }
        i++
    }
    if (textBuf.isNotEmpty()) segments.add(MdSegment.TextSegment(textBuf.toString()))
    return segments
}

@Composable
private fun MarkdownBody(
    content: String,
    streaming: Boolean,
    onCopy: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
) {
    val segments = remember(content) { splitMarkdown(content) }
    Column(modifier = Modifier.fillMaxWidth()) {
        segments.forEach { segment ->
            when (segment) {
                is MdSegment.TextSegment ->
                    if (segment.md.isNotBlank()) {
                        Markdown(content = segment.md, modifier = Modifier.fillMaxWidth())
                    }
                is MdSegment.CodeSegment ->
                    CodeBlockCard(segment.lang, segment.code, onCopy, onOpenArtifacts)
            }
        }
        if (streaming) StreamingCursor()
    }
}

@Composable
private fun StreamingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(width = 7.dp, height = 15.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CintraThemePalette.textPrimary.copy(alpha = alpha)),
    )
}

@Composable
private fun CodeBlockCard(
    lang: String,
    code: String,
    onCopy: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CodeBg)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodeHeaderBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                lang,
                color = CodeMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            CodeAction("معاينة في اللوحة") { onOpenArtifacts(lang, code) }
            Box(Modifier.width(6.dp))
            CodeAction("نسخ") { onCopy(code) }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val highlighted = remember(code) { highlightKotlin(code) }
                Text(
                    text = highlighted,
                    color = CodeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.8.sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun CodeAction(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = CodeText, modifier = Modifier.size(12.dp))
        Text(label, color = CodeText, fontSize = 11.5.sp)
    }
}

private val KotlinTokenPattern = Regex(
    "(\"[^\"]*\"|//[^\\n]*|@[A-Za-z_]+|\\b(fun|val|var|class|data|return|if|else|when|import|package|object|suspend|override|private|public|internal|enum|interface|true|false|null)\\b|\\b\\d+(?:\\.\\d+)?\\b)",
)

private fun highlightKotlin(code: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in KotlinTokenPattern.findAll(code)) {
        append(code, last, match.range.first)
        val token = match.value
        val style = when {
            token.startsWith("//") -> SpanStyle(color = TokComment, fontStyle = FontStyle.Italic)
            token.startsWith("\"") -> SpanStyle(color = TokString)
            token.startsWith("@") -> SpanStyle(color = TokFn)
            token.first().isDigit() -> SpanStyle(color = TokNumber)
            else -> SpanStyle(color = TokKeyword, fontWeight = FontWeight.Medium)
        }
        withStyle(style) { append(token) }
        last = match.range.last + 1
    }
    append(code, last, code.length)
}