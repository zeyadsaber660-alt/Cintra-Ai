package com.cintra.app.data.network

import com.cintra.app.data.AiStreamService
import com.cintra.app.data.security.CintraSecureVault
import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import com.cintra.app.ui.model.ChecklistItem
import com.cintra.app.ui.model.CintraModelId
import com.cintra.app.ui.model.GenUi
import com.cintra.app.ui.model.GeneratedGenericFile
import com.cintra.app.ui.model.GeneratedDocxDoc
import com.cintra.app.ui.model.GeneratedPdfDoc
import com.cintra.app.ui.model.GeneratedPptxDoc
import com.cintra.app.utils.DocxSectionData
import com.cintra.app.utils.PptxSlideData
import com.cintra.app.ui.model.MetricItem
import com.cintra.app.ui.model.VisualGridItem
import com.cintra.app.ui.model.WebPhotoItem
import com.cintra.app.utils.CintraPdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CintraWorkerStreamService(
    private val autoWebSearchProvider: () -> Boolean,
    private val illustrativeImagesProvider: () -> Boolean,
    private val customInstructionsProvider: () -> String,
) : AiStreamService {

    override fun streamReply(request: AiStreamRequest): Flow<AiStreamChunk> = flow {
        if (!CintraSecureVault.isProvisioned()) {
            emit(AiStreamChunk.ContentDelta("الخدمة قيد الصيانة المؤقتة. حاول مرة أخرى بعد قليل."))
            emit(AiStreamChunk.Complete(genUi = null))
            return@flow
        }

        val hasFiles = request.attachments.isNotEmpty()
        emit(
            AiStreamChunk.StatusUpdate(
                when {
                    hasFiles -> "جارٍ قراءة وتحليل المرفق..."
                    request.tool?.name == "WEB" -> "جارٍ البحث في الويب..."
                    request.tool?.name == "THINKING" -> "جارٍ التفكير العميق..."
                    else -> "جارٍ تحليل طلبك..."
                }
            )
        )

        val endpoint = "${CintraSecureVault.resolveEndpoint()}/v1/chat/stream"
        val appToken = CintraSecureVault.resolveAppToken()
        val modelKey = when (request.model.id) {
            CintraModelId.PRO -> "cintra-pro"
            CintraModelId.THINK -> "cintra-think"
            CintraModelId.CORE -> "cintra-core"
            CintraModelId.VISION -> "cintra-vision"
        }

        // Build enriched user message including extracted file text & Vision images
        val promptWithFiles = buildString {
            append(request.prompt)
            val textAttachments = request.attachments.filter { !it.extractedText.isNullOrBlank() }
            if (textAttachments.isNotEmpty()) {
                val perFileBudget = (28_000 / textAttachments.size).coerceAtLeast(6_000)
                append("\n\n--- ATTACHED FILES ---")
                textAttachments.forEach { att ->
                    append("\n[File: ${att.name}]")
                    if (att.pageCount != null) append(" (${att.pageCount} pages)")
                    append("\n")
                    append(att.extractedText?.take(perFileBudget).orEmpty())
                    append("\n--- END FILE ---")
                }
            }
        }

        val imageUrls = request.attachments.mapNotNull { it.imageDataUrl }.take(3)
        val userMessageObj = JSONObject().apply {
            put("role", "user")
            if (imageUrls.isNotEmpty()) {
                val contentParts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", promptWithFiles)
                    })
                    imageUrls.forEach { dataUrl ->
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply { put("url", dataUrl) })
                        })
                    }
                }
                put("content", contentParts)
            } else {
                put("content", promptWithFiles)
            }
        }

        val bodyJson = JSONObject().apply {
            put("model", modelKey)
            put("stream", true)
            put("tool", request.tool?.name?.lowercase() ?: JSONObject.NULL)
            put("autoWebSearch", autoWebSearchProvider() && !hasFiles)
            put("generativeUi", true)
            put("illustrativeImages", illustrativeImagesProvider() && !hasFiles)
            put("customInstructions", customInstructionsProvider())
            put("rawPrompt", request.prompt)
            put("messages", JSONArray().apply {
                request.history.forEach { msg ->
                    val cleaned = msg.content
                        .replace(Regex("```(?:genui-[a-z-]+|cintra-pdf|cintra-file)[\\s\\S]*?(?:```|$)"), "")
                        .trim()
                    if (cleaned.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("role", if (msg.isUser) "user" else "assistant")
                            put("content", cleaned.take(1_800))
                        })
                    }
                }
                put(userMessageObj)
            })
        }

        var conn: HttpURLConnection? = null
        val fullContent = StringBuilder()
        val fullReasoning = StringBuilder()

        var attempt = 0
        var succeeded = false
        while (!succeeded && attempt < 180) {
            attempt++
            fullContent.clear()
            fullReasoning.clear()
        try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 25_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                if (appToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $appToken")
                }
            }

            conn.outputStream.use { os ->
                os.write(bodyJson.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                if ((code == 502 || code == 503 || code == 504) && attempt < 3) {
                    emit(AiStreamChunk.StatusUpdate("جارٍ تنشيط الخادم وإعادة المحاولة..."))
                    delay(2_500L)
                    continue
                }
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val errMsg = runCatching { JSONObject(errBody).optString("error") }.getOrDefault("")
                val friendly = when {
                    code == 502 || code == 503 || code == 504 ->
                        "الخادم مشغول مؤقتاً الآن، اضغط إعادة للمحاولة."
                    errMsg.isNotBlank() -> errMsg
                    else -> "تعذّر إكمال الطلب حالياً، حاول مرة أخرى."
                }
                emit(AiStreamChunk.ContentDelta(friendly))
                emit(AiStreamChunk.Complete(genUi = null))
                return@flow
            }

            var hasReasoning = false
            var reasoningDone = false

            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty() || data == "[DONE]") break

                    runCatching {
                        val json = JSONObject(data)

                        // Live status step pushed by Cintra Worker
                        val statusText = json.optString("cintra_status", "")
                        if (statusText.isNotEmpty()) {
                            emit(AiStreamChunk.StatusUpdate(statusText))
                            return@runCatching
                        }

                        // Real Web Photos pushed by Cintra Worker (only for visual/travel topics)
                        val photosArr = json.optJSONArray("cintra_photos")
                        if (photosArr != null && photosArr.length() > 0) {
                            val photos = (0 until photosArr.length()).mapNotNull { i ->
                                val obj = photosArr.optJSONObject(i) ?: return@mapNotNull null
                                val u = obj.optString("url", "")
                                if (u.startsWith("https://")) {
                                    WebPhotoItem(
                                        url = u,
                                        title = obj.optString("title", ""),
                                        source = obj.optString("source", ""),
                                    )
                                } else null
                            }
                            if (photos.isNotEmpty()) emit(AiStreamChunk.WebPhotosReady(photos))
                            return@runCatching
                        }

                        val choices = json.optJSONArray("choices") ?: return@runCatching
                        if (choices.length() == 0) return@runCatching
                        val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return@runCatching

                        val reasoning = delta.optString("reasoning_content", "")
                            .ifEmpty { delta.optString("reasoning", "") }
                        if (reasoning.isNotEmpty()) {
                            if (!hasReasoning) {
                                emit(AiStreamChunk.StatusUpdate("جارٍ التفكير وترتيب الإجابة..."))
                            }
                            hasReasoning = true
                            fullReasoning.append(reasoning)
                            emit(AiStreamChunk.ReasoningDelta(reasoning))
                        }

                        val rawContent = delta.optString("content", "")
                        val content = sanitizeCjkIfNeeded(rawContent, request.prompt)
                        if (content.isNotEmpty()) {
                            if (hasReasoning && !reasoningDone) {
                                reasoningDone = true
                                emit(AiStreamChunk.ReasoningComplete)
                            }
                            fullContent.append(content)
                            emit(AiStreamChunk.ContentDelta(content))
                            if (fullContent.length > 300 && isClientLooping(fullContent.toString())) {
                                break
                            }
                        }
                    }
                }
            }

            if (hasReasoning && !reasoningDone) {
                emit(AiStreamChunk.ReasoningComplete)
            }
            val finalText = fullContent.toString()
            val pdfFromContent = extractPdfFromMarkdown(finalText)
            val rescuedPdf = if (pdfFromContent == null && finalText.isBlank()) {
                extractPdfFromMarkdown(fullReasoning.toString())
            } else null
            val finalPdf = pdfFromContent ?: rescuedPdf
            if (finalText.isBlank() && rescuedPdf != null) {
                emit(AiStreamChunk.ContentDelta("تم تجهيز ملف الـ PDF المطلوب بنجاح، يمكنك تحميله الآن:"))
            }
            succeeded = true
            emit(
                AiStreamChunk.Complete(
                    genUi = extractGenUiFromMarkdown(finalText),
                    generatedPdf = finalPdf,
                    generatedFile = extractGenericFileFromMarkdown(finalText, request.prompt),
                    generatedPptx = extractPptxFromMarkdown(finalText, request.prompt),
                    generatedDocx = extractDocxFromMarkdown(finalText),
                    suggestions = extractSuggestionsFromMarkdown(finalText),
                )
            )
        } catch (e: java.io.IOException) {
            emit(AiStreamChunk.StatusUpdate("جارٍ العمل والمتابعة تلقائياً..."))
            delay(3_000L)
        } catch (_: Exception) {
            emit(AiStreamChunk.StatusUpdate("جارٍ إعادة المحاولة تلقائياً..."))
            delay(3_000L)
        } finally {
            conn?.disconnect()
            conn = null
        }
        }
        if (!succeeded) {
            emit(AiStreamChunk.ContentDelta("انتهت مهلة انتظار عودة الاتصال. أعد المحاولة عند توفر الإنترنت."))
            emit(AiStreamChunk.Complete(genUi = null))
        }
    }.flowOn(Dispatchers.IO)

    private fun extractGenericFileFromMarkdown(text: String, userPrompt: String): GeneratedGenericFile? {
        Regex("```cintra-file\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            val block = match.groupValues[1].trim()
            val parts = block.split("---CONTENT---", limit = 2)
            val meta = parts.firstOrNull().orEmpty()
            val body = parts.getOrNull(1)?.trim().orEmpty()
            if (body.isBlank()) return null
            val title = Regex("^TITLE:\\s*(.+)$", RegexOption.MULTILINE).find(meta)?.groupValues?.get(1)?.trim() ?: "ملف جاهز للتحميل"
            val fileName = Regex("^FILENAME:\\s*(.+)$", RegexOption.MULTILINE).find(meta)?.groupValues?.get(1)?.trim() ?: "cintra_file.txt"
            val mime = Regex("^MIME:\\s*(.+)$", RegexOption.MULTILINE).find(meta)?.groupValues?.get(1)?.trim() ?: "text/plain"
            val summary = Regex("^SUMMARY:\\s*(.+)$", RegexOption.MULTILINE).find(meta)?.groupValues?.get(1)?.trim() ?: "ملف مُنشأ بواسطة Cintra AI"
            return GeneratedGenericFile(title, fileName, mime, body, summary)
        }

        // ONLY auto-create .txt or .json when user asked for a prompt / JSON / text file (never .md for normal chats)
        val askedForPdf = Regex("(pdf|بي دي اف|بي دي إف|pptx|powerpoint|presentation|بريزنتيشن|باوربوينت)", RegexOption.IGNORE_CASE).containsMatchIn(userPrompt)
        if (!askedForPdf && !text.contains("```cintra-pdf")) {
            val askedForJson = Regex("(json|جيسون)", RegexOption.IGNORE_CASE).containsMatchIn(userPrompt)
            val askedForPromptOrTxt = Regex("(برومبت|prompt|ملف\\s*نصي|ملف\\s*تكست|\\.txt)", RegexOption.IGNORE_CASE)
                .containsMatchIn(userPrompt)
            if ((askedForJson || askedForPromptOrTxt) && text.length >= 600) {
                val clean = text.replace(Regex("```(?:genui-[a-z-]+|cintra-suggestions)\\s*[\\s\\S]*?(?:```|$)"), "").trim()
                val isJson = askedForJson || (clean.startsWith("{") && clean.endsWith("}")) || (clean.startsWith("[") && clean.endsWith("]"))
                val ext = if (isJson) "json" else "txt"
                val mime = if (isJson) "application/json" else "text/plain"
                val stamp = System.currentTimeMillis() % 100000
                return GeneratedGenericFile(
                    title = if (isJson) "ملف JSON جاهز" else "ملف برومبت / نص (.txt)",
                    fileName = "Cintra_${stamp}.$ext",
                    mimeType = mime,
                    content = clean,
                    summary = "تم حفظ البرومبت/المحتوى تلقائياً بصيغة .$ext للتحميل",
                )
            }
        }
        return null
    }

    private fun extractSuggestionsFromMarkdown(text: String): List<String> {
        Regex("```cintra-suggestions\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            return runCatching {
                val arr = JSONArray(match.groupValues[1].trim())
                (0 until arr.length()).mapNotNull { i -> arr.optString(i).trim().takeIf { it.isNotEmpty() } }.take(3)
            }.getOrDefault(emptyList())
        }
        return emptyList()
    }

    private fun extractPptxFromMarkdown(text: String, userPrompt: String): GeneratedPptxDoc? {
        Regex("```cintra-pptx\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            return runCatching {
                val obj = JSONObject(match.groupValues[1].trim())
                val title = obj.optString("title", "Cintra Presentation")
                val fileName = obj.optString("fileName", "Cintra_Presentation.pptx")
                    .let { if (it.endsWith(".pptx", true)) it else "$it.pptx" }
                val summary = obj.optString("summary", "عرض تقديمي PowerPoint جاهز للتحميل")
                val tObj = obj.optJSONObject("theme")
                val theme = if (tObj != null) {
                    com.cintra.app.utils.PptxThemeData(
                        bg = tObj.optString("bg", "0F172A"),
                        slideBg = tObj.optString("slideBg", "F8FAFC"),
                        headerBg = tObj.optString("headerBg", tObj.optString("bg", "0F172A")),
                        accent = tObj.optString("accent", "0EA5E9"),
                        accent2 = tObj.optString("accent2", "10B981"),
                        cardBg = tObj.optString("cardBg", "FFFFFF"),
                        text = tObj.optString("text", "1E293B"),
                        subtext = tObj.optString("subtext", "64748B"),
                    )
                } else com.cintra.app.utils.PptxThemeData()
                val arr = obj.optJSONArray("slides") ?: return@runCatching null
                val slides = (0 until arr.length()).mapNotNull { i ->
                    val s = arr.optJSONObject(i) ?: return@mapNotNull null
                    val bArr = s.optJSONArray("bullets")
                    val bullets = if (bArr != null) (0 until bArr.length()).mapNotNull { j -> bArr.optString(j).takeIf { it.isNotBlank() } } else emptyList()
                    PptxSlideData(
                        title = s.optString("title", "Slide ${i + 1}"),
                        bullets = bullets,
                        layout = s.optString("layout", if (i == 0) "cover" else "cards"),
                        subtitle = s.optString("subtitle", ""),
                        leftTitle = s.optString("leftTitle", ""),
                        rightTitle = s.optString("rightTitle", ""),
                        accentOverride = s.optString("accent", "").ifBlank { null },
                    )
                }
                if (slides.isNotEmpty()) GeneratedPptxDoc(title, fileName, slides, summary, theme) else null
            }.getOrNull()
        }
        val askedForPptx = Regex("(pptx|powerpoint|presentation|بريزنتيشن|باوربوينت|عرض\\s*تقديمي)", RegexOption.IGNORE_CASE)
            .containsMatchIn(userPrompt)
        if (askedForPptx) {
            val slideMatches = Regex("(?:#{1,3}\\s*)?(?:Slide|شريحة)\\s*\\d+[:\\-\\s]*([^\\n]+)([\\s\\S]*?)(?=(?:#{1,3}\\s*)?(?:Slide|شريحة)\\s*\\d+|$)", RegexOption.IGNORE_CASE)
                .findAll(text)
                .toList()
            if (slideMatches.isNotEmpty()) {
                val slides = slideMatches.map { sm ->
                    val sTitle = sm.groupValues[1].replace("**", "").trim()
                    val bullets = sm.groupValues[2].lines()
                        .map { it.trim().removePrefix("-").removePrefix("*").removePrefix("•").replace("**", "").trim() }
                        .filter { it.length >= 4 && !it.startsWith("```") }
                        .take(5)
                    PptxSlideData(title = sTitle.ifBlank { "Slide" }, bullets = bullets)
                }
                val deckTitle = slides.firstOrNull()?.title ?: "Cintra Presentation"
                return GeneratedPptxDoc(
                    title = deckTitle,
                    fileName = "Cintra_Presentation.pptx",
                    slides = slides,
                    summary = "عرض تقديمي PowerPoint مصمم (${slides.size} شرائح)",
                )
            }
        }
        return null
    }

    private fun extractDocxFromMarkdown(text: String): GeneratedDocxDoc? {
        Regex("```cintra-docx\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            return runCatching {
                val obj = JSONObject(match.groupValues[1].trim())
                val title = obj.optString("title", "Cintra Document")
                val fileName = obj.optString("fileName", "Cintra_Document.docx")
                    .let { if (it.endsWith(".docx", true)) it else "$it.docx" }
                val summary = obj.optString("summary", "مستند Word جاهز للتحميل")
                val accent = obj.optString("accent", "0284C7")
                val arr = obj.optJSONArray("sections") ?: return@runCatching null
                val sections = (0 until arr.length()).mapNotNull { i ->
                    val s = arr.optJSONObject(i) ?: return@mapNotNull null
                    val pArr = s.optJSONArray("paragraphs")
                    val paras = if (pArr != null) (0 until pArr.length()).mapNotNull { j -> pArr.optString(j).takeIf { it.isNotBlank() } } else emptyList()
                    DocxSectionData(heading = s.optString("heading", ""), paragraphs = paras)
                }
                if (sections.isNotEmpty()) GeneratedDocxDoc(title, fileName, sections, summary, accent) else null
            }.getOrNull()
        }
        return null
    }

    private fun extractPdfFromMarkdown(text: String): GeneratedPdfDoc? {
        val match = Regex("```cintra-pdf\\s*([\\s\\S]*?)(?:```|$)").find(text) ?: return null
        val block = match.groupValues[1].trim()
        val parts = block.split("---HTML---", limit = 2)
        val metaText = parts.firstOrNull().orEmpty()
        val rawHtml = parts.getOrNull(1)?.trim().orEmpty()
        if (rawHtml.length < 40) return null

        val title = Regex("^TITLE:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
            ?: "مستند Cintra AI"
        val fileName = Regex("^FILENAME:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
            ?.let { if (it.endsWith(".pdf", true)) it else "$it.pdf" }
            ?: "Cintra_Document.pdf"
        val summary = Regex("^SUMMARY:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
            ?: "ملف PDF عربي RTL جاهز للتحميل"
        val template = Regex("^TEMPLATE:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
            ?: "study_guide"

        return GeneratedPdfDoc(
            title = title,
            fileName = fileName,
            htmlContent = CintraPdfExporter.buildFromTemplate(title, summary, rawHtml, template),
            summary = summary,
        )
    }

    private fun extractGenUiFromMarkdown(text: String): GenUi? {
        Regex("```genui-grid\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            return runCatching {
                val obj = JSONObject(match.groupValues[1].trim())
                val arr = obj.getJSONArray("items")
                val items = (0 until arr.length()).map { i ->
                    val it = arr.getJSONObject(i)
                    VisualGridItem(
                        title = it.getString("title"),
                        subtitle = it.optString("subtitle", ""),
                        tone = it.optString("tone", "blue"),
                    )
                }
                if (items.isNotEmpty()) GenUi.VisualGrid(obj.optString("title", ""), items) else null
            }.getOrNull()
        }

        Regex("```genui-checklist\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            return runCatching {
                val arr = JSONArray(match.groupValues[1].trim())
                val items = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    ChecklistItem(obj.getString("text"), obj.optBoolean("done", false))
                }
                if (items.isNotEmpty()) GenUi.Checklist(items) else null
            }.getOrNull()
        }

        Regex("```genui-comparison\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            return runCatching {
                val obj = JSONObject(match.groupValues[1].trim())
                val tabs = mutableMapOf<String, List<MetricItem>>()
                obj.keys().forEach { key ->
                    val arr = obj.getJSONArray(key)
                    tabs[key] = (0 until arr.length()).map { i ->
                        val item = arr.getJSONObject(i)
                        MetricItem(item.getString("label"), item.getInt("value"))
                    }
                }
                if (tabs.isNotEmpty()) GenUi.Comparison(activeTab = tabs.keys.first(), tabs = tabs) else null
            }.getOrNull()
        }

        Regex("```genui-image\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            val prompt = match.groupValues[1].trim()
            if (prompt.isNotEmpty()) return GenUi.GeneratedImage(prompt)
        }
        return null
    }

    private val cjkRegex = Regex("[\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF\u3000-\u303F]+")

    private fun sanitizeCjkIfNeeded(chunk: String, prompt: String): String {
        if (chunk.isEmpty() || !cjkRegex.containsMatchIn(chunk)) return chunk
        val wantsCjk = cjkRegex.containsMatchIn(prompt) ||
            Regex("(صيني|الصينية|ياباني|اليابانية|كوري|الكورية|chinese|japanese|korean)", RegexOption.IGNORE_CASE).containsMatchIn(prompt)
        return if (wantsCjk) chunk else chunk.replace(cjkRegex, "")
    }

    private fun isClientLooping(text: String): Boolean {
        val tail = if (text.length > 1200) text.takeLast(1200) else text
        for (len in intArrayOf(36, 60, 96)) {
            if (tail.length < len * 3) continue
            val c1 = tail.substring(tail.length - len)
            if (c1.trim().length < 18) continue
            val c2 = tail.substring(tail.length - len * 2, tail.length - len)
            val c3 = tail.substring(tail.length - len * 3, tail.length - len * 2)
            if (c1 == c2 && c2 == c3) return true
        }
        return false
    }
}
