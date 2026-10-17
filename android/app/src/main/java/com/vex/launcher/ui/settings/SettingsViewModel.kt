package com.vex.launcher.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vex.launcher.core.model.AccentTheme
import com.vex.launcher.core.model.AnimationLevel
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.core.model.WallpaperChoice
import com.vex.launcher.data.AppRepository
import com.vex.launcher.data.SettingsRepository
import com.vex.launcher.data.WallpaperRepository
import com.vex.launcher.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ayarlar ekranında kullanıcıya gösterilebilen tek seferlik mesajlar. */
sealed interface SettingsMessage {
    data object WallpaperApplied : SettingsMessage
    data object WallpaperFailed : SettingsMessage
    data object SettingsReset : SettingsMessage
}

/**
 * Ayarlar ViewModel'i.
 * Tüm kalıcı değişiklikler DataStore üzerinden yapılır; UI doğrudan DataStore'a dokunmaz.
 */
class SettingsViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val wallpaperRepository: WallpaperRepository,
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = appRepository.apps

    val settings: StateFlow<LauncherSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LauncherSettings(),
    )

    private val _message = MutableStateFlow<SettingsMessage?>(null)
    val message: StateFlow<SettingsMessage?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    fun setDarkTheme(enabled: Boolean) = update { it.copy(darkTheme = enabled) }

    fun setAccent(accent: AccentTheme) = update { it.copy(accent = accent) }

    fun setIconScale(scale: Float) = update { it.copy(iconScale = scale) }

    fun setGridColumns(columns: Int) = update { it.copy(gridColumns = columns) }

    fun setGridRows(rows: Int) = update { it.copy(gridRows = rows) }

    fun setShowLabels(show: Boolean) = update { it.copy(showLabels = show) }

    fun setLabelScale(scale: Float) = update { it.copy(labelScale = scale) }

    fun setAnimationLevel(level: AnimationLevel) = update { it.copy(animationLevel = level) }

    fun setHomePages(pages: Int) = update { it.copy(homePages = pages) }

    fun setDockSlot(index: Int, packageName: String?) {
        update { settings ->
            val dock = settings.dockPackages.toMutableList()
            while (dock.size <= index) {
                dock.add("")
            }
            dock[index] = packageName.orEmpty()
            settings.copy(dockPackages = dock.filter { it.isNotBlank() })
        }
    }

    fun toggleHidden(packageName: String, hidden: Boolean) {
        update { settings ->
            val hiddenPackages = settings.hiddenPackages.toMutableSet()
            if (hidden) {
                hiddenPackages.add(packageName)
            } else {
                hiddenPackages.remove(packageName)
            }
            settings.copy(hiddenPackages = hiddenPackages.toList())
        }
    }

    fun setWallpaper(choice: WallpaperChoice) {
        update { it.copy(wallpaper = choice) }
        viewModelScope.launch {
            val result = wallpaperRepository.applyBuiltin(choice)
            _message.value = if (result.isSuccess) {
                SettingsMessage.WallpaperApplied
            } else {
                SettingsMessage.WallpaperFailed
            }
        }
    }

    fun applyPhotoWallpaper(uri: Uri) {
        update { it.copy(wallpaper = WallpaperChoice.CUSTOM) }
        viewModelScope.launch {
            val result = wallpaperRepository.applyPhoto(uri)
            _message.value = if (result.isSuccess) {
                SettingsMessage.WallpaperApplied
            } else {
                SettingsMessage.WallpaperFailed
            }
        }
    }

    fun openSystemWallpaper() {
        viewModelScope.launch {
            wallpaperRepository.openSystemWallpaper()
                .onFailure { _message.value = SettingsMessage.WallpaperFailed }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.reset()
            _message.value = SettingsMessage.SettingsReset
        }
    }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.appRepository,
                    container.settingsRepository,
                    container.wallpaperRepository,
                )
            }
        }
    }
}
