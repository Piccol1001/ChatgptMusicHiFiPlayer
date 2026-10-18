package com.piccol.hifi.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.widgets.*

@UnstableApi
@Composable
fun WidgetSettings(vm: PlayerViewModel) {
    val context = LocalContext.current
    val style by vm.widgetAppearance.collectAsStateWithLifecycle()
    val manager = remember(context) { context.getSystemService(AppWidgetManager::class.java) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Home-screen widgets", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Transparent with dark scrim", Modifier.weight(1f))
            Switch(style.transparent, { v -> vm.widgetAppearance { it.copy(transparent = v) } })
        }
        Row {
            listOf("Blue", "Mint", "Violet").forEachIndexed { i, name ->
                FilterChip(
                    style.accent == i,
                    { vm.widgetAppearance { it.copy(accent = i) } },
                    label = { Text(name) },
                )
            }
        }
        if (manager.isRequestPinAppWidgetSupported) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                        "Compact" to CompactWidgetReceiver::class.java,
                        "Standard" to StandardWidgetReceiver::class.java,
                        "Expanded" to ExpandedWidgetReceiver::class.java,
                    )
                    .forEach { (name, receiver) ->
                        TextButton({
                            if (
                                !manager.requestPinAppWidget(
                                    ComponentName(context, receiver),
                                    null,
                                    null,
                                )
                            )
                                vm.message.value =
                                    "Add HiFi Player from your launcher's widget picker."
                        }) {
                            Text("Add $name")
                        }
                    }
            }
        } else
            Text(
                "Add Compact, Standard or Expanded from your launcher's widget picker.",
                style = MaterialTheme.typography.bodySmall,
            )
        Text(
            "Widgets share this player's queue and controls. Tap artwork or progress to open Now Playing.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@UnstableApi
@Composable
fun MotionSettings(vm: PlayerViewModel) {
    val visual by vm.visualSettings.collectAsStateWithLifecycle()
    SettingRow(
        "Reduce motion",
        "Stops decorative rotation and shader animation",
        Icons.Rounded.Animation,
        trailing = {
            Switch(visual.reducedMotion, { v -> vm.changeVisual { it.copy(reducedMotion = v) } })
        },
    )
}
