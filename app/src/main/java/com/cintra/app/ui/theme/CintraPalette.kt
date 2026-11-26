package com.cintra.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cintra design tokens — calibrated for true Frosted Liquid Glass (زجاج مصفّر):
 *  - [ObsidianPalette]  (dark)  — deep charcoal canvas with high-contrast frosted obsidian glass.
 *  - [AlabasterPalette] (light) — clean daylight canvas with frosted alabaster glass.
 *
 * High-opacity frosted tints guarantee zero text bleed-through over scrolling chats.
 * Zero neon, zero default purple/indigo.
 */
@Immutable
data class CintraPalette(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surface2: Color,
    val surfaceHover: Color,
    val userBubble: Color,
    val codeBackground: Color,
    val codeHeader: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val accentSolid: Color,
    val accentSolidText: Color,
    val accent: Color,
    val accentSoft: Color,
    val emerald: Color,
    val emeraldSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val rose: Color,
    val roseSoft: Color,
    // Frosted Liquid Glass tokens
    val glassTint: Color,
    val glassTintModal: Color,
    val glassBorder: Color,
    val rimTop: Color,
    val rimSide: Color,
    val rimBottom: Color,
    val scrim: Color,
)

val ObsidianPalette = CintraPalette(
    isDark = true,
    canvas = Color(0xFF0C0D10),
    surface = Color(0xFF151820),
    surface2 = Color(0xFF1E222C),
    surfaceHover = Color(0xFF262B38),
    userBubble = Color(0xFF212530),
    codeBackground = Color(0xFF0D0F14),
    codeHeader = Color(0xFF171A22),
    textPrimary = Color(0xFFF5F6F8),
    textSecondary = Color(0xFFA4AAB8),
    textMuted = Color(0xFF6F7686),
    borderSubtle = Color(0x1FFFFFFF),
    accentSolid = Color(0xFFF5F6F8),
    accentSolidText = Color(0xFF0C0D10),
    accent = Color(0xFF38BDF8),
    accentSoft = Color(0x2638BDF8),
    emerald = Color(0xFF34D399),
    emeraldSoft = Color(0x2634D399),
    amber = Color(0xFFFBBF24),
    amberSoft = Color(0x26FBBF24),
    rose = Color(0xFFFB7185),
    roseSoft = Color(0x26FB7185),
    // Frosted Dark Glass (~76% bar, ~86% modal/drawer)
    glassTint = Color(0xC2171A22),
    glassTintModal = Color(0xDC13161E),
    glassBorder = Color(0x2EFFFFFF),
    rimTop = Color(0x52FFFFFF),
    rimSide = Color(0x24FFFFFF),
    rimBottom = Color(0x14FFFFFF),
    scrim = Color(0x73040508),
)

val AlabasterPalette = CintraPalette(
    isDark = false,
    canvas = Color(0xFFF2F3F6),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEBECEF),
    surfaceHover = Color(0xFFE2E4E9),
    userBubble = Color(0xFFE6E8ED),
    codeBackground = Color(0xFF12151C),
    codeHeader = Color(0xFF1C202A),
    textPrimary = Color(0xFF101319),
    textSecondary = Color(0xFF4E5666),
    textMuted = Color(0xFF7B8394),
    borderSubtle = Color(0x1A101319),
    accentSolid = Color(0xFF101319),
    accentSolidText = Color(0xFFFFFFFF),
    accent = Color(0xFF0284C7),
    accentSoft = Color(0x1F0284C7),
    emerald = Color(0xFF059669),
    emeraldSoft = Color(0x1F059669),
    amber = Color(0xFFD97706),
    amberSoft = Color(0x1FD97706),
    rose = Color(0xFFE11D48),
    roseSoft = Color(0x1FE11D48),
    // Frosted Light Glass (~82% bar, ~90% modal/drawer — crisp, zero bleed-through)
    glassTint = Color(0xD1FAFBFD),
    glassTintModal = Color(0xE6F7F8FA),
    glassBorder = Color(0x1F0F172A),
    rimTop = Color(0xFFFFFFFF),
    rimSide = Color(0xB8FFFFFF),
    rimBottom = Color(0x1F0F172A),
    scrim = Color(0x380F172A),
)