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

data class PptxSlideData(
    val title: String,
    val bullets: List<String>,
)

data class DocxSectionData(
    val heading: String,
    val paragraphs: List<String>,
)

/**
 * Pure-Kotlin Studio-Grade OpenXML generator for `.pptx` (PowerPoint) and `.docx` (Word).
 * PowerPoint decks feature:
 *  - Deep Navy (#0B1120) executive cover & closing bookend slides with Sky (#0EA5E9) accent geometry
 *  - Clean Alabaster (#F8FAFC) content slides with top Navy header bar, numbered slide badge,
 *    styled white content cards with left Sky accent strips, and bottom footer bar.
 */
object CintraOfficeExporter {

    private fun xmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    fun savePptxToDownloads(
        context: Context,
        title: String,
        slides: List<PptxSlideData>,
        fileName: String,
        onSuccess: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val safeBase = fileName.removeSuffix(".pptx").replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_").trim().ifBlank { "Cintra_Presentation" }
                val finalName = "$safeBase.pptx"
                val allSlides = if (slides.isEmpty()) listOf(PptxSlideData(title, listOf("Created by Cintra AI"))) else slides
                val bytes = buildPptxBytes(title, allSlides)
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
        onSuccess: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val safeBase = fileName.removeSuffix(".docx").replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_").trim().ifBlank { "Cintra_Document" }
                val finalName = "$safeBase.docx"
                val bytes = buildDocxBytes(title, sections)
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

    private fun buildPptxBytes(deckTitle: String, slides: List<PptxSlideData>): ByteArray {
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
                val isCover = idx == 0
                val isClosing = idx == slides.lastIndex && slides.size > 2
                val shapes = StringBuilder()
                val bgHex = if (isCover || isClosing) "0B1120" else "F8FAFC"

                if (isCover || isClosing) {
                    // Geometric executive dark cover/closing slide
                    shapes.append(rectShape(2, "SidePanel", 8600000L, 0L, 3592000L, 6858000L, "111C33"))
                    shapes.append(rectShape(3, "AccentBar", 685800L, 1420000L, 1100000L, 85000L, "0EA5E9", rounded = true))
                    shapes.append(
                        textShape(
                            4, "Badge", 685800L, 1020000L, 4500000L, 360000L,
                            """<a:p><a:r><a:rPr lang="en-US" sz="1300" b="1"><a:solidFill><a:srgbClr val="38BDF8"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>CINTRA AI PRESENTATION</a:t></a:r></a:p>""",
                        ),
                    )
                    shapes.append(
                        textShape(
                            5, "CoverTitle", 685800L, 1680000L, 7600000L, 1750000L,
                            """<a:p><a:r><a:rPr lang="en-US" sz="4200" b="1"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(slide.title)}</a:t></a:r></a:p>""",
                        ),
                    )
                    val subXml = slide.bullets.take(3).joinToString("") { b ->
                        """<a:p><a:pPr spcAft="12000"/><a:r><a:rPr lang="en-US" sz="1900"><a:solidFill><a:srgbClr val="CBD5E1"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(b)}</a:t></a:r></a:p>"""
                    }
                    shapes.append(textShape(6, "CoverSub", 685800L, 3620000L, 7600000L, 2200000L, subXml))
                    shapes.append(rectShape(7, "BottomLine", 685800L, 6150000L, 10820400L, 18000L, "1E293B"))
                } else {
                    // Top Navy Executive Header Band + Slide Number Badge
                    shapes.append(rectShape(2, "HeaderBand", 0L, 0L, 12192000L, 1220000L, "0F172A"))
                    shapes.append(rectShape(3, "HeaderAccent", 0L, 1220000L, 12192000L, 55000L, "0EA5E9"))
                    shapes.append(rectShape(4, "NumBadge", 10700000L, 310000L, 860000L, 580000L, "0284C7", rounded = true))
                    shapes.append(
                        textShape(
                            5, "NumText", 10700000L, 380000L, 860000L, 440000L,
                            """<a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="1800" b="1"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></a:rPr><a:t>${"%02d".format(idx + 1)}</a:t></a:r></a:p>""",
                        ),
                    )
                    shapes.append(
                        textShape(
                            6, "SlideTitle", 580000L, 260000L, 9800000L, 720000L,
                            """<a:p><a:r><a:rPr lang="en-US" sz="2800" b="1"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(slide.title)}</a:t></a:r></a:p>""",
                        ),
                    )

                    // Styled White Content Cards (1 card per bullet up to 4 cards, or 1 grouped card if > 4)
                    val bullets = slide.bullets.ifEmpty { listOf("Key insights for ${slide.title}") }
                    var nextId = 10
                    if (bullets.size <= 4) {
                        val cardCount = bullets.size
                        val startY = 1580000L
                        val totalH = 4450000L
                        val gap = 160000L
                        val cardH = (totalH - gap * (cardCount - 1)) / cardCount
                        bullets.forEachIndexed { bIdx, bullet ->
                            val cy = startY + bIdx * (cardH + gap)
                            val accentCol = when (bIdx % 3) {
                                0 -> "0284C7"
                                1 -> "059669"
                                else -> "D97706"
                            }
                            shapes.append(rectShape(nextId++, "CardBg$bIdx", 580000L, cy, 11032000L, cardH, "FFFFFF", borderHex = "E2E8F0", rounded = true))
                            shapes.append(rectShape(nextId++, "CardBar$bIdx", 580000L, cy + 90000L, 95000L, (cardH - 180000L).coerceAtLeast(180000L), accentCol, rounded = true))
                            shapes.append(
                                textShape(
                                    nextId++, "CardTxt$bIdx", 840000L, cy + 120000L, 10520000L, (cardH - 220000L).coerceAtLeast(220000L),
                                    """<a:p><a:r><a:rPr lang="en-US" sz="1900"><a:solidFill><a:srgbClr val="1E293B"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(bullet)}</a:t></a:r></a:p>""",
                                ),
                            )
                        }
                    } else {
                        shapes.append(rectShape(nextId++, "MainCard", 580000L, 1580000L, 11032000L, 4500000L, "FFFFFF", borderHex = "E2E8F0", rounded = true))
                        shapes.append(rectShape(nextId++, "MainBar", 580000L, 1740000L, 95000L, 4180000L, "0284C7", rounded = true))
                        val pXml = bullets.joinToString("") { b ->
                            """<a:p><a:pPr marL="240000" indent="-150000" spcAft="14000"><a:buChar char="▪"/></a:pPr><a:r><a:rPr lang="en-US" sz="1750"><a:solidFill><a:srgbClr val="1E293B"/></a:solidFill><a:latin typeface="Segoe UI"/></a:rPr><a:t>${xmlEscape(b)}</a:t></a:r></a:p>"""
                        }
                        shapes.append(textShape(nextId++, "MainTxt", 850000L, 1740000L, 10500000L, 4180000L, pXml))
                    }

                    // Bottom Footer
                    shapes.append(rectShape(nextId++, "FooterLine", 580000L, 6280000L, 11032000L, 15000L, "CBD5E1"))
                    shapes.append(
                        textShape(
                            nextId++, "FooterTxt", 580000L, 6340000L, 9000000L, 380000L,
                            """<a:p><a:r><a:rPr lang="en-US" sz="1100"><a:solidFill><a:srgbClr val="64748B"/></a:solidFill></a:rPr><a:t>${xmlEscape(deckTitle)}  •  Cintra AI</a:t></a:r></a:p>""",
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

    private fun buildDocxBytes(docTitle: String, sections: List<DocxSectionData>): ByteArray {
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
            bodyParts.append("""<w:p><w:pPr>$bidiTag<w:shd w:val="clear" w:color="auto" w:fill="0F172A"/><w:spacing w:before="120" w:after="260"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="FFFFFF"/><w:sz w:val="40"/></w:rPr><w:t>  ${xmlEscape(docTitle)}  </w:t></w:r></w:p>""")
            sections.forEach { sec ->
                if (sec.heading.isNotBlank()) {
                    bodyParts.append("""<w:p><w:pPr>$bidiTag<w:pBdr><w:bottom w:val="single" w:sz="8" w:space="4" w:color="0284C7"/></w:pBdr><w:spacing w:before="220" w:after="110"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="0369A1"/><w:sz w:val="28"/></w:rPr><w:t>${xmlEscape(sec.heading)}</w:t></w:r></w:p>""")
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
