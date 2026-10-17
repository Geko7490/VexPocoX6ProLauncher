package com.vex.launcher.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.vex.launcher.core.model.AppInfo

/** Uygulamanın başlatma bileşeni. */
fun AppInfo.component(): ComponentName = ComponentName(packageName, activityName)

/**
 * Uygulamayı başlatan intent. FLAG_ACTIVITY_NEW_TASK, launcher aktivitesinden
 * doğrudan açılan uygulamaların görev yığınına doğru eklenmesini sağlar.
 */
fun AppInfo.launchIntent(context: Context): Intent? =
    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

/** Sistem uygulama bilgileri ekranı. */
fun AppInfo.detailsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

/** Sistem kaldırma onayı. */
fun AppInfo.uninstallIntent(): Intent =
    Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
