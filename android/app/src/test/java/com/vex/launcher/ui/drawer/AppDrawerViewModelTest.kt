package com.vex.launcher.ui.drawer

import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.testing.FakeAppRepository
import com.vex.launcher.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uygulama çekmecesi ViewModel'i birim testleri: arama, gizleme ve hızlı kaydırma indeksi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun app(label: String, packageName: String): AppInfo = AppInfo(
        packageName = packageName,
        label = label,
        activityName = "$packageName.MainActivity",
        isSystemApp = false,
        lastUpdateTime = 0L,
    )

    @Test
    fun `arama sonuclari filtreler`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Ayarlar", "com.android.settings"),
            ),
        )
        val viewModel = AppDrawerViewModel(apps, FakeSettingsRepository())

        viewModel.onQueryChange("kamera")
        val state = viewModel.uiState.first { it.query == "kamera" }

        assertEquals(1, state.apps.size)
        assertEquals("com.android.camera", state.apps.first().packageName)
    }

    @Test
    fun `bos arama tum uygulamalari gosterir`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Ayarlar", "com.android.settings"),
            ),
        )
        val viewModel = AppDrawerViewModel(apps, FakeSettingsRepository())

        val state = viewModel.uiState.first { it.apps.isNotEmpty() }
        assertEquals(2, state.apps.size)
    }

    @Test
    fun `gizli uygulamalar cekmecede gorunmez`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Gizli", "com.example.hidden"),
            ),
        )
        val settings = FakeSettingsRepository(
            LauncherSettings(hiddenPackages = listOf("com.example.hidden")),
        )
        val viewModel = AppDrawerViewModel(apps, settings)

        val state = viewModel.uiState.first { it.apps.isNotEmpty() }
        assertEquals(1, state.apps.size)
        assertTrue(state.letterIndex.containsKey('K'))
    }

    @Test
    fun `arama temizlendiginde liste tam olur`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Ayarlar", "com.android.settings"),
            ),
        )
        val viewModel = AppDrawerViewModel(apps, FakeSettingsRepository())

        viewModel.onQueryChange("kamera")
        viewModel.uiState.first { it.query == "kamera" }

        viewModel.clearQuery()
        val state = viewModel.uiState.first { it.query.isEmpty() && it.apps.size == 2 }
        assertEquals(2, state.apps.size)
    }
}
