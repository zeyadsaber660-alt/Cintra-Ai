package com.cintra.app.ui.chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cintra.app.R
import com.cintra.app.ui.chat.components.ArtifactsGlassSheet
import com.cintra.app.ui.chat.components.CintraLiveOverlay
import com.cintra.app.ui.chat.components.FloatingGlassComposer
import com.cintra.app.ui.chat.components.FloatingGlassTopBar
import com.cintra.app.ui.chat.components.MessageItem
import com.cintra.app.ui.chat.components.ModelPickerGlassModal
import com.cintra.app.ui.chat.components.PlusAction
import com.cintra.app.ui.model.ActiveTool
import com.cintra.app.ui.model.Attachment
import com.cintra.app.ui.model.AttachmentType
import com.cintra.app.ui.model.CintraModels
import com.cintra.app.ui.pages.CustomizeCintraScreen
import com.cintra.app.ui.pages.FeatureDestination
import com.cintra.app.ui.pages.ImagesGalleryScreen
import com.cintra.app.ui.pages.LibraryScreen
import com.cintra.app.ui.pages.ProjectsScreen
import com.cintra.app.ui.auth.CintraOnboardingScreen
import com.cintra.app.ui.settings.CintraSettingsScreen
import com.cintra.app.utils.CintraPdfExporter
import com.cintra.app.ui.theme.CintraIcons
import com.cintra.app.ui.theme.CintraPalette
import com.cintra.app.ui.theme.CintraTheme
import com.cintra.app.ui.theme.CintraThemePalette
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CintraChatScreen(
    viewModel: CintraChatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CintraTheme(darkTheme = state.darkTheme, isArabic = state.isArabic) {
        if (!state.hasCompletedOnboarding) {
            CintraOnboardingScreen(
                isArabic = state.isArabic,
                onContinue = viewModel::completeOnboarding,
            )
        } else {
            CintraChatScreenContent(viewModel, state)
        }
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

    val nativePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> viewModel.attachUri(uri, AttachmentType.PHOTO) }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach { uri -> viewModel.attachUri(uri, AttachmentType.FILE) }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.setMicRecording(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                viewModel.appendDictatedText(spoken)
            }
        }
    }


    // Native Android Back gesture / button support across all overlays, pages, and drawer
    val hasOpenOverlay = state.settingsOpen ||
        state.activeFeature != FeatureDestination.NONE ||
        state.modelPickerOpen ||
        state.artifactsOpen ||
        state.isLiveActive ||
        state.plusMenuOpen ||
        drawerState.isOpen

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible || hasOpenOverlay) {
        when {
            isImeVisible -> {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            state.modelPickerOpen -> viewModel.closeModelPicker()
            state.artifactsOpen -> viewModel.closeArtifacts()
            state.isLiveActive -> viewModel.closeLive()
            state.plusMenuOpen -> viewModel.closePlusMenu()
            state.activeFeature != FeatureDestination.NONE -> viewModel.closeFeature()
            state.settingsOpen -> viewModel.closeSettings()
            drawerState.isOpen -> scope.launch { drawerState.close() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ChatGptStyleDrawer(
                    hazeState = hazeState,
                    state = state,
                    onToggleSearch = viewModel::toggleDrawerSearch,
                    onSearchChange = viewModel::onDrawerSearchChange,
                    onNewChat = { viewModel.newChat(); scope.launch { drawerState.close() } },
                    onSelectChat = { id -> viewModel.switchSession(id); scope.launch { drawerState.close() } },
                    onOpenModels = { scope.launch { drawerState.close() }; viewModel.openModelPicker() },
                    onOpenSettings = { scope.launch { drawerState.close() }; viewModel.openSettings() },
                    onOpenFeature = { dest -> scope.launch { drawerState.close() }; viewModel.openFeature(dest) },
                )
            },
            scrimColor = palette.scrim,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .cintraMesh(palette)
                        .layerBackdrop(backdrop)
                        .hazeSource(state = hazeState),
                ) {
                    AnimatedContent(
                        targetState = state.activeSessionId,
                        transitionSpec = {
                            (fadeIn(tween(240)) + scaleIn(initialScale = 0.97f, animationSpec = tween(240)))
                                .togetherWith(fadeOut(tween(150)))
                        },
                        label = "chatSwitch",
                    ) { _ ->
                        ChatStream(state = state, viewModel = viewModel)
                    }
                }

                FloatingGlassTopBar(
                    backdrop = backdrop,
                    modelName = state.selectedModel.name,
                    darkTheme = state.darkTheme,
                    onMenu = { scope.launch { drawerState.open() } },
                    onModel = viewModel::openModelPicker,
                    onTheme = viewModel::toggleTheme,
                    onNewChat = viewModel::newChat,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                if (state.plusMenuOpen) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
                            .background(palette.scrim.copy(alpha = 0.35f))
                            .clickable(onClick = viewModel::closePlusMenu),
                    )
                }

                FloatingGlassComposer(
                    backdrop = backdrop,
                    hazeState = hazeState,
                    text = state.composerText,
                    onTextChange = viewModel::onComposerTextChange,
                    activeTool = state.activeTool,
                    attachments = state.attachments,
                    plusOpen = state.plusMenuOpen,
                    onTogglePlus = viewModel::togglePlusMenu,
                    onPlusAction = { action ->
                        when (action) {
                            PlusAction.THINKING -> viewModel.toggleTool(ActiveTool.THINKING)
                            PlusAction.WEB -> viewModel.toggleTool(ActiveTool.WEB)
                            PlusAction.IMAGE -> viewModel.toggleTool(ActiveTool.IMAGE)
                            PlusAction.PHOTOS -> {
                                viewModel.closePlusMenu()
                                nativePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                )
                            }
                            PlusAction.FILES -> {
                                viewModel.closePlusMenu()
                                filePicker.launch(arrayOf("*/*"))
                            }
                        }
                    },
                    onClearTool = viewModel::clearTool,
                    onRemoveAttachment = viewModel::removeAttachment,
                    isRecordingMic = state.isRecordingMic,
                    onMic = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (state.isArabic) "ar-EG" else "en-US")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, if (state.isArabic) "تحدث الآن..." else "Speak now...")
                        }
                        runCatching {
                            viewModel.setMicRecording(true)
                            speechLauncher.launch(intent)
                        }.onFailure {
                            viewModel.setMicRecording(false)
                            viewModel.showToast(if (state.isArabic) "الإملاء الصوتي غير متاح" else "Voice input unavailable")
                        }
                    },
                    isGenerating = state.isGenerating,
                    isArabic = state.isArabic,
                    onSend = { viewModel.sendMessage() },
                    onStop = viewModel::stopGeneration,
                    onLive = viewModel::openLive,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // HyperOS 4 Liquid Glass Model Picker
        ModelPickerGlassModal(
            backdrop = backdrop,
            hazeState = hazeState,
            visible = state.modelPickerOpen,
            models = CintraModels,
            selected = state.selectedModel,
            onSelect = viewModel::selectModel,
            onDismiss = viewModel::closeModelPicker,
        )

        // Real Working Feature Pages (Zero fake Toasts!)
        ImagesGalleryScreen(
            visible = state.activeFeature == FeatureDestination.IMAGES,
            hazeState = hazeState,
            photos = state.allPhotos,
            isArabic = state.isArabic,
            onAddPhoto = {
                nativePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onBack = viewModel::closeFeature,
        )

        LibraryScreen(
            visible = state.activeFeature == FeatureDestination.LIBRARY,
            hazeState = hazeState,
            files = state.allFiles,
            codeSnippets = state.allCodeSnippets,
            isArabic = state.isArabic,
            onOpenCode = { lang, code ->
                viewModel.closeFeature()
                viewModel.openArtifacts("Artifact — ${lang.uppercase()}", code)
            },
            onBack = viewModel::closeFeature,
        )

        ProjectsScreen(
            visible = state.activeFeature == FeatureDestination.PROJECTS,
            hazeState = hazeState,
            projects = state.projects,
            activeProjectId = state.activeProjectId,
            isArabic = state.isArabic,
            onCreateProject = viewModel::createProject,
            onSelectProject = viewModel::selectProject,
            onBack = viewModel::closeFeature,
        )

        CustomizeCintraScreen(
            visible = state.activeFeature == FeatureDestination.CUSTOMIZE,
            hazeState = hazeState,
            userName = state.userName,
            customInstructions = state.customInstructions,
            isArabic = state.isArabic,
            onSave = viewModel::saveProfileAndInstructions,
            onBack = viewModel::closeFeature,
        )

        // Real Settings Screen (Zero fake auth/upgrade)
        CintraSettingsScreen(
            visible = state.settingsOpen,
            backdrop = backdrop,
            hazeState = hazeState,
            userName = state.userName,
            authModeLabel = if (state.isArabic) state.authMode.labelAr else state.authMode.labelEn,
            fontScale = state.fontScale,
            responseTone = state.responseTone,
            darkTheme = state.darkTheme,
            isArabic = state.isArabic,
            autoWebSearch = state.autoWebSearch,
            illustrativeImages = state.illustrativeImages,
            totalChatsCount = state.sessions.size,
            totalMessagesCount = state.totalMessagesCount,
            onSetDarkTheme = viewModel::setDarkTheme,
            onSetArabic = viewModel::setArabic,
            onToggleAutoWebSearch = viewModel::toggleAutoWebSearch,
            onToggleIllustrativeImages = viewModel::toggleIllustrativeImages,
            onSetFontScale = viewModel::setFontScale,
            onSetResponseTone = viewModel::setResponseTone,
            onSwitchAuthMode = viewModel::reopenOnboarding,
            onTestSamplePdf = {
                CintraPdfExporter.exportHtmlToPdf(
                    context = context,
                    htmlContent = CintraPdfExporter.sampleArabicInvoiceHtml(),
                    fileName = "Cintra_Sample_Report_AR",
                )
            },
            onOpenCustomize = { viewModel.openFeature(FeatureDestination.CUSTOMIZE) },
            onClearAllChats = viewModel::clearAllChats,
            onShowToast = viewModel::showToast,
            onClose = viewModel::closeSettings,
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
                context.startActivity(Intent(context, com.cintra.app.GlassFlutterActivity::class.java))
            },
        )

        state.toast?.let { message ->
            LaunchedEffect(message) {
                delay(1900)
                viewModel.consumeToast()
            }
            ToastPill(message)
        }
    }
}

