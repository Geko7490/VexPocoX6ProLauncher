package com.vex.launcher.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vex.launcher.core.model.AccentTheme
import com.vex.launcher.core.model.AnimationLevel
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.core.model.WallpaperChoice
import com.vex.launcher.core.util.HomePaginator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val TAG = "VexSettings"
private const val LIST_SEPARATOR = "|"

/**
 * DataStore Preferences tabanlı ayar deposu.
 * Tüm anahtarlar tek bir yerde tanımlanır; okuma/yazma tek noktadan yönetilir.
 */
private val Context.vexSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "vex_launcher_settings",
)

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT = stringPreferencesKey("accent")
        val ICON_SCALE = intPreferencesKey("icon_scale")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val LABEL_SCALE = intPreferencesKey("label_scale")
        val ANIMATION_LEVEL = stringPreferencesKey("animation_level")
        val DOCK_PACKAGES = stringPreferencesKey("dock_packages")
        val HIDDEN_PACKAGES = stringPreferencesKey("hidden_packages")
        val HOME_PAGES = intPreferencesKey("home_pages")
        val WALLPAPER = stringPreferencesKey("wallpaper")
    }

    override val settings: Flow<LauncherSettings> = context.vexSettingsStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Ayarlar okunamadı, varsayılanlar kullanılıyor", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toSettings() }

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        context.vexSettingsStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            preferences[Keys.DARK_THEME] = updated.darkTheme
            preferences[Keys.ACCENT] = updated.accent.name
            preferences[Keys.ICON_SCALE] = (updated.iconScale * 100).toInt()
            preferences[Keys.GRID_COLUMNS] = updated.gridColumns
            preferences[Keys.GRID_ROWS] = updated.gridRows
            preferences[Keys.SHOW_LABELS] = updated.showLabels
            preferences[Keys.LABEL_SCALE] = (updated.labelScale * 100).toInt()
            preferences[Keys.ANIMATION_LEVEL] = updated.animationLevel.name
            preferences[Keys.DOCK_PACKAGES] = updated.dockPackages.encode()
            preferences[Keys.HIDDEN_PACKAGES] = updated.hiddenPackages.encode()
            preferences[Keys.HOME_PAGES] = updated.homePages
            preferences[Keys.WALLPAPER] = updated.wallpaper.name
        }
    }

    override suspend fun reset() {
        context.vexSettingsStore.edit { preferences -> preferences.clear() }
    }

    private fun Preferences.toSettings(): LauncherSettings {
        val defaults = LauncherSettings()
        return LauncherSettings(
            darkTheme = this[Keys.DARK_THEME] ?: defaults.darkTheme,
            accent = this[Keys.ACCENT]?.toEnum(defaults.accent) ?: defaults.accent,
            iconScale = (this[Keys.ICON_SCALE] ?: 100).coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f,
            gridColumns = (this[Keys.GRID_COLUMNS] ?: defaults.gridColumns)
                .coerceIn(HomePaginator.MIN_COLUMNS, HomePaginator.MAX_COLUMNS),
            gridRows = (this[Keys.GRID_ROWS] ?: defaults.gridRows)
                .coerceIn(HomePaginator.MIN_ROWS, HomePaginator.MAX_ROWS),
            showLabels = this[Keys.SHOW_LABELS] ?: defaults.showLabels,
            labelScale = (this[Keys.LABEL_SCALE] ?: 100).coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f,
            animationLevel = this[Keys.ANIMATION_LEVEL]?.toEnum(defaults.animationLevel)
                ?: defaults.animationLevel,
            dockPackages = this[Keys.DOCK_PACKAGES].decodeList(),
            hiddenPackages = this[Keys.HIDDEN_PACKAGES].decodeList(),
            homePages = (this[Keys.HOME_PAGES] ?: defaults.homePages)
                .coerceIn(HomePaginator.MIN_PAGES, HomePaginator.MAX_PAGES),
            wallpaper = this[Keys.WALLPAPER]?.toEnum(defaults.wallpaper) ?: defaults.wallpaper,
        )
    }

    private inline fun <reified T : Enum<T>> String.toEnum(fallback: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: fallback

    private fun List<String>.encode(): String = joinToString(LIST_SEPARATOR)

    private fun String?.decodeList(): List<String> =
        if (this.isNullOrBlank()) emptyList() else split(LIST_SEPARATOR).filter { it.isNotBlank() }

    private companion object {
        const val MIN_PERCENT = 60
        const val MAX_PERCENT = 150
    }
}
