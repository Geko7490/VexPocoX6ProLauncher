package com.vex.launcher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vex.launcher.ui.VexApp

/**
 * Tek aktivite. Tüm ekranlar Compose içinde yönetilir.
 */
class MainActivity : ComponentActivity() {

    private val container: com.vex.launcher.di.AppContainer
        get() = (application as VexApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kenardan kenara (edge-to-edge) çizim; koyu tema için açık renkli sistem ikonları.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            VexApp(container = container)
        }
    }
}
