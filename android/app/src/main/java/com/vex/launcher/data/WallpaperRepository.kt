package com.vex.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Log
import com.vex.launcher.core.model.WallpaperChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Duvar kağıdı işlemleri.
 *
 * - Dahili gradyanlar her zaman uygulanabilir (API 26+).
 * - Galeriden seçilen fotoğraf "cihazın desteklediği" ölçüde uygulanır; hata durumunda
 *   çağıran taraf bilgilendirilir. Bazı üretici ROM'ları varsayılan ana ekran dışından
 *   duvar kağıdı değişimini kısıtlayabilir; bu durumda işlem başarısız olarak döner.
 */
class WallpaperRepository(private val context: Context) {

    private val appContext = context.applicationContext

    /** Dahili duvar kağıdını sisteme uygular. */
    suspend fun applyBuiltin(choice: WallpaperChoice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = createGradientBitmap(choice)
            WallpaperManager.getInstance(appContext).setBitmap(bitmap)
            bitmap.recycle()
        }.onFailure { error -> Log.w(TAG, "Dahili duvar kağıdı uygulanamadı", error) }
    }

    /** Kullanıcının seçtiği fotoğrafı duvar kağıdı olarak uygular. */
    suspend fun applyPhoto(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeBitmap(uri) ?: error("Görsel okunamadı")
            WallpaperManager.getInstance(appContext).setBitmap(bitmap)
            bitmap.recycle()
        }.onFailure { error -> Log.w(TAG, "Fotoğraf duvar kağıdı uygulanamadı", error) }
    }

    /** Sistem duvar kağıdı ekranını açar. */
    suspend fun openSystemWallpaper(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Settings.ACTION_WALLPAPER_SETTINGS her sürümde herkese açık değildir;
            // bu yüzden sistem aksiyonu doğrudan kullanılır.
            val intent = android.content.Intent("android.settings.WALLPAPER_SETTINGS")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
    }

    private fun createGradientBitmap(choice: WallpaperChoice): Bitmap {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors: IntArray
        val positions: FloatArray
        when (choice) {
            WallpaperChoice.OBSIDIAN -> {
                colors = intArrayOf(0xFF07070B.toInt(), 0xFF0C0C12.toInt())
                positions = floatArrayOf(0f, 1f)
            }
            WallpaperChoice.PURPLE -> {
                colors = intArrayOf(0xFF140D26.toInt(), 0xFF1D1132.toInt(), 0xFF07070B.toInt())
                positions = floatArrayOf(0f, 0.45f, 1f)
            }
            WallpaperChoice.MIDNIGHT -> {
                colors = intArrayOf(0xFF060A16.toInt(), 0xFF0B1226.toInt(), 0xFF05060C.toInt())
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            WallpaperChoice.GRAPHITE -> {
                colors = intArrayOf(0xFF0F0F13.toInt(), 0xFF17171D.toInt())
                positions = floatArrayOf(0f, 1f)
            }
            WallpaperChoice.CUSTOM -> {
                colors = intArrayOf(0xFF0A0A0F.toInt(), 0xFF121219.toInt())
                positions = floatArrayOf(0f, 1f)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                colors,
                positions,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(uri)
        } else {
            decodeWithBitmapFactory(uri)
        }
    }

    private fun decodeWithImageDecoder(uri: Uri): Bitmap? = runCatching {
        val source = android.graphics.ImageDecoder.createSource(appContext.contentResolver, uri)
        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val longest = max(width, height)
            if (longest > MAX_PHOTO_EDGE) {
                val scale = MAX_PHOTO_EDGE.toFloat() / longest
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()

    private fun decodeWithBitmapFactory(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return appContext.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var longest = max(width, height)
        while (longest / sampleSize > MAX_PHOTO_EDGE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private companion object {
        const val TAG = "VexWallpaper"
        const val MAX_PHOTO_EDGE = 2400
    }
}
