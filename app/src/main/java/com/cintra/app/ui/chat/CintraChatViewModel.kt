package com.cintra.app.ui.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cintra.app.data.AiStreamService
import com.cintra.app.data.ChatRepository
import com.cintra.app.data.mock.MockChatRepository
import com.cintra.app.data.network.CintraWorkerStreamService
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.model.AttachmentType
import com.cintra.app.ui.model.ChatMessage
import com.cintra.app.ui.model.ChatSession
import com.cintra.app.ui.model.CintraModel
import com.cintra.app.ui.model.CintraModelId
import com.cintra.app.ui.model.DefaultCintraModel
import com.cintra.app.ui.model.GenUi
import com.cintra.app.ui.pages.CintraProject
import com.cintra.app.ui.auth.AuthMode
import com.cintra.app.ui.pages.FeatureDestination
import com.cintra.app.service.CintraBackgroundService
import com.cintra.app.utils.CintraAttachmentReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CintraChatUiState(
    val ready: Boolean = false,
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String = "",
    val drawerSearchQuery: String = "",
    val drawerSearchVisible: Boolean = false,
    val selectedModel: CintraModel = DefaultCintraModel,
    val darkTheme: Boolean = true,
    val isArabic: Boolean = true,
    val autoWebSearch: Boolean = true,
    val illustrativeImages: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val authMode: AuthMode = AuthMode.GUEST,
    val userName: String = "Zeyad",
    val userEmail: String = "",
    val fontScale: Float = 1.0f,
    val responseTone: String = "متوازن",
    val customInstructions: String = "",
    val projects: List<CintraProject> = emptyList(),
    val activeProjectId: String? = null,
    val activeFeature: FeatureDestination = FeatureDestination.NONE,
    val activeTool: ActiveTool? = null,
    val attachments: List<Attachment> = emptyList(),
    val composerText: String = "",
    val plusMenuOpen: Boolean = false,
    val isGenerating: Boolean = false,
    val isRecordingMic: Boolean = false,
    val isLiveActive: Boolean = false,
    val isLiveMuted: Boolean = false,
    val modelPickerOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val artifactsOpen: Boolean = false,
    val artifactTitle: String = "",
    val artifactCode: String = "",
    val toast: String? = null,
    val isTemporaryChat: Boolean = false,
    val pinnedSessionIds: Set<String> = emptySet(),
    val chatSearchOpen: Boolean = false,
    val chatSearchQuery: String = "",
) {
    val activeSession: ChatSession?
        get() = sessions.firstOrNull { it.id == activeSessionId }

    val messages: List<ChatMessage>
        get() = activeSession?.messages ?: emptyList()

    val totalMessagesCount: Int
        get() = sessions.sumOf { it.messages.size }

    val allPhotos: List<Attachment>
        get() = (sessions.flatMap { s -> s.messages.flatMap { it.attachments } } + attachments)
            .filter { it.type == AttachmentType.PHOTO }
            .distinctBy { it.uri ?: it.name }

    val allFiles: List<Attachment>
        get() = (sessions.flatMap { s -> s.messages.flatMap { it.attachments } } + attachments)
            .filter { it.type == AttachmentType.FILE }
            .distinctBy { it.uri ?: it.name }

    val allCodeSnippets: List<Pair<String, String>>
        get() {
            val regex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
            return sessions.flatMap { s ->
                s.messages.filter { !it.isUser }.flatMap { m ->
                    regex.findAll(m.content).map { match ->
                        match.groupValues[1].ifBlank { "code" } to match.groupValues[2].trim()
                    }.toList()
                }
            }
        }

    val filteredSessions: List<ChatSession>
        get() {
            val q = drawerSearchQuery.trim()
            if (q.isEmpty()) return sessions
            return sessions.filter { s ->
                s.title.contains(q, ignoreCase = true) ||
                    s.messages.any { it.content.contains(q, ignoreCase = true) }
            }
        }
}

class CintraChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cintra_user_prefs", Context.MODE_PRIVATE)
    private val repository: ChatRepository = MockChatRepository()
    private val streamService: AiStreamService = CintraWorkerStreamService(
        autoWebSearchProvider = { _state.value.autoWebSearch },
        illustrativeImagesProvider = { _state.value.illustrativeImages },
        customInstructionsProvider = { buildEffectiveInstructions() },
    )

    private val _state = MutableStateFlow(
        CintraChatUiState(
            hasCompletedOnboarding = prefs.getBoolean("onboarded", false),
            authMode = runCatching { AuthMode.valueOf(prefs.getString("auth_mode", "GUEST") ?: "GUEST") }.getOrDefault(AuthMode.GUEST),
            userName = prefs.getString("user_name", "Zeyad") ?: "Zeyad",
            userEmail = prefs.getString("user_email", "") ?: "",
            fontScale = prefs.getFloat("font_scale", 1.0f),
            responseTone = prefs.getString("response_tone", "متوازن") ?: "متوازن",
            customInstructions = prefs.getString("custom_instructions", "") ?: "",
            darkTheme = prefs.getBoolean("dark_theme", true),
            isArabic = prefs.getBoolean("is_arabic", true),
        ),
    )
    val state: StateFlow<CintraChatUiState> = _state.asStateFlow()

    private val sessionJobs = mutableMapOf<String, Job>()
    private val localAttachmentCache = mutableMapOf<String, Attachment>()

    init {
        viewModelScope.launch {
            val loaded = loadPersistedSessions()
            val sessions = if (loaded.isNotEmpty()) loaded else repository.getInitialSessions()
            _state.update {
                it.copy(
                    ready = true,
                    sessions = sessions,
                    activeSessionId = sessions.firstOrNull()?.id ?: "",
                )
            }
        }
    }

    private fun sessionsFile(): File = File(getApplication<Application>().filesDir, "cintra_sessions_v1.json")

    private fun persistSessions() {
        runCatching {
            val arr = JSONArray()
            val tempId = if (_state.value.isTemporaryChat) _state.value.activeSessionId else null
            _state.value.sessions.filter { it.id != tempId }.take(40).forEach { s ->
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("title", s.title)
                val mArr = JSONArray()
                s.messages.takeLast(60).forEach { m ->
                    val mObj = JSONObject()
                    mObj.put("id", m.id)
                    mObj.put("isUser", m.isUser)
                    mObj.put("content", m.content)
                    if (m.modelName != null) mObj.put("modelName", m.modelName)
                    if (m.attachments.isNotEmpty()) {
                        val aArr = JSONArray()
                        m.attachments.forEach { a ->
                            aArr.put(JSONObject().apply {
                                put("type", a.type.name)
                                put("name", a.name)
                                if (a.uri != null) put("uri", a.uri)
                                put("mimeType", a.mimeType)
                                if (a.pageCount != null) put("pageCount", a.pageCount)
                            })
                        }
                        mObj.put("attachments", aArr)
                    }
                    m.generatedPdf?.let { pdf ->
                        mObj.put("generatedPdf", JSONObject().apply {
                            put("title", pdf.title)
                            put("fileName", pdf.fileName)
                            put("htmlContent", pdf.htmlContent)
                            put("summary", pdf.summary)
                        })
                    }
                    if (m.followUpSuggestions.isNotEmpty()) {
                        mObj.put("suggestions", JSONArray(m.followUpSuggestions))
                    }
                    mArr.put(mObj)
                }
                sObj.put("messages", mArr)
                arr.put(sObj)
            }
            sessionsFile().writeText(arr.toString(), Charsets.UTF_8)
        }
    }

    private fun loadPersistedSessions(): List<ChatSession> {
        return runCatching {
            val f = sessionsFile()
            if (!f.exists()) return emptyList()
            val arr = JSONArray(f.readText(Charsets.UTF_8))
            (0 until arr.length()).mapNotNull { i ->
                val sObj = arr.optJSONObject(i) ?: return@mapNotNull null
                val mArr = sObj.optJSONArray("messages") ?: JSONArray()
                val msgs = (0 until mArr.length()).mapNotNull { j ->
                    val mObj = mArr.optJSONObject(j) ?: return@mapNotNull null
                    val attArr = mObj.optJSONArray("attachments")
                    val atts = if (attArr != null) {
                        (0 until attArr.length()).mapNotNull { k ->
                            val ao = attArr.optJSONObject(k) ?: return@mapNotNull null
                            Attachment(
                                type = runCatching { AttachmentType.valueOf(ao.optString("type", "FILE")) }.getOrDefault(AttachmentType.FILE),
                                name = ao.optString("name", "ملف"),
                                uri = ao.optString("uri", "").ifBlank { null },
                                mimeType = ao.optString("mimeType", ""),
                                pageCount = if (ao.has("pageCount")) ao.optInt("pageCount") else null,
                            )
                        }
                    } else emptyList()
                    val pdfObj = mObj.optJSONObject("generatedPdf")
                    val pdf = if (pdfObj != null) {
                        com.cintra.app.ui.model.GeneratedPdfDoc(
                            title = pdfObj.optString("title", "مستند"),
                            fileName = pdfObj.optString("fileName", "document.pdf"),
                            htmlContent = pdfObj.optString("htmlContent", ""),
                            summary = pdfObj.optString("summary", ""),
                        )
                    } else null
                    val sugArr = mObj.optJSONArray("suggestions")
                    val sugs = if (sugArr != null) (0 until sugArr.length()).mapNotNull { k -> sugArr.optString(k).takeIf { it.isNotBlank() } } else emptyList()
                    ChatMessage(
                        id = mObj.optString("id", "m-$j"),
                        isUser = mObj.optBoolean("isUser", false),
                        content = mObj.optString("content", ""),
                        modelName = mObj.optString("modelName", "").ifBlank { null },
                        attachments = atts,
                        generatedPdf = pdf,
                        followUpSuggestions = sugs,
                    )
                }
                ChatSession(
                    id = sObj.optString("id", "s-$i"),
                    title = sObj.optString("title", "محادثة"),
                    messages = msgs,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun buildEffectiveInstructions(): String {
        val s = _state.value
        val activeProject = s.projects.firstOrNull { it.id == s.activeProjectId }
        return buildList {
            if (s.userName.isNotBlank()) add("User's name: ${s.userName}")
            add("Response tone: ${s.responseTone}")
            if (s.customInstructions.isNotBlank()) add(s.customInstructions)
            if (activeProject != null) add("Active project (${activeProject.name}): ${activeProject.instructions}")
        }.joinToString("\n")
    }


    fun completeOnboarding(mode: AuthMode, displayName: String, email: String) {
        prefs.edit()
            .putBoolean("onboarded", true)
            .putString("auth_mode", mode.name)
            .putString("user_name", displayName)
            .putString("user_email", email)
            .apply()
        _state.update {
            it.copy(
                hasCompletedOnboarding = true,
                authMode = mode,
                userName = displayName,
                userEmail = email,
                settingsOpen = false,
            )
        }
    }

    fun reopenOnboarding() {
        _state.update { it.copy(hasCompletedOnboarding = false, settingsOpen = false) }
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat("font_scale", scale).apply()
        _state.update { it.copy(fontScale = scale) }
    }

    fun setResponseTone(tone: String) {
        prefs.edit().putString("response_tone", tone).apply()
        _state.update { it.copy(responseTone = tone) }
    }

    fun toggleTheme() = setDarkTheme(!_state.value.darkTheme)

    fun setDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean("dark_theme", dark).apply()
        _state.update { it.copy(darkTheme = dark) }
    }

    fun setArabic(arabic: Boolean) {
        prefs.edit().putBoolean("is_arabic", arabic).apply()
        _state.update {
            it.copy(
                isArabic = arabic,
                toast = if (arabic) "تم التبديل إلى العربية (RTL)" else "Switched to English (LTR)",
            )
        }
    }

    fun saveProfileAndInstructions(userName: String, instructions: String) {
        prefs.edit()
            .putString("user_name", userName)
            .putString("custom_instructions", instructions)
            .apply()
        _state.update {
            it.copy(
                userName = userName,
                customInstructions = instructions,
                activeFeature = FeatureDestination.NONE,
                toast = if (it.isArabic) "تم حفظ التخصيص" else "Preferences saved",
            )
        }
    }

    fun createProject(name: String, instructions: String) {
        val project = CintraProject(id = "proj-${System.nanoTime()}", name = name, instructions = instructions)
        _state.update {
            it.copy(
                projects = listOf(project) + it.projects,
                activeProjectId = project.id,
                toast = if (it.isArabic) "تم إنشاء وتفعيل المشروع: $name" else "Project activated: $name",
            )
        }
    }

    fun selectProject(projectId: String?) {
        _state.update { it.copy(activeProjectId = projectId) }
    }

    fun openFeature(dest: FeatureDestination) = _state.update {
        it.copy(activeFeature = dest, settingsOpen = false, plusMenuOpen = false)
    }

    fun closeFeature() = _state.update { it.copy(activeFeature = FeatureDestination.NONE) }

    fun clearAllChats() {
        stopGeneration()
        viewModelScope.launch {
            val fresh = repository.createSession(if (_state.value.isArabic) "محادثة جديدة" else "New Chat")
            _state.update {
                it.copy(
                    sessions = listOf(fresh),
                    activeSessionId = fresh.id,
                    composerText = "",
                    attachments = emptyList(),
                )
            }
        }
    }

    fun toggleAutoWebSearch() = _state.update { it.copy(autoWebSearch = !it.autoWebSearch) }
    fun toggleIllustrativeImages() = _state.update { it.copy(illustrativeImages = !it.illustrativeImages) }

    fun toggleDrawerSearch() = _state.update {
        it.copy(
            drawerSearchVisible = !it.drawerSearchVisible,
            drawerSearchQuery = if (it.drawerSearchVisible) "" else it.drawerSearchQuery,
        )
    }

    fun onDrawerSearchChange(query: String) = _state.update { it.copy(drawerSearchQuery = query) }

    fun openModelPicker() = _state.update { it.copy(modelPickerOpen = true, plusMenuOpen = false) }
    fun closeModelPicker() = _state.update { it.copy(modelPickerOpen = false) }

    fun selectModel(model: CintraModel) = _state.update {
        it.copy(
            selectedModel = model,
            modelPickerOpen = false,
            toast = if (it.isArabic) "تم التبديل إلى ${model.name}" else "Switched to ${model.name}",
        )
    }

    fun openSettings() = _state.update { it.copy(settingsOpen = true, plusMenuOpen = false) }
    fun closeSettings() = _state.update { it.copy(settingsOpen = false) }

    fun togglePlusMenu() = _state.update { it.copy(plusMenuOpen = !it.plusMenuOpen) }
    fun closePlusMenu() = _state.update { it.copy(plusMenuOpen = false) }

    fun toggleTool(tool: ActiveTool) = _state.update {
        it.copy(activeTool = if (it.activeTool == tool) null else tool, plusMenuOpen = false)
    }

    fun clearTool() = _state.update { it.copy(activeTool = null) }

    fun addAttachment(attachment: Attachment) = _state.update {
        it.copy(attachments = it.attachments + attachment, plusMenuOpen = false)
    }

    fun attachUri(uri: Uri, type: AttachmentType) {
        val ctx = getApplication<Application>()
        val uriStr = uri.toString()
        localAttachmentCache[uriStr]?.let { cached ->
            _state.update { it.copy(attachments = it.attachments + cached, plusMenuOpen = false) }
            return
        }
        val initialName = CintraAttachmentReader.queryDisplayName(
            context = ctx,
            uri = uri,
            fallback = if (type == AttachmentType.PHOTO) "صورة" else "ملف",
        )
        val placeholder = Attachment(
            type = type,
            name = initialName,
            uri = uriStr,
            isLoading = true,
        )
        _state.update {
            it.copy(attachments = it.attachments + placeholder, plusMenuOpen = false)
        }
        viewModelScope.launch {
            val extracted = CintraAttachmentReader.extract(ctx, uri, isPhoto = type == AttachmentType.PHOTO)
            _state.update { s ->
                s.copy(
                    attachments = s.attachments.map { att ->
                        if (att.uri == uriStr) {
                            att.copy(
                                name = extracted.displayName,
                                mimeType = extracted.mimeType,
                                extractedText = extracted.textContent,
                                imageDataUrl = extracted.imageDataUrl,
                                pageCount = extracted.pageCount,
                                isLoading = false,
                            ).also { localAttachmentCache[uriStr] = it }
                        } else att
                    },
                )
            }
        }
    }

    fun removeAttachment(index: Int) = _state.update {
        it.copy(attachments = it.attachments.filterIndexed { i, _ -> i != index })
    }

    fun onComposerTextChange(text: String) {
        if (text.length >= 1800) {
            val nextIdx = _state.value.attachments.count { it.type == AttachmentType.FILE } + 1
            val att = Attachment(
                type = AttachmentType.FILE,
                name = "pasted_text_$nextIdx.txt",
                mimeType = "text/plain",
                extractedText = text,
                isLoading = false,
            )
            _state.update { s ->
                s.copy(
                    composerText = "",
                    attachments = s.attachments + att,
                    toast = if (s.isArabic) "تم تحويل النص الطويل تلقائياً إلى ملف .txt مرفق" else "Long text converted to .txt attachment",
                )
            }
        } else {
            _state.update { it.copy(composerText = text) }
        }
    }

    fun setMicRecording(recording: Boolean) = _state.update { it.copy(isRecordingMic = recording) }

    fun appendDictatedText(spoken: String) = _state.update {
        val trimmed = spoken.trim()
        if (trimmed.isEmpty()) it.copy(isRecordingMic = false)
        else {
            val merged = if (it.composerText.isBlank()) trimmed else "${it.composerText} $trimmed"
            it.copy(isRecordingMic = false, composerText = merged)
        }
    }

    fun switchSession(id: String) {
        _state.update {
            val targetGenerating = sessionJobs[id]?.isActive == true
            it.copy(
                activeSessionId = id,
                isGenerating = targetGenerating,
                plusMenuOpen = false,
                modelPickerOpen = false,
                artifactsOpen = false,
                composerText = "",
                attachments = emptyList(),
                activeTool = null,
            )
        }
    }

    fun newChat() {
        viewModelScope.launch {
            val current = _state.value.activeSession
            if (current != null && current.messages.isEmpty()) {
                _state.update { it.copy(plusMenuOpen = false, modelPickerOpen = false, artifactsOpen = false) }
                return@launch
            }
            val title = if (_state.value.isArabic) "محادثة جديدة" else "New Chat"
            val session = repository.createSession(title)
            _state.update {
                it.copy(
                    sessions = listOf(session) + it.sessions,
                    activeSessionId = session.id,
                    isGenerating = false,
                    plusMenuOpen = false,
                    modelPickerOpen = false,
                    artifactsOpen = false,
                    composerText = "",
                    attachments = emptyList(),
                    activeTool = null,
                )
            }
            persistSessions()
        }
    }

    fun openArtifacts(title: String, code: String) =
        _state.update { it.copy(artifactsOpen = true, artifactTitle = title, artifactCode = code) }

    fun closeArtifacts() = _state.update { it.copy(artifactsOpen = false) }

    fun openLive() = _state.update { it.copy(isLiveActive = true, isLiveMuted = false) }
    fun closeLive() = _state.update { it.copy(isLiveActive = false, isLiveMuted = false) }
    fun toggleLiveMute() = _state.update { it.copy(isLiveMuted = !it.isLiveMuted) }

    fun showToast(message: String) = _state.update { it.copy(toast = message) }
    fun consumeToast() = _state.update { it.copy(toast = null) }

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
        val prevUserMsg = session.messages[index - 1]
        _state.update { s ->
            val newSessions = s.sessions.map {
                if (it.id == s.activeSessionId) {
                    it.copy(messages = it.messages.take(index - 1))
                } else it
            }
            s.copy(
                sessions = newSessions,
                attachments = prevUserMsg.attachments,
            )
        }
        sendMessage(prevUserMsg.content)
    }

    fun editUserMessage(messageId: String) {
        stopGeneration()
        val session = _state.value.activeSession ?: return
        val index = session.messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val userMsg = session.messages[index]
        _state.update { s ->
            val newSessions = s.sessions.map {
                if (it.id == s.activeSessionId) it.copy(messages = it.messages.take(index)) else it
            }
            s.copy(
                sessions = newSessions,
                composerText = userMsg.content,
                attachments = userMsg.attachments,
                toast = if (s.isArabic) "يمكنك تعديل الرسالة وإعادة إرسالها" else "Edit and resend your message",
            )
        }
    }

    fun resendUserMessage(messageId: String) {
        if (_state.value.isGenerating) return
        val session = _state.value.activeSession ?: return
        val index = session.messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val userMsg = session.messages[index]
        _state.update { s ->
            val newSessions = s.sessions.map {
                if (it.id == s.activeSessionId) it.copy(messages = it.messages.take(index)) else it
            }
            s.copy(
                sessions = newSessions,
                attachments = userMsg.attachments,
            )
        }
        sendMessage(userMsg.content)
    }

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
            liveStatus = if (usedAttachments.isNotEmpty()) "جارٍ قراءة الملف المرفق..." else "جارٍ تحليل طلبك...",
        )

        _state.update { s ->
            val newSessions = s.sessions.map { session ->
                when {
                    session.id != sessionId -> session
                    session.messages.isEmpty() -> session.copy(
                        title = finalPrompt.take(30) + if (finalPrompt.length > 30) "..." else "",
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

        val priorHistory = (_state.value.activeSession?.messages ?: emptyList())
            .filter { !it.isStreaming && it.id != userMessage.id && it.id != assistantMessage.id }
            .takeLast(6)
        val effectiveAttachments = if (usedAttachments.isEmpty()) {
            priorHistory.asReversed()
                .flatMap { it.attachments }
                .filter { !it.extractedText.isNullOrBlank() }
                .take(1)
        } else usedAttachments
        val request = AiStreamRequest(
            prompt = finalPrompt,
            model = model,
            tool = usedTool,
            attachments = effectiveAttachments,
            history = priorHistory,
        )
        val assistantId = assistantMessage.id

        sessionJobs[sessionId]?.cancel()
        CintraBackgroundService.start(
            getApplication(),
            if (usedAttachments.isNotEmpty()) "جارٍ تحليل الملف وكتابة الرد في الخلفية..." else "جارٍ إكمال الرد في الخلفية...",
        )
        sessionJobs[sessionId] = viewModelScope.launch {
            val pendingContent = StringBuilder()
            val pendingReasoning = StringBuilder()
            var lastFlushMs = 0L

            fun flushBuffers(force: Boolean = false) {
                val now = System.currentTimeMillis()
                if (!force && now - lastFlushMs < 75L) return
                if (pendingContent.isEmpty() && pendingReasoning.isEmpty()) return
                val cDelta = pendingContent.toString()
                val rDelta = pendingReasoning.toString()
                pendingContent.clear()
                pendingReasoning.clear()
                lastFlushMs = now
                updateSessionMessage(sessionId, assistantId) {
                    it.copy(
                        content = if (cDelta.isNotEmpty()) it.content + cDelta else it.content,
                        reasoning = if (rDelta.isNotEmpty()) (it.reasoning ?: "") + rDelta else it.reasoning,
                    )
                }
            }

            streamService.streamReply(request).collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.StatusUpdate ->
                        updateSessionMessage(sessionId, assistantId) { it.copy(liveStatus = chunk.text) }

                    is AiStreamChunk.ReasoningDelta -> {
                        pendingReasoning.append(chunk.text)
                        flushBuffers()
                    }

                    AiStreamChunk.ReasoningComplete -> {
                        flushBuffers(force = true)
                        updateSessionMessage(sessionId, assistantId) { it.copy(reasoningExpanded = false) }
                    }

                    is AiStreamChunk.WebPhotosReady ->
                        updateSessionMessage(sessionId, assistantId) { it.copy(webPhotos = chunk.photos) }

                    is AiStreamChunk.SourcesReady ->
                        updateSessionMessage(sessionId, assistantId) { it.copy(webSources = chunk.sources) }

                    is AiStreamChunk.ContentDelta -> {
                        pendingContent.append(chunk.text)
                        flushBuffers()
                    }

                    is AiStreamChunk.Complete -> {
                        flushBuffers(force = true)
                        updateSessionMessage(sessionId, assistantId) {
                            val cleanContent = stripGenUiBlocks(it.content)
                            it.copy(
                                content = cleanContent,
                                isStreaming = false,
                                liveStatus = null,
                                genUi = chunk.genUi ?: it.genUi,
                                generatedPdf = chunk.generatedPdf ?: it.generatedPdf,
                                generatedFile = chunk.generatedFile ?: it.generatedFile,
                                generatedPptx = chunk.generatedPptx ?: it.generatedPptx,
                                generatedDocx = chunk.generatedDocx ?: it.generatedDocx,
                                followUpSuggestions = chunk.suggestions.ifEmpty { it.followUpSuggestions },
                            )
                        }
                        sessionJobs.remove(sessionId)
                        _state.update { s ->
                            s.copy(isGenerating = sessionJobs[s.activeSessionId]?.isActive == true)
                        }
                        persistSessions()
                        if (sessionJobs.values.none { it.isActive }) {
                            CintraBackgroundService.stop(getApplication())
                        }
                        if (!com.cintra.app.CintraAppState.isInForeground) {
                            val doneTitle = when {
                                chunk.generatedPptx != null -> "عرض PowerPoint جاهز في Cintra AI"
                                chunk.generatedPdf != null -> "ملف PDF جاهز في Cintra AI"
                                chunk.generatedDocx != null -> "مستند Word جاهز في Cintra AI"
                                else -> "اكتمل رد Cintra AI"
                            }
                            val replySnippet = _state.value.sessions
                                .firstOrNull { it.id == sessionId }
                                ?.messages?.firstOrNull { it.id == assistantId }
                                ?.content.orEmpty()
                                .replace(Regex("```[\\s\\S]*?(?:```|$)"), "")
                                .replace(Regex("<[^>]+>"), " ")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            val doneBody = chunk.generatedPptx?.title
                                ?: chunk.generatedPdf?.title
                                ?: chunk.generatedDocx?.title
                                ?: replySnippet.take(90).ifBlank { "اضغط للعودة وعرض الإجابة كاملة" }
                            CintraBackgroundService.notifyDone(getApplication(), doneTitle, doneBody)
                        }
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        val activeId = _state.value.activeSessionId
        sessionJobs.remove(activeId)?.cancel()
        if (sessionJobs.values.none { it.isActive }) {
            CintraBackgroundService.stop(getApplication())
        }
        _state.update { s ->
            val newSessions = s.sessions.map { session ->
                if (session.id == activeId) {
                    session.copy(messages = session.messages.map {
                        if (it.isStreaming) it.copy(isStreaming = false) else it
                    })
                } else session
            }
            s.copy(sessions = newSessions, isGenerating = false)
        }
        persistSessions()
    }

    private fun stripGenUiBlocks(md: String): String =
        md.replace(Regex("```(?:genui-[a-z-]+|cintra-pdf|cintra-file|cintra-pptx|cintra-docx|cintra-suggestions)[\\s\\S]*?(?:```|$)"), "").trim()

    private inline fun updateSessionMessage(
        targetSessionId: String,
        messageId: String,
        crossinline transform: (ChatMessage) -> ChatMessage,
    ) {
        _state.update { s ->
            val newSessions = s.sessions.map { session ->
                if (session.id == targetSessionId) {
                    session.copy(messages = session.messages.map {
                        if (it.id == messageId) transform(it) else it
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

    override fun onCleared() {
        super.onCleared()
        CintraBackgroundService.stop(getApplication())
    }

    fun toggleTemporaryChat() {
        val current = _state.value
        val nextTemp = !current.isTemporaryChat
        if (nextTemp && current.messages.isNotEmpty()) {
            val fresh = ChatSession(id = "temp_${System.currentTimeMillis()}", title = "محادثة مؤقتة")
            _state.update {
                it.copy(
                    sessions = listOf(fresh) + it.sessions,
                    activeSessionId = fresh.id,
                    isTemporaryChat = true,
                    chatSearchOpen = false,
                    chatSearchQuery = "",
                )
            }
        } else {
            _state.update { it.copy(isTemporaryChat = nextTemp) }
        }
    }

    fun togglePinActiveChat() {
        val id = _state.value.activeSessionId
        _state.update { s ->
            val next = if (id in s.pinnedSessionIds) s.pinnedSessionIds - id else s.pinnedSessionIds + id
            val sorted = s.sessions.sortedByDescending { it.id in next }
            s.copy(pinnedSessionIds = next, sessions = sorted)
        }
    }

    fun deleteActiveChat() {
        val id = _state.value.activeSessionId
        sessionJobs.remove(id)?.cancel()
        val remaining = _state.value.sessions.filterNot { it.id == id }
        if (remaining.isEmpty()) {
            val fresh = ChatSession(id = "s_${System.currentTimeMillis()}", title = "محادثة جديدة")
            _state.update {
                it.copy(
                    sessions = listOf(fresh),
                    activeSessionId = fresh.id,
                    isTemporaryChat = false,
                    chatSearchOpen = false,
                    chatSearchQuery = "",
                )
            }
        } else {
            _state.update {
                it.copy(
                    sessions = remaining,
                    activeSessionId = remaining.first().id,
                    isTemporaryChat = false,
                    chatSearchOpen = false,
                    chatSearchQuery = "",
                )
            }
        }
        persistSessions()
    }

    fun toggleChatSearch() {
        _state.update {
            it.copy(
                chatSearchOpen = !it.chatSearchOpen,
                chatSearchQuery = if (it.chatSearchOpen) "" else it.chatSearchQuery,
            )
        }
    }

    fun onChatSearchChange(q: String) {
        _state.update { it.copy(chatSearchQuery = q) }
    }

    fun activeChatShareText(): String {
        val msgs = _state.value.messages
        return msgs.joinToString("\n\n") { m ->
            val who = if (m.isUser) "أنت" else (m.modelName ?: "Cintra")
            val body = m.content.replace(Regex("```[a-zA-Z0-9_-]*\\n([\\s\\S]*?)```"), "$1")
                .replace(Regex("<[^>]+>"), " ").replace(Regex("\\s{2,}"), " ").trim()
            "$who:\n$body"
        }
    }

    fun activeChatAttachments(): List<Attachment> =
        _state.value.messages.flatMap { it.attachments }
}
