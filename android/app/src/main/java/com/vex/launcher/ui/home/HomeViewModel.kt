package com.vex.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.core.util.HomePaginator
import com.vex.launcher.data.AppRepository
import com.vex.launcher.data.SettingsRepository
import com.vex.launcher.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ana ekranın görünüm durumu. */
data class HomeUiState(
    val pages: List<List<AppInfo>> = emptyList(),
    val dockApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val visibleAppCount: Int = 0,
)

/**
 * Ana ekran ViewModel'i.
 *
 * - Uygulama listesi, yükleme durumu, hata ve ayarlar tek bir akışta birleştirilir.
 * - Sayfalama ve dock eşlemesi saf fonksiyonlarla yapılır (test edilebilir).
 */
class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.apps,
        appRepository.isLoading,
        appRepository.error,
        settingsRepository.settings,
    ) { apps, isLoading, error, settings ->
        val hidden = settings.hiddenPackages.toSet()
        val visible = apps.filter { it.packageName !in hidden }
        val dockApps = settings.dockPackages.mapNotNull { packageName ->
            visible.firstOrNull { it.packageName == packageName }
        }
        // Dock'ta bulunan uygulamalar ana ekran sayfalarında tekrarlanmaz.
        val pageApps = visible.filter { it.packageName !in settings.dockPackages.toSet() }
        HomeUiState(
            pages = HomePaginator.paginate(
                apps = pageApps,
                pages = settings.homePages,
                columns = settings.gridColumns,
                rows = settings.gridRows,
            ),
            dockApps = dockApps,
            isLoading = isLoading,
            error = error,
            visibleAppCount = visible.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    /** Uygulama listesini yeniden tarar (hata sonrası "tekrar dene"). */
    fun refresh() {
        viewModelScope.launch {
            appRepository.refresh()
        }
    }

    fun addToDock(packageName: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                if (settings.dockPackages.contains(packageName) ||
                    settings.dockPackages.size >= MAX_DOCK_ITEMS
                ) {
                    settings
                } else {
                    settings.copy(dockPackages = settings.dockPackages + packageName)
                }
            }
        }
    }

    fun removeFromDock(packageName: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(dockPackages = settings.dockPackages - packageName)
            }
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                if (settings.hiddenPackages.contains(packageName)) {
                    settings
                } else {
                    settings.copy(hiddenPackages = settings.hiddenPackages + packageName)
                }
            }
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(hiddenPackages = settings.hiddenPackages - packageName)
            }
        }
    }

    companion object {
        const val MAX_DOCK_ITEMS = 5
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(container.appRepository, container.settingsRepository)
            }
        }
    }
}
