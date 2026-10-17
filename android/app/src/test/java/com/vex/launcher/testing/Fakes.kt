package com.vex.launcher.testing

import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.data.AppRepository
import com.vex.launcher.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Testler için bellek içi uygulama deposu. */
class FakeAppRepository(
    initialApps: List<AppInfo> = emptyList(),
    initialLoading: Boolean = false,
    initialError: String? = null,
) : AppRepository {

    private val _apps = MutableStateFlow(initialApps)
    override val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(initialLoading)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow(initialError)
    override val error: StateFlow<String?> = _error.asStateFlow()

    var refreshCount: Int = 0
        private set

    fun setApps(value: List<AppInfo>) {
        _apps.value = value
    }

    override suspend fun refresh() {
        refreshCount += 1
    }

    override fun close() = Unit
}

/** Testler için bellek içi ayar deposu. */
class FakeSettingsRepository(
    initial: LauncherSettings = LauncherSettings(),
) : SettingsRepository {

    private val _settings = MutableStateFlow(initial)
    override val settings: Flow<LauncherSettings> = _settings.asStateFlow()

    val current: LauncherSettings get() = _settings.value

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        _settings.value = transform(_settings.value)
    }

    override suspend fun reset() {
        _settings.value = LauncherSettings()
    }
}
