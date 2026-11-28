package com.cintra.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class PptxThemeData(
    val bg: String = "0F172A",
    val slideBg: String = "F8FAFC",
    val headerBg: String = "0F172A",
    val accent: String = "0EA5E9",
    val accent2: String = "10B981",
    val cardBg: String = "FFFFFF",
    val text: String = "1E293B",
    val subtext: String = "64748B",
)

data class PptxSlideData(
    val title: String,
    val bullets: List<String>,
    val layout: String = "cards", // cover, cards, two_column, stats, timeline, bullets, closing
    val subtitle: String = "",
    val leftTitle: String = "",
    val rightTitle: String = "",
    val accentOverride: String? = null,
)

data class DocxSectionData(
    val heading: String,
    val paragraphs: List<String>,
)

/**
 * Pure-Kotlin Creative OpenXML generator for `.pptx` (PowerPoint) and `.docx` (Word).
 * v25: Zero rigid theme lock-in — dynamically applies the model's chosen color palette
 * and per-slide layout (`cover`, `cards`, `two_column`, `stats`, `timeline`, `bullets`, `closing`)
 * with full Arabic RTL & English LTR support.
 */
object CintraOfficeExporter {

    private fun xmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun cleanHex(raw: String?, fallback: String): String {
        val c = raw.orEmpty().trim().removePrefix("#").uppercase()
        return if (c.length == 6 && c.all { it in '0'..'9' || it in 'A'..'F' }) c else fallback
    }

    private fun isDarkHex(hex: String): Boolean {
        val h = cleanHex(hex, "0F172A")
        val r = h.substring(0, 2).toInt(16)
        val g = h.substring(2, 4).toInt(16)
        val b = h.substring(4, 6).toInt(16)
        val luma = (0.299 * r + 0.587 * g + 0.114 * b)
        return luma < 140.0
    }

