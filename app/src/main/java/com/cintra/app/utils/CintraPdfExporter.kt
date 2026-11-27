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
 * Token-efficient Arabic RTL HTML + Tailwind CSS + KaTeX PDF Engine.
 *
 * Includes 4 built-in magazine-grade templates (`study_guide`, `report`, `invoice`, `cheat_sheet`)
 * so the AI only needs to emit compact body content (saving ~70% output tokens) while getting
 * Tailwind CSS, Cairo/Tajawal fonts, and KaTeX math rendering automatically.
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
     * Wraps compact HTML body into a full, pre-styled Arabic RTL template with
     * Tailwind CSS CDN, Cairo/Tajawal fonts, and KaTeX auto-render for math.
     */

    private fun formatHtmlExponents(html: String): String {
        var s = html
        s = s.replace(Regex("([A-Za-z0-9)}\u0600-\u06FF])\\^\\(([^()]{1,12})\\)"), "$1<sup>$2</sup>")
        s = s.replace(Regex("([A-Za-z0-9)}\u0600-\u06FF])\\^\\{([^\\{\\}]{1,12})\\}"), "$1<sup>$2</sup>")
        s = s.replace(Regex("([A-Za-z0-9)}])\\^(-?[A-Za-z0-9]{1,4})\\b"), "$1<sup>$2</sup>")
        val entityFixes = listOf(
            "&mathbb;N" to "ℕ", "&mathbb;Z" to "ℤ", "&mathbb;Q" to "ℚ",
            "&mathbb;R" to "ℝ", "&mathbb;C" to "ℂ", "&mathcal;P" to "𝒫",
            "\\mathbb{N}" to "ℕ", "\\mathbb{Z}" to "ℤ", "\\mathbb{Q}" to "ℚ",
            "\\mathbb{R}" to "ℝ", "\\mathbb{C}" to "ℂ", "\\mathcal{P}" to "𝒫",
            "\\{" to "\u007B", "\\}" to "\u007D",
            "\\qquad" to "  ", "\\quad" to "  ", "\\;" to " ", "\\," to " ", "\\:" to " ", "\\!" to "",
            "\\cup" to "∪", "\\cap" to "∩", "\\subseteq" to "⊆", "\\subset" to "⊂",
            "\\in" to "∈", "\\notin" to "∉", "\\varnothing" to "∅", "\\emptyset" to "∅",
            "\\neq" to "≠", "\\leq" to "≤", "\\geq" to "≥", "\\times" to "×",
            "\\forall" to "∀", "\\exists" to "∃", "\\iff" to "⇔", "\\implies" to "⇒",
        )
        for ((k, v) in entityFixes) s = s.replace(k, v)
        // Convert residual Markdown inside PDF HTML into clean styled HTML
        s = s.replace(Regex("```[a-zA-Z0-9_-]*\\n([\\s\\S]*?)```")) { m ->
            "<pre style=\"direction:ltr;background:#0F172A;color:#E2E8F0;padding:12px 14px;border-radius:10px;font-family:monospace;font-size:12px;white-space:pre-wrap;margin:10px 0;\">" +
                m.groupValues[1].replace("<", "&lt;").replace(">", "&gt;") + "</pre>"
        }
        s = s.replace(Regex("\\*\\*([^*\\n]+?)\\*\\*"), "<strong>$1</strong>")
        s = s.replace(Regex("`([^`\\n]+?)`"), "<code style=\"direction:ltr;display:inline-block;background:#EFF6FF;color:#1D4ED8;padding:1px 6px;border-radius:6px;font-family:monospace;font-size:12.5px;\">$1</code>")
        return s
    }

    fun buildFromTemplate(
        title: String,
        subtitle: String,
        bodyHtml: String,
        template: String = "study_guide",
    ): String {
        val cleanBody = formatHtmlExponents(bodyHtml)
        if (cleanBody.contains("<html", ignoreCase = true) && cleanBody.contains("</html>", ignoreCase = true)) {
            return ensureFullArabicHtml(title, cleanBody)
        }
        val (heroGrad, accentHex, badgeLabel) = when (template.lowercase().trim()) {
            "invoice" -> Triple("from-slate-900 via-emerald-900 to-teal-700", "#059669", "فاتورة رسمية")
            "report" -> Triple("from-slate-900 via-slate-800 to-sky-800", "#0284C7", "تقرير احترافي")
            "cheat_sheet" -> Triple("from-indigo-950 via-slate-900 to-sky-900", "#0369A1", "ورقة مراجعة سريعة")
            else -> Triple("from-slate-900 via-sky-900 to-cyan-700", "#0284C7", "ملخص دراسي • Cintra AI")
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=794, initial-scale=1.0, maximum-scale=1.0" />
              <script src="https://cdn.tailwindcss.com"></script>
              <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&family=Tajawal:wght@400;500;700&display=swap" rel="stylesheet" />
              <style>
                html, body { width: 794px; max-width: 794px; margin: 0 auto; background: #FFFFFF; color: #0F172A; font-family: 'Cairo', 'Tajawal', sans-serif; line-height: 1.85; font-size: 14px; -webkit-text-size-adjust: 100%; }
                * { box-sizing: border-box; }
                .hero-fallback { background: linear-gradient(135deg, #0F172A 0%, $accentHex 100%); color: #FFFFFF; padding: 26px 30px; border-radius: 18px; margin-bottom: 22px; }
                h2 { font-size: 17px; font-weight: 800; color: #0369A1; background: #F0F9FF; border-right: 4px solid $accentHex; padding: 8px 14px; border-radius: 10px; margin: 22px 0 12px; }
                h3 { font-size: 15px; font-weight: 700; color: #1E293B; margin: 14px 0 6px; }
                table { width: 100%; border-collapse: collapse; margin: 14px 0; border-radius: 12px; overflow: hidden; font-size: 13px; }
                th { background: #0F172A; color: #FFFFFF; padding: 10px 12px; text-align: right; font-weight: 700; }
                td { padding: 9px 12px; border-bottom: 1px solid #E2E8F0; }
                tr:nth-child(even) td { background: #F8FAFC; }
                .math { direction: ltr; unicode-bidi: isolate; display: inline-block; font-family: Georgia, serif; background: #F1F5F9; border: 1px solid #E2E8F0; padding: 1px 7px; border-radius: 6px; margin: 0 2px; font-size: 13.5px; vertical-align: middle; }
                .math-block { direction: ltr; unicode-bidi: isolate; display: block; font-family: Georgia, serif; background: #F8FAFC; border: 1px solid #E2E8F0; padding: 9px 14px; border-radius: 10px; text-align: center; margin: 10px 0; font-size: 15px; }
                sup { font-size: 0.72em; line-height: 0; position: relative; vertical-align: baseline; top: -0.45em; }
                .box, blockquote { background: #F0F9FF; border-right: 4px solid $accentHex; padding: 12px 16px; border-radius: 10px; margin: 12px 0; }
                .box-green { background: #ECFDF5; border-right: 4px solid #10B981; padding: 12px 16px; border-radius: 10px; margin: 12px 0; }
                .box-amber { background: #FFFBEB; border-right: 4px solid #F59E0B; padding: 12px 16px; border-radius: 10px; margin: 12px 0; }
                thead { display: table-header-group; } tr, .math-block { page-break-inside: avoid; break-inside: avoid; }
              </style>
            </head>
            <body class="bg-white text-slate-900 p-9">
              <div class="hero-fallback bg-gradient-to-l $heroGrad text-white p-7 rounded-2xl mb-6 flex justify-between items-center">
                <div>
                  <span class="inline-block bg-white/20 text-white text-xs font-bold px-3 py-1 rounded-full mb-2">$badgeLabel</span>
                  <h1 class="text-2xl font-extrabold m-0 text-white">$title</h1>
                  ${if (subtitle.isNotBlank()) """<p class="text-sky-100 text-sm mt-1 mb-0">$subtitle</p>""" else ""}
                </div>
              </div>
              <div class="space-y-4">
                $cleanBody
              </div>
              <div class="mt-8 pt-4 border-t border-slate-200 text-center text-xs text-slate-400">
                تم التصميم والتنسيق بواسطة Cintra AI
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun ensureFullArabicHtml(title: String, rawHtml: String): String {
        val trimmed = formatHtmlExponents(rawHtml.trim())
        val viewportMeta = """<meta name="viewport" content="width=794, initial-scale=1.0, maximum-scale=1.0" />"""
        val tailwindScript = """<script src="https://cdn.tailwindcss.com"></script>"""
        val fontLinks = """<link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&family=Tajawal:wght@400;500;700&display=swap" rel="stylesheet" />"""
        val safeResetCss = """<style>html,body{width:794px;max-width:794px;margin:0 auto;box-sizing:border-box;overflow-x:hidden;font-family:'Cairo','Tajawal',sans-serif;-webkit-text-size-adjust:100%;background:#FFFFFF;color:#0F172A;}*{box-sizing:border-box;}thead{display:table-header-group;}tr,.math-block{page-break-inside:avoid;break-inside:avoid;}sup{font-size:0.72em;vertical-align:super;line-height:0;}</style>"""

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
        return buildFromTemplate(title = title, subtitle = "", bodyHtml = trimmed, template = "study_guide")
    }

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
                        mainHandler.postDelayed({
                            // Smart JS page-break protector: pushes any box/row/heading crossing a 1123px boundary cleanly to next page
                            val paginateJs = """
                                (function(){
                                  var H = 1123, M = 34;
                                  var els = document.querySelectorAll('h2,h3,p,tr,.math-block,.box,.box-green,.box-amber');
                                  for (var i = 0; i < els.length; i++) {
                                    var el = els[i];
                                    var r = el.getBoundingClientRect();
                                    var h = r.height;
                                    if (h <= 0 || h > 280) continue;
                                    var top = r.top + window.scrollY;
                                    var bottom = top + h;
                                    var pageIdx = Math.floor(top / H);
                                    var pageEnd = (pageIdx + 1) * H - M;
                                    if (bottom > pageEnd) {
                                      var push = ((pageIdx + 1) * H + M) - top;
                                      var cur = parseFloat(getComputedStyle(el).marginTop) || 0;
                                      el.style.marginTop = (cur + push) + 'px';
                                    }
                                  }
                                  return Math.ceil(document.body.scrollHeight);
                                })();
                            """.trimIndent()
                            webView.evaluateJavascript(paginateJs) { jsHeightStr ->
                                mainHandler.postDelayed({
                            try {
                                val jsHeight = jsHeightStr?.trim('"')?.toDoubleOrNull()?.toInt() ?: 0
                                val rawContentHeight = maxOf(webView.contentHeight, jsHeight, pageHeightPx)
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
                                }, 180L)
                            }
                        }, 1050L)
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

        return buildFromTemplate(title = title, subtitle = "", bodyHtml = "<div>$escapedBody</div>")
    }

    fun sampleArabicInvoiceHtml(): String = buildFromTemplate(
        title = "تقرير وفاتورة تجريبية — Cintra AI",
        subtitle = "قالب Tailwind CSS مدمج لتوفير التوكنز بأعلى جودة طباعة",
        template = "invoice",
        bodyHtml = """
            <div class="grid grid-cols-3 gap-4 mb-5">
              <div class="bg-sky-50 border border-sky-200 p-4 rounded-xl"><div class="text-xs text-sky-700 font-bold">المحرك</div><div class="text-base font-extrabold mt-1">Tailwind + Chromium</div></div>
              <div class="bg-emerald-50 border border-emerald-200 p-4 rounded-xl"><div class="text-xs text-emerald-700 font-bold">توفير التوكنز</div><div class="text-base font-extrabold mt-1">حتى 70% أسرع</div></div>
              <div class="bg-amber-50 border border-amber-200 p-4 rounded-xl"><div class="text-xs text-amber-700 font-bold">الاتجاه</div><div class="text-base font-extrabold mt-1">عربي RTL كامل</div></div>
            </div>
            <table>
              <thead><tr><th>البند</th><th>الوصف</th><th>الحالة</th></tr></thead>
              <tbody>
                <tr><td>قوالب مدمجة</td><td>Study Guide / Report / Invoice / Cheat Sheet</td><td>مفعّل</td></tr>
                <tr><td>رسومات SVG</td><td>أشكال Venn ومخططات توضيحية</td><td>مفعّل</td></tr>
              </tbody>
            </table>
        """.trimIndent(),
    )
}
