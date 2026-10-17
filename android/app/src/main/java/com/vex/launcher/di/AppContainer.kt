package com.vex.launcher.di

import android.content.Context
import com.vex.launcher.data.AppRepository
import com.vex.launcher.data.AppRepositoryImpl
import com.vex.launcher.data.IconRepository
import com.vex.launcher.data.SettingsRepository
import com.vex.launcher.data.SettingsRepositoryImpl
import com.vex.launcher.data.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Elle (manuel) bağımlılık konteyneri.
 *
 * DI kütüphanesi kullanılmaz; bağımlılıklar tek bir yerde görünür, test edilebilir
 * ve derleme süresi kısa kalır.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Uygulama geneli arka plan işleri (yükleme, temizlik) için kapsam. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(appContext)
    val appRepository: AppRepository = AppRepositoryImpl(appContext, applicationScope)
    val iconRepository: IconRepository = IconRepository(appContext)
    val wallpaperRepository: WallpaperRepository = WallpaperRepository(appContext)
}
