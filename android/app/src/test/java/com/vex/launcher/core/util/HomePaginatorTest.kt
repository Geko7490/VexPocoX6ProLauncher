package com.vex.launcher.core.util

import com.vex.launcher.core.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ana ekran sayfalama mantığının birim testleri.
 */
class HomePaginatorTest {

    private fun app(index: Int): AppInfo = AppInfo(
        packageName = "com.example.app$index",
        label = "Uygulama $index",
        activityName = "com.example.app$index.Main",
        isSystemApp = false,
        lastUpdateTime = 0L,
    )

    @Test
    fun `sayfa basina eleman sayisi sutun satir carpimidir`() {
        assertEquals(8, HomePaginator.itemsPerPage(columns = 4, rows = 2))
    }

    @Test
    fun `girdiler sinir degerlerine kirpilir`() {
        assertEquals(
            HomePaginator.MIN_COLUMNS * HomePaginator.MIN_ROWS,
            HomePaginator.itemsPerPage(columns = 1, rows = 1),
        )
        assertEquals(
            HomePaginator.MAX_COLUMNS * HomePaginator.MAX_ROWS,
            HomePaginator.itemsPerPage(columns = 99, rows = 99),
        )
    }

    @Test
    fun `uygulamalar sayfalara bolunur`() {
        val apps = (1..10).map { app(it) }
        val pages = HomePaginator.paginate(apps, pages = 2, columns = 2, rows = 2)

        assertEquals(2, pages.size)
        assertEquals(4, pages[0].size)
        assertEquals(4, pages[1].size)
        assertEquals("com.example.app1", pages[0].first().packageName)
        assertEquals("com.example.app5", pages[1].first().packageName)
    }

    @Test
    fun `sayfa sayisi korunur ve bos sayfalar birakilmaz`() {
        val pages = HomePaginator.paginate(emptyList(), pages = 3, columns = 4, rows = 4)
        assertEquals(3, pages.size)
        assertTrue(pages.all { it.isEmpty() })
    }

    @Test
    fun `sayfa sayisi sinir degerlerine kirpilir`() {
        val apps = (1..5).map { app(it) }
        assertEquals(5, HomePaginator.paginate(apps, pages = 99, columns = 4, rows = 4).size)
        assertEquals(1, HomePaginator.paginate(apps, pages = 0, columns = 4, rows = 4).size)
    }
}
