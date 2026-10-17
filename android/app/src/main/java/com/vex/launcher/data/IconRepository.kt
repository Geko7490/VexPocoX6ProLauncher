package com.vex.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.util.component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Uygulama simgelerini önbelleğe alan depo.
 *
 * - Simgeler PackageManager'dan çekilir, hedef boyutta bitmap'e dönüştürülür ve
 *   byte tabanlı bir LRU önbellekte saklanır.
 * - Ana iş parçacığı hiçbir zaman disk/IO beklemez; tüm işler Dispatchers.IO'dadır.
 * - Önbellek anahtarı "paket + aktivite + boyut" üçlüsüdür; boyut değişince
 *   yeni bir bitmap üretilir.
 */
class IconRepository(context: Context) {

    private val appContext = context.applicationContext

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /**
     * Verilen uygulamanın simgesini yükler. Simge bulunamazsa null döner;
     * çağıran taraf yer tutucu gösterir.
     */
    suspend fun load(app: AppInfo, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val targetSize = sizePx.coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val key = "${app.packageName}/${app.activityName}@$targetSize"

        cache.get(key)?.let { return@withContext it.asImageBitmap() }

        val drawable = loadDrawable(app)

        val bitmap = drawable?.let { source ->
            runCatching {
                val width = if (source.intrinsicWidth > 0) source.intrinsicWidth else targetSize
                val height = if (source.intrinsicHeight > 0) source.intrinsicHeight else targetSize
                source.toBitmap(width, height, Bitmap.Config.ARGB_8888)
            }.getOrNull()
        } ?: return@withContext null

        cache.put(key, bitmap)
        bitmap.asImageBitmap()
    }

    /** Simgeyi güvenli şekilde yükler; hata durumunda null döner. */
    private fun loadDrawable(app: AppInfo): android.graphics.drawable.Drawable? {
        val activityIcon = runCatching {
            appContext.packageManager.getActivityIcon(app.component())
        }.getOrNull()
        if (activityIcon != null) return activityIcon

        return runCatching {
            val applicationInfo = appContext.packageManager.getApplicationInfo(app.packageName, 0)
            appContext.packageManager.getApplicationIcon(applicationInfo)
        }.getOrNull()
    }

    /** Önbelleği temizler (bellek baskısı / düşük bellek sinyali). */
    fun clear() {
        cache.evictAll()
    }

    /** Simgenin yüklenemediği durumlar için yedek bitmap üretir. */
    fun fallbackIcon(sizePx: Int): ImageBitmap {
        val size = sizePx.coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(0xFF2A2A36.toInt())
        return bitmap.asImageBitmap()
    }

    private companion object {
        const val MAX_CACHE_BYTES = 24 * 1024 * 1024
        const val MIN_SIZE_PX = 48
        const val MAX_SIZE_PX = 256
    }
}
