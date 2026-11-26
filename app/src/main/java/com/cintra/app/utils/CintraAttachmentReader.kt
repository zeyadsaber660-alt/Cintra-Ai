package com.cintra.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

data class ExtractedAttachment(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val textContent: String? = null,
    val imageDataUrl: String? = null,
    val pageCount: Int? = null,
)

/**
 * Reads real display names, text/code content, PDF page counts & text, and
 * resized Vision-ready JPEG base64 thumbnails from Android Storage Access Framework URIs.
 */
object CintraAttachmentReader {

    fun queryDisplayName(context: Context, uri: Uri, fallback: String): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { !it.startsWith("document:") } ?: fallback
    }

    suspend fun extract(context: Context, uri: Uri, isPhoto: Boolean): ExtractedAttachment = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        var displayName = if (isPhoto) "صورة.jpg" else "ملف"
        var sizeBytes = 0L

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (nIdx >= 0) c.getString(nIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    if (sIdx >= 0 && !c.isNull(sIdx)) sizeBytes = c.getLong(sIdx)
                }
            }
        }

        val lowerName = displayName.lowercase()

        // 1. Image attachment -> resize & encode as JPEG data URL for Vision models
        if (isPhoto || mime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp")) {
            val dataUrl = runCatching { encodeImageUri(context, uri) }.getOrNull()
            return@withContext ExtractedAttachment(
                displayName = displayName,
                mimeType = mime.ifBlank { "image/jpeg" },
                sizeBytes = sizeBytes,
                imageDataUrl = dataUrl,
            )
        }

        // 2. PDF attachment -> count pages via PdfRenderer + extract embedded text + render page 1 preview
        if (mime == "application/pdf" || lowerName.endsWith(".pdf")) {
            var pages: Int? = null
            var page1Image: String? = null
            runCatching {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pages = renderer.pageCount
                        if (renderer.pageCount > 0) {
                            renderer.openPage(0).use { page ->
                                val scale = (960f / page.width.coerceAtLeast(1)).coerceAtMost(2f)
                                val w = (page.width * scale).toInt().coerceAtLeast(1)
                                val h = (page.height * scale).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                Canvas(bmp).drawColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page1Image = bitmapToJpegDataUrl(bmp, 78)
                                bmp.recycle()
                            }
                        }
                    }
                }
            }
            val extractedPdfText = runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    extractBasicPdfText(stream.readBytes())
                }
            }.getOrNull()?.takeIf { it.length >= 24 }

            val summaryText = buildString {
                append("PDF File: $displayName")
                if (pages != null) append(" ($pages pages)")
                if (!extractedPdfText.isNullOrBlank()) {
                    append("\n\nExtracted Text:\n")
                    append(extractedPdfText.take(18_000))
                } else {
                    append("\n(Scanned or font-encoded PDF — page 1 preview image is attached for visual reading.)")
                }
            }

            return@withContext ExtractedAttachment(
                displayName = displayName,
                mimeType = "application/pdf",
                sizeBytes = sizeBytes,
                textContent = summaryText,
                imageDataUrl = page1Image,
                pageCount = pages,
            )
        }

        // 3. Text / Markdown / Code / JSON / CSV / HTML files -> read UTF-8 directly
        val text = runCatching {
            resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buf = CharArray(24_000)
                val read = reader.read(buf)
                if (read > 0) String(buf, 0, read) else ""
            }
        }.getOrNull().orEmpty()

        return@withContext ExtractedAttachment(
            displayName = displayName,
            mimeType = mime.ifBlank { "text/plain" },
            sizeBytes = sizeBytes,
            textContent = text.ifBlank { "(Binary file: $displayName, size=${sizeBytes} bytes)" },
        )
    }

    private fun encodeImageUri(context: Context, uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sample = 1
        while (opts.outWidth / sample > 1024 || opts.outHeight / sample > 1024) sample *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
        val dataUrl = bitmapToJpegDataUrl(bmp, 80)
        bmp.recycle()
        return dataUrl
    }

    private fun bitmapToJpegDataUrl(bitmap: Bitmap, quality: Int): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    /**
     * Lightweight pure-Kotlin PDF text extractor (handles uncompressed & FlateDecode text streams)
     * with zero extra APK dependencies.
     */
    private fun extractBasicPdfText(pdfBytes: ByteArray): String {
        val raw = String(pdfBytes, Charsets.ISO_8859_1)
        val out = StringBuilder()
        val streamRegex = Regex("stream\\r?\\n([\\s\\S]*?)\\r?\\nendstream")
        for (match in streamRegex.findAll(raw)) {
            val chunkBytes = match.groupValues[1].toByteArray(Charsets.ISO_8859_1)
            val decoded = tryInflate(chunkBytes) ?: match.groupValues[1]
            // Extract text inside (...) Tj / TJ operators
            val tjRegex = Regex("\\(([^()\\\\]{2,})\\)")
            for (tm in tjRegex.findAll(decoded)) {
                val s = tm.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\r", " ")
                    .replace("\\t", " ")
                    .trim()
                if (s.any { it.isLetterOrDigit() }) {
                    out.append(s).append(' ')
                }
            }
            if (out.length > 18_000) break
        }
        return out.toString().replace(Regex("\\s{2,}"), " ").trim()
    }

    private fun tryInflate(data: ByteArray): String? {
        return runCatching {
            val inflater = Inflater()
            inflater.setInput(data)
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var guard = 0
            while (!inflater.finished() && guard < 128) {
                val count = inflater.inflate(buf)
                if (count <= 0) break
                baos.write(buf, 0, count)
                guard++
            }
            inflater.end()
            if (baos.size() > 0) String(baos.toByteArray(), Charsets.UTF_8) else null
        }.getOrNull()
    }
}
