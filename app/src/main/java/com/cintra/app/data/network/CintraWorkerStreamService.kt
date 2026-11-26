package com.cintra.app.data.network

import com.cintra.app.data.AiStreamService
import com.cintra.app.data.security.CintraSecureVault
import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import com.cintra.app.ui.model.ChecklistItem
import com.cintra.app.ui.model.CintraModelId
import com.cintra.app.ui.model.GenUi
import com.cintra.app.ui.model.MetricItem
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
                        else "تعذّر إكمال الطلب حالياً (رمز $code). تأكد من إضافة مفتاح API للنموذج في لوحة التحكم."
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
            emit(AiStreamChunk.Complete(genUi = extractGenUiFromMarkdown(fullContent.toString())))
        } catch (_: Exception) {
            emit(AiStreamChunk.ContentDelta("تعذّر الاتصال بالإنترنت. تحقق من اتصالك وحاول مرة أخرى."))
            emit(AiStreamChunk.Complete(genUi = null))
        } finally {
            conn?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractGenUiFromMarkdown(text: String): GenUi? {
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
