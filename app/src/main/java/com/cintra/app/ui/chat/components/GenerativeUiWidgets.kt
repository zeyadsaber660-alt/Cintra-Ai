package com.cintra.app.ui.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import com.cintra.app.data.security.CintraSecureVault
import com.cintra.app.ui.model.GenUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.cintra.app.ui.model.GeneratedGenericFile
import com.cintra.app.ui.model.GeneratedPdfDoc
import com.cintra.app.utils.CintraPdfExporter
import com.cintra.app.ui.model.WebPhotoItem
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraThemePalette

@Composable
fun GenUiView(
    genUi: GenUi,
    onTab: (String) -> Unit,
    onToggleCheck: (Int) -> Unit,
) {
    when (genUi) {
        is GenUi.Comparison -> ComparisonCard(genUi, onTab)
        is GenUi.Checklist -> ChecklistCard(genUi, onToggleCheck)
        is GenUi.GeneratedImage -> GeneratedImageCard(genUi.prompt)
        is GenUi.RealPhotos -> RealWebPhotosCarousel(genUi.photos)
        is GenUi.VisualGrid -> VisualPastelGridCard(genUi)
    }
}

/**
 * Horizontal carousel of real web photos fetched by Cintra Worker (matching ChatGPT Mobile 8265/8268).
 */
@Composable
fun RealWebPhotosCarousel(photos: List<WebPhotoItem>) {
    if (photos.isEmpty()) return
    val palette = CintraThemePalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        photos.forEach { photo ->
            var loadFailed by remember(photo.url) { mutableStateOf(false) }
            if (loadFailed) return@forEach
            Box(
                modifier = Modifier
                    .width(210.dp)
                    .height(145.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface2)
                    .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(18.dp)),
            ) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                    onError = { loadFailed = true },
                    modifier = Modifier.matchParentSize(),
                )
                if (photo.title.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                                ),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = photo.title,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2x2 Pastel Visual Concept Grid (matching ChatGPT Flowchart / concept cards in 8255.jpg).
 */
