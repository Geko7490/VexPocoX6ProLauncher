package com.vex.launcher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.util.AppSearch
import com.vex.launcher.data.AppRepository
import com.vex.launcher.data.SettingsRepository
import com.vex.launcher.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Uygulama çekmecesi görünüm durumu. */
data class DrawerUiState(
    val apps: List<AppInfo> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val letterIndex: Map<Char, Int> = emptyMap(),
)

/**
 * Uygulama çekmecesi ViewModel'i.
 *
 * - Arama sorgusu yalnızca ViewModel tarafında tutulur; her tuş vuruşunda tüm liste
 *   yeniden hesaplanmaz, filtreleme Dispatchers.Default üzerinde yapılır.
 * - Yazma sırasındaki yinelenen sorgular `collectLatest` ile iptal edilir.
 */
class AppDrawerViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Geciktirilmiş (debounce) sorgu: yalnızca yazma sırasında bekletir. */
    private val debouncedQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = query.asStateFlow()

    val uiState: StateFlow<DrawerUiState> = combine(
        appRepository.apps,
        appRepository.isLoading,
        appRepository.error,
        debouncedQuery,
        settingsRepository.settings,
    ) { apps, isLoading, error, searchQuery, settings ->
        val hidden = settings.hiddenPackages.toSet()
        val visible = apps.filter { it.packageName !in hidden }
        val filtered = AppSearch.filter(visible, searchQuery)
        DrawerUiState(
            apps = filtered,
            query = searchQuery,
            isLoading = isLoading,
            error = error,
            letterIndex = AppSearch.letterIndex(filtered),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DrawerUiState(),
        )

    init {
        viewModelScope.launch {
            query.collectLatest { value ->
                if (value.isEmpty()) {
                    debouncedQuery.value = ""
                } else {
                    delay(DEBOUNCE_MS)
                    debouncedQuery.value = value
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
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
        private const val DEBOUNCE_MS = 80L
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppDrawerViewModel(container.appRepository, container.settingsRepository)
            }
        }
    }
}