    fun savePptxToDownloads(
        context: Context,
        title: String,
        slides: List<PptxSlideData>,
        fileName: String,
        theme: PptxThemeData = PptxThemeData(),
        onSuccess: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val safeBase = fileName.removeSuffix(".pptx").replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_").trim().ifBlank { "Cintra_Presentation" }
                val finalName = "$safeBase.pptx"
                val allSlides = if (slides.isEmpty()) listOf(PptxSlideData(title, listOf("Created by Cintra AI"), layout = "cover")) else slides
                val bytes = buildPptxBytes(title, allSlides, theme)
                val uri = writeDownloadsFile(
                    context = context,
                    fileName = finalName,
                    mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    bytes = bytes,
                )
                mainHandler.post { onSuccess(uri, finalName) }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "تعذّر إنشاء ملف PowerPoint") }
            }
        }.start()
    }

    fun saveDocxToDownloads(
        context: Context,
        title: String,
        sections: List<DocxSectionData>,
        fileName: String,
        accentHex: String = "0284C7",
        onSuccess: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val safeBase = fileName.removeSuffix(".docx").replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_").trim().ifBlank { "Cintra_Document" }
                val finalName = "$safeBase.docx"
                val bytes = buildDocxBytes(title, sections, cleanHex(accentHex, "0284C7"))
                val uri = writeDownloadsFile(
                    context = context,
                    fileName = finalName,
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    bytes = bytes,
                )
                mainHandler.post { onSuccess(uri, finalName) }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "تعذّر إنشاء ملف Word") }
            }
        }.start()
    }

    private fun writeDownloadsFile(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Cintra")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("تعذّر إنشاء الملف في مجلد التنزيلات")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("تعذّر كتابة الملف")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun rectShape(id: Int, name: String, x: Long, y: Long, cx: Long, cy: Long, fillHex: String, borderHex: String? = null, rounded: Boolean = false): String {
        val geom = if (rounded) "roundRect" else "rect"
        val ln = if (borderHex != null) """<a:ln w="12700"><a:solidFill><a:srgbClr val="$borderHex"/></a:solidFill></a:ln>""" else """<a:ln><a:noFill/></a:ln>"""
        return """      <p:sp>
        <p:nvSpPr><p:cNvPr id="$id" name="$name"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="$geom"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="$fillHex"/></a:solidFill>$ln</p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody>
      </p:sp>"""
    }

    private fun textShape(id: Int, name: String, x: Long, y: Long, cx: Long, cy: Long, paragraphsXml: String): String {
        return """      <p:sp>
        <p:nvSpPr><p:cNvPr id="$id" name="$name"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
        <p:txBody><a:bodyPr wrap="square" lIns="91440" rIns="91440" tIns="45720" bIns="45720"/><a:lstStyle/>$paragraphsXml</p:txBody>
      </p:sp>"""
    }

    private fun buildPptxBytes(deckTitle: String, slides: List<PptxSlideData>, rawTheme: PptxThemeData): ByteArray {
        val coverBg = cleanHex(rawTheme.bg, "0F172A")
        val slideBg = cleanHex(rawTheme.slideBg, "F8FAFC")
        val headerBg = cleanHex(rawTheme.headerBg, coverBg)
        val accent1 = cleanHex(rawTheme.accent, "0EA5E9")
        val accent2 = cleanHex(rawTheme.accent2, "10B981")
        val cardBg = cleanHex(rawTheme.cardBg, if (isDarkHex(slideBg)) "1E293B" else "FFFFFF")
        val cardBorder = if (isDarkHex(slideBg)) "334155" else "E2E8F0"
        val bodyText = cleanHex(rawTheme.text, if (isDarkHex(cardBg)) "F1F5F9" else "1E293B")
        val subText = cleanHex(rawTheme.subtext, if (isDarkHex(slideBg)) "94A3B8" else "64748B")
        val coverTitleColor = if (isDarkHex(coverBg)) "FFFFFF" else "0F172A"
        val coverSubColor = if (isDarkHex(coverBg)) "CBD5E1" else "475569"
        val headerTextColor = if (isDarkHex(headerBg)) "FFFFFF" else "0F172A"

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun putEntry(path: String, xml: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(xml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val slideOverrides = slides.indices.joinToString("\n") { i ->
                """  <Override PartName="/ppt/slides/slide${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>"""
            }
            putEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
$slideOverrides
</Types>""",
            )

            putEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""",
            )

            val sldIdLst = slides.indices.joinToString("\n") { i ->
                """    <p:sldId id="${256 + i}" r:id="rId${i + 2}"/>"""
            }
            putEntry(
                "ppt/presentation.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
  <p:sldIdLst>
$sldIdLst
  </p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>""",
            )

            val presRels = slides.indices.joinToString("\n") { i ->
                """  <Relationship Id="rId${i + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${i + 1}.xml"/>"""
            }
            putEntry(
                "ppt/_rels/presentation.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
$presRels
</Relationships>""",
            )

            putEntry(
                "ppt/slideMasters/slideMaster1.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
</p:sldMaster>""",
            )
            putEntry(
                "ppt/slideMasters/_rels/slideMaster1.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>""",
            )

            putEntry(
                "ppt/slideLayouts/slideLayout1.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
</p:sldLayout>""",
            )
            putEntry(
                "ppt/slideLayouts/_rels/slideLayout1.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>""",
            )

            slides.forEachIndexed { idx, slide ->
                val layout = slide.layout.lowercase().trim()
                val isCover = layout == "cover" || idx == 0
                val isClosing = layout == "closing" || (idx == slides.lastIndex && slides.size > 2 && layout == "cards" && slide.bullets.size <= 2)
                val slideAccent = cleanHex(slide.accentOverride, accent1)
                val isRtl = slide.title.any { it in '\u0600'..'\u06FF' }
                val rtlAttr = if (isRtl) """ rtl="1" algn="r"""" else ""
                val shapes = StringBuilder()
                val bgHex = if (isCover || isClosing) coverBg else slideBg

                if (isCover || isClosing) {
                    shapes.append(rectShape(2, "AccentStrip", 0L, 0L, 260000L, 6858000L, slideAccent))
                    shapes.append(rectShape(3, "AccentPill", 720000L, 1280000L, 1200000L, 85000L, accent2, rounded = true))
                    shapes.append(
                        textShape(
                            4, "CoverTitle", 720000L, 1580000L, 10400000L, 1850000L,
                            """<a:p><a:pPr$rtlAttr/><a:r><a:rPr lang="en-US" sz="4200" b="1"><a:solidFill><a:srgbClr val="$coverTitleColor"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(slide.title)}</a:t></a:r></a:p>""",
                        ),
                    )
                    val subLines = buildList {
                        if (slide.subtitle.isNotBlank()) add(slide.subtitle)
                        addAll(slide.bullets.take(3))
                    }
                    val subXml = subLines.joinToString("") { b ->
                        """<a:p><a:pPr$rtlAttr spcAft="12000"/><a:r><a:rPr lang="en-US" sz="1950"><a:solidFill><a:srgbClr val="$coverSubColor"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(b)}</a:t></a:r></a:p>"""
                    }
                    shapes.append(textShape(5, "CoverSub", 720000L, 3550000L, 10400000L, 2300000L, subXml))
                    shapes.append(rectShape(6, "BottomBar", 720000L, 6180000L, 10750000L, 22000L, slideAccent))
                } else {
                    // Header Band
                    shapes.append(rectShape(2, "HeaderBand", 0L, 0L, 12192000L, 1180000L, headerBg))
                    shapes.append(rectShape(3, "HeaderAccent", 0L, 1180000L, 12192000L, 52000L, slideAccent))
                    shapes.append(rectShape(4, "NumBadge", 10750000L, 300000L, 840000L, 560000L, slideAccent, rounded = true))
                    shapes.append(
                        textShape(
                            5, "NumText", 10750000L, 365000L, 840000L, 420000L,
                            """<a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="1750" b="1"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></a:rPr><a:t>${"%02d".format(idx + 1)}</a:t></a:r></a:p>""",
                        ),
                    )
                    shapes.append(
                        textShape(
                            6, "SlideTitle", 580000L, 250000L, 9850000L, 700000L,
                            """<a:p><a:pPr$rtlAttr/><a:r><a:rPr lang="en-US" sz="2800" b="1"><a:solidFill><a:srgbClr val="$headerTextColor"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(slide.title)}</a:t></a:r></a:p>""",
                        ),
                    )

                    val bullets = slide.bullets.ifEmpty { listOf(slide.subtitle.ifBlank { slide.title }) }
                    var nextId = 10

                    when (layout) {
                        "stats" -> {
                            val items = bullets.take(3)
                            val colW = (11032000L - (items.size - 1) * 240000L) / items.size
                            items.forEachIndexed { i, raw ->
                                val parts = raw.split("|", ":", limit = 2)
                                val bigVal = parts.firstOrNull()?.trim().orEmpty()
                                val label = parts.getOrNull(1)?.trim().orEmpty()
                                val cx = 580000L + i * (colW + 240000L)
                                val barColor = if (i % 2 == 0) slideAccent else accent2
                                shapes.append(rectShape(nextId++, "StatCard$i", cx, 1720000L, colW, 3850000L, cardBg, borderHex = cardBorder, rounded = true))
                                shapes.append(rectShape(nextId++, "StatTop$i", cx, 1720000L, colW, 120000L, barColor))
                                shapes.append(
                                    textShape(
                                        nextId++, "StatVal$i", cx + 180000L, 2280000L, colW - 360000L, 1250000L,
                                        """<a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="4000" b="1"><a:solidFill><a:srgbClr val="$barColor"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(bigVal)}</a:t></a:r></a:p>""",
                                    ),
                                )
                                shapes.append(
                                    textShape(
                                        nextId++, "StatLbl$i", cx + 180000L, 3650000L, colW - 360000L, 1500000L,
                                        """<a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="1850"><a:solidFill><a:srgbClr val="$bodyText"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(label.ifBlank { bigVal })}</a:t></a:r></a:p>""",
                                    ),
                                )
                            }
                        }

                        "two_column" -> {
                            val mid = (bullets.size + 1) / 2
                            val leftItems = bullets.take(mid)
                            val rightItems = bullets.drop(mid)
                            val colW = 5380000L
                            listOf(
                                Triple(580000L, slide.leftTitle.ifBlank { "Key Highlights" }, Pair(leftItems, slideAccent)),
                                Triple(6232000L, slide.rightTitle.ifBlank { "Details & Impact" }, Pair(rightItems, accent2)),
                            ).forEachIndexed { colIdx, (cx, colHead, pair) ->
                                val (colBullets, colAccent) = pair
                                shapes.append(rectShape(nextId++, "ColCard$colIdx", cx, 1540000L, colW, 4520000L, cardBg, borderHex = cardBorder, rounded = true))
                                shapes.append(rectShape(nextId++, "ColTop$colIdx", cx, 1540000L, colW, 110000L, colAccent))
                                shapes.append(
                                    textShape(
                                        nextId++, "ColHead$colIdx", cx + 220000L, 1780000L, colW - 440000L, 520000L,
                                        """<a:p><a:pPr$rtlAttr/><a:r><a:rPr lang="en-US" sz="2100" b="1"><a:solidFill><a:srgbClr val="$colAccent"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(colHead)}</a:t></a:r></a:p>""",
                                    ),
                                )
                                val pXml = colBullets.joinToString("") { b ->
                                    """<a:p><a:pPr$rtlAttr spcAft="14000"/><a:r><a:rPr lang="en-US" sz="1750"><a:solidFill><a:srgbClr val="$bodyText"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>• ${xmlEscape(b)}</a:t></a:r></a:p>"""
                                }
                                shapes.append(textShape(nextId++, "ColBody$colIdx", cx + 220000L, 2400000L, colW - 440000L, 3450000L, pXml))
                            }
                        }

                        "timeline" -> {
                            val steps = bullets.take(4)
                            val colW = (11032000L - (steps.size - 1) * 180000L) / steps.size
                            shapes.append(rectShape(nextId++, "TimeTrack", 580000L, 2150000L, 11032000L, 55000L, slideAccent))
                            steps.forEachIndexed { i, step ->
                                val cx = 580000L + i * (colW + 180000L)
                                val nodeColor = if (i % 2 == 0) slideAccent else accent2
                                shapes.append(rectShape(nextId++, "StepNum$i", cx + (colW / 2) - 340000L, 1840000L, 680000L, 680000L, nodeColor, rounded = true))
                                shapes.append(
                                    textShape(
                                        nextId++, "StepN$i", cx + (colW / 2) - 340000L, 1960000L, 680000L, 450000L,
                                        """<a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="2000" b="1"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></a:rPr><a:t>${i + 1}</a:t></a:r></a:p>""",
                                    ),
                                )
                                shapes.append(rectShape(nextId++, "StepCard$i", cx, 2760000L, colW, 3150000L, cardBg, borderHex = cardBorder, rounded = true))
                                shapes.append(
                                    textShape(
                                        nextId++, "StepTxt$i", cx + 160000L, 2980000L, colW - 320000L, 2700000L,
                                        """<a:p><a:pPr$rtlAttr/><a:r><a:rPr lang="en-US" sz="1700"><a:solidFill><a:srgbClr val="$bodyText"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(step)}</a:t></a:r></a:p>""",
                                    ),
                                )
                            }
                        }

                        else -> {
                            // Dynamic cards (<=4 bullets) or clean grouped list (>4 bullets)
                            if (bullets.size <= 4 && layout != "bullets") {
                                val cardCount = bullets.size
                                val startY = 1540000L
                                val totalH = 4480000L
                                val gap = 160000L
                                val cardH = (totalH - gap * (cardCount - 1)) / cardCount
                                bullets.forEachIndexed { bIdx, bullet ->
                                    val cy = startY + bIdx * (cardH + gap)
                                    val barCol = if (bIdx % 2 == 0) slideAccent else accent2
                                    shapes.append(rectShape(nextId++, "CardBg$bIdx", 580000L, cy, 11032000L, cardH, cardBg, borderHex = cardBorder, rounded = true))
                                    val barX = if (isRtl) 580000L + 11032000L - 95000L else 580000L
                                    shapes.append(rectShape(nextId++, "CardBar$bIdx", barX, cy + 85000L, 95000L, (cardH - 170000L).coerceAtLeast(160000L), barCol, rounded = true))
                                    val txtX = if (isRtl) 740000L else 840000L
                                    shapes.append(
                                        textShape(
                                            nextId++, "CardTxt$bIdx", txtX, cy + 110000L, 10450000L, (cardH - 200000L).coerceAtLeast(200000L),
                                            """<a:p><a:pPr$rtlAttr/><a:r><a:rPr lang="en-US" sz="1900"><a:solidFill><a:srgbClr val="$bodyText"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(bullet)}</a:t></a:r></a:p>""",
                                        ),
                                    )
                                }
                            } else {
                                shapes.append(rectShape(nextId++, "MainCard", 580000L, 1540000L, 11032000L, 4520000L, cardBg, borderHex = cardBorder, rounded = true))
                                val barX = if (isRtl) 580000L + 11032000L - 95000L else 580000L
                                shapes.append(rectShape(nextId++, "MainBar", barX, 1700000L, 95000L, 4200000L, slideAccent, rounded = true))
                                val pXml = bullets.joinToString("") { b ->
                                    """<a:p><a:pPr$rtlAttr spcAft="14000"/><a:r><a:rPr lang="en-US" sz="1750"><a:solidFill><a:srgbClr val="$bodyText"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>▪ ${xmlEscape(b)}</a:t></a:r></a:p>"""
                                }
                                shapes.append(textShape(nextId++, "MainTxt", 840000L, 1700000L, 10450000L, 4200000L, pXml))
                            }
                        }
                    }

                    // Bottom Footer
                    shapes.append(rectShape(nextId++, "FooterLine", 580000L, 6280000L, 11032000L, 15000L, cardBorder))
                    shapes.append(
                        textShape(
                            nextId++, "FooterTxt", 580000L, 6340000L, 9000000L, 380000L,
                            """<a:p><a:r><a:rPr lang="en-US" sz="1100"><a:solidFill><a:srgbClr val="$subText"/></a:solidFill></a:rPr><a:t>${xmlEscape(deckTitle)}  •  Cintra AI</a:t></a:r></a:p>""",
                        ),
                    )
                }

                val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="$bgHex"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
$shapes
    </p:spTree>
  </p:cSld>
</p:sld>"""
                putEntry("ppt/slides/slide${idx + 1}.xml", slideXml)
                putEntry(
                    "ppt/slides/_rels/slide${idx + 1}.xml.rels",
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>""",
                )
            }
        }
        return baos.toByteArray()
    }

    private fun buildDocxBytes(docTitle: String, sections: List<DocxSectionData>, accentHex: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun putEntry(path: String, xml: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(xml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            putEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""",
            )
            putEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""",
            )

            val isArabic = docTitle.any { it in '\u0600'..'\u06FF' }
            val bidiTag = if (isArabic) "<w:bidi/>" else ""

            val bodyParts = StringBuilder()
            bodyParts.append("""<w:p><w:pPr>$bidiTag<w:shd w:val="clear" w:color="auto" w:fill="$accentHex"/><w:spacing w:before="120" w:after="260"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="FFFFFF"/><w:sz w:val="40"/></w:rPr><w:t>  ${xmlEscape(docTitle)}  </w:t></w:r></w:p>""")
            sections.forEach { sec ->
                if (sec.heading.isNotBlank()) {
                    bodyParts.append("""<w:p><w:pPr>$bidiTag<w:pBdr><w:bottom w:val="single" w:sz="8" w:space="4" w:color="$accentHex"/></w:pBdr><w:spacing w:before="220" w:after="110"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="$accentHex"/><w:sz w:val="28"/></w:rPr><w:t>${xmlEscape(sec.heading)}</w:t></w:r></w:p>""")
                }
                sec.paragraphs.forEach { p ->
                    bodyParts.append("""<w:p><w:pPr>$bidiTag<w:spacing w:after="130"/></w:pPr><w:r><w:rPr><w:color w:val="1E293B"/><w:sz w:val="23"/></w:rPr><w:t>${xmlEscape(p)}</w:t></w:r></w:p>""")
                }
            }

            putEntry(
                "word/document.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $bodyParts
  </w:body>
</w:document>""",
            )
        }
        return baos.toByteArray()
    }
}
