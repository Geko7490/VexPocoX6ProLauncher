package com.vex.launcher.ui.theme

import androidx.compose.ui.graphics.Color
import com.vex.launcher.core.model.AccentTheme
import com.vex.launcher.core.model.WallpaperChoice

// --- Temel yüzeyler (One UI benzeri koyu yüzeyler) ---
val Obsidian = Color(0xFF07070B)
val Charcoal = Color(0xFF14141B)
val CharcoalHigh = Color(0xFF1C1C25)
val CharcoalHighest = Color(0xFF23232E)
val OutlineDark = Color(0xFF2C2C38)

// --- Vurgu renkleri ---
val Violet = Color(0xFF8B5CF6)
val VioletBright = Color(0xFFA78BFA)
val Magenta = Color(0xFFD946EF)
val Indigo = Color(0xFF6366F1)
val Teal = Color(0xFF14B8A6)
val Amber = Color(0xFFF59E0B)

// --- Metin ---
val TextPrimaryDark = Color(0xFFF2F2F7)
val TextSecondaryDark = Color(0xFF9E9EAE)
val TextPrimaryLight = Color(0xFF14141B)
val TextSecondaryLight = Color(0xFF56566A)

/** Seçilen vurgu temasının ana rengi. */
fun AccentTheme.primary(): Color = when (this) {
    AccentTheme.VIOLET -> Violet
    AccentTheme.MAGENTA -> Magenta
    AccentTheme.INDIGO -> Indigo
    AccentTheme.TEAL -> Teal
    AccentTheme.AMBER -> Amber
}

/** Vurgu temasının açık varyantı (ikon arka planları, seçim halkaları). */
fun AccentTheme.primaryBright(): Color = when (this) {
    AccentTheme.VIOLET -> VioletBright
    AccentTheme.MAGENTA -> Color(0xFFE879F9)
    AccentTheme.INDIGO -> Color(0xFF818CF8)
    AccentTheme.TEAL -> Color(0xFF2DD4BF)
    AccentTheme.AMBER -> Color(0xFFFBBF24)
}

/** Duvar kağıdı seçeneğinin uygulama içi gradyan renkleri. */
fun WallpaperChoice.gradientColors(): List<Color> = when (this) {
    WallpaperChoice.OBSIDIAN -> listOf(Color(0xFF07070B), Color(0xFF0C0C12))
    WallpaperChoice.PURPLE -> listOf(Color(0xFF140D26), Color(0xFF1D1132), Color(0xFF07070B))
    WallpaperChoice.MIDNIGHT -> listOf(Color(0xFF060A16), Color(0xFF0B1226), Color(0xFF05060C))
    WallpaperChoice.GRAPHITE -> listOf(Color(0xFF0F0F13), Color(0xFF17171D))
    WallpaperChoice.CUSTOM -> listOf(Color(0xFF0A0A0F), Color(0xFF121219))
}
