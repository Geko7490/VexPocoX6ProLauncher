package com.vex.launcher

import android.app.Application
import com.vex.launcher.di.AppContainer

/**
 * Vex Launcher uygulama sınıfı.
 * Bağımlılık konteyneri burada oluşturulur ve tüm ekranlara tek kaynaktan dağıtılır.
 */
class VexApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
