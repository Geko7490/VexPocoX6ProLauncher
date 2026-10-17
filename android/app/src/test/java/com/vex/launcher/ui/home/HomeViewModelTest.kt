package com.vex.launcher.ui.home

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
 * Ana ekran ViewModel'i birim testleri.
 * MVVM katmanının veri akışı saf Kotlin olarak test edilir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun `gizli uygulamalar listeye girmez`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Ayarlar", "com.android.settings"),
                app("Gizli", "com.example.hidden"),
            ),
        )
        val settings = FakeSettingsRepository(
            LauncherSettings(hiddenPackages = listOf("com.example.hidden")),
        )

        val viewModel = HomeViewModel(apps, settings)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(2, state.visibleAppCount)
        assertTrue(state.pages.flatten().none { it.packageName == "com.example.hidden" })
    }

    @Test
    fun `dock uygulamalari sirayla listelenir ve sayfalarda tekrarlanmaz`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Ayarlar", "com.android.settings"),
                app("Müzik", "com.android.music"),
            ),
        )
        val settings = FakeSettingsRepository(
            LauncherSettings(dockPackages = listOf("com.android.music", "com.android.camera")),
        )

        val viewModel = HomeViewModel(apps, settings)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(
            listOf("com.android.music", "com.android.camera"),
            state.dockApps.map { it.packageName },
        )
        assertTrue(state.pages.flatten().none { it.packageName == "com.android.camera" })
    }

    @Test
    fun `dock ekleme ve cikarma ayarlari gunceller`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(listOf(app("Kamera", "com.android.camera")))
        val settings = FakeSettingsRepository()
        val viewModel = HomeViewModel(apps, settings)

        viewModel.addToDock("com.android.camera")
        assertEquals(listOf("com.android.camera"), settings.current.dockPackages)

        viewModel.removeFromDock("com.android.camera")
        assertTrue(settings.current.dockPackages.isEmpty())
    }

    @Test
    fun `dock doluysa yeni uygulama eklenmez`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val apps = FakeAppRepository(
            listOf(
                app("Kamera", "com.android.camera"),
                app("Ayarlar", "com.android.settings"),
            ),
        )
        val settings = FakeSettingsRepository(
            LauncherSettings(dockPackages = List(HomeViewModel.MAX_DOCK_ITEMS) { "com.filled.$it" }),
        )
        val viewModel = HomeViewModel(apps, settings)

        viewModel.addToDock("com.android.camera")

        assertEquals(HomeViewModel.MAX_DOCK_ITEMS, settings.current.dockPackages.size)
    }
}
