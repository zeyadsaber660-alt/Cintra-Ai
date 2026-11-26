package com.cintra.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cintra.app.ui.chat.components.ArtifactsGlassSheet
import com.cintra.app.ui.chat.components.CintraLiveOverlay
import com.cintra.app.ui.chat.components.FloatingGlassComposer
import com.cintra.app.ui.chat.components.FloatingGlassTopBar
import com.cintra.app.ui.chat.components.GooglePhotosSheet
import com.cintra.app.ui.chat.components.MessageItem
import com.cintra.app.ui.chat.components.ModelPickerGlassModal
import com.cintra.app.ui.chat.components.PlusAction
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.model.AttachmentType
import com.cintra.app.ui.model.CintraModels
import com.cintra.app.ui.theme.CintraTheme
import com.cintra.app.ui.theme.CintraThemePalette
import com.cintra.app.ui.theme.cintraLiquidGlass
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/**
 * Root Cintra chat screen. Hosts the liquid glass backdrop + Haze source, the
 * modal navigation drawer, floating top bar, chat stream, floating composer and
 * all glass overlays. Phase 1 is fully interactive on mock state.
 */
@Composable
fun CintraChatScreen(
    viewModel: CintraChatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CintraTheme(darkTheme = state.darkTheme) {
        CintraChatScreenContent(viewModel, state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CintraChatScreenContent(
    viewModel: CintraChatViewModel,
    state: CintraChatUiState,
) {
    val palette = CintraThemePalette
    val backdrop = rememberLayerBackdrop()
    val hazeState = rememberHazeState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "مستند"
            viewModel.addAttachment(Attachment(AttachmentType.FILE, name, uri.toString()))
            viewModel.showToast("تم إرفاق $name")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CintraDrawerContent(
                    hazeState = hazeState,
                    state = state,
                    onNewChat = { viewModel.newChat(); scope.launch { drawerState.close() } },
                    onSelectChat = { id -> viewModel.switchSession(id); scope.launch { drawerState.close() } },
                    onToggleTheme = viewModel::toggleTheme,
                )
            },
            scrimColor = palette.scrim,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Source layer: everything that should refract/blur behind glass.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .cintraMesh(palette)
                        .layerBackdrop(backdrop)
                        .hazeSource(state = hazeState),
                ) {
                    ChatStream(state = state, viewModel = viewModel)
                }

                FloatingGlassTopBar(
                    backdrop = backdrop,
                    modelName = state.selectedModel.name,
                    darkTheme = state.darkTheme,
                    onMenu = { scope.launch { drawerState.open() } },
                    onModel = viewModel::openModelPicker,
                    onTheme = viewModel::toggleTheme,
                    onNewChat = viewModel::newChat,
                )

                if (state.plusMenuOpen) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(onClick = viewModel::closePlusMenu),
                    )
                }

                FloatingGlassComposer(
                    backdrop = backdrop,
                    text = state.composerText,
                    onTextChange = viewModel::onComposerTextChange,
                    activeTool = state.activeTool,
                    attachments = state.attachments,
                    plusOpen = state.plusMenuOpen,
                    onTogglePlus = viewModel::togglePlusMenu,
                    onPlusAction = { action -> handlePlusAction(action, viewModel, filePicker) },
                    onClearTool = viewModel::clearTool,
                    onRemoveAttachment = viewModel::removeAttachment,
                    isRecordingMic = state.isRecordingMic,
                    onMic = viewModel::toggleMic,
                    isGenerating = state.isGenerating,
                    onSend = { viewModel.sendMessage() },
                    onStop = viewModel::stopGeneration,
                    onLive = viewModel::openLive,
                )
            }
        }

        // ---- Overlays (above drawer) ----
        ModelPickerGlassModal(
            backdrop = backdrop,
            visible = state.modelPickerOpen,
            models = CintraModels,
            selected = state.selectedModel,
            onSelect = viewModel::selectModel,
            onDismiss = viewModel::closeModelPicker,
        )

        GooglePhotosSheet(
            backdrop = backdrop,
            visible = state.photosPickerOpen,
            onPick = { name ->
                viewModel.addAttachment(Attachment(AttachmentType.PHOTO, name))
                viewModel.showToast("تم استيراد الصورة من صور جوجل")
            },
            onDismiss = viewModel::closePhotosPicker,
        )

        ArtifactsGlassSheet(
            hazeState = hazeState,
            visible = state.artifactsOpen,
            title = state.artifactTitle,
            code = state.artifactCode,
            onClose = viewModel::closeArtifacts,
            onCopy = { copyToClipboard(context, it); viewModel.showToast("تم النسخ") },
        )

        CintraLiveOverlay(
            hazeState = hazeState,
            visible = state.isLiveActive,
            modelName = state.selectedModel.name,
            muted = state.isLiveMuted,
            onToggleMute = viewModel::toggleLiveMute,
            onEnd = { viewModel.closeLive(); viewModel.showToast("تم إنهاء التحدث المباشر") },
            onOpenFlutterGlass = {
                context.startActivity(android.content.Intent(context, com.cintra.app.GlassFlutterActivity::class.java))
            },
        )

        state.toast?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(1900)
                viewModel.consumeToast()
            }
            ToastPill(message)
        }
    }
}

