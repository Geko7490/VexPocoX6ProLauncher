package com.vex.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.vex.launcher.core.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PackageManager tabanlı uygulama keşfi.
 *
 * - Sorgu [Dispatchers.IO] üzerinde çalışır, ana iş parçacığını bloklamaz.
 * - Paket değişiklikleri (ekleme, silme, güncelleme) BroadcastReceiver ile izlenir.
 * - Aynı anda gelen çoklu bildirimler "conflated" kanal ile tek taramaya indirgenir.
 */
class AppRepositoryImpl(
    context: Context,
    private val scope: CoroutineScope,
) : AppRepository {

    private val appContext = context.applicationContext

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    override val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val refreshSignal = Channel<Unit>(Channel.CONFLATED)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> refreshSignal.trySend(Unit)
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                packageReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.onFailure { error -> Log.w(TAG, "Paket dinleyicisi kaydedilemedi", error) }

        scope.launch {
            for (signal in refreshSignal) {
                // Arka arkaya gelen bildirimler tek taramada birleştirilir.
                delay(REFRESH_DEBOUNCE_MS)
                loadApps()
            }
        }
        scope.launch { loadApps() }
    }

    override suspend fun refresh() = loadApps()

    override fun close() {
        runCatching { appContext.unregisterReceiver(packageReceiver) }
            .onFailure { Log.w(TAG, "Paket dinleyicisi kaldırılamadı", it) }
        refreshSignal.close()
    }

    private suspend fun loadApps() {
        _isLoading.value = true
        val result = runCatching { queryLaunchableApps() }
        result
            .onSuccess { list ->
                _apps.value = list
                _error.value = null
            }
            .onFailure { throwable ->
                Log.e(TAG, "Uygulama listesi alınamadı", throwable)
                _error.value = throwable.message ?: throwable::class.java.simpleName
            }
        _isLoading.value = false
    }

    private suspend fun queryLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )

        // Güncelleme zamanları tek bir sistem çağrısıyla alınır (paket başına çağrı yapılmaz).
        val lastUpdateTimes: Map<String, Long> = runCatching {
            packageManager.getInstalledPackages(0)
                .associate { it.packageName to it.lastUpdateTime }
        }.getOrDefault(emptyMap())

        resolved.asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val applicationInfo = activityInfo.applicationInfo ?: return@mapNotNull null
                val label = runCatching {
                    packageManager.getApplicationLabel(applicationInfo).toString()
                }.getOrDefault(applicationInfo.packageName)

                AppInfo(
                    packageName = applicationInfo.packageName,
                    label = label,
                    activityName = activityInfo.name,
                    isSystemApp = (applicationInfo.flags and
                        (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0,
                    lastUpdateTime = lastUpdateTimes[applicationInfo.packageName] ?: 0L,
                )
            }
            // Aynı paket için birden fazla başlatma aktivitesi varsa biri seçilir.
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    private companion object {
        const val TAG = "VexApps"
        const val REFRESH_DEBOUNCE_MS = 250L
    }
}
