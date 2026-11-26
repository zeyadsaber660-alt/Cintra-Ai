package com.cintra.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cintra.app.data.AiStreamService
import com.cintra.app.data.ChatRepository
import com.cintra.app.data.mock.MockAiStreamService
import com.cintra.app.data.mock.MockChatRepository
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.model.ChatMessage
import com.cintra.app.ui.model.ChatSession
import com.cintra.app.ui.model.CintraModel
import com.cintra.app.ui.model.CintraModelId
import com.cintra.app.ui.model.DefaultCintraModel
import com.cintra.app.ui.model.GenUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable UI snapshot for the whole Cintra chat surface. */
data class CintraChatUiState(
    val ready: Boolean = false,
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String = "",
    val selectedModel: CintraModel = DefaultCintraModel,
    val darkTheme: Boolean = true,
    val activeTool: ActiveTool? = null,
    val attachments: List<Attachment> = emptyList(),
    val composerText: String = "",
    val plusMenuOpen: Boolean = false,
    val isGenerating: Boolean = false,
    val isRecordingMic: Boolean = false,
    val isLiveActive: Boolean = false,
    val isLiveMuted: Boolean = false,
    val modelPickerOpen: Boolean = false,
    val photosPickerOpen: Boolean = false,
    val artifactsOpen: Boolean = false,
    val artifactTitle: String = "",
    val artifactCode: String = "",
    val toast: String? = null,
) {
    val activeSession: ChatSession?
        get() = sessions.firstOrNull { it.id == activeSessionId }

    val messages: List<ChatMessage>
        get() = activeSession?.messages ?: emptyList()
}

/**
 * MVVM state holder. Owns all Phase 1 mock state and the streaming lifecycle.
 * Data access is entirely behind [ChatRepository] / [AiStreamService] so Phase 2
 * (Room + Ktor SSE) plugs in without UI changes.
 */
