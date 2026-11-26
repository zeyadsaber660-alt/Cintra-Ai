package com.cintra.app.data.network

import com.cintra.app.data.AiStreamService
import com.cintra.app.data.security.CintraSecureVault
import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import com.cintra.app.ui.model.ChecklistItem
import com.cintra.app.ui.model.CintraModelId
import com.cintra.app.ui.model.GenUi
import com.cintra.app.ui.model.GeneratedPdfDoc
import com.cintra.app.ui.model.MetricItem
import com.cintra.app.ui.model.VisualGridItem
import com.cintra.app.ui.model.WebPhotoItem
import com.cintra.app.utils.CintraPdfExporter
import kotlinx.coroutines.Dispatchers
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

        val endpoint = "${CintraSecureVault.resolveEndpoint()}/v1/chat/stream"
        val appToken = CintraSecureVault.resolveAppToken()
        val modelKey = when (request.model.id) {
            CintraModelId.PRO -> "cintra-pro"
            CintraModelId.THINK -> "cintra-think"
            CintraModelId.CORE -> "cintra-core"
            CintraModelId.VISION -> "cintra-vision"
        }

        val bodyJson = JSONObject().apply {
            put("model", modelKey)
            put("stream", true)
            put("tool", request.tool?.name?.lowercase() ?: JSONObject.NULL)
            put("autoWebSearch", autoWebSearchProvider())
            put("generativeUi", true)
            put("illustrativeImages", illustrativeImagesProvider())
            put("customInstructions", customInstructionsProvider())
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", request.prompt)
                })
            })
        }

        var conn: HttpURLConnection? = null
        val fullContent = StringBuilder()

        try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 90_000
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
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val errMsg = runCatching { JSONObject(errBody).optString("error") }.getOrDefault("")
                emit(
                    AiStreamChunk.ContentDelta(
                        if (errMsg.isNotBlank()) "تعذّر إكمال الطلب ($code): $errMsg"
                        else "تعذّر إكمال الطلب حالياً (رمز $code)."
                    )
                )
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

                        // Real Web Photos pushed by Cintra Worker
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
                            hasReasoning = true
                            emit(AiStreamChunk.ReasoningDelta(reasoning))
                        }

                        val content = delta.optString("content", "")
                        if (content.isNotEmpty()) {
                            if (hasReasoning && !reasoningDone) {
                                reasoningDone = true
                                emit(AiStreamChunk.ReasoningComplete)
                            }
                            fullContent.append(content)
                            emit(AiStreamChunk.ContentDelta(content))
                        }
                    }
                }
            }

            if (hasReasoning && !reasoningDone) {
                emit(AiStreamChunk.ReasoningComplete)
            }
            val finalText = fullContent.toString()
            emit(
                AiStreamChunk.Complete(
                    genUi = extractGenUiFromMarkdown(finalText),
                    generatedPdf = extractPdfFromMarkdown(finalText, request.prompt),
                )
            )
        } catch (_: Exception) {
            emit(AiStreamChunk.ContentDelta("تعذّر الاتصال بالإنترنت. تحقق من اتصالك وحاول مرة أخرى."))
            emit(AiStreamChunk.Complete(genUi = null))
        } finally {
            conn?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractPdfFromMarkdown(text: String, userPrompt: String): GeneratedPdfDoc? {
        // 1. Primary structured ```cintra-pdf ... ``` block emitted by AI
        Regex("```cintra-pdf\\s*([\\s\\S]*?)```").find(text)?.let { match ->
            val block = match.groupValues[1].trim()
            val parts = block.split("---HTML---", limit = 2)
            val metaText = parts.firstOrNull().orEmpty()
            val rawHtml = parts.getOrNull(1)?.trim().orEmpty()

            val title = Regex("^TITLE:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
                ?: "مستند Cintra AI"
            val fileName = Regex("^FILENAME:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
                ?.let { if (it.endsWith(".pdf", true)) it else "$it.pdf" }
                ?: "Cintra_Document.pdf"
            val summary = Regex("^SUMMARY:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
                ?: "ملف PDF عربي RTL جاهز للتحميل"

            if (rawHtml.isNotBlank()) {
                return GeneratedPdfDoc(
                    title = title,
                    fileName = fileName,
                    htmlContent = CintraPdfExporter.ensureFullArabicHtml(title, rawHtml),
                    summary = summary,
                )
            }
        }

        // 2. Smart fallback: if the user explicitly asked for a PDF file, auto-build a downloadable PDF card
        val askedForPdf = Regex("(pdf|بي دي اف|بي دي إف|ملف\\s+تقرير|ملف\\s+ملخص|فاتورة\\s+pdf)", RegexOption.IGNORE_CASE)
            .containsMatchIn(userPrompt)
        if (askedForPdf && text.length > 120) {
            val cleanMd = text.replace(Regex("```genui-(?:checklist|comparison|image|grid)[\\s\\S]*?```"), "").trim()
            val title = userPrompt.take(42).trim()
            return GeneratedPdfDoc(
                title = title,
                fileName = "Cintra_${System.currentTimeMillis() % 100000}.pdf",
                htmlContent = CintraPdfExporter.buildArabicHtmlFromMarkdown(title, cleanMd),
                summary = "تم تحويل الشرح تلقائياً إلى ملف PDF جاهز للتحميل",
            )
        }
        return null
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
}
