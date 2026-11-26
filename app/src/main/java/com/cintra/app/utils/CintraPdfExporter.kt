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
 * Free, zero-overhead Arabic RTL HTML + Tailwind CSS to PDF & multi-format exporter for Android.
 * Enables JavaScript in the offscreen Chromium WebView so Tailwind Play CDN compiles all
 * utility classes before crisp A4 multi-page PDF capture (off the UI thread).
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
     * Ensures any HTML document has `<html dir="rtl" lang="ar">`, fixed 794px A4 viewport,
     * Tailwind CSS Play CDN, Cairo/Tajawal Google Fonts, and clean offline fallback CSS.
     */
    fun ensureFullArabicHtml(title: String, rawHtml: String): String {
        val trimmed = rawHtml.trim()
        val viewportMeta = """<meta name="viewport" content="width=794, initial-scale=1.0, maximum-scale=1.0" />"""
        val tailwindScript = """<script src="https://cdn.tailwindcss.com"></script>"""
        val fontLinks = """<link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&family=Tajawal:wght@400;500;700&display=swap" rel="stylesheet" />"""
        val safeResetCss = """<style>html,body{width:794px;max-width:794px;margin:0 auto;box-sizing:border-box;overflow-x:hidden;font-family:'Cairo','Tajawal',sans-serif;-webkit-text-size-adjust:100%;background:#FFFFFF;color:#0F172A;}*{box-sizing:border-box;}table,tr,blockquote,.card,.callout{page-break-inside:avoid;break-inside:avoid;}</style>"""

        if (trimmed.contains("<html", ignoreCase = true) && trimmed.contains("</html>", ignoreCase = true)) {
            val hasTailwind = trimmed.contains("tailwindcss", ignoreCase = true)
            val inject = buildString {
                append("\n").append(viewportMeta)
                if (!hasTailwind) append("\n").append(tailwindScript)
                append("\n").append(fontLinks)
                append("\n").append(safeResetCss)
            }
            return if (trimmed.contains("<head", ignoreCase = true)) {
                trimmed.replaceFirst(Regex("<head[^>]*>", RegexOption.IGNORE_CASE), "$0$inject")
            } else {
                trimmed.replaceFirst(
                    Regex("<html[^>]*>", RegexOption.IGNORE_CASE),
                    "$0\n<head><meta charset=\"UTF-8\" />$inject</head>",
                )
            }
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
              <meta charset="UTF-8" />
              $viewportMeta
              $tailwindScript
              $fontLinks
              $safeResetCss
              <style>
                body { padding: 36px 42px; line-height: 1.85; font-size: 14.5px; }
                .hero { background: linear-gradient(135deg, #0F172A 0%, #0369A1 100%); color: #FFFFFF; padding: 24px 28px; border-radius: 18px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }
                .hero h1 { font-size: 22px; font-weight: 800; color: #FFFFFF; margin: 0; }
                .hero-badge { background: rgba(255,255,255,0.18); color: #FFFFFF; padding: 5px 14px; border-radius: 999px; font-size: 12px; font-weight: 700; }
                h2 { font-size: 17px; color: #0369A1; margin: 20px 0 8px; padding-bottom: 4px; border-bottom: 1.5px solid #E0F2FE; font-weight: 700; }
                h3 { font-size: 15px; color: #1E293B; margin: 14px 0 6px; font-weight: 700; }
                table { width: 100%; border-collapse: collapse; margin: 16px 0; border-radius: 10px; overflow: hidden; }
                th { background: #0F172A; color: #FFFFFF; padding: 10px 12px; text-align: right; font-weight: 700; font-size: 13px; }
                td { padding: 10px 12px; border-bottom: 1px solid #E2E8F0; font-size: 13.5px; }
                tr:nth-child(even) td { background: #F8FAFC; }
                blockquote { border-right: 4px solid #0284C7; background: #F0F9FF; margin: 14px 0; padding: 12px 16px; border-radius: 10px; color: #0C4A6E; }
                .doc-footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #E2E8F0; font-size: 11px; color: #64748B; text-align: center; }
              </style>
            </head>
            <body class="bg-white text-slate-900">
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
     * Renders Arabic RTL HTML + Tailwind CSS via Chromium WebView into a crisp A4 PDF
     * and writes directly to `Downloads/Cintra/` off the UI thread.
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

                val pageWidthPx = 794
                val pageHeightPx = 1123
                val pdfWidthPt = 595
                val pdfHeightPt = 842

                val activity = context.findActivity()
                val rootView = activity?.window?.decorView as? ViewGroup

                val webView = WebView(activity ?: context).apply {
                    setBackgroundColor(AndroidColor.WHITE)
                    alpha = 0f
                    translationX = -10_000f
                    setInitialScale(100)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
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
                        // Wait for Tailwind Play CDN + Google Fonts to finish layout
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
                        }, 1150L)
                    }
                }

                webView.loadDataWithBaseURL(
                    "https://cdn.tailwindcss.com",
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
        val mainHandler = Handler(Looper.getMainLooper())
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.defaultTextEncodingName = "utf-8"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                mainHandler.postDelayed({
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return@postDelayed
                    val jobName = fileName.removeSuffix(".pdf")
                    val adapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, adapter, attributes)
                    onReady()
                }, 900L)
            }
        }

        webView.loadDataWithBaseURL(
            "https://cdn.tailwindcss.com",
            ensureFullArabicHtml(fileName, htmlContent),
            "text/html",
            "UTF-8",
            null,
        )
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
          <script src="https://cdn.tailwindcss.com"></script>
          <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet" />
        </head>
        <body class="bg-white text-slate-900 p-10" style="font-family:'Cairo',sans-serif;">
          <div class="bg-gradient-to-l from-slate-900 to-sky-700 text-white p-6 rounded-2xl flex justify-between items-center mb-6">
            <div>
              <h1 class="text-2xl font-extrabold">تقرير تجريبي — Cintra AI</h1>
              <p class="text-sky-200 text-sm mt-1">تصميم احترافي بـ HTML + Tailwind CSS وخط Cairo</p>
            </div>
            <span class="bg-white/20 px-4 py-1.5 rounded-full text-sm font-bold">#CINTRA-2026</span>
          </div>
          <div class="grid grid-cols-3 gap-4 mb-6">
            <div class="bg-sky-50 border border-sky-200 p-4 rounded-xl"><div class="text-xs text-sky-700 font-semibold">المحرك</div><div class="text-lg font-bold mt-1">Tailwind + Chromium</div></div>
            <div class="bg-emerald-50 border border-emerald-200 p-4 rounded-xl"><div class="text-xs text-emerald-700 font-semibold">اللغة</div><div class="text-lg font-bold mt-1">عربي RTL كامل</div></div>
            <div class="bg-amber-50 border border-amber-200 p-4 rounded-xl"><div class="text-xs text-amber-700 font-semibold">الجودة</div><div class="text-lg font-bold mt-1">A4 فائقة الوضوح</div></div>
          </div>
          <table class="w-full border-collapse rounded-xl overflow-hidden text-sm">
            <thead><tr class="bg-slate-900 text-white"><th class="p-3 text-right">الميزة</th><th class="p-3 text-right">الوصف</th><th class="p-3 text-right">الحالة</th></tr></thead>
            <tbody>
              <tr class="border-b"><td class="p-3 font-bold">Tailwind CSS</td><td class="p-3">بطاقات وتدرجات وجداول احترافية</td><td class="p-3 text-emerald-700 font-bold">مفعّل</td></tr>
              <tr class="bg-slate-50 border-b"><td class="p-3 font-bold">رسومات SVG</td><td class="p-3">أشكال توضيحية ومخططات فين</td><td class="p-3 text-emerald-700 font-bold">مفعّل</td></tr>
            </tbody>
          </table>
        </body>
        </html>
    """.trimIndent()
}
