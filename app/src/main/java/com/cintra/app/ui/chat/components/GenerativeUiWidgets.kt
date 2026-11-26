package com.cintra.app.ui.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cintra.app.ui.model.GenUi
import com.cintra.app.ui.theme.CintraPalette
import com.cintra.app.ui.theme.CintraThemePalette

/** Dispatches a [GenUi] widget to its native Compose renderer. Always-on. */
@Composable
fun GenUiView(
    genUi: GenUi,
    onTab: (String) -> Unit,
    onToggleCheck: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (genUi) {
        is GenUi.Comparison -> ComparisonCard(genUi, onTab, modifier)
        is GenUi.Checklist -> ChecklistCard(genUi, onToggleCheck, modifier)
        is GenUi.GeneratedImage -> GeneratedImageCard(genUi, modifier)
    }
}

@Composable
private fun GenUiCard(modifier: Modifier, content: @Composable () -> Unit) {
    val palette = CintraThemePalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun ComparisonCard(
    genUi: GenUi.Comparison,
    onTab: (String) -> Unit,
    modifier: Modifier,
) {
    val palette = CintraThemePalette
    val tabs = listOf("speed" to "السرعة", "reasoning" to "التفكير", "arabic" to "العربية")
    val items = genUi.tabs[genUi.activeTab] ?: emptyList()
    GenUiCard(modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BarChart, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
            Text(
                " مقارنة أداء نماذج Cintra",
                color = palette.textPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface2)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { (key, label) ->
                val active = genUi.activeTab == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) palette.surface else Color.Transparent)
                        .clickable { onTab(key) }
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    Text(label, color = if (active) palette.textPrimary else palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item -> MetricRow(item, palette) }
        }
    }
}

@Composable
private fun MetricRow(item: com.cintra.app.ui.model.MetricItem, palette: CintraPalette) {
    val target = (item.value.coerceIn(0, 100)) / 100f
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(500), label = "metric")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.label, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${item.value}%", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surface2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.accent),
            )
        }
    }
}

@Composable
private fun ChecklistCard(
    genUi: GenUi.Checklist,
    onToggle: (Int) -> Unit,
    modifier: Modifier,
) {
    val palette = CintraThemePalette
    val done = genUi.items.count { it.done }
    val total = genUi.items.size
    val pct = if (total == 0) 0 else (done * 100) / total
    GenUiCard(modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Checklist, contentDescription = null, tint = palette.emerald, modifier = Modifier.size(15.dp))
            Text(" خطة التنفيذ التفاعلية", color = palette.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$done/$total ($pct%)", color = palette.emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            genUi.items.forEachIndexed { index, item ->
                val rowBg = palette.surface2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(rowBg)
                        .clickable { onToggle(index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (item.done) palette.emerald else Color.Transparent)
                            .border(1.5.dp, if (item.done) palette.emerald else palette.textMuted, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                    Text(
                        item.text,
                        color = if (item.done) palette.textMuted else palette.textPrimary,
                        fontSize = 13.5.sp,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageCard(genUi: GenUi.GeneratedImage, modifier: Modifier) {
    val palette = CintraThemePalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = 0.45f),
                            palette.amber.copy(alpha = 0.32f),
                            palette.canvas,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.glassTintModal)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Text("معاينة صورة مولّدة بواسطة Cintra Vision", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(15.dp))
            Text(
                genUi.prompt,
                color = palette.textSecondary,
                fontSize = 12.5.sp,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
        }
    }
}