package com.vex.launcher.core.util

import com.vex.launcher.core.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arama/filtreleme mantığının birim testleri (JVM, cihaz gerektirmez).
 */
class AppSearchTest {

    private fun app(label: String, packageName: String): AppInfo = AppInfo(
        packageName = packageName,
        label = label,
        activityName = "$packageName.MainActivity",
        isSystemApp = false,
        lastUpdateTime = 0L,
    )

    private val apps = listOf(
        app("Ayarlar", "com.android.settings"),
        app("Kamera", "com.android.camera"),
        app("İstanbul", "com.example.istanbul"),
        app("Şekerbank", "com.bank.seker"),
        app("Iğdır", "com.example.igdir"),
        app("Notlar", "com.example.notes"),
    )

    @Test
    fun `bos sorgu tum uygulamalari dondurur`() {
        assertEquals(apps, AppSearch.filter(apps, ""))
        assertEquals(apps, AppSearch.filter(apps, "   "))
    }

    @Test
    fun `etikete gore buyuk kucuk harf duyarsiz arama`() {
        val result = AppSearch.filter(apps, "KAMERA")
        assertEquals(1, result.size)
        assertEquals("com.android.camera", result.first().packageName)
    }

    @Test
    fun `turkce karakterler normalize edilir`() {
        assertEquals(1, AppSearch.filter(apps, "istanbul").size)
        assertEquals(1, AppSearch.filter(apps, "sekerbank").size)
        assertEquals(1, AppSearch.filter(apps, "igdir").size)
    }

    @Test
    fun `paket adi ile de arama yapilabilir`() {
        val result = AppSearch.filter(apps, "com.example.notes")
        assertEquals(1, result.size)
        assertEquals("Notlar", result.first().label)
    }

    @Test
    fun `bulunmayan sonuc bos liste doner`() {
        assertTrue(AppSearch.filter(apps, "bulunmayanuygulama").isEmpty())
    }

    @Test
    fun `harf indeksi ilk occurrence konumunu gosterir`() {
        val index = AppSearch.letterIndex(apps)
        assertEquals(0, index['A'])
        assertEquals(1, index['K'])
        assertEquals(2, index['İ'])
        assertEquals(5, index['N'])
    }
}
