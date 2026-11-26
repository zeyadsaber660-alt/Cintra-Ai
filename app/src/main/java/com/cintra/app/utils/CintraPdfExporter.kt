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
 * Free, zero-overhead Arabic RTL HTML-to-PDF engine for Android.
 *
 * Uses Android's built-in Chromium engine (WebView + PdfDocument / PrintManager) —
 * giving 100% accurate Arabic ligatures, RTL layout (`<html dir="rtl" lang="ar">`),
 * modern CSS tables/colors, and Google Fonts (Cairo / Tajawal), with direct one-tap
 * download into `Downloads/Cintra/` plus instant Open & Share.
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
     * Ensures any HTML snippet or full document has `<html dir="rtl" lang="ar">`
     * and Cairo Google Font styling for clean Arabic RTL PDF rendering.
     */
    fun ensureFullArabicHtml(title: String, rawHtml: String): String {
        val trimmed = rawHtml.trim()
        if (trimmed.contains("<html", ignoreCase = true) && trimmed.contains("</html>", ignoreCase = true)) {
            return trimmed
        }
        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
              <meta charset="UTF-8" />
              <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700&display=swap" rel="stylesheet" />
              <style>
                * { box-sizing: border-box; }
                body {
                  font-family: 'Cairo', 'Tajawal', sans-serif;
                  color: #0F172A;
                  background: #FFFFFF;
                  margin: 0;
                  padding: 36px 40px;
                  line-height: 1.85;
                  font-size: 14.5px;
                }
                .doc-header {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  border-bottom: 2px solid #0284C7;
                  padding-bottom: 14px;
                  margin-bottom: 24px;
                }
                .doc-title { font-size: 22px; font-weight: 700; color: #0F172A; margin: 0; }
                .doc-badge { background: #E0F2FE; color: #0369A1; padding: 5px 14px; border-radius: 999px; font-size: 12px; font-weight: 700; }
                h1 { font-size: 20px; color: #0F172A; margin: 18px 0 8px; }
                h2 { font-size: 17px; color: #0369A1; margin: 16px 0 6px; }
                h3 { font-size: 15px; color: #1E293B; margin: 12px 0 6px; }
                table { width: 100%; border-collapse: collapse; margin: 16px 0; }
                th { background: #F1F5F9; padding: 10px 12px; text-align: right; border-bottom: 2px solid #CBD5E1; font-weight: 700; }
                td { padding: 10px 12px; border-bottom: 1px solid #E2E8F0; }
                code { font-family: monospace; background: #F1F5F9; padding: 2px 6px; border-radius: 6px; direction: ltr; display: inline-block; }
                pre { background: #0F172A; color: #E2E8F0; padding: 14px; border-radius: 12px; direction: ltr; overflow-x: auto; font-size: 12.5px; }
                blockquote { border-right: 4px solid #0284C7; background: #F8FAFC; margin: 12px 0; padding: 10px 16px; border-radius: 8px; }
                .doc-footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #E2E8F0; font-size: 11px; color: #64748B; text-align: center; }
              </style>
            </head>
            <body>
              <div class="doc-header">
                <h1 class="doc-title">$title</h1>
                <span class="doc-badge">Cintra AI</span>
              </div>
              $trimmed
              <div class="doc-footer">تم إنشاء هذا المستند بواسطة Cintra AI</div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Renders Arabic RTL HTML via Chromium WebView into a real multi-page A4 PDF file
     * and saves it directly to `Downloads/Cintra/<fileName>.pdf` (no print dialog needed).
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

                val pageWidthPx = 794   // A4 width at 96 DPI
                val pageHeightPx = 1123 // A4 height at 96 DPI
                val pdfWidthPt = 595    // A4 width in PostScript points
                val pdfHeightPt = 842   // A4 height in PostScript points

                val activity = context.findActivity()
                val rootView = activity?.window?.decorView as? ViewGroup

                val webView = WebView(activity ?: context).apply {
                    setBackgroundColor(AndroidColor.WHITE)
                    alpha = 0.01f
                    settings.javaScriptEnabled = false
                    settings.defaultTextEncodingName = "utf-8"
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
                        // Allow Google Fonts & layout pass to settle
                        mainHandler.postDelayed({
                            try {
                                val measuredContentPx = (webView.contentHeight * webView.scale)
                                    .toInt()
                                    .coerceAtLeast(pageHeightPx)
                                    .coerceAtMost(pageHeightPx * 20)

                                webView.measure(
                                    View.MeasureSpec.makeMeasureSpec(pageWidthPx, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(measuredContentPx, View.MeasureSpec.EXACTLY),
                                )
                                webView.layout(0, 0, pageWidthPx, measuredContentPx)

                                val pdfDocument = PdfDocument()
                                val totalPages = ((measuredContentPx + pageHeightPx - 1) / pageHeightPx).coerceAtLeast(1)
                                val scaleX = pdfWidthPt.toFloat() / pageWidthPx.toFloat()

                                for (pageIdx in 0 until totalPages) {
                                    val pageInfo = PdfDocument.PageInfo.Builder(pdfWidthPt, pdfHeightPt, pageIdx + 1).create()
                                    val page = pdfDocument.startPage(pageInfo)
                                    val canvas = page.canvas
                                    canvas.save()
                                    canvas.scale(scaleX, scaleX)
                                    canvas.translate(0f, -(pageIdx * pageHeightPx).toFloat())
                                    webView.draw(canvas)
                                    canvas.restore()
                                    pdfDocument.finishPage(page)
                                }

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

                                rootView?.removeView(webView)
                                webView.destroy()
                                onSuccess(uri, finalFileName)
                            } catch (e: Exception) {
                                rootView?.removeView(webView)
                                runCatching { webView.destroy() }
                                onError(e.message ?: "تعذّر حفظ ملف PDF")
                            }
                        }, 750L)
                    }
                }

                webView.loadDataWithBaseURL(
                    "https://fonts.googleapis.com",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null,
                )
            } catch (e: Exception) {
                onError(e.message ?: "تعذّر بدء إنشاء ملف PDF")
            }
        }
    }

    fun openPdfUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "فتح ملف PDF"))
            true
        }.getOrDefault(false)
    }

    fun sharePdfUri(context: Context, uri: Uri, title: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "مشاركة ملف PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        }.getOrDefault(false)
    }

    /**
     * Converts any full Arabic RTL HTML document into a print/save-ready PDF via Android PrintManager.
     */
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

        webView.loadDataWithBaseURL("https://fonts.googleapis.com", htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Wraps any Cintra Markdown response into a styled Arabic RTL HTML document
     * with Google Fonts (Cairo) ready for crisp PDF export.
     */
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

    /**
     * Built-in sample Arabic RTL report/invoice HTML to test PDF export immediately.
     */
    fun sampleArabicInvoiceHtml(): String = """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
          <meta charset="UTF-8" />
          <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700&display=swap" rel="stylesheet" />
          <style>
            @page { size: A4; margin: 16mm; }
            body { font-family: 'Cairo', sans-serif; color: #0F172A; line-height: 1.75; padding: 32px; background: #fff; }
            .top { display: flex; justify-content: space-between; border-bottom: 2px solid #0284C7; padding-bottom: 12px; }
            table { width: 100%; border-collapse: collapse; margin-top: 18px; }
            th { background: #F1F5F9; padding: 10px; text-align: right; border-bottom: 2px solid #CBD5E1; }
            td { padding: 10px; border-bottom: 1px solid #E2E8F0; }
            .total { margin-top: 18px; background: #E0F2FE; color: #0369A1; padding: 12px 16px; border-radius: 12px; font-weight: 700; }
          </style>
        </head>
        <body>
          <div class="top">
            <div><h1 style="margin:0;font-size:22px;">تقرير وفاتورة تجريبية — Cintra AI</h1><p style="margin:4px 0 0;color:#64748B;">تصدير عربي RTL كامل بخط Cairo</p></div>
            <div style="text-align:left;color:#0284C7;font-weight:700;">#INV-2026-01</div>
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
