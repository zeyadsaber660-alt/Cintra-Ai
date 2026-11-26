package com.cintra.app.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.cintra.app.ui.model.CintraModel
import com.cintra.app.ui.theme.HyperOsGlassSelectionMenu
import com.cintra.app.ui.theme.HyperOsMenuOption
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState

/**
 * Cintra 4-model picker powered by [HyperOsGlassSelectionMenu] (HyperOS 4 Liquid Glass
 * spring physics, sliding selection pill, haptic feedback, and 30ms staggered reveal).
 */
@Composable
fun ModelPickerGlassModal(
    backdrop: Backdrop,
    hazeState: HazeState,
    visible: Boolean,
    models: List<CintraModel>,
    selected: CintraModel,
    onSelect: (CintraModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember(models) {
        models.map { m ->
            HyperOsMenuOption(
                value = m,
                title = m.name,
                subtitle = m.description,
                badge = m.badge,
            )
        }
    }

    HyperOsGlassSelectionMenu(
        backdrop = backdrop,
        hazeState = hazeState,
        visible = visible,
        title = "نماذج Cintra",
        subtitle = "اختر النموذج الأنسب لمهمتك",
        options = options,
        selectedValue = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
        alignment = Alignment.TopCenter,
        transformOrigin = TransformOrigin(0.5f, 0f),
        topPadding = 96.dp,
        itemHeight = 64.dp,
    )
}