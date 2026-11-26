package com.cintra.app.ui.model

enum class AttachmentType { PHOTO, FILE }

data class Attachment(
    val type: AttachmentType,
    val name: String,
    val uri: String? = null,
)

enum class ActiveTool(val label: String) {
    THINKING("تفكير عميق"),
    WEB("بحث ويب مباشر"),
    IMAGE("إنشاء صور"),
}

data class MetricItem(val label: String, val value: Int)

data class ChecklistItem(val text: String, val done: Boolean)

data class WebPhotoItem(
    val url: String,
    val title: String = "",
    val source: String = "",
)

data class VisualGridItem(
    val title: String,
    val subtitle: String,
    val tone: String = "blue", // blue, green, amber, slate
)

data class GeneratedPdfDoc(
    val title: String,
    val fileName: String,
    val htmlContent: String,
    val summary: String = "",
)

sealed interface GenUi {
    data class Comparison(
        val activeTab: String = "speed",
        val tabs: Map<String, List<MetricItem>>,
    ) : GenUi

    data class Checklist(val items: List<ChecklistItem>) : GenUi

    data class GeneratedImage(val prompt: String) : GenUi

    data class RealPhotos(
        val photos: List<WebPhotoItem>,
        val caption: String = "",
    ) : GenUi

    data class VisualGrid(
        val title: String,
        val items: List<VisualGridItem>,
    ) : GenUi
}

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
    val webPhotos: List<WebPhotoItem> = emptyList(),
    val generatedPdf: GeneratedPdfDoc? = null,
)

data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
)

data class AiStreamRequest(
    val prompt: String,
    val model: CintraModel,
    val tool: ActiveTool?,
    val attachments: List<Attachment>,
)

sealed interface AiStreamChunk {
    data class ReasoningDelta(val text: String) : AiStreamChunk
    data class ContentDelta(val text: String) : AiStreamChunk
    data object ReasoningComplete : AiStreamChunk
    data class WebPhotosReady(val photos: List<WebPhotoItem>) : AiStreamChunk
    data class Complete(
        val genUi: GenUi?,
        val generatedPdf: GeneratedPdfDoc? = null,
    ) : AiStreamChunk
}