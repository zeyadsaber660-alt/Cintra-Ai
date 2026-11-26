package com.cintra.app.data.mock

import com.cintra.app.data.AiStreamService
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import com.cintra.app.ui.model.ChecklistItem
import com.cintra.app.ui.model.CintraModelId
import com.cintra.app.ui.model.GenUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Phase 1 realistic Arabic/Kotlin streaming simulator. Mirrors the reference
 * `buildReply()` logic: image generation, interactive checklist plans, and a
 * default Markdown + table + Kotlin reply. Emits character batches so the UI
 * shows a live streaming cursor.
 */
class MockAiStreamService : AiStreamService {

    override fun streamReply(request: AiStreamRequest): Flow<AiStreamChunk> = flow {
        val reply = buildReply(request)
        val showReasoning = request.tool == ActiveTool.THINKING ||
            request.model.id == CintraModelId.THINK

        if (showReasoning && reply.reasoning.isNotEmpty()) {
            val chars = reply.reasoning.toList()
            var i = 0
            while (i < chars.size) {
                val batch = chars.subList(i, minOf(i + 5, chars.size)).joinToString("")
                emit(AiStreamChunk.ReasoningDelta(batch))
                i += 5
                delay(16)
            }
            emit(AiStreamChunk.ReasoningComplete)
            delay(120)
        }

        val contentChars = reply.content.toList()
        var c = 0
        while (c < contentChars.size) {
            val batch = contentChars.subList(c, minOf(c + 7, contentChars.size)).joinToString("")
            emit(AiStreamChunk.ContentDelta(batch))
            c += 7
            delay(14)
        }

        emit(AiStreamChunk.Complete(reply.genUi))
    }

    private data class Reply(val reasoning: String, val content: String, val genUi: GenUi?)

    private fun buildReply(request: AiStreamRequest): Reply {
        val prompt = request.prompt
        val lower = prompt.lowercase()
        val model = request.model.name

        return when {
            request.tool == ActiveTool.IMAGE || lower.contains("صور") || lower.contains("تصور") -> Reply(
                reasoning = "تفعيل محرك الصور في Cintra Vision وتوليد مفهوم بصري نظيف بدون نيون.",
                content = "تم توليد التصور البصري المطلوب عبر **$model**:",
                genUi = GenUi.GeneratedImage(prompt = prompt),
            )

            lower.contains("خطة") || lower.contains("مهام") -> Reply(
                reasoning = "تقسيم بناء تطبيق Cintra على مراحل واضحة وتضمين قائمة مهام تفاعلية تلقائياً.",
                content = "### خارطة طريق بناء Cintra (Android Kotlin Compose)\n\n" +
                    "> يمكنك الضغط على كل مهمة بالأسفل لتتبع التقدم الفعلي:",
                genUi = GenUi.Checklist(
                    items = listOf(
                        ChecklistItem("تهيئة مشروع Gradle + Jetpack Compose Material 3", done = true),
                        ChecklistItem("دمج AndroidLiquidGlass وHaze للشرائط العائمة والنماذج", done = false),
                        ChecklistItem("تفعيل قائمة + (بحث، تفكير، صور جوجل، ملفات) والميكروفون", done = false),
                        ChecklistItem("ربط Ktor SSE Streaming وواجهة Cintra Live الصوتية", done = false),
                    ),
                ),
            )

            else -> Reply(
                reasoning = (if (request.tool == ActiveTool.WEB) "بحث ويب مباشر وتجميع أحدث المصادر...\n" else "") +
                    "تحليل الطلب عبر $model وتنسيق الرد بـ Markdown الكامل.",
                content = "### رد $model" + (if (request.tool == ActiveTool.WEB) " (مدعوم ببحث الويب)" else "") + "\n\n" +
                    "| الطبقة | المكتبة المقترحة في Android | الدور |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| الزجاج السائل | `AndroidLiquidGlass` | انكسار بصري حقيقي |\n" +
                    "| التضبيب السريع | `Haze` | تضبيب القوائم والخلفيات |\n" +
                    "| الشبكة | `Ktor Client SSE` | بث مباشر للردود |\n\n" +
                    "```kotlin\n" +
                    "fun streamCintra(client: HttpClient): Flow<String> = flow {\n" +
                    "    client.sse(\"https://api.cintra.ai/v1/chat/completions\") {\n" +
                    "        incoming.collect { event ->\n" +
                    "            event.data?.takeIf { it != \"[DONE]\" }?.let { emit(it) }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n" +
                    "```",
                genUi = null,
            )
        }
    }
}