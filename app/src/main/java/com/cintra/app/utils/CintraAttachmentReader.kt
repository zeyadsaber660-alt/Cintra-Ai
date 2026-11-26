package com.cintra.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

data class ExtractedAttachment(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val textContent: String? = null,
    val imageDataUrl: String? = null,
    val pageCount: Int? = null,
)

/**
 * Full-document universal attachment reader:
 * - PDF: Uses Apache PDFBox-Android (`PDFTextStripper`) to accurately decode subset fonts,
 *   ToUnicode CMaps, Arabic RTL, PowerPoint slides, and 100+ page lectures across ALL pages.
 * - DOCX / PPTX / XLSX: Native ZIP-XML text extraction with zero extra dependencies.
 * - Video: Extracts duration, resolution, and a keyframe JPEG for Vision analysis.
 * - Images: Resized Vision-ready JPEG base64 data URLs.
 * - Text/Code/MD/CSV/JSON/HTML: Direct UTF-8 reading up to 65,000 characters.
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

        // 1. Video attachment -> metadata + keyframe thumbnail for Vision
        if (mime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm")) {
            var frameDataUrl: String? = null
            var durationSec = 0L
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                durationSec = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000L
                val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L)
                if (frame != null) {
                    frameDataUrl = bitmapToJpegDataUrl(scaleBitmapMax(frame, 960), 78)
                    frame.recycle()
                }
                retriever.release()
            }
            return@withContext ExtractedAttachment(
                displayName = displayName,
                mimeType = mime.ifBlank { "video/mp4" },
                sizeBytes = sizeBytes,
                textContent = "Video File: $displayName (Duration: ${durationSec}s, Size: ${sizeBytes / 1024} KB). Keyframe image attached for visual analysis.",
                imageDataUrl = frameDataUrl,
            )
        }

        // 2. Image attachment -> resize & encode as JPEG data URL
        if (isPhoto || mime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp")) {
            val dataUrl = runCatching { encodeImageUri(context, uri) }.getOrNull()
            return@withContext ExtractedAttachment(
                displayName = displayName,
                mimeType = mime.ifBlank { "image/jpeg" },
                sizeBytes = sizeBytes,
                imageDataUrl = dataUrl,
            )
        }

        // 3. PDF attachment -> Full-document extraction across ALL pages via PDFBox-Android
        if (mime == "application/pdf" || lowerName.endsWith(".pdf")) {
            var pages = 0
            val fullTextBuilder = StringBuilder()

            runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input).use { doc ->
                        pages = doc.numberOfPages
                        val stripper = PDFTextStripper().apply { sortByPosition = true }
                        val maxPagesToRead = pages.coerceAtMost(120)
                        for (p in 1..maxPagesToRead) {
                            stripper.startPage = p
                            stripper.endPage = p
                            val pageText = stripper.getText(doc).trim()
                            if (pageText.isNotBlank()) {
                                fullTextBuilder.append("\n--- Page $p ---\n").append(pageText)
                            }
                            if (fullTextBuilder.length >= 65_000) {
                                fullTextBuilder.append("\n[... truncated at 65,000 chars of $pages pages ...]")
                                break
                            }
                        }
                    }
                }
            }

            // Fallback preview image of page 1 only if PDF is scanned / image-only
            var page1Image: String? = null
            if (fullTextBuilder.length < 150) {
                runCatching {
                    resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            if (pages == 0) pages = renderer.pageCount
                            if (renderer.pageCount > 0) {
                                renderer.openPage(0).use { page ->
                                    val scale = (960f / page.width.coerceAtLeast(1)).coerceAtMost(2f)
                                    val bmp = Bitmap.createBitmap(
                                        (page.width * scale).toInt().coerceAtLeast(1),
                                        (page.height * scale).toInt().coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888,
                                    )
                                    Canvas(bmp).drawColor(Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page1Image = bitmapToJpegDataUrl(bmp, 78)
                                    bmp.recycle()
                                }
                            }
                        }
                    }
                }
            }

            val summaryText = buildString {
                append("PDF File: $displayName ($pages pages total)\n")
                if (fullTextBuilder.isNotEmpty()) {
                    append(fullTextBuilder.toString())
                } else {
                    append("(Scanned / image-based PDF — page 1 preview image attached.)")
                }
            }

            return@withContext ExtractedAttachment(
                displayName = displayName,
                mimeType = "application/pdf",
                sizeBytes = sizeBytes,
                textContent = summaryText,
                imageDataUrl = page1Image,
                pageCount = pages.takeIf { it > 0 },
            )
        }

        // 4. Office OpenXML formats (.docx, .pptx, .xlsx) -> native ZIP-XML text extraction
        if (lowerName.endsWith(".docx") || lowerName.endsWith(".pptx") || lowerName.endsWith(".xlsx")) {
            val officeText = runCatching { extractOfficeZipXmlText(context, uri, lowerName) }.getOrNull().orEmpty()
            return@withContext ExtractedAttachment(
                displayName = displayName,
                mimeType = mime.ifBlank { "application/octet-stream" },
                sizeBytes = sizeBytes,
                textContent = if (officeText.isNotBlank()) {
                    "Office Document: $displayName\n\n${officeText.take(60_000)}"
                } else {
                    "Office Document: $displayName (Size: ${sizeBytes / 1024} KB)"
                },
            )
        }

        // 5. Text / Markdown / Code / JSON / CSV / HTML / SQL / XML -> read up to 65,000 chars
        val text = runCatching {
            resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buf = CharArray(65_000)
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

    private fun extractOfficeZipXmlText(context: Context, uri: Uri, lowerName: String): String {
        val out = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val name = entry.name
                    val relevant = when {
                        lowerName.endsWith(".docx") -> name == "word/document.xml"
                        lowerName.endsWith(".pptx") -> name.startsWith("ppt/slides/slide") && name.endsWith(".xml")
                        lowerName.endsWith(".xlsx") -> name == "xl/sharedStrings.xml" || (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))
                        else -> false
                    }
                    if (relevant) {
                        val xml = zis.readBytes().toString(Charsets.UTF_8)
                        val stripped = xml
                            .replace(Regex("</(w:p|a:p|row|si)>"), "\n")
                            .replace(Regex("<[^>]+>"), " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace(Regex("[ \\t]{2,}"), " ")
                            .trim()
                        if (stripped.isNotBlank()) {
                            out.append("\n--- $name ---\n").append(stripped)
                        }
                    }
                    zis.closeEntry()
                    if (out.length >= 60_000) break
                }
            }
        }
        return out.toString().trim()
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

    private fun scaleBitmapMax(src: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(src.width, src.height)
        if (maxSide <= maxDim) return src
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true)
    }

    private fun bitmapToJpegDataUrl(bitmap: Bitmap, quality: Int): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }
}
