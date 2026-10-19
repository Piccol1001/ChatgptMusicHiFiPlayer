package com.piccol.hifi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline notices: list documents first, then lay out only the selected local text. */
@Composable
fun LicenseSettings() {
    var visible by remember { mutableStateOf(false) }
    TextButton(onClick = { visible = true }) { Text("Open-source licenses") }
    if (visible) {
        val context = LocalContext.current
        var selected by remember { mutableStateOf<String?>(null) }
        val names by
            produceState<List<String>?>(null, context) {
                value =
                    withContext(Dispatchers.IO) {
                        context.assets.list("licenses").orEmpty().sorted()
                    }
            }
        val document by
            produceState<String?>(null, selected, context) {
                value =
                    selected?.let { name ->
                        withContext(Dispatchers.IO) {
                            context.assets.open("licenses/$name").bufferedReader().use {
                                it.readText()
                            }
                        }
                    }
            }
        AlertDialog(
            onDismissRequest = { visible = false },
            title = { Text(selected ?: "Open-source licenses") },
            text = {
                if (selected == null) {
                    val loaded = names
                    if (loaded == null) Text("Loading local notices…")
                    else
                        LazyColumn(Modifier.heightIn(max = 440.dp)) {
                            items(loaded, key = { it }) { name ->
                                TextButton({ selected = name }, Modifier.fillMaxWidth()) {
                                    Text(name)
                                }
                            }
                        }
                } else {
                    val loaded = document
                    if (loaded == null) Text("Loading local notice…")
                    else
                        SelectionContainer {
                            Text(
                                loaded,
                                Modifier.heightIn(max = 440.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                }
            },
            dismissButton = {
                if (selected != null) TextButton({ selected = null }) { Text("All licenses") }
            },
            confirmButton = { TextButton({ visible = false }) { Text("Close licenses") } },
        )
    }
}
