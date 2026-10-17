package com.vex.launcher.core.util

import com.vex.launcher.core.model.AppInfo
import kotlin.math.max

/**
 * Ana ekran sayfalama mantığı. Saf fonksiyon olduğu için birim testleriyle doğrulanır.
 */
object HomePaginator {

    const val MIN_COLUMNS = 3
    const val MAX_COLUMNS = 8
    const val MIN_ROWS = 3
    const val MAX_ROWS = 8
    const val MIN_PAGES = 1
    const val MAX_PAGES = 5

    /** Sayfa başına düşen uygulama sayısı. */
    fun itemsPerPage(columns: Int, rows: Int): Int =
        max(1, columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS) * rows.coerceIn(MIN_ROWS, MAX_ROWS))

    /**
     * Uygulama listesini verilen sayfa sayısına göre böler.
     * Boş sayfalar da döndürülür; sayfa göstergesi her zaman sabit sayıda nokta gösterir.
     */
    fun paginate(apps: List<AppInfo>, pages: Int, columns: Int, rows: Int): List<List<AppInfo>> {
        val perPage = itemsPerPage(columns, rows)
        val pageCount = pages.coerceIn(MIN_PAGES, MAX_PAGES)
        return (0 until pageCount).map { page ->
            apps.drop(page * perPage).take(perPage)
        }
    }
}
