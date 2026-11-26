package com.cintra.app.ui.chat.components

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraThemePalette
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

private val CodeBg = Color(0xFF0D0F14)
private val CodeHeaderBg = Color(0xFF171A22)
private val CodeText = Color(0xFFE2E8F0)
private val CodeMuted = Color(0xFF9CA3AF)
private val TokKeyword = Color(0xFF7DD3FC)
private val TokString = Color(0xFF86EFAC)
private val TokNumber = Color(0xFFFCD34D)
private val TokComment = Color(0xFF64748B)
private val TokFn = Color(0xFFF9A8D4)

@Composable
fun MessageItem(
    message: ChatMessage,
    onToggleReasoning: () -> Unit,
    onCopy: (String) -> Unit,
    onExportPdf: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
    onRegenerate: () -> Unit,
    onTab: (String) -> Unit,
    onToggleCheck: (Int) -> Unit,
    onEditUser: () -> Unit = {},
    onResendUser: () -> Unit = {},
    fontScale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val enterProgress = remember(message.id) { Animatable(0f) }
    LaunchedEffect(message.id) {
        enterProgress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
    }
    val animatedMod = modifier.graphicsLayer {
        alpha = enterProgress.value
        translationY = (1f - enterProgress.value) * 24f
    }
    if (message.isUser) {
        UserBubble(message, onCopy, onEditUser, onResendUser, animatedMod)
    } else {
        AssistantRow(
            message = message,
            onToggleReasoning = onToggleReasoning,
            onCopy = onCopy,
            onExportPdf = onExportPdf,
            onOpenArtifacts = onOpenArtifacts,
            fontScale = fontScale,
            onRegenerate = onRegenerate,
            onTab = onTab,
            onToggleCheck = onToggleCheck,
            modifier = animatedMod,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserBubble(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier,
) {
    val palette = CintraThemePalette
    val bubbleShape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
    var menuExpanded by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(bubbleShape)
                    .background(palette.userBubble)
                    .border(0.75.dp, palette.borderSubtle, bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuExpanded = true },
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                message.attachments.forEach { att ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surface)
                            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            if (att.type == AttachmentType.PHOTO) CintraIcons.Images else CintraIcons.Folder,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(att.name, color = palette.textSecondary, fontSize = 12.sp)
                    }
                }
                if (message.content.isNotEmpty()) {
                    Text(message.content, color = palette.textPrimary, fontSize = 15.sp, lineHeight = 23.sp)
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = palette.surface,
            ) {
                DropdownMenuItem(
                    text = { Text("تعديل وإعادة إرسال", color = palette.textPrimary, fontSize = 13.5.sp) },
                    onClick = { menuExpanded = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("إعادة الإرسال", color = palette.textPrimary, fontSize = 13.5.sp) },
                    onClick = { menuExpanded = false; onResend() },
                )
                DropdownMenuItem(
                    text = { Text("نسخ الرسالة", color = palette.textPrimary, fontSize = 13.5.sp) },
                    onClick = { menuExpanded = false; onCopy(message.content) },
                )
            }
        }
    }
}

