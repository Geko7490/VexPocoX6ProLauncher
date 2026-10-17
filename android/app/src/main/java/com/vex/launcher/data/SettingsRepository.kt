package com.vex.launcher.data

import com.vex.launcher.core.model.LauncherSettings
import kotlinx.coroutines.flow.Flow

/** Kalıcı kullanıcı ayarları (DataStore Preferences). */
interface SettingsRepository {

    /** Ayarların güncel akışı. */
    val settings: Flow<LauncherSettings>

    /** Ayar değiştirir. */
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)

    /** Tüm ayarları varsayılana döndürür. */
    suspend fun reset()
}
