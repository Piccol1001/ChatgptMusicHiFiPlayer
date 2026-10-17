package com.piccol.hifi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline, selectable copies of the notices distributed in this APK. */
@Composable
fun LicenseSettings() {
    var visible by remember { mutableStateOf(false) }
    TextButton(onClick = { visible = true }) { Text("Open-source licenses") }
    if (visible) {
        val context = LocalContext.current
        val texts by
            produceState<List<Pair<String, String>>?>(null, context) {
                value =
                    withContext(Dispatchers.IO) {
                        context.assets.list("licenses").orEmpty().sorted().map { name ->
                            name to
                                context.assets.open("licenses/$name").bufferedReader().use {
                                    it.readText()
                                }
                        }
                    }
            }
        AlertDialog(
            onDismissRequest = { visible = false },
            title = { Text("Open-source licenses") },
            text = {
                val loaded = texts
                if (loaded == null) Text("Loading local notices…")
                else
                    LazyColumn(
                        Modifier.heightIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(loaded, key = { it.first }) { (name, text) ->
                            Column {
                                Text(name, style = MaterialTheme.typography.titleSmall)
                                SelectionContainer {
                                    Text(text, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
            },
            confirmButton = { TextButton(onClick = { visible = false }) { Text("Close licenses") } },
        )
    }
}