@Composable
private fun AssistantRow(
    message: ChatMessage,
    onToggleReasoning: () -> Unit,
    onCopy: (String) -> Unit,
    onExportPdf: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
    onRegenerate: () -> Unit,
    onTab: (String) -> Unit,
    onToggleCheck: (Int) -> Unit,
    fontScale: Float,
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
                    .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CintraIcons.Sparkle, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(14.dp))
            }
            Text(message.modelName ?: "Cintra", color = palette.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        val showThinking = !message.reasoning.isNullOrBlank() &&
            (message.reasoning.length >= 60 || (message.isStreaming && message.content.isEmpty()))
        if (showThinking) {
            ThinkingCard(
                reasoning = message.reasoning.orEmpty(),
                expanded = message.reasoningExpanded,
                streaming = message.isStreaming && message.content.isEmpty(),
                onToggle = onToggleReasoning,
            )
        }

        // Real web photos carousel fetched automatically by Cintra Worker (ChatGPT Mobile style)
        if (message.webPhotos.isNotEmpty()) {
            RealWebPhotosCarousel(photos = message.webPhotos)
        }

        val isBuildingPdf = message.isStreaming && (message.content.contains("```cintra-pdf") || message.content.contains("```cintra-file"))
        val isBuildingGenUi = message.isStreaming && message.content.contains("```genui-")
        val visibleContent = remember(message.content, message.isStreaming) {
            var cleaned = message.content
            for (tag in listOf("```cintra-pdf", "```cintra-file", "```genui-")) {
                if (cleaned.contains(tag)) cleaned = cleaned.substringBefore(tag)
            }
            // Hide an unclosed ```cintra-svg block while it is still streaming
            val lastSvg = cleaned.lastIndexOf("```cintra-svg")
            if (lastSvg >= 0 && cleaned.indexOf("```", lastSvg + 13) < 0) {
                cleaned = cleaned.substring(0, lastSvg)
            }
            cleaned.trim()
        }

        if (message.isStreaming && visibleContent.isEmpty() && !isBuildingPdf) {
            CintraWaitingLoader(statusText = message.liveStatus ?: "جارٍ التحضير...")
        } else if (visibleContent.isNotEmpty()) {
            if (message.isStreaming) {
                val splitIdx = visibleContent.lastIndexOf("\n\n")
                val tail = if (splitIdx > 0) visibleContent.substring(splitIdx + 2) else visibleContent
                val tailHasTable = tail.trimStart().startsWith("|") && tail.contains("---")
                if (tailHasTable) {
                    // Render table row-by-row LIVE as soon as header + divider arrive!
                    MarkdownBody(
                        content = visibleContent,
                        streaming = false,
                        fontScale = fontScale,
                        onCopy = onCopy,
                        onOpenArtifacts = onOpenArtifacts,
                    )
                } else if (splitIdx > 0) {
                    val stablePart = visibleContent.substring(0, splitIdx)
                    MarkdownBody(
                        content = stablePart,
                        streaming = false,
                        fontScale = fontScale,
                        onCopy = onCopy,
                        onOpenArtifacts = onOpenArtifacts,
                    )
                    if (tail.isNotBlank() && !tail.trimStart().startsWith("|")) {
                        StreamingPlainBody(
                            content = tail,
                            fontScale = fontScale,
                            showCursor = !isBuildingPdf && !isBuildingGenUi,
                        )
                    }
                } else if (!tail.trimStart().startsWith("|")) {
                    StreamingPlainBody(
                        content = visibleContent,
                        fontScale = fontScale,
                        showCursor = !isBuildingPdf && !isBuildingGenUi,
                    )
                }
            } else {
                MarkdownBody(
                    content = visibleContent,
                    streaming = false,
                    fontScale = fontScale,
                    onCopy = onCopy,
                    onOpenArtifacts = onOpenArtifacts,
                )
            }
        }

        if (isBuildingPdf) {
            CintraPdfBuildingPill()
        }

        if (!message.isStreaming && message.generatedPdf != null) {
            GeneratedPdfFileCard(pdf = message.generatedPdf)
        }

        if (!message.isStreaming && message.generatedFile != null) {
            GeneratedGenericFileCard(file = message.generatedFile)
        }

        if (!message.isStreaming && message.webSources.isNotEmpty()) {
            WebSourcesRow(sources = message.webSources)
        }

        if (!message.isStreaming && message.genUi != null) {
            GenUiView(genUi = message.genUi, onTab = onTab, onToggleCheck = onToggleCheck)
        }

        if (!message.isStreaming) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MessageAction(Icons.Rounded.ContentCopy, "نسخ") { onCopy(message.content) }
                MessageAction(Icons.Rounded.Refresh, "إعادة", onRegenerate)
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
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(CintraIcons.Brain, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
            Text(
                if (streaming) "جارٍ التفكير..." else "عملية التفكير",
                color = palette.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            ) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(palette.accent))
                val cleanSteps = remember(reasoning, streaming) {
                    val steps = mutableListOf<String>()
                    steps.add("• تحليل طلبك وسياق المحادثة")
                    if (reasoning.contains("PDF", true) || reasoning.contains("html", true) || reasoning.contains("file", true)) {
                        steps.add("• تجهيز بيئة التنسيق وقالب المستند")
                    }
                    if (reasoning.contains("svg", true) || reasoning.contains("diagram", true) || reasoning.contains("venn", true)) {
                        steps.add("• إعداد الرسومات والأشكال التوضيحية")
                    }
                    if (reasoning.contains("search", true) || reasoning.contains("web", true) || reasoning.contains("source", true)) {
                        steps.add("• مراجعة نتائج البحث والمصادر")
                    }
                    steps.add(if (streaming) "• جارٍ صياغة الرد النهائي..." else "• تم إكمال التحليل وتجهيز الإجابة")
                    steps.joinToString("\n")
                }
                Text(
                    cleanSteps,
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

// ---- Smart Markdown Splitter (Text + Custom Full-Wrap Tables + Code) ------

private sealed interface MdSegment {
    data class TextSegment(val md: String) : MdSegment
    data class TableSegment(val headers: List<String>, val rows: List<List<String>>) : MdSegment
    data class CodeSegment(val lang: String, val code: String) : MdSegment
    data class CalloutSegment(val text: String) : MdSegment
    data class MathSegment(val expr: String) : MdSegment
    data class SvgSegment(val svg: String) : MdSegment
}


private fun formatReadableMath(input: String, isolateLtr: Boolean = true): String {
    var s = input
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace("\u0024\u0024", "")
        .replace("\\(", "")
        .replace("\\)", "")
        .replace("\\[", "")
        .replace("\\]", "")

    fun convertSymbols(raw: String): String {
        var r = raw
        r = r.replace(Regex("\\\\frac\\{([^\\}]+)\\}\\{([^\\}]+)\\}"), "($1) / ($2)")
        r = r.replace(Regex("\\\\sqrt\\{([^\\}]+)\\}"), "√($1)")
        r = r.replace(Regex("\\\\overline\\{([^\\}]+)\\}"), "$1′")
        r = r.replace(Regex("\\\\(?:text|mathrm|mathbf)\\{([^\\}]+)\\}"), "$1")
        val replacements = listOf(
            "\\mathbb{N}" to "ℕ", "\\mathbb{Z}" to "ℤ", "\\mathbb{Q}" to "ℚ",
            "\\mathbb{R}" to "ℝ", "\\mathbb{C}" to "ℂ", "\\mathcal{P}" to "𝒫",
            "\\varnothing" to "∅", "\\emptyset" to "∅",
            "\\subseteq" to "⊆", "\\subset" to "⊂", "\\supseteq" to "⊇", "\\supset" to "⊃",
            "\\notin" to "∉", "\\in" to "∈",
            "\\cup" to "∪", "\\cap" to "∩", "\\setminus" to " \\ ",
            "\\times" to "×", "\\triangle" to "△", "\\oplus" to "⊕",
            "\\neq" to "≠", "\\leq" to "≤", "\\geq" to "≥",
            "\\implies" to "⇒", "\\Rightarrow" to "⇒", "\\iff" to "⇔", "\\rightarrow" to "→",
            "\\land" to "∧", "\\lor" to "∨", "\\neg" to "¬",
            "\\forall" to "∀", "\\exists" to "∃", "\\infty" to "∞",
            "\\cdots" to "…", "\\ldots" to "…", "\\dots" to "…",
            "\\mid" to "|", "\\{" to "\u007B", "\\}" to "\u007D",
        )
        for ((k, v) in replacements) r = r.replace(k, v)
        return r
    }

    s = s.replace(Regex("\\u0024([^\\u0024\\n]+?)\\u0024")) { m ->
        val inner = convertSymbols(m.groupValues[1].trim())
        if (isolateLtr) "\u2066$inner\u2069" else inner
    }
    s = convertSymbols(s)
    return s.replace("  ", " ")
}

private fun isTableDivider(line: String): Boolean {
    val t = line.trim()
    return t.startsWith("|") && t.contains("---") && t.all { it == '|' || it == '-' || it == ':' || it == ' ' }
}

private fun splitTableCells(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim().replace("**", "") }

private fun splitMarkdown(md: String): List<MdSegment> {
    val segments = mutableListOf<MdSegment>()
    val lines = md.split("\n")
    var i = 0
    val textBuf = StringBuilder()

    fun flushText() {
        if (textBuf.isNotEmpty()) {
            segments.add(MdSegment.TextSegment(textBuf.toString()))
            textBuf.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Fenced code block
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim().ifEmpty { "code" }
            flushText()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.append(lines[i]).append("\n")
                i++
            }
            val lowerLang = lang.lowercase()
            if (lowerLang == "cintra-svg" || lowerLang == "svg") {
                val svgRaw = code.toString().trim()
                if (svgRaw.contains("<svg", ignoreCase = true)) {
                    segments.add(MdSegment.SvgSegment(svgRaw))
                }
            } else if (!lowerLang.startsWith("genui-") && lowerLang != "cintra-pdf" && lowerLang != "cintra-file") {
                segments.add(MdSegment.CodeSegment(lang, code.toString().trimEnd()))
            }
            i++
            continue
        }

        // 1a. Centered LTR Display Math Block ($$ ... $$)
        if (trimmed.startsWith("\u0024\u0024")) {
            flushText()
            if (trimmed.length > 4 && trimmed.endsWith("\u0024\u0024")) {
                segments.add(MdSegment.MathSegment(trimmed.removePrefix("\u0024\u0024").removeSuffix("\u0024\u0024").trim()))
                i++
                continue
            }
            val mathBuf = StringBuilder()
            val firstLine = trimmed.removePrefix("\u0024\u0024").trim()
            if (firstLine.isNotEmpty()) mathBuf.append(firstLine).append("\n")
            i++
            while (i < lines.size && !lines[i].trim().startsWith("\u0024\u0024")) {
                mathBuf.append(lines[i].trim()).append("\n")
                i++
            }
            segments.add(MdSegment.MathSegment(mathBuf.toString().trim()))
            i++
            continue
        }

        // 1b. Tinted Callout / Highlighted Answer Box (> ...)
        if (trimmed.startsWith(">")) {
            flushText()
            val calloutBuf = StringBuilder()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                calloutBuf.append(lines[i].trim().removePrefix(">").trim()).append("\n")
                i++
            }
            segments.add(MdSegment.CalloutSegment(calloutBuf.toString().trim()))
            continue
        }

        // 2. Markdown Table (extracted for zero-ellipsis full-wrap rendering)
        if (trimmed.startsWith("|") && i + 1 < lines.size && isTableDivider(lines[i + 1])) {
            flushText()
            val headers = splitTableCells(trimmed)
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                rows.add(splitTableCells(lines[i]))
                i++
            }
            segments.add(MdSegment.TableSegment(headers, rows))
            continue
        }

        textBuf.append(line).append("\n")
        i++
    }
    flushText()
    return segments
}

