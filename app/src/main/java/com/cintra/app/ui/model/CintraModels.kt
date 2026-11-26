package com.cintra.app.ui.model

/**
 * The four Cintra models shown in the centered model pill + picker modal.
 * The collapsible Thinking card is driven by the active model id / deep-thinking
 * tool (see CintraChatViewModel), not by a per-model flag.
 */
enum class CintraModelId { PRO, THINK, CORE, VISION }

data class CintraModel(
    val id: CintraModelId,
    val name: String,
    val badge: String,
    val description: String,
)

val CintraModels: List<CintraModel> = listOf(
    CintraModel(
        id = CintraModelId.PRO,
        name = "Cintra Pro",
        badge = "الأشمل",
        description = "البرمجة والمشاريع والواجهات التفاعلية",
    ),
    CintraModel(
        id = CintraModelId.THINK,
        name = "Cintra Think",
        badge = "تفكير عميق",
        description = "تحليل منطقي خطوة بخطوة",
    ),
    CintraModel(
        id = CintraModelId.CORE,
        name = "Cintra Core",
        badge = "سريع",
        description = "استجابة فورية للمحادثات اليومية",
    ),
    CintraModel(
        id = CintraModelId.VISION,
        name = "Cintra Vision",
        badge = "وسائط",
        description = "فهم صور جوجل والملفات والمستندات",
    ),
)

val DefaultCintraModel: CintraModel = CintraModels.first { it.id == CintraModelId.PRO }