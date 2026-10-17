package com.vex.launcher.core.util

import com.vex.launcher.core.model.AppInfo

/**
 * Uygulama arama/filtreleme mantığı.
 *
 * Saf Kotlin (Android bağımsız) olarak yazılmıştır; birim testleriyle doğrulanır.
 * Türkçe karakterler (ı, İ, ş, ğ, ç, ö, ü) normalleştirilerek "İstanbul" aramasının
 * "istanbul" yazımıyla da eşleşmesi sağlanır.
 */
object AppSearch {

    /** Metni arama için normalize eder (küçük harf + Türkçe karakter sadeleştirmesi). */
    fun normalize(value: String): String {
        val builder = StringBuilder(value.length)
        for (char in value) {
            when (char) {
                'İ', 'I' -> builder.append('i')
                'ı' -> builder.append('i')
                'Ş', 'ş' -> builder.append('s')
                'Ğ', 'ğ' -> builder.append('g')
                'Ç', 'ç' -> builder.append('c')
                'Ö', 'ö' -> builder.append('o')
                'Ü', 'ü' -> builder.append('u')
                else -> builder.append(char.lowercaseChar())
            }
        }
        return builder.toString()
    }

    /** Etiket veya paket adında geçen uygulamaları filtreler. */
    fun filter(apps: List<AppInfo>, query: String): List<AppInfo> {
        val needle = normalize(query.trim())
        if (needle.isEmpty()) return apps
        return apps.filter { app ->
            normalize(app.label).contains(needle) ||
                app.packageName.contains(needle, ignoreCase = true)
        }
    }

    /**
     * Hızlı kaydırma (A-Z) çubuğu için harf -> ilk öğe indeksi eşlemesi üretir.
     */
    fun letterIndex(apps: List<AppInfo>): Map<Char, Int> {
        val index = LinkedHashMap<Char, Int>()
        apps.forEachIndexed { position, app ->
            val letter = app.label.trim().firstOrNull()?.uppercaseChar() ?: return@forEachIndexed
            if (!index.containsKey(letter)) index[letter] = position
        }
        return index
    }
}