@Composable
private fun MarkdownBody(
    content: String,
    streaming: Boolean,
    fontScale: Float,
    onCopy: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
) {
    val segments = remember(content) { splitMarkdown(content) }
    val cintraMdTypography = markdownTypography(
        h1 = TextStyle(fontSize = (20f * fontScale).sp, lineHeight = (28f * fontScale).sp, fontWeight = FontWeight.Bold),
        h2 = TextStyle(fontSize = (18f * fontScale).sp, lineHeight = (26f * fontScale).sp, fontWeight = FontWeight.Bold),
        h3 = TextStyle(fontSize = (16f * fontScale).sp, lineHeight = (24f * fontScale).sp, fontWeight = FontWeight.SemiBold),
        h4 = TextStyle(fontSize = (15.5f * fontScale).sp, lineHeight = (23f * fontScale).sp, fontWeight = FontWeight.SemiBold),
        text = TextStyle(fontSize = (15f * fontScale).sp, lineHeight = (24f * fontScale).sp, fontWeight = FontWeight.Normal),
        paragraph = TextStyle(fontSize = (15f * fontScale).sp, lineHeight = (24f * fontScale).sp, fontWeight = FontWeight.Normal),
        ordered = TextStyle(fontSize = (15f * fontScale).sp, lineHeight = (24f * fontScale).sp, fontWeight = FontWeight.Normal),
        bullet = TextStyle(fontSize = (15f * fontScale).sp, lineHeight = (24f * fontScale).sp, fontWeight = FontWeight.Normal),
        quote = TextStyle(fontSize = (14f * fontScale).sp, lineHeight = (22f * fontScale).sp, fontStyle = FontStyle.Normal),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MdSegment.TextSegment ->
                    if (segment.md.isNotBlank()) {
                        Markdown(
                            content = formatReadableMath(segment.md).replace(Regex("==(.+?)=="), "`$1`"),
                            typography = cintraMdTypography,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                is MdSegment.TableSegment ->
                    CintraMarkdownTable(segment.headers, segment.rows)
                is MdSegment.CodeSegment ->
                    CodeBlockCard(segment.lang, segment.code, onCopy, onOpenArtifacts)
                is MdSegment.CalloutSegment ->
                    CintraCalloutBox(segment.text, fontScale)
                is MdSegment.MathSegment ->
                    CintraDisplayMathCard(segment.expr, fontScale)
                is MdSegment.SvgSegment ->
                    CintraInlineSvgCard(segment.svg)
            }
        }
        if (streaming) StreamingCursor()
    }
}

