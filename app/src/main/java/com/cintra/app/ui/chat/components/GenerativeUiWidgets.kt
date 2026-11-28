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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cintra.app.ui.model.GeneratedDocxDoc
import com.cintra.app.ui.model.GeneratedPptxDoc
import com.cintra.app.utils.CintraOfficeExporter
import kotlinx.coroutines.delay
import kotlin.math.sin
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
import com.cintra.app.ui.theme.CintraToastManager

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
    val context = LocalContext.current
    var resolvedUrl by remember(prompt) { mutableStateOf<String?>(null) }
    var decodedBitmap by remember(prompt) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember(prompt) { mutableStateOf(true) }
    var progressPct by remember(prompt) { mutableStateOf(8) }
    var errorMsg by remember(prompt) { mutableStateOf<String?>(null) }
    var retryTick by remember(prompt) { mutableStateOf(0) }
    var fullscreenOpen by remember { mutableStateOf(false) }

    // Smooth ChatGPT-style progress ticker while waiting for user's Image API
    LaunchedEffect(isLoading) {
        if (!isLoading) return@LaunchedEffect
        progressPct = 10
        while (isLoading && progressPct < 92) {
            delay(380L)
            progressPct = (progressPct + (3..7).random()).coerceAtMost(92)
        }
    }

    LaunchedEffect(prompt, retryTick) {
        val cacheDir = java.io.File(context.filesDir, "cintra_images").apply { mkdirs() }
        val cacheKey = prompt.trim().hashCode().toUInt().toString(16)
        val cacheFile = java.io.File(cacheDir, "img_$cacheKey.img")

        if (retryTick == 0 && cacheFile.exists() && cacheFile.length() > 800L) {
            val cachedBmp = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(cacheFile.absolutePath) }.getOrNull()
            }
            if (cachedBmp != null) {
                decodedBitmap = cachedBmp
                progressPct = 100
                isLoading = false
                return@LaunchedEffect
            }
        }

        isLoading = true
        errorMsg = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "${CintraSecureVault.resolveEndpoint()}/v1/images/generate"
                val token = CintraSecureVault.resolveAppToken()
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 75_000
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
                Triple(json.optString("url", ""), json.optString("b64_json", ""), json.optString("error", ""))
            }.getOrNull()
        }
        if (result != null && result.second.isNotBlank()) {
            val bytes = runCatching { Base64.decode(result.second, Base64.DEFAULT) }.getOrNull()
            if (bytes != null) {
                withContext(Dispatchers.IO) { runCatching { cacheFile.writeBytes(bytes) } }
                decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            progressPct = 100
            isLoading = false
        } else if (result != null && result.first.startsWith("http")) {
            val urlStr = result.first
            resolvedUrl = urlStr
            val downloaded = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = URL(urlStr).openStream().use { it.readBytes() }
                    if (bytes.size > 800) {
                        cacheFile.writeBytes(bytes)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                }.getOrNull()
            }
            if (downloaded != null) decodedBitmap = downloaded
            progressPct = 100
            isLoading = false
        } else {
            errorMsg = result?.third?.takeIf { it.isNotBlank() } ?: "تعذّر توليد الصورة من محرك الـ API الخاص بك"
            isLoading = false
        }
    }

    val stageLabel = when {
        progressPct < 30 -> "جارٍ التخطيط..."
        progressPct < 60 -> "جارٍ رسم المسودة الأولى..."
        else -> "جارٍ ضبط المشهد والتفاصيل..."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isLoading) {
            Text(stageLabel, color = palette.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.surface)
                    .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(22.dp)),
            ) {
                val inf = rememberInfiniteTransition(label = "dotWave")
                val phase by inf.animateFloat(
                    initialValue = 0f,
                    targetValue = 6.28318f,
                    animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
                    label = "wavePhase",
                )
                Canvas(modifier = Modifier.matchParentSize().padding(18.dp)) {
                    val cols = 15
                    val rows = 15
                    val stepX = size.width / (cols - 1)
                    val stepY = size.height / (rows - 1)
                    val cx = cols / 2f
                    val cy = rows / 2f
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val dist = kotlin.math.hypot((c - cx).toDouble(), (r - cy).toDouble()).toFloat()
                            val wave = (sin(dist * 0.7f - phase) + 1f) * 0.5f
                            val radius = (1.6f + wave * 3.0f).dp.toPx()
                            val alpha = (0.14f + wave * 0.72f).coerceIn(0.12f, 0.9f)
                            drawCircle(
                                color = Color(0xFF0EA5E9).copy(alpha = alpha),
                                radius = radius,
                                center = Offset(c * stepX, r * stepY),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surface2.copy(alpha = 0.92f))
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("$progressPct%", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (errorMsg != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
                    .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(CintraIcons.Images, contentDescription = null, tint = palette.rose, modifier = Modifier.size(20.dp))
                Text(errorMsg.orEmpty(), color = palette.textSecondary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.accent)
                        .clickable { retryTick++ }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("إعادة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.surface2)
                    .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(22.dp))
                    .clickable { fullscreenOpen = true },
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f))
                        .clickable { fullscreenOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = "عرض", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (fullscreenOpen && (decodedBitmap != null || resolvedUrl != null)) {
        Dialog(
            onDismissRequest = { fullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(16.dp),
            ) {
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap!!.asImageBitmap(),
                        contentDescription = prompt,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    )
                } else if (resolvedUrl != null) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = prompt,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 28.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable { fullscreenOpen = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(CintraIcons.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun GeneratedPptxCard(pptx: GeneratedPptxDoc) {
    val palette = CintraThemePalette
    val context = LocalContext.current
    var isSaving by remember(pptx.fileName) { mutableStateOf(false) }
    var savedUri by remember(pptx.fileName) { mutableStateOf<Uri?>(null) }
    var savedName by remember(pptx.fileName) { mutableStateOf(pptx.fileName) }
    val pptxTint = Color(0xFFEA580C)
    val mime = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(0.9.dp, palette.borderSubtle, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(pptxTint.copy(alpha = if (palette.isDark) 0.20f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("PPT", color = pptxTint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pptx.title, color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (savedUri != null) "تم الحفظ في التنزيلات/Cintra • $savedName" else "${pptx.fileName} • ${pptx.slides.size} شرائح",
                    color = if (savedUri != null) palette.emerald else palette.textMuted,
                    fontSize = 11.5.sp,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (savedUri == null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pptxTint)
                        .clickable(enabled = !isSaving) {
                            isSaving = true
                            CintraOfficeExporter.savePptxToDownloads(
                                context = context,
                                title = pptx.title,
                                slides = pptx.slides,
                                fileName = pptx.fileName,
                                theme = pptx.theme,
                                onSuccess = { uri, finalName ->
                                    isSaving = false; savedUri = uri; savedName = finalName
                                    CintraToastManager.show("تم حفظ $finalName في التنزيلات")
                                },
                                onError = { err -> isSaving = false; CintraToastManager.show(err, error = true) },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaving) "جارٍ إنشاء العرض..." else "تحميل PowerPoint (.pptx)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.emerald)
                        .clickable { CintraPdfExporter.openFileUri(context, savedUri!!, mime) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فتح العرض التقديمي", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface2)
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                        .clickable { CintraPdfExporter.shareFileUri(context, savedUri!!, pptx.title, mime) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "مشاركة", tint = palette.textPrimary, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
fun GeneratedDocxCard(docx: GeneratedDocxDoc) {
    val palette = CintraThemePalette
    val context = LocalContext.current
    var isSaving by remember(docx.fileName) { mutableStateOf(false) }
    var savedUri by remember(docx.fileName) { mutableStateOf<Uri?>(null) }
    var savedName by remember(docx.fileName) { mutableStateOf(docx.fileName) }
    val docxTint = Color(0xFF2563EB)
    val mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(0.9.dp, palette.borderSubtle, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(docxTint.copy(alpha = if (palette.isDark) 0.20f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("DOC", color = docxTint, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(docx.title, color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (savedUri != null) "تم الحفظ في التنزيلات/Cintra • $savedName" else docx.fileName,
                    color = if (savedUri != null) palette.emerald else palette.textMuted,
                    fontSize = 11.5.sp,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (savedUri == null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(docxTint)
                        .clickable(enabled = !isSaving) {
                            isSaving = true
                            CintraOfficeExporter.saveDocxToDownloads(
                                context = context,
                                title = docx.title,
                                sections = docx.sections,
                                fileName = docx.fileName,
                                accentHex = docx.accentHex,
                                onSuccess = { uri, finalName ->
                                    isSaving = false; savedUri = uri; savedName = finalName
                                    CintraToastManager.show("تم حفظ $finalName في التنزيلات")
                                },
                                onError = { err -> isSaving = false; CintraToastManager.show(err, error = true) },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaving) "جارٍ إنشاء Word..." else "تحميل Word (.docx)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.emerald)
                        .clickable { CintraPdfExporter.openFileUri(context, savedUri!!, mime) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فتح ملف Word", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface2)
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                        .clickable { CintraPdfExporter.shareFileUri(context, savedUri!!, docx.title, mime) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "مشاركة", tint = palette.textPrimary, modifier = Modifier.size(17.dp))
                }
            }
        }
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
                                    CintraToastManager.show("تم حفظ $finalName في التنزيلات")
                                },
                                onError = { err ->
                                    isSaving = false
                                    CintraToastManager.show(err, error = true)
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
                                isLandscape = pdf.isLandscape,
                                singlePage = pdf.singlePage,
                                onSuccess = { uri, finalName ->
                                    isSaving = false
                                    savedUri = uri
                                    savedName = finalName
                                    CintraToastManager.show("تم حفظ $finalName في التنزيلات")
                                },
                                onError = { err ->
                                    isSaving = false
                                    CintraToastManager.show(err, error = true)
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
                            if (!ok) CintraToastManager.show("لا يوجد تطبيق لفتح ملفات PDF", error = true)
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
                        CintraPdfExporter.exportHtmlToPdf(context, pdf.htmlContent, pdf.fileName, pdf.isLandscape)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Print, contentDescription = "معاينة وطباعة", tint = palette.textPrimary, modifier = Modifier.size(17.dp))
            }
        }
    }
}