class CintraChatViewModel(
    private val repository: ChatRepository = MockChatRepository(),
    private val streamService: AiStreamService = MockAiStreamService(),
) : ViewModel() {

    private val _state = MutableStateFlow(CintraChatUiState())
    val state: StateFlow<CintraChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var micJob: Job? = null

    init {
        viewModelScope.launch {
            val sessions = repository.getInitialSessions()
            _state.update {
                it.copy(
                    ready = true,
                    sessions = sessions,
                    activeSessionId = sessions.firstOrNull()?.id ?: "",
                )
            }
        }
    }

    // ---- Theme -------------------------------------------------------------

    fun toggleTheme() = _state.update { it.copy(darkTheme = !it.darkTheme) }

    // ---- Model selection ---------------------------------------------------

    fun openModelPicker() = _state.update { it.copy(modelPickerOpen = true, plusMenuOpen = false) }
    fun closeModelPicker() = _state.update { it.copy(modelPickerOpen = false) }

    fun selectModel(model: CintraModel) = _state.update {
        it.copy(
            selectedModel = model,
            modelPickerOpen = false,
            toast = "تم التبديل إلى ${model.name}",
        )
    }

    // ---- Plus menu & tools -------------------------------------------------

    fun togglePlusMenu() = _state.update { it.copy(plusMenuOpen = !it.plusMenuOpen) }
    fun closePlusMenu() = _state.update { it.copy(plusMenuOpen = false) }

    fun toggleTool(tool: ActiveTool) = _state.update {
        it.copy(activeTool = if (it.activeTool == tool) null else tool, plusMenuOpen = false)
    }

    fun clearTool() = _state.update { it.copy(activeTool = null) }

    // ---- Attachments -------------------------------------------------------

    fun addAttachment(attachment: Attachment) = _state.update {
        it.copy(attachments = it.attachments + attachment, photosPickerOpen = false)
    }

    fun removeAttachment(index: Int) = _state.update {
        it.copy(attachments = it.attachments.filterIndexed { i, _ -> i != index })
    }

    fun openPhotosPicker() = _state.update { it.copy(photosPickerOpen = true, plusMenuOpen = false) }
    fun closePhotosPicker() = _state.update { it.copy(photosPickerOpen = false) }

    // ---- Composer ----------------------------------------------------------

    fun onComposerTextChange(text: String) = _state.update { it.copy(composerText = text) }

    // ---- Sessions ----------------------------------------------------------

    fun switchSession(id: String) {
        stopGeneration()
        _state.update {
            it.copy(
                activeSessionId = id,
                plusMenuOpen = false,
                modelPickerOpen = false,
                photosPickerOpen = false,
                artifactsOpen = false,
                composerText = "",
                attachments = emptyList(),
                activeTool = null,
            )
        }
    }

    fun newChat() {
        stopGeneration()
        viewModelScope.launch {
            val session = repository.createSession("محادثة جديدة")
            _state.update {
                it.copy(
                    sessions = listOf(session) + it.sessions,
                    activeSessionId = session.id,
                    plusMenuOpen = false,
                    modelPickerOpen = false,
                    photosPickerOpen = false,
                    artifactsOpen = false,
                    composerText = "",
                    attachments = emptyList(),
                    activeTool = null,
                )
            }
        }
    }

    // ---- Artifacts ---------------------------------------------------------

    fun openArtifacts(title: String, code: String) =
        _state.update { it.copy(artifactsOpen = true, artifactTitle = title, artifactCode = code) }

    fun closeArtifacts() = _state.update { it.copy(artifactsOpen = false) }

    // ---- Cintra Live -------------------------------------------------------

    fun openLive() = _state.update { it.copy(isLiveActive = true, isLiveMuted = false) }
    fun closeLive() = _state.update { it.copy(isLiveActive = false, isLiveMuted = false) }
    fun toggleLiveMute() = _state.update { it.copy(isLiveMuted = !it.isLiveMuted) }

    // ---- Toast -------------------------------------------------------------

    fun showToast(message: String) = _state.update { it.copy(toast = message) }
    fun consumeToast() = _state.update { it.copy(toast = null) }

    // ---- Mic dictation (mock) ---------------------------------------------

    fun toggleMic() {
        if (_state.value.isRecordingMic) {
            micJob?.cancel()
            _state.update { it.copy(isRecordingMic = false) }
            return
        }
        _state.update { it.copy(isRecordingMic = true, toast = "جارٍ الاستماع لصوتك...") }
        micJob = viewModelScope.launch {
            delay(1600)
            _state.update {
                val base = it.composerText
                val sample = "اعمل لي خطة تنفيذ تفاعلية لبناء تطبيق Cintra بـ Kotlin"
                val merged = if (base.isBlank()) sample else "$base $sample"
                it.copy(isRecordingMic = false, composerText = merged, toast = "تم تحويل الصوت إلى نص")
            }
        }
    }

    // ---- Generative UI interactions ---------------------------------------

    fun toggleReasoningExpanded(messageId: String) = updateActiveMessage(messageId) {
        it.copy(reasoningExpanded = !it.reasoningExpanded)
    }

    fun setComparisonTab(messageId: String, tab: String) = updateActiveMessage(messageId) { msg ->
        val gen = msg.genUi as? GenUi.Comparison
        if (gen != null) msg.copy(genUi = gen.copy(activeTab = tab)) else msg
    }

    fun toggleChecklistItem(messageId: String, index: Int) = updateActiveMessage(messageId) { msg ->
        val gen = msg.genUi as? GenUi.Checklist
        if (gen != null && index in gen.items.indices) {
            val items = gen.items.toMutableList()
            items[index] = items[index].copy(done = !items[index].done)
            msg.copy(genUi = gen.copy(items = items))
        } else msg
    }

    fun regenerate(messageId: String) {
        if (_state.value.isGenerating) return
        val session = _state.value.activeSession ?: return
        val index = session.messages.indexOfFirst { it.id == messageId }
        if (index <= 0) return
        val prevPrompt = session.messages[index - 1].content
        _state.update { s ->
            val newSessions = s.sessions.map {
                if (it.id == s.activeSessionId) {
                    // Drop both the assistant message and its preceding user
                    // message; sendMessage() re-adds a fresh user+assistant pair.
                    it.copy(messages = it.messages.filterIndexed { i, _ -> i != index && i != index - 1 })
                } else it
            }
            s.copy(sessions = newSessions)
        }
        sendMessage(prevPrompt)
    }

    // ---- Send / stream -----------------------------------------------------

    fun sendMessage(forcedText: String? = null) {
        if (_state.value.isGenerating) {
            stopGeneration()
            return
        }
        val current = _state.value
        val text = (forcedText ?: current.composerText).trim()
        if (text.isEmpty() && current.attachments.isEmpty()) return

        val usedTool = current.activeTool
        val usedAttachments = current.attachments
        val finalPrompt = text.ifEmpty {
            "تحليل المرفق: " + usedAttachments.joinToString("، ") { it.name }
        }
        val sessionId = current.activeSessionId
        val model = current.selectedModel
        val shouldShowThinking = usedTool == ActiveTool.THINKING || model.id == CintraModelId.THINK

        val userMessage = ChatMessage(
            id = "u-${System.nanoTime()}",
            isUser = true,
            content = finalPrompt,
            attachments = usedAttachments,
        )
        val assistantMessage = ChatMessage(
            id = "a-${System.nanoTime()}",
            isUser = false,
            modelName = model.name,
            reasoning = if (shouldShowThinking) "" else null,
            reasoningExpanded = shouldShowThinking,
            isStreaming = true,
        )

        _state.update { s ->
            val newSessions = s.sessions.map { session ->
                when {
                    session.id != sessionId -> session
                    session.messages.isEmpty() -> session.copy(
                        title = finalPrompt.take(28) + if (finalPrompt.length > 28) "..." else "",
                        messages = listOf(userMessage, assistantMessage),
                    )
                    else -> session.copy(messages = session.messages + userMessage + assistantMessage)
                }
            }
            s.copy(
                sessions = newSessions,
                composerText = "",
                attachments = emptyList(),
                activeTool = null,
                plusMenuOpen = false,
                isGenerating = true,
            )
        }

        val request = AiStreamRequest(
            prompt = finalPrompt,
            model = model,
            tool = usedTool,
            attachments = usedAttachments,
        )
        val assistantId = assistantMessage.id

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            streamService.streamReply(request).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.ReasoningDelta ->
                        updateActiveMessage(assistantId) { it.copy(reasoning = (it.reasoning ?: "") + chunk.text) }

                    AiStreamChunk.ReasoningComplete ->
                        updateActiveMessage(assistantId) { it.copy(reasoningExpanded = false) }

                    is AiStreamChunk.ContentDelta ->
                        updateActiveMessage(assistantId) { it.copy(content = it.content + chunk.text) }

                    is AiStreamChunk.Complete -> {
                        updateActiveMessage(assistantId) {
                            it.copy(isStreaming = false, genUi = chunk.genUi ?: it.genUi)
                        }
                        _state.update { s -> s.copy(isGenerating = false) }
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        val wasGenerating = _state.value.isGenerating
        if (wasGenerating) {
            updateActiveMessageStreamingDone()
            _state.update { it.copy(isGenerating = false) }
        }
    }

    // ---- helpers -----------------------------------------------------------

    private fun updateActiveMessageStreamingDone() {
        _state.update { s ->
            val newSessions = s.sessions.map { session ->
                if (session.id == s.activeSessionId) {
                    session.copy(messages = session.messages.map {
                        if (it.isStreaming) it.copy(isStreaming = false) else it
                    })
                } else session
            }
            s.copy(sessions = newSessions)
        }
    }

    private inline fun updateActiveMessage(
        messageId: String,
        crossinline transform: (ChatMessage) -> ChatMessage,
    ) {
        _state.update { s ->
            val newSessions = s.sessions.map { session ->
                if (session.id == s.activeSessionId) {
                    session.copy(messages = session.messages.map {
                        if (it.id == messageId) transform(it) else it
                    })
                } else session
            }
            s.copy(sessions = newSessions)
        }
    }
}