/**
 * Custom full-wrap Markdown table — never truncates cells with "...".
 * Uses clean thin dividers and horizontal scroll only when 3+ wide columns exist.
 */
@Composable
private fun CintraDisplayMathCard(expr: String, fontScale: Float) {
    val palette = CintraThemePalette
    val formatted = remember(expr) { formatReadableMath(expr, isolateLtr = false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface.copy(alpha = 0.72f))
                .border(0.7.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatted,
                color = palette.textPrimary,
                fontFamily = FontFamily.Serif,
                fontSize = (17f * fontScale).sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = (26f * fontScale).sp,
            )
        }
    }
}

@Composable
private fun CintraInlineSvgCard(svgMarkup: String) {
    val palette = CintraThemePalette
    val htmlDoc = remember(svgMarkup, palette.isDark) {
        val bgHex = if (palette.isDark) "#141821" else "#F8FAFC"
        """
        <!DOCTYPE html>
        <html><head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/>
        <style>
          html,body{margin:0;padding:8px;width:100%;height:100%;box-sizing:border-box;background:$bgHex;display:flex;align-items:center;justify-content:center;overflow:hidden;font-family:sans-serif;}
          svg{max-width:100%;max-height:100%;width:auto;height:auto;}
        </style></head><body>$svgMarkup</body></html>
        """.trimIndent()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (palette.isDark) Color(0xFF141821) else Color(0xFFF8FAFC))
            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(18.dp)),
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    loadDataWithBaseURL(null, htmlDoc, "text/html", "UTF-8", null)
                }
            },
            update = { wv ->
                wv.loadDataWithBaseURL(null, htmlDoc, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxWidth().height(205.dp),
        )
    }
}

