package com.vex.launcher.core.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * "Varsayılan ana ekran" rolünü yönetir.
 *
 * API 29+ (Android 10) için RoleManager kullanılır.
 * API 26-28 için sistem ana ekran ayarları ekranı açılır.
 * HyperOS gibi bazı üretici ROM'larında rol istemi reddedilebilir; bu durumda
 * yardımcı bir geri dönüş yolu sunulur ve mevcut sistem başlatıcısı korunur.
 */
class DefaultHomeRoleController(context: Context) {

    private val appContext = context.applicationContext

    private val roleManager: RoleManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { appContext.getSystemService(RoleManager::class.java) }.getOrNull()
        } else {
            null
        }

    /** Cihazda "ana ekran" rolü destekleniyor mu? */
    fun isSupported(): Boolean {
        val manager = roleManager ?: return false
        return runCatching { manager.isRoleAvailable(RoleManager.ROLE_HOME) }.getOrDefault(false)
    }

    /** Vex Launcher şu anda varsayılan ana ekran mı? */
    fun isHeld(): Boolean = roleManager?.let {
        runCatching { it.isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(false)
    } ?: false

    /** Sistem rol istemi (API 29+). */
    fun createRequestIntent(): Intent? = roleManager?.let {
        runCatching { it.createRequestRoleIntent(RoleManager.ROLE_HOME) }
            .onFailure { error -> Log.w(TAG, "Rol isteği oluşturulamadı", error) }
            .getOrNull()
    }

    /** API 26-28 ve rol isteğinin desteklenmediği cihazlar için sistem ayarları. */
    fun legacySettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_HOME_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }

    private companion object {
        const val TAG = "VexRole"
    }
}
