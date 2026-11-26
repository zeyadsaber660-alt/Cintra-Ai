package com.cintra.app.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraMotion
import com.cintra.app.ui.theme.CintraThemePalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

data class CintraProject(
    val id: String,
    val name: String,
    val instructions: String,
)

enum class FeatureDestination {
    NONE,
    IMAGES,
    LIBRARY,
    PROJECTS,
    CUSTOMIZE,
}

@Composable
fun FeatureScreenShell(
    visible: Boolean,
    title: String,
    hazeState: HazeState,
    onBack: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = CintraThemePalette
    AnimatedVisibility(
        visible = visible,
        enter = CintraMotion.iosPopEnter(),
        exit = CintraMotion.iosPopExit(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(state = hazeState, style = HazeMaterials.thick())
                .background(palette.canvas.copy(alpha = 0.96f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(palette.surface)
                            .border(0.85.dp, palette.borderSubtle, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(CintraIcons.ArrowBack, contentDescription = "رجوع", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
                    }

                    Text(title, color = palette.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)

                    if (trailingAction != null) {
                        trailingAction()
                    } else {
                        Spacer(Modifier.size(42.dp))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

/** Real Images Gallery Screen showing all images attached across chats + native photo import. */
@Composable
fun ImagesGalleryScreen(
    visible: Boolean,
    hazeState: HazeState,
    photos: List<Attachment>,
    isArabic: Boolean,
    onAddPhoto: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = CintraThemePalette
    FeatureScreenShell(
        visible = visible,
        title = if (isArabic) "الصور" else "Images",
        hazeState = hazeState,
        onBack = onBack,
        trailingAction = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.accentSolid)
                    .clickable(onClick = onAddPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CintraIcons.Plus, contentDescription = "إضافة صورة", tint = palette.accentSolidText, modifier = Modifier.size(20.dp))
            }
        },
    ) {
        if (photos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(CintraIcons.Images, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isArabic) "لا توجد صور مرفقة بعد" else "No images yet",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isArabic) "اضغط + بالأعلى لاختيار صور من جهازك أو صور جوجل" else "Tap + above to pick photos from your device",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(photos) { photo ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.surface)
                            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(20.dp)),
                    ) {
                        if (!photo.uri.isNullOrBlank()) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.1f),
                            )
                        }
                        Text(
                            text = photo.name,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Real Library Screen listing all files and code blocks extracted from chats. */
@Composable
fun LibraryScreen(
    visible: Boolean,
    hazeState: HazeState,
    files: List<Attachment>,
    codeSnippets: List<Pair<String, String>>,
    isArabic: Boolean,
    onOpenCode: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    val palette = CintraThemePalette
    FeatureScreenShell(
        visible = visible,
        title = if (isArabic) "المكتبة" else "Library",
        hazeState = hazeState,
        onBack = onBack,
    ) {
        if (files.isEmpty() && codeSnippets.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(CintraIcons.Library, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isArabic) "المكتبة فارغة حالياً" else "Library is empty",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isArabic) "ستظهر هنا تلقائياً كل الملفات والأكواد من محادثاتك" else "Files and code snippets from your chats appear here",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(files) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surface)
                            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(CintraIcons.Folder, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                        Text(file.name, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                items(codeSnippets) { (lang, code) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surface)
                            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
                            .clickable { onOpenCode(lang, code) }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(lang.uppercase(), color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = code.take(140),
                            color = palette.textSecondary,
                            fontSize = 12.5.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Real Projects Screen: create custom projects with dedicated system prompts & select active project. */
@Composable
fun ProjectsScreen(
    visible: Boolean,
    hazeState: HazeState,
    projects: List<CintraProject>,
    activeProjectId: String?,
    isArabic: Boolean,
    onCreateProject: (name: String, instructions: String) -> Unit,
    onSelectProject: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val palette = CintraThemePalette
    var newName by remember { mutableStateOf("") }
    var newPrompt by remember { mutableStateOf("") }

    FeatureScreenShell(
        visible = visible,
        title = if (isArabic) "المشاريع" else "Projects",
        hazeState = hazeState,
        onBack = onBack,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.surface)
                        .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = if (isArabic) "إنشاء مشروع جديد" else "Create New Project",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    StyledInput(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = if (isArabic) "اسم المشروع (مثلاً: تطبيق أندرويد)" else "Project name",
                    )
                    StyledInput(
                        value = newPrompt,
                        onValueChange = { newPrompt = it },
                        placeholder = if (isArabic) "تعليمات المشروع المخصصة للذكاء الاصطناعي..." else "Custom instructions for this project...",
                        singleLine = false,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accentSolid)
                            .clickable {
                                if (newName.isNotBlank()) {
                                    onCreateProject(newName.trim(), newPrompt.trim())
                                    newName = ""
                                    newPrompt = ""
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isArabic) "إضافة المشروع" else "Add Project",
                            color = palette.accentSolidText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            items(projects, key = { it.id }) { proj ->
                val isSelected = proj.id == activeProjectId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) palette.accentSoft else palette.surface)
                        .border(
                            width = if (isSelected) 1.2.dp else 0.75.dp,
                            color = if (isSelected) palette.accent else palette.borderSubtle,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelectProject(if (isSelected) null else proj.id) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(proj.name, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (proj.instructions.isNotBlank()) {
                            Text(
                                text = proj.instructions,
                                color = palette.textSecondary,
                                fontSize = 12.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(CintraIcons.Check, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** Real Customize Cintra Screen: edit local profile name + permanent Custom Instructions sent to Worker. */
@Composable
fun CustomizeCintraScreen(
    visible: Boolean,
    hazeState: HazeState,
    userName: String,
    customInstructions: String,
    isArabic: Boolean,
    onSave: (userName: String, instructions: String) -> Unit,
    onBack: () -> Unit,
) {
    val palette = CintraThemePalette
    var nameInput by remember(visible, userName) { mutableStateOf(userName) }
    var instructionsInput by remember(visible, customInstructions) { mutableStateOf(customInstructions) }

    FeatureScreenShell(
        visible = visible,
        title = if (isArabic) "تخصيص Cintra" else "Customize Cintra",
        hazeState = hazeState,
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.surface)
                    .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isArabic) "الاسم المحلي (يظهر في ملفك الشخصي)" else "Local Display Name",
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                StyledInput(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = if (isArabic) "اكتب اسمك..." else "Your name...",
                )

                Text(
                    text = if (isArabic) "التعليمات الدائمة لـ Cintra (تُرسل مع كل محادثة)" else "Custom Instructions (sent with every chat)",
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                StyledInput(
                    value = instructionsInput,
                    onValueChange = { instructionsInput = it },
                    placeholder = if (isArabic) "مثلاً: أجب بإيجاز، واستخدم أمثلة Kotlin عملية..." else "e.g., Be concise and prefer practical Kotlin examples...",
                    singleLine = false,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accentSolid)
                        .clickable { onSave(nameInput.trim().ifEmpty { "User" }, instructionsInput.trim()) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isArabic) "حفظ التخصيص" else "Save Preferences",
                        color = palette.accentSolidText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
) {
    val palette = CintraThemePalette
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(color = palette.textPrimary, fontSize = 14.sp, lineHeight = 20.sp),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface2)
            .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = palette.textMuted, fontSize = 13.5.sp)
            }
            inner()
        },
    )
}