@Composable
private fun CintraCalloutBox(text: String, fontScale: Float) {
    val palette = CintraThemePalette
    val isAnswer = text.contains("الإجابة") || text.contains("الناتج") || text.contains("الخلاصة") || text.contains("✓")
    val isWarn = text.contains("ركز") || text.contains("تنبيه") || text.contains("ملاحظة") || text.contains("قاعدة")
    val bg = when {
        isAnswer -> if (palette.isDark) Color(0x2210B981) else Color(0xFFECFDF5)
        isWarn -> if (palette.isDark) Color(0x22F59E0B) else Color(0xFFFFFBEB)
        else -> if (palette.isDark) Color(0x2238BDF8) else Color(0xFFF0F9FF)
    }
    val bar = when {
        isAnswer -> Color(0xFF10B981)
        isWarn -> Color(0xFFF59E0B)
        else -> palette.accent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(0.8.dp, bar.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .height(IntrinsicSize.Min),
    ) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(bar))
        Text(
            text = formatReadableMath(text).replace("**", "").replace("==", ""),
            color = palette.textPrimary,
            fontSize = (14.5f * fontScale).sp,
            lineHeight = (23f * fontScale).sp,
            fontWeight = if (isAnswer) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

private fun buildTableCellAnnotated(
    raw: String,
    linkColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
): AnnotatedString {
    val clean = formatReadableMath(raw)
    val linkRegex = Regex("\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)")
    if (!linkRegex.containsMatchIn(clean)) return AnnotatedString(clean)
    return buildAnnotatedString {
        var last = 0
        for (m in linkRegex.findAll(clean)) {
            append(clean, last, m.range.first)
            val label = m.groupValues[1]
            val url = m.groupValues[2]
            pushLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                    linkInteractionListener = { uriHandler.openUri(url) },
                ),
            )
            append(label)
            pop()
            last = m.range.last + 1
        }
        append(clean, last, clean.length)
    }
}

