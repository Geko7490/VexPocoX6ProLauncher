package com.vex.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vex.launcher.core.model.AnimationLevel
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.core.system.DefaultHomeRoleController
import com.vex.launcher.di.AppContainer
import com.vex.launcher.ui.drawer.AppDrawerScreen
import com.vex.launcher.ui.home.HomeScreen
import com.vex.launcher.ui.settings.SettingsScreen
import com.vex.launcher.ui.theme.VexTheme

private const val SCREEN_HOME = "home"
private const val SCREEN_DRAWER = "drawer"
private const val SCREEN_SETTINGS = "settings"

/**
 * Uygulamanın kök kompozisyonu.
 *
 * - Ekranlar arası geçiş, kullanıcının animasyon tercihi ve sistem animasyon ölçeğine göre yapılır.
 * - "Varsayılan ana ekran" rolü burada istenir; rol alınmadıysa ana ekranda bilgilendirme kartı görünür.
 */
@Composable
fun VexApp(container: AppContainer) {
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = LauncherSettings())

    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
    var focusSearchToken by rememberSaveable { mutableStateOf(0) }

    val context = LocalContext.current
    val roleController = remember { DefaultHomeRoleController(context) }
    var isDefaultHome by remember { mutableStateOf(roleController.isHeld()) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefaultHome = roleController.isHeld()
    }

    // Sistem ana ekranı değiştirildiğinde (ya da kullanıcı geri döndüğünde) durum tazelenir.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultHome = roleController.isHeld()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestDefaultHome: () -> Unit = {
        val intent = roleController.createRequestIntent()
        if (intent != null) {
            val launched = runCatching { roleLauncher.launch(intent) }
            if (launched.isFailure) {
                runCatching { context.startActivity(roleController.legacySettingsIntent()) }
            }
        } else {
            runCatching { context.startActivity(roleController.legacySettingsIntent()) }
        }
        Unit
    }

    val systemScale = rememberSystemAnimationScale()
    val effectiveLevel = if (systemScale <= 0f) {
        AnimationLevel.OFF
    } else {
        settings.animationLevel
    }
    val durationMillis = when (effectiveLevel) {
        AnimationLevel.OFF -> 0
        AnimationLevel.REDUCED -> 140
        AnimationLevel.FULL -> 280
    }

    VexTheme(settings = settings) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val enter = if (targetState == SCREEN_DRAWER) {
                    slideInVertically(animationSpec = tween(durationMillis)) { it / 8 } +
                        fadeIn(animationSpec = tween(durationMillis))
                } else {
                    fadeIn(animationSpec = tween(durationMillis))
                }
                val exit = if (targetState == SCREEN_DRAWER) {
                    fadeOut(animationSpec = tween(durationMillis))
                } else {
                    slideOutVertically(animationSpec = tween(durationMillis)) { it / 8 } +
                        fadeOut(animationSpec = tween(durationMillis))
                }
                enter togetherWith exit
            },
            label = "vex-screen-transition",
        ) { target ->
            when (target) {
                SCREEN_DRAWER -> AppDrawerScreen(
                    container = container,
                    settings = settings,
                    focusSearch = focusSearchToken > 0,
                    onClose = {
                        screen = SCREEN_HOME
                        focusSearchToken = 0
                    },
                    onOpenSettings = { screen = SCREEN_SETTINGS },
                )

                SCREEN_SETTINGS -> SettingsScreen(
                    container = container,
                    isDefaultHome = isDefaultHome,
                    onRequestDefaultHome = requestDefaultHome,
                    onBack = { screen = SCREEN_HOME },
                )

                else -> HomeScreen(
                    container = container,
                    settings = settings,
                    isDefaultHome = isDefaultHome,
                    onOpenDrawer = { focusSearch ->
                        if (focusSearch) {
                            focusSearchToken += 1
                        }
                        screen = SCREEN_DRAWER
                    },
                    onOpenSettings = { screen = SCREEN_SETTINGS },
                    onRequestDefaultHome = requestDefaultHome,
                )
            }
        }
    }
}
