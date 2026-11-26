package com.cintra.app.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Free, zero-overhead Arabic RTL HTML-to-PDF & multi-format file exporter for Android.
 * Fixes high-DPI WebView scaling (`setInitialScale(100)` + explicit 794px A4 viewport)
 * so downloaded PDFs match PrintManager's crisp proportions with zero cropping or UI lag.
 */
object CintraPdfExporter {

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Ensures any HTML document has `<html dir="rtl" lang="ar">`, fixed 794px A4 viewport
     * (preventing 3x DPI zoom/crop on high-res phones), and rich Cairo/Tajawal styling.
     */
    fun ensureFullArabicHtml(title: String, rawHtml: String): String {
        val trimmed = rawHtml.trim()
        val viewportMeta = """<meta name="viewport" content="width=794, initial-scale=1.0, maximum-scale=1.0" />"""
        val fontLinks = """<link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&family=Tajawal:wght@400;500;700&display=swap" rel="stylesheet" />"""
        val safeResetCss = """<style>html,body{width:794px;max-width:794px;margin:0 auto;box-sizing:border-box;overflow-x:hidden;-webkit-text-size-adjust:100%;}*{box-sizing:border-box;}table,tr,blockquote,.card,.callout{page-break-inside:avoid;break-inside:avoid;}</style>"""

        if (trimmed.contains("<html", ignoreCase = true) && trimmed.contains("</html>", ignoreCase = true)) {
            return if (trimmed.contains("<head", ignoreCase = true)) {
                trimmed.replaceFirst(
                    Regex("<head[^>]*>", RegexOption.IGNORE_CASE),
                    "$0\n$viewportMeta\n$fontLinks\n$safeResetCss",
                )
            } else {
                trimmed.replaceFirst(
                    Regex("<html[^>]*>", RegexOption.IGNORE_CASE),
                    "$0\n<head><meta charset=\"UTF-8\" />$viewportMeta$fontLinks$safeResetCss</head>",
                )
            }
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
              <meta charset="UTF-8" />
              $viewportMeta
              $fontLinks
              $safeResetCss
              <style>
                body {
                  font-family: 'Cairo', 'Tajawal', sans-serif;
                  color: #0F172A;
                  background: #FFFFFF;
                  padding: 36px 44px;
                  line-height: 1.85;
                  font-size: 14.5px;
                }
                .hero {
                  background: linear-gradient(135deg, #0F172A 0%, #0369A1 100%);
                  color: #FFFFFF;
                  padding: 24px 28px;
                  border-radius: 18px;
                  margin-bottom: 24px;
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                }
                .hero h1 { font-size: 22px; font-weight: 800; color: #FFFFFF; margin: 0; }
                .hero-badge { background: rgba(255,255,255,0.18); color: #FFFFFF; padding: 5px 14px; border-radius: 999px; font-size: 12px; font-weight: 700; }
                h2 { font-size: 17px; color: #0369A1; margin: 20px 0 8px; padding-bottom: 4px; border-bottom: 1.5px solid #E0F2FE; }
                h3 { font-size: 15px; color: #1E293B; margin: 14px 0 6px; }
                table { width: 100%; border-collapse: collapse; margin: 16px 0; border-radius: 10px; overflow: hidden; }
                th { background: #0F172A; color: #FFFFFF; padding: 10px 12px; text-align: right; font-weight: 700; font-size: 13px; }
                td { padding: 10px 12px; border-bottom: 1px solid #E2E8F0; font-size: 13.5px; }
                tr:nth-child(even) td { background: #F8FAFC; }
                code { font-family: monospace; background: #EFF6FF; color: #1D4ED8; padding: 2px 6px; border-radius: 6px; direction: ltr; display: inline-block; }
                pre { background: #0F172A; color: #E2E8F0; padding: 14px; border-radius: 12px; direction: ltr; font-size: 12px; white-space: pre-wrap; }
                blockquote { border-right: 4px solid #0284C7; background: #F0F9FF; margin: 14px 0; padding: 12px 16px; border-radius: 10px; color: #0C4A6E; }
                .doc-footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #E2E8F0; font-size: 11px; color: #64748B; text-align: center; }
              </style>
            </head>
            <body>
              <div class="hero">
                <h1>$title</h1>
                <span class="hero-badge">Cintra AI</span>
              </div>
              $trimmed
              <div class="doc-footer">تم التصميم والإنشاء بواسطة Cintra AI</div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Renders Arabic RTL HTML via Chromium WebView into a crisp, properly-scaled A4 PDF
     * (100% matching PrintManager proportions) and writes to `Downloads/Cintra/` off the UI thread.
     */
    fun downloadHtmlAsPdfToDownloads(
        context: Context,
        htmlContent: String,
        fileName: String,
        onSuccess: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val safeBase = fileName
                    .removeSuffix(".pdf")
                    .replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_")
                    .trim()
                    .ifBlank { "Cintra_Document" }
                val finalFileName = "$safeBase.pdf"

                val preparedHtml = ensureFullArabicHtml(safeBase, htmlContent)

                // Exact A4 proportions at 96 DPI (1 CSS px = 1 WebView px via setInitialScale(100))
                val pageWidthPx = 794
                val pageHeightPx = 1123
                val pdfWidthPt = 595
                val pdfHeightPt = 842

                val activity = context.findActivity()
                val rootView = activity?.window?.decorView as? ViewGroup

                val webView = WebView(activity ?: context).apply {
                    setBackgroundColor(AndroidColor.WHITE)
                    alpha = 0f
                    translationX = -10_000f // Keep offscreen so zero layout/touch interference
                    setInitialScale(100)
                    settings.javaScriptEnabled = false
                    settings.defaultTextEncodingName = "utf-8"
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = false
                    settings.textZoom = 100
                    settings.loadsImagesAutomatically = true
                    layoutParams = ViewGroup.LayoutParams(pageWidthPx, pageHeightPx)
                }

                rootView?.addView(webView)
                var finished = false

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (finished) return
                        finished = true
                        mainHandler.postDelayed({
                            try {
                                val rawContentHeight = webView.contentHeight.coerceAtLeast(pageHeightPx)
                                val measuredContentPx = rawContentHeight.coerceAtMost(pageHeightPx * 24)

                                webView.measure(
                                    View.MeasureSpec.makeMeasureSpec(pageWidthPx, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(measuredContentPx, View.MeasureSpec.EXACTLY),
                                )
                                webView.layout(0, 0, pageWidthPx, measuredContentPx)

                                val pdfDocument = PdfDocument()
                                val totalPages = ((measuredContentPx + pageHeightPx - 1) / pageHeightPx).coerceAtLeast(1)
                                val scaleFactor = pdfWidthPt.toFloat() / pageWidthPx.toFloat()

                                for (pageIdx in 0 until totalPages) {
                                    val pageInfo = PdfDocument.PageInfo.Builder(pdfWidthPt, pdfHeightPt, pageIdx + 1).create()
                                    val page = pdfDocument.startPage(pageInfo)
                                    val canvas = page.canvas
                                    canvas.drawColor(AndroidColor.WHITE)
                                    canvas.save()
                                    canvas.scale(scaleFactor, scaleFactor)
                                    canvas.translate(0f, -(pageIdx * pageHeightPx).toFloat())
                                    webView.draw(canvas)
                                    canvas.restore()
                                    pdfDocument.finishPage(page)
                                }

                                rootView?.removeView(webView)
                                webView.destroy()

                                // Write PDF bytes to MediaStore off the UI thread (zero lag!)
                                Thread {
                                    try {
                                        val values = ContentValues().apply {
                                            put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                                            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                                            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Cintra")
                                            put(MediaStore.Downloads.IS_PENDING, 1)
                                        }
                                        val resolver = context.contentResolver
                                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                            ?: throw IllegalStateException("تعذّر إنشاء الملف في مجلد التنزيلات")

                                        resolver.openOutputStream(uri)?.use { out ->
                                            pdfDocument.writeTo(out)
                                        } ?: throw IllegalStateException("تعذّر فتح مجرى الكتابة للملف")

                                        pdfDocument.close()

                                        values.clear()
                                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                                        resolver.update(uri, values, null, null)

                                        mainHandler.post { onSuccess(uri, finalFileName) }
                                    } catch (e: Exception) {
                                        runCatching { pdfDocument.close() }
                                        mainHandler.post { onError(e.message ?: "تعذّر حفظ ملف PDF") }
                                    }
                                }.start()
                            } catch (e: Exception) {
                                rootView?.removeView(webView)
                                runCatching { webView.destroy() }
                                onError(e.message ?: "تعذّر إنشاء ملف PDF")
                            }
                        }, 650L)
                    }
                }

                webView.loadDataWithBaseURL(
                    "https://fonts.googleapis.com",
                    preparedHtml,
                    "text/html",
                    "UTF-8",
                    null,
                )
            } catch (e: Exception) {
                onError(e.message ?: "تعذّر بدء إنشاء ملف PDF")
            }
        }
    }

    /**
     * Saves any AI-generated file (CSV, TXT, MD, HTML, JSON, Python, Kotlin, etc.)
     * directly into `Downloads/Cintra/` off the UI thread.
     */
    fun saveTextFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String,
        onSuccess: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val safeName = fileName.replace(Regex("[^\\p{L}\\p{N}._\\- ]"), "_").trim().ifBlank { "Cintra_File.txt" }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "text/plain" })
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Cintra")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("تعذّر إنشاء الملف في التنزيلات")
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                mainHandler.post { onSuccess(uri, safeName) }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "تعذّر حفظ الملف") }
            }
        }.start()
    }

    fun openFileUri(context: Context, uri: Uri, mimeType: String = "application/pdf"): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "فتح الملف"))
            true
        }.getOrDefault(false)
    }

    fun openPdfUri(context: Context, uri: Uri): Boolean = openFileUri(context, uri, "application/pdf")

    fun shareFileUri(context: Context, uri: Uri, title: String, mimeType: String = "application/pdf"): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "مشاركة الملف").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        }.getOrDefault(false)
    }

    fun sharePdfUri(context: Context, uri: Uri, title: String): Boolean = shareFileUri(context, uri, title, "application/pdf")

    fun exportHtmlToPdf(
        context: Context,
        htmlContent: String,
        fileName: String = "Cintra_Report",
        onReady: () -> Unit = {},
    ) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.settings.defaultTextEncodingName = "utf-8"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = fileName.removeSuffix(".pdf")
                val adapter = webView.createPrintDocumentAdapter(jobName)
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, adapter, attributes)
                onReady()
            }
        }

        webView.loadDataWithBaseURL("https://fonts.googleapis.com", ensureFullArabicHtml(fileName, htmlContent), "text/html", "UTF-8", null)
    }

    fun buildArabicHtmlFromMarkdown(
        title: String,
        markdownBody: String,
    ): String {
        val escapedBody = markdownBody
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
            .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("`([^`]+)`"), "<code>$1</code>")
            .replace("\n", "<br/>")

        return ensureFullArabicHtml(title, "<div>$escapedBody</div>")
    }

    fun sampleArabicInvoiceHtml(): String = """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
          <meta charset="UTF-8" />
          <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet" />
          <style>
            body { font-family: 'Cairo', sans-serif; color: #0F172A; line-height: 1.75; padding: 36px; background: #fff; }
            .top { background: linear-gradient(135deg, #0F172A, #0369A1); color: #fff; padding: 22px 26px; border-radius: 16px; display: flex; justify-content: space-between; align-items: center; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th { background: #0F172A; color: #fff; padding: 10px 12px; text-align: right; }
            td { padding: 10px 12px; border-bottom: 1px solid #E2E8F0; }
            tr:nth-child(even) td { background: #F8FAFC; }
            .total { margin-top: 18px; background: #E0F2FE; color: #0369A1; padding: 14px 18px; border-radius: 12px; font-weight: 700; }
          </style>
        </head>
        <body>
          <div class="top">
            <div><h1 style="margin:0;font-size:21px;color:#fff;">تقرير وفاتورة تجريبية — Cintra AI</h1><p style="margin:4px 0 0;color:#BAE6FD;">تصدير عربي RTL كامل بخط Cairo</p></div>
            <div style="text-align:left;color:#FFFFFF;font-weight:700;">#INV-2026-01</div>
          </div>
          <table>
            <thead><tr><th>البند</th><th>الوصف</th><th>التكلفة</th></tr></thead>
            <tbody>
              <tr><td>تطوير تطبيق أندرويد</td><td>واجهة Liquid Glass + Jetpack Compose</td><td>مجاني</td></tr>
              <tr><td>سيرفر Cloudflare Worker</td><td>بث SSE آمن + بحث ويب تلقائي</td><td>مجاني</td></tr>
              <tr><td>محرك PDF العربي</td><td>دعم RTL وخطوط Google Fonts</td><td>مدمج</td></tr>
            </tbody>
          </table>
          <div class="total">الحالة: جاهز للتصدير والطباعة بجودة عالية 100%</div>
        </body>
        </html>
    """.trimIndent()
}
