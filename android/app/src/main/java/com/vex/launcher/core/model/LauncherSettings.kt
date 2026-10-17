package com.vex.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Kullanıcı tarafından değiştirilebilen tüm ayarların tek bir immutable kopyası.
 * DataStore bu modeli okur/yazar; UI katmanı yalnızca bu modeli görür.
 */
@Immutable
data class LauncherSettings(
    val darkTheme: Boolean = true,
    val accent: AccentTheme = AccentTheme.VIOLET,
    val iconScale: Float = 1f,
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val showLabels: Boolean = true,
    val labelScale: Float = 1f,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
    val dockPackages: List<String> = emptyList(),
    val hiddenPackages: List<String> = emptyList(),
    val homePages: Int = 2,
    val wallpaper: WallpaperChoice = WallpaperChoice.PURPLE,
)

/** Animasyon yoğunluğu. Sistem animasyon ölçeği 0 ise zorla OFF uygulanır. */
enum class AnimationLevel { OFF, REDUCED, FULL }

/** Vurgu (accent) renk paletleri. */
enum class AccentTheme { VIOLET, MAGENTA, INDIGO, TEAL, AMBER }

/** Duvar kağıdı seçenekleri. */
enum class WallpaperChoice { OBSIDIAN, PURPLE, MIDNIGHT, GRAPHITE, CUSTOM }
