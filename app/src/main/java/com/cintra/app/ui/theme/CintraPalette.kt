package com.cintra.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cintra design tokens.
 *
 * Two optically-calibrated palettes:
 *  - [Obsidian]  (dark)  — near-black canvas with physical glass.
 *  - [Alabaster] (light) — soft daylight canvas with bright glass.
 *
 * No neon, no default purple/indigo. The only restrained accent is Steel Sky Blue.
 */
@Immutable
data class CintraPalette(
    val isDark: Boolean,
    // Canvas & surfaces
    val canvas: Color,
    val surface: Color,
    val surface2: Color,
    val surfaceHover: Color,
    val userBubble: Color,
    // Code surfaces (always dark for contrast in both themes)
    val codeBackground: Color,
    val codeHeader: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Borders
    val borderSubtle: Color,
    // Solid accent (used for primary action button / brand mark)
    val accentSolid: Color,
    val accentSolidText: Color,
    // Steel Sky Blue accent + soft container
    val accent: Color,
    val accentSoft: Color,
    // Semantic accents
    val emerald: Color,
    val emeraldSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val rose: Color,
    val roseSoft: Color,
    // Liquid glass
    val glassTint: Color,
    val glassTintModal: Color,
    val rimTop: Color,
    val rimSide: Color,
    val rimBottom: Color,
    val scrim: Color,
)

val ObsidianPalette = CintraPalette(
    isDark = true,
    canvas = Color(0xFF0C0D10),
    surface = Color(0xFF13151B),
    surface2 = Color(0xFF1A1D25),
    surfaceHover = Color(0xFF222631),
    userBubble = Color(0x17FFFFFF),
    codeBackground = Color(0xFF0D0F14),
    codeHeader = Color(0xFF171A22),
    textPrimary = Color(0xFFF5F6F8),
    textSecondary = Color(0xFFA1A7B5),
    textMuted = Color(0xFF6C7382),
    borderSubtle = Color(0x17FFFFFF),
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
    glassTint = Color(0x59161921),
    glassTintModal = Color(0x7A12151C),
    rimTop = Color(0x6BFFFFFF),
    rimSide = Color(0x29FFFFFF),
    rimBottom = Color(0x14FFFFFF),
    scrim = Color(0x61040508),
)

val AlabasterPalette = CintraPalette(
    isDark = false,
    canvas = Color(0xFFEFF1F4),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF4F5F8),
    surfaceHover = Color(0xFFE7EAF0),
    userBubble = Color(0x120F172A),
    codeBackground = Color(0xFF12151C),
    codeHeader = Color(0xFF1C202A),
    textPrimary = Color(0xFF101319),
    textSecondary = Color(0xFF4E5666),
    textMuted = Color(0xFF7C8496),
    borderSubtle = Color(0x17101319),
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
    glassTint = Color(0x70FFFFFF),
    glassTintModal = Color(0x8FFFFFFF),
    rimTop = Color(0xFAFFFFFF),
    rimSide = Color(0xA6FFFFFF),
    rimBottom = Color(0x240F172A),
    scrim = Color(0x290F172A),
)