private fun handlePlusAction(
    action: PlusAction,
    viewModel: CintraChatViewModel,
    filePicker: androidx.activity.result.ActivityResultLauncher<Array<String>>,
) {
    when (action) {
        PlusAction.THINKING -> viewModel.toggleTool(ActiveTool.THINKING)
        PlusAction.WEB -> viewModel.toggleTool(ActiveTool.WEB)
        PlusAction.IMAGE -> viewModel.toggleTool(ActiveTool.IMAGE)
        PlusAction.PHOTOS -> viewModel.openPhotosPicker()
        PlusAction.FILES -> filePicker.launch(arrayOf("*/*"))
    }
}

// ---- Chat stream ----------------------------------------------------------

@Composable
private fun ChatStream(state: CintraChatUiState, viewModel: CintraChatViewModel) {
    val messages = state.messages
    val context = LocalContext.current
    val listState = rememberLazyListState()
    // Follow the stream when a message is added or when streaming starts/stops,
    // rather than scrolling on every streamed chunk (avoids per-chunk layout churn).
    val lastStreaming = messages.lastOrNull()?.isStreaming == true
    LaunchedEffect(messages.size, lastStreaming) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    if (messages.isEmpty()) {
        WelcomeView(onPrompt = { viewModel.sendMessage(it) })
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 84.dp,
            bottom = 182.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            MessageItem(
                message = message,
                onToggleReasoning = { viewModel.toggleReasoningExpanded(message.id) },
                onCopy = { viewModel.showToast("تم النسخ"); copyToClipboard(context, it) },
                onOpenArtifacts = { lang, code ->
                    viewModel.openArtifacts("Artifact — ${lang.uppercase()}", code)
                },
                onRegenerate = { viewModel.regenerate(message.id) },
                onTab = { tab -> viewModel.setComparisonTab(message.id, tab) },
                onToggleCheck = { index -> viewModel.toggleChecklistItem(message.id, index) },
                modifier = Modifier.widthIn(max = 780.dp).fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeView(onPrompt: (String) -> Unit) {
    val palette = CintraThemePalette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 84.dp, bottom = 182.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surface)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("مرحباً بك في Cintra", color = palette.textPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "اضغط على + لفتح أدوات البحث والتفكير وإنشاء الصور واستيراد ملفاتك، أو اضغط أيقونة الموجات للتحدث المباشر.",
            color = palette.textSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            modifier = Modifier.widthIn(max = 380.dp),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PromptTile(Icons.Filled.Checklist, "خطة تفاعلية", "اعمل لي خطة تنفيذ تفاعلية لبناء تطبيق Cintra بـ Kotlin", palette, onPrompt)
            PromptTile(Icons.Filled.AutoAwesome, "إنشاء صور", "أنشئ لي تصوراً بصرياً لواجهة تطبيق زجاجي عصري", palette, onPrompt)
            PromptTile(Icons.Filled.Code, "كود + جدول", "اكتب كود Ktor SSE مع جدول مقارنة للطبقات", palette, onPrompt)
        }
    }
}

@Composable
private fun PromptTile(
    icon: ImageVector,
    tag: String,
    text: String,
    palette: com.cintra.app.ui.theme.CintraPalette,
    onClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(20.dp))
            .clickable { onClick(text) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(13.dp))
            Text(tag, color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(text, color = palette.textPrimary, fontSize = 13.5.sp, lineHeight = 20.sp)
    }
}

// ---- Drawer ---------------------------------------------------------------

@Composable
private fun CintraDrawerContent(
    hazeState: dev.chrisbanes.haze.HazeState,
    state: CintraChatUiState,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val palette = CintraThemePalette
    val shape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp)
    Column(
        modifier = Modifier
            .widthIn(max = 306.dp)
            .fillMaxWidth(0.84f)
            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
            .clip(shape)
            .border(1.dp, palette.borderSubtle, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.accentSolid),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accentSolidText, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text("Cintra", color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.surface)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
                .clickable(onClick = onNewChat)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("محادثة جديدة", color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
        }

        Text(
            "المحادثات الأخيرة",
            color = palette.textMuted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.sessions.forEach { session ->
                val active = session.id == state.activeSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) palette.surface else Color.Transparent)
                        .clickable { onSelectChat(session.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        session.title,
                        color = if (active) palette.textPrimary else palette.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(1.dp)
                .background(palette.borderSubtle),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("المظهر", color = palette.textSecondary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surface)
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (state.darkTheme) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Text(if (state.darkTheme) "نهاري" else "ليلي", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---- Misc -----------------------------------------------------------------

@Composable
private fun ToastPill(message: String) {
    val palette = CintraThemePalette
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .padding(top = 70.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.glassTintModal)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(message, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Modifier.cintraMesh(palette: com.cintra.app.ui.theme.CintraPalette): Modifier =
    this
        .background(palette.canvas)
        .drawBehind {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.65f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.textPrimary.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.84f),
                    radius = w * 0.55f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.48f),
                    radius = w * 0.5f,
                ),
            )
        }

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("cintra", text))
}