@Composable
private fun CintraMarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
) {
    val palette = CintraThemePalette
    val uriHandler = LocalUriHandler.current
    val colCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
    val useScroll = colCount >= 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
            .then(if (useScroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .then(if (!useScroll) Modifier.fillMaxWidth() else Modifier)
                .background(palette.surface2.copy(alpha = 0.7f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (c in 0 until colCount) {
                val cellModifier = when {
                    useScroll -> Modifier.width(if (c == 0) 140.dp else 185.dp)
                    colCount == 2 -> Modifier.weight(if (c == 0) 0.38f else 0.62f)
                    else -> Modifier.weight(1f)
                }
                Text(
                    text = formatReadableMath(headers.getOrElse(c) { "" }),
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp,
                    softWrap = true,
                    modifier = cellModifier,
                )
            }
        }

        rows.forEach { row ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.65.dp)
                    .background(palette.borderSubtle),
            )
            Row(
                modifier = Modifier
                    .then(if (!useScroll) Modifier.fillMaxWidth() else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for (c in 0 until colCount) {
                    val cellModifier = when {
                        useScroll -> Modifier.width(if (c == 0) 140.dp else 185.dp)
                        colCount == 2 -> Modifier.weight(if (c == 0) 0.38f else 0.62f)
                        else -> Modifier.weight(1f)
                    }
                    val cellAnnotated = remember(row.getOrElse(c) { "" }) {
                        buildTableCellAnnotated(row.getOrElse(c) { "" }, palette.accent, uriHandler)
                    }
                    Text(
                        text = cellAnnotated,
                        color = if (c == 0) palette.textPrimary else palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (c == 0) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 20.sp,
                        softWrap = true,
                        modifier = cellModifier,
                    )
                }
            }
        }
    }
}



@Composable
private fun WebSourcesRow(sources: List<com.cintra.app.ui.model.WebSourceItem>) {
    val palette = CintraThemePalette
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "المصادر",
            color = palette.textMuted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sources.forEach { src ->
                val host = remember(src.url) {
                    runCatching { java.net.URI(src.url).host?.removePrefix("www.") ?: src.title }
                        .getOrDefault(src.title)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.surface)
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                        .clickable { runCatching { uriHandler.openUri(src.url) } }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(CintraIcons.Globe, contentDescription = null, tint = palette.accent, modifier = Modifier.size(13.dp))
                    Text(
                        text = host,
                        color = palette.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CintraPdfBuildingPill() {
    val palette = CintraThemePalette
    val transition = rememberInfiniteTransition(label = "pdfBuild")
    val alpha by transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pdfAlpha")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.PictureAsPdf,
            contentDescription = null,
            tint = Color(0xFFEF4444).copy(alpha = alpha),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "جارٍ تصميم وتجهيز ملف الـ PDF للتحميل...",
            color = palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StreamingPlainBody(
    content: String,
    fontScale: Float,
    showCursor: Boolean,
) {
    val palette = CintraThemePalette
    val display = remember(content) {
        content
            .replace(Regex("^#{1,4}\\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = display,
            color = palette.textPrimary,
            fontSize = (15f * fontScale).sp,
            lineHeight = (24.5f * fontScale).sp,
        )
        if (showCursor) StreamingCursor()
    }
}

@Composable
private fun CintraWaitingLoader(statusText: String = "جارٍ التحضير...") {
    val palette = CintraThemePalette
    val transition = rememberInfiniteTransition(label = "waitingWave")
    val p1 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, delayMillis = 0), RepeatMode.Reverse), label = "d1")
    val p2 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, delayMillis = 160), RepeatMode.Reverse), label = "d2")
    val p3 by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, delayMillis = 320), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surface)
            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(p1, p2, p3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = alpha)),
            )
        }
        Text(
            text = statusText,
            color = palette.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun StreamingCursor() {
    val palette = CintraThemePalette
    val transition = rememberInfiniteTransition(label = "typingPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .size(8.dp)
            .clip(CircleShape)
            .background(palette.accent.copy(alpha = alpha)),
    )
}

@Composable
private fun CodeBlockCard(
    lang: String,
    code: String,
    onCopy: (String) -> Unit,
    onOpenArtifacts: (lang: String, code: String) -> Unit,
) {
    val isArabicPlainText = (lang.equals("text", true) || lang.equals("txt", true) || lang.equals("plain", true)) &&
        code.any { it in '\u0600'..'\u06FF' }
    val codeDirection = if (isArabicPlainText) LayoutDirection.Rtl else LayoutDirection.Ltr

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CodeBg)
            .border(0.85.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
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
            CompositionLocalProvider(LocalLayoutDirection provides codeDirection) {
                if (isArabicPlainText) {
                    Text(
                        text = code,
                        color = CodeText,
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp,
                    )
                } else {
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
        Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = CodeText, modifier = Modifier.size(12.dp))
        Text(label, color = CodeText, fontSize = 11.5.sp)
    }
}

private val KotlinTokenPattern = Regex(
    "(\"[^\"]*\"|//[^\\n]*|@[A-Za-z_]+|\\b(fun|val|var|class|data|return|if|else|when|import|package|object|suspend|override|private|public|internal|enum|interface|true|false|null|def|for|in|while|import|from)\\b|\\b\\d+(?:\\.\\d+)?\\b)",
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