@Composable
private fun VisualPastelGridCard(grid: GenUi.VisualGrid) {
    val palette = CintraThemePalette
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (grid.title.isNotBlank()) {
            Text(grid.title, color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        grid.items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    val pillBg = when (item.tone) {
                        "green" -> if (palette.isDark) Color(0x2E34D399) else Color(0xFFD9F3E7)
                        "amber" -> if (palette.isDark) Color(0x2EFBBF24) else Color(0xFFFEF0C7)
                        "slate" -> if (palette.isDark) Color(0x2E94A3B8) else Color(0xFFE7ECF3)
                        else -> if (palette.isDark) Color(0x2E38BDF8) else Color(0xFFDCEBFF)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surface)
                            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBg)
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.title,
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            text = item.subtitle,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                        )
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComparisonCard(comp: GenUi.Comparison, onTab: (String) -> Unit) {
    val palette = CintraThemePalette
    val items = comp.tabs[comp.activeTab] ?: comp.tabs.values.firstOrNull().orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("مقارنة تفاعلية", color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface2)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                comp.tabs.keys.forEach { key ->
                    val active = key == comp.activeTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) palette.surface else Color.Transparent)
                            .clickable { onTab(key) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = key,
                            color = if (active) palette.textPrimary else palette.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { m ->
                val frac by animateFloatAsState(
                    targetValue = (m.value.coerceIn(0, 100)) / 100f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 360f),
                    label = "bar",
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.label, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${m.value}%", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(palette.surface2),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.accent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistCard(checklist: GenUi.Checklist, onToggle: (Int) -> Unit) {
    val palette = CintraThemePalette
    val done = checklist.items.count { it.done }
    val pct = if (checklist.items.isEmpty()) 0 else (done * 100) / checklist.items.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("خطة التنفيذ التفاعلية", color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text("$done/${checklist.items.size} ($pct%)", color = palette.emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            checklist.items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface2)
                        .clickable { onToggle(idx) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (item.done) palette.emerald else Color.Transparent)
                            .border(1.5.dp, if (item.done) palette.emerald else palette.textMuted, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.done) {
                            Icon(CintraIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                    Text(
                        text = item.text,
                        color = if (item.done) palette.textMuted else palette.textPrimary,
                        fontSize = 13.5.sp,
                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageCard(prompt: String) {
    val palette = CintraThemePalette
    var resolvedUrl by remember(prompt) { mutableStateOf<String?>(null) }
    var decodedBitmap by remember(prompt) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember(prompt) { mutableStateOf(true) }
    var errorMsg by remember(prompt) { mutableStateOf<String?>(null) }

    LaunchedEffect(prompt) {
        isLoading = true
        errorMsg = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "${CintraSecureVault.resolveEndpoint()}/v1/images/generate"
                val token = CintraSecureVault.resolveAppToken()
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                }
                conn.outputStream.use { os ->
                    os.write(JSONObject().put("prompt", prompt).toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                val json = JSONObject(body)
                val url = json.optString("url", "")
                val b64 = json.optString("b64_json", "")
                Pair(url, b64)
            }.getOrNull()
        }
        if (result != null && result.second.isNotBlank()) {
            val bytes = runCatching { Base64.decode(result.second, Base64.DEFAULT) }.getOrNull()
            decodedBitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            isLoading = false
        } else if (result != null && result.first.startsWith("http")) {
            resolvedUrl = result.first
            isLoading = false
        } else {
            // Fallback only if custom image API is not configured yet
            resolvedUrl = "https://image.pollinations.ai/prompt/${Uri.encode(prompt)}?width=1024&height=768&nologo=true"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(0.85.dp, palette.borderSubtle, RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .background(palette.surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (decodedBitmap != null) {
                Image(
                    bitmap = decodedBitmap!!.asImageBitmap(),
                    contentDescription = prompt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else if (resolvedUrl != null) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = prompt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text("جارٍ توليد الصورة عبر محركك...", color = palette.textSecondary, fontSize = 12.5.sp)
                }
            }
        }
        Text(
            text = prompt,
            color = palette.textSecondary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun GeneratedGenericFileCard(file: GeneratedGenericFile) {
    val palette = CintraThemePalette
    val context = LocalContext.current
    var isSaving by remember(file.fileName) { mutableStateOf(false) }
    var savedUri by remember(file.fileName) { mutableStateOf<Uri?>(null) }
    var savedName by remember(file.fileName) { mutableStateOf(file.fileName) }

    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(palette.surface)
            .border(0.9.dp, palette.borderSubtle, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accent.copy(alpha = if (palette.isDark) 0.18f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CintraIcons.Folder, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(file.title, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (savedUri != null) "تم الحفظ في التنزيلات/Cintra • $savedName" else file.fileName,
                    color = if (savedUri != null) palette.emerald else palette.textMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (savedUri == null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accent)
                        .clickable(enabled = !isSaving) {
                            isSaving = true
                            CintraPdfExporter.saveTextFileToDownloads(
                                context = context,
                                fileName = file.fileName,
                                mimeType = file.mimeType,
                                content = file.content,
                                onSuccess = { uri, finalName ->
                                    isSaving = false
                                    savedUri = uri
                                    savedName = finalName
                                    Toast.makeText(context, "تم حفظ $finalName في التنزيلات", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    isSaving = false
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تحميل ${file.fileName.substringAfterLast('.', "الملف").uppercase()}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.emerald)
                        .clickable { CintraPdfExporter.openFileUri(context, savedUri!!, file.mimeType) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فتح الملف", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface2)
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                        .clickable { CintraPdfExporter.shareFileUri(context, savedUri!!, file.title, file.mimeType) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "مشاركة", tint = palette.textPrimary, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

/**
 * Interactive Frosted Liquid Glass PDF File Card rendered directly inside chat messages.
 * Downloads a real multi-page Arabic RTL PDF directly to Downloads/Cintra/ in one tap,
 * then offers instant Open, Share, and Print Preview actions.
 */
@Composable
fun GeneratedPdfFileCard(pdf: GeneratedPdfDoc) {
    val palette = CintraThemePalette
    val context = LocalContext.current
    var isSaving by remember(pdf.fileName) { mutableStateOf(false) }
    var savedUri by remember(pdf.fileName) { mutableStateOf<Uri?>(null) }
    var savedName by remember(pdf.fileName) { mutableStateOf(pdf.fileName) }

    val cardShape = RoundedCornerShape(22.dp)
    val pdfTint = Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(palette.surface)
            .border(0.9.dp, palette.borderSubtle, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(pdfTint.copy(alpha = if (palette.isDark) 0.18f else 0.12f))
                    .border(0.8.dp, pdfTint.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = pdfTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = pdf.title,
                    color = palette.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (savedUri != null) "تم الحفظ في التنزيلات/Cintra • $savedName" else pdf.fileName,
                    color = if (savedUri != null) palette.emerald else palette.textMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (pdf.summary.isNotBlank()) {
                    Text(
                        text = pdf.summary,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (savedUri == null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accent)
                        .clickable(enabled = !isSaving) {
                            isSaving = true
                            CintraPdfExporter.downloadHtmlAsPdfToDownloads(
                                context = context,
                                htmlContent = pdf.htmlContent,
                                fileName = pdf.fileName,
                                onSuccess = { uri, finalName ->
                                    isSaving = false
                                    savedUri = uri
                                    savedName = finalName
                                    Toast.makeText(context, "تم حفظ $finalName في التنزيلات", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    isSaving = false
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("جارٍ إنشاء PDF...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تحميل ملف PDF", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.emerald)
                        .clickable {
                            val ok = CintraPdfExporter.openPdfUri(context, savedUri!!)
                            if (!ok) Toast.makeText(context, "لا يوجد تطبيق لفتح ملفات PDF", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فتح الملف", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface2)
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                        .clickable {
                            CintraPdfExporter.sharePdfUri(context, savedUri!!, pdf.title)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "مشاركة", tint = palette.textPrimary, modifier = Modifier.size(17.dp))
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface2)
                    .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                    .clickable {
                        CintraPdfExporter.exportHtmlToPdf(context, pdf.htmlContent, pdf.fileName)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Print, contentDescription = "معاينة وطباعة", tint = palette.textPrimary, modifier = Modifier.size(17.dp))
            }
        }
    }
}
