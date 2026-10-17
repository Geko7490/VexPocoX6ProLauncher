package com.vex.launcher.data

import com.vex.launcher.core.model.AppInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Cihazda kurulu başlatılabilir uygulamaların kaynağı.
 * Uygulama ekleme/silme/güncelleme olaylarında akış otomatik olarak güncellenir.
 */
interface AppRepository {

    /** Alfabetik sıralı uygulama listesi. */
    val apps: StateFlow<List<AppInfo>>

    /** İlk yükleme sürüyor mu? */
    val isLoading: StateFlow<Boolean>

    /** Hata mesajı (varsa). */
    val error: StateFlow<String?>

    /** Listeyi yeniden tarar. */
    suspend fun refresh()

    /** Kayıtlı dinleyicileri temizler. */
    fun close()
}
