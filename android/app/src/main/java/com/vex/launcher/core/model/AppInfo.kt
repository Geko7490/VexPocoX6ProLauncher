package com.vex.launcher.core.model

import androidx.compose.runtime.Immutable

/**
 * Cihazda kurulu, başlatılabilir bir uygulamanın değişmez (immutable) tanımı.
 *
 * @Immutable sayesinde Compose, listede bir öğe değişmediğinde yeniden çizim yapmaz.
 * Bu sınıf bilinçli olarak Android sınıfları içermez; bu sayede saf JVM birim testlerinde
 * doğrudan kullanılabilir.
 */
@Immutable
data class AppInfo(
    val packageName: String,
    val label: String,
    val activityName: String,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long,
)
