package com.piccol.hifi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.ui.HiFiApp
import com.piccol.hifi.ui.Screen
import com.piccol.hifi.ui.screenForIntent

@UnstableApi
class MainActivity : ComponentActivity() {
    private var openRoute by mutableStateOf<Screen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openRoute = screenForIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            HiFiApp(viewModel(), openRoute) {
                openRoute = null
                listOf(
                        "open_player",
                        "open_equalizer",
                        "open_visualizer",
                        "route",
                        "screen",
                        "destination",
                    )
                    .forEach(intent::removeExtra)
                intent.data = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRoute = screenForIntent(intent)
    }
}
