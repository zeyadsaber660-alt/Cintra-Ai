package com.cintra.app.data.mock

import com.cintra.app.data.ChatRepository
import com.cintra.app.ui.model.ChatMessage
import com.cintra.app.ui.model.ChatSession
import com.cintra.app.ui.model.GenUi
import com.cintra.app.ui.model.MetricItem
import kotlinx.coroutines.delay

/**
 * Phase 1 in-memory [ChatRepository]. Holds the seeded demo conversation that
 * mirrors the HTML reference (Markdown + table + Kotlin code + comparison GenUI)
 * plus an empty "محادثة جديدة" session.
 */
class MockChatRepository : ChatRepository {

    private val sessions = mutableListOf(
        demoSession(),
        ChatSession(id = "cintra-empty", title = "محادثة جديدة", messages = emptyList()),
    )

    override suspend fun getInitialSessions(): List<ChatSession> {
        delay(120)
        return sessions.toList()
    }

    override suspend fun createSession(title: String): ChatSession {
        val session = ChatSession(id = "c-${System.nanoTime()}", title = title, messages = emptyList())
        sessions.add(0, session)
        return session
    }

    private fun demoSession(): ChatSession {
        val assistant = ChatMessage(
            id = "a1",
            isUser = false,
            modelName = "Cintra Pro",
            reasoning = "1. Markdown الكامل وGenerative UI مفعّلان تلقائياً دائماً في Cintra دون الحاجة لأزرار إضافية.\n" +
                "2. عرض جدول معماري + كود Kotlin Compose + بطاقة مقارنة تفاعلية بين نماذج Cintra الأربعة.",
            reasoningExpanded = false,
            content = DEMO_MARKDOWN,
            genUi = GenUi.Comparison(
                activeTab = "speed",
                tabs = mapOf(
                    "speed" to listOf(
                        MetricItem("Cintra Core", 98),
                        MetricItem("Cintra Vision", 90),
                        MetricItem("Cintra Pro", 86),
                        MetricItem("Cintra Think", 79),
                    ),
                    "reasoning" to listOf(
                        MetricItem("Cintra Think", 97),
                        MetricItem("Cintra Pro", 95),
                        MetricItem("Cintra Vision", 88),
                        MetricItem("Cintra Core", 82),
                    ),
                    "arabic" to listOf(
                        MetricItem("Cintra Pro", 96),
                        MetricItem("Cintra Think", 94),
                        MetricItem("Cintra Core", 92),
                        MetricItem("Cintra Vision", 91),
                    ),
                ),
            ),
        )
        val user = ChatMessage(
            id = "u1",
            isUser = true,
            content = "اعرض لي قدرات Cintra في Markdown والواجهات التفاعلية المدمجة تلقائياً.",
        )
        return ChatSession(
            id = "cintra-demo",
            title = "مراجعة واجهة Cintra الزجاجية",
            messages = listOf(user, assistant),
        )
    }

    companion object {
        const val DEMO_MARKDOWN: String =
            "## تجربة Cintra الموحدة\n\n" +
                "> كل رد في **Cintra** يدعم **Markdown الكامل** و**Generative UI** و**معاينة Artifacts** تلقائياً، بينما يظل مربع الكتابة نظيفاً مع قائمة `+` للأدوات والوسائط والميكروفون والتحدث المباشر.\n\n" +
                "| النموذج | الاستخدام الأمثل | الميزة الأساسية |\n" +
                "| :--- | :--- | :--- |\n" +
                "| `Cintra Pro` | البرمجة والمشاريع | توازن الذكاء والتنفيذ الدقيق |\n" +
                "| `Cintra Think` | التحليل العميق | تفكير خطوة بخطوة قبل الإجابة |\n" +
                "| `Cintra Core` | المهام اليومية | سرعة بث عالية جداً |\n" +
                "| `Cintra Vision` | الصور والملفات | قراءة صور جوجل والمستندات |\n\n" +
                "```kotlin\n" +
                "@Composable\n" +
                "fun CintraTopModelPill(backdrop: Backdrop, model: String, onClick: () -> Unit) {\n" +
                "    Row(\n" +
                "        modifier = Modifier\n" +
                "            .drawBackdrop(backdrop = backdrop, shape = { CircleShape }) {\n" +
                "                blur(14.dp.toPx())\n" +
                "            }\n" +
                "            .clickable(onClick = onClick)\n" +
                "            .padding(horizontal = 18.dp, vertical = 10.dp)\n" +
                "    ) {\n" +
                "        Text(text = model, fontWeight = FontWeight.SemiBold)\n" +
                "    }\n" +
                "}\n" +
                "```"
    }
}