@Composable
private fun ChatStream(state: CintraChatUiState, viewModel: CintraChatViewModel) {
    val messages = state.messages
    val context = LocalContext.current
    val palette = CintraThemePalette
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val showScrollDown by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible < total - 1
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !showScrollDown) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        WelcomeView(isArabic = state.isArabic)
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 74.dp,
                bottom = 152.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageItem(
                    message = message,
                    onToggleReasoning = { viewModel.toggleReasoningExpanded(message.id) },
                    onCopy = { viewModel.showToast("تم النسخ"); copyToClipboard(context, it) },
                    onExportPdf = { md ->
                        val html = CintraPdfExporter.buildArabicHtmlFromMarkdown("تقرير Cintra AI", md)
                        CintraPdfExporter.exportHtmlToPdf(context, html, "Cintra_Response_${System.currentTimeMillis()}")
                    },
                    fontScale = state.fontScale,
                    onOpenArtifacts = { lang, code ->
                        viewModel.openArtifacts("Artifact — ${lang.uppercase()}", code)
                    },
                    onRegenerate = { viewModel.regenerate(message.id) },
                    onTab = { tab -> viewModel.setComparisonTab(message.id, tab) },
                    onToggleCheck = { index -> viewModel.toggleChecklistItem(message.id, index) },
                    onEditUser = { viewModel.editUserMessage(message.id) },
                    onResendUser = { viewModel.resendUserMessage(message.id) },
                    onSuggestionClick = { q -> viewModel.sendMessage(q) },
                    modifier = Modifier
                        .animateItem()
                        .widthIn(max = 780.dp)
                        .fillMaxWidth(),
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollDown,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.surface.copy(alpha = 0.94f))
                    .border(0.85.dp, palette.borderSubtle, CircleShape)
                    .clickable {
                        scope.launch {
                            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CintraIcons.ChevronDown,
                    contentDescription = "النزول للأسفل",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun WelcomeView(isArabic: Boolean) {
    val palette = CintraThemePalette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 74.dp, bottom = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.cintra_logo),
            contentDescription = "Cintra Logo",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp)),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isArabic) "بمَ أساعدك اليوم؟" else "What can I help with?",
            color = palette.textPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isArabic) {
                "اكتب سؤالك أو اضغط + لإضافة صور وملفات أو استخدم التحدث المباشر."
            } else {
                "Type your question, tap + for photos & files, or start Cintra Live."
            },
            color = palette.textSecondary,
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

