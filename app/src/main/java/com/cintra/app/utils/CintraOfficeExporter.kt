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
 * Pure-Kotlin native OpenXML generator for real `.pptx` (PowerPoint) and `.docx` (Word) files
 * with zero extra APK dependencies. Saves directly to `Downloads/Cintra/` off the UI thread.
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
                val bgHex = if (isCover) "0F172A" else "FFFFFF"
                val titleHex = if (isCover) "FFFFFF" else "0F172A"
                val bodyHex = if (isCover) "BAE6FD" else "334155"
                val bulletsXml = slide.bullets.joinToString("\n") { b ->
                    """          <a:p><a:pPr marL="285750" indent="-171450"><a:buChar char="•"/></a:pPr><a:r><a:rPr lang="en-US" sz="${if (isCover) 2200 else 2000}"><a:solidFill><a:srgbClr val="$bodyHex"/></a:solidFill></a:rPr><a:t>${xmlEscape(b)}</a:t></a:r></a:p>"""
                }
                val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="$bgHex"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2" name="Title"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="685800" y="${if (isCover) 1800000 else 540000}"/><a:ext cx="10820400" cy="1100000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
        <p:txBody>
          <a:bodyPr wrap="square"/>
          <a:lstStyle/>
          <a:p><a:r><a:rPr lang="en-US" sz="${if (isCover) 4000 else 3000}" b="1"><a:solidFill><a:srgbClr val="$titleHex"/></a:solidFill></a:rPr><a:t>${xmlEscape(slide.title)}</a:t></a:r></a:p>
        </p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="3" name="Body"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="685800" y="${if (isCover) 3100000 else 1800000}"/><a:ext cx="10820400" cy="4400000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
        <p:txBody>
          <a:bodyPr wrap="square"/>
          <a:lstStyle/>
$bulletsXml
        </p:txBody>
      </p:sp>
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
            bodyParts.append("""<w:p><w:pPr>$bidiTag<w:spacing w:after="240"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="0F172A"/><w:sz w:val="40"/></w:rPr><w:t>${xmlEscape(docTitle)}</w:t></w:r></w:p>""")
            sections.forEach { sec ->
                if (sec.heading.isNotBlank()) {
                    bodyParts.append("""<w:p><w:pPr>$bidiTag<w:spacing w:before="200" w:after="100"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="0369A1"/><w:sz w:val="28"/></w:rPr><w:t>${xmlEscape(sec.heading)}</w:t></w:r></w:p>""")
                }
                sec.paragraphs.forEach { p ->
                    bodyParts.append("""<w:p><w:pPr>$bidiTag<w:spacing w:after="120"/></w:pPr><w:r><w:rPr><w:color w:val="1E293B"/><w:sz w:val="23"/></w:rPr><w:t>${xmlEscape(p)}</w:t></w:r></w:p>""")
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
