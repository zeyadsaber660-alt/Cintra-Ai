package com.cintra.app.ui.model

/** Composer / message attachment kinds. */
enum class AttachmentType { PHOTO, FILE }

data class Attachment(
    val type: AttachmentType,
    val name: String,
    val uri: String? = null,
)

/** Active composer tools selectable from the `+` popover. */
enum class ActiveTool(val label: String) {
    THINKING("تفكير عميق"),
    WEB("بحث ويب مباشر"),
    IMAGE("إنشاء صور"),
}

/** A single metric row inside a comparison Generative UI card. */
data class MetricItem(val label: String, val value: Int)

/** A single checklist entry inside an interactive Generative UI card. */
data class ChecklistItem(val text: String, val done: Boolean)

/**
 * Generative UI: native Compose widgets embedded directly inside assistant
 * messages. Always-on (no toggles). Sealed so Phase 2 can add new widget types.
 */
sealed interface GenUi {
    data class Comparison(
        val activeTab: String = "speed",
        val tabs: Map<String, List<MetricItem>>,
    ) : GenUi

    data class Checklist(val items: List<ChecklistItem>) : GenUi

    data class GeneratedImage(val prompt: String) : GenUi
}

/** A single chat message (user or assistant). */
data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val content: String = "",
    val modelName: String? = null,
    val reasoning: String? = null,
    val reasoningExpanded: Boolean = false,
    val isStreaming: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val genUi: GenUi? = null,
)

/** A conversation session shown in the navigation drawer. */
data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
)

/** Request passed to [com.cintra.app.data.AiStreamService]. */
data class AiStreamRequest(
    val prompt: String,
    val model: CintraModel,
    val tool: ActiveTool?,
    val attachments: List<Attachment>,
)

/** Incremental streaming events emitted by [com.cintra.app.data.AiStreamService]. */
sealed interface AiStreamChunk {
    data class ReasoningDelta(val text: String) : AiStreamChunk
    data class ContentDelta(val text: String) : AiStreamChunk
    data object ReasoningComplete : AiStreamChunk
    data class Complete(val genUi: GenUi?) : AiStreamChunk
}