@Composable
private fun ChatGptStyleDrawer(
    hazeState: HazeState,
    state: CintraChatUiState,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFeature: (FeatureDestination) -> Unit,
) {
    val palette = CintraThemePalette
    val isArabic = state.isArabic
    val shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    val initials = remember(state.userName) {
        state.userName.trim().split(" ").filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "CI" }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 312.dp)
            .fillMaxWidth(0.84f)
            .clip(shape)
            .hazeEffect(state = hazeState, style = HazeMaterials.thick())
            .background(palette.glassTintModal)
            .border(0.85.dp, palette.glassBorder, shape)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Cintra", color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.surface.copy(alpha = 0.85f))
                    .border(0.75.dp, palette.borderSubtle, CircleShape)
                    .clickable(onClick = onToggleSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CintraIcons.Search, contentDescription = "بحث", tint = palette.textPrimary, modifier = Modifier.size(19.dp))
            }
        }

        AnimatedVisibility(visible = state.drawerSearchVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surface)
                    .border(0.75.dp, palette.borderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(CintraIcons.Search, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = state.drawerSearchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(color = palette.textPrimary, fontSize = 13.5.sp),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (state.drawerSearchQuery.isEmpty()) {
                            Text(if (isArabic) "ابحث في المحادثات..." else "Search chats...", color = palette.textMuted, fontSize = 13.5.sp)
                        }
                        inner()
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DrawerShortcutRow(if (isArabic) "الصور" else "Images", CintraIcons.Images) {
                onOpenFeature(FeatureDestination.IMAGES)
            }
            DrawerShortcutRow(if (isArabic) "المكتبة" else "Library", CintraIcons.Library) {
                onOpenFeature(FeatureDestination.LIBRARY)
            }
            DrawerShortcutRow(if (isArabic) "تخصيص" else "Customize", CintraIcons.Sliders) {
                onOpenFeature(FeatureDestination.CUSTOMIZE)
            }
            DrawerShortcutRow(if (isArabic) "المشاريع" else "Projects", CintraIcons.Folder) {
                onOpenFeature(FeatureDestination.PROJECTS)
            }
            DrawerShortcutRow(if (isArabic) "استكشاف النماذج" else "Explore", CintraIcons.ExploreGrid) {
                onOpenModels()
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(palette.borderSubtle))

        Text(
            text = if (isArabic) "المحادثات الأخيرة" else "Recent",
            color = palette.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.filteredSessions, key = { it.id }) { session ->
                val active = session.id == state.activeSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) palette.surface.copy(alpha = 0.92f) else Color.Transparent)
                        .clickable { onSelectChat(session.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = session.title,
                        color = if (active) palette.textPrimary else palette.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF00B894))
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(CintraIcons.NewChatSquare, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                Text(
                    text = if (isArabic) "محادثة" else "Chat",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.surface)
                    .border(1.dp, palette.borderSubtle, CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA63A2E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DrawerShortcutRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val palette = CintraThemePalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(20.dp))
        Text(text = title, color = palette.textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToastPill(message: String) {
    val palette = CintraThemePalette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 62.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.glassTintModal)
                .border(0.85.dp, palette.glassBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(message, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Modifier.cintraMesh(palette: CintraPalette): Modifier =
    this
        .background(palette.canvas)
        .drawBehind {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = if (palette.isDark) 0.08f else 0.05f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.65f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.textPrimary.copy(alpha = 0.03f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.84f),
                    radius = w * 0.55f,
                ),
            )
        }

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("cintra", text))
}