package com.vex.launcher.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Sistem animasyon ölçeğini okur.
 * Kullanıcı sistemde animasyonları kapatmışsa uygulama da animasyonsuz çalışır.
 */
@Composable
fun rememberSystemAnimationScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
    }
}
