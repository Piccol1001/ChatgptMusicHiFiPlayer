package com.piccol.hifi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.ui.HiFiApp

@UnstableApi
class MainActivity : ComponentActivity() {
    private var openPlayer by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPlayer = intent.getBooleanExtra("open_player", false)
        enableEdgeToEdge()
        setContent {
            HiFiApp(viewModel(), openPlayer) {
                openPlayer = false
                intent.removeExtra("open_player")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openPlayer = intent.getBooleanExtra("open_player", false)
    }
}
