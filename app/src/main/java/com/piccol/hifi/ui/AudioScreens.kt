package com.piccol.hifi.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.audio.*
import com.piccol.hifi.data.*
import com.piccol.hifi.library.readLimited
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf

@UnstableApi
@Composable
fun ProfilesScreen(vm: PlayerViewModel) {
    val state by vm.profileState.collectAsStateWithLifecycle()
    val route by vm.outputRoute.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<AutoEqImport?>(null) }
    var name by remember { mutableStateOf("Headphone correction") }
    var rename by remember { mutableStateOf<CorrectionProfile?>(null) }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    try {
                        preview =
                            withContext(Dispatchers.IO) {
                                parseAutoEq(
                                    context.contentResolver.openInputStream(uri)!!.use {
                                        readLimited(it)
                                    }
                                )
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        vm.message.value = e.message ?: "Could not read profile"
                    }
                }
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Headphone & output profiles", style = MaterialTheme.typography.titleLarge)
            Text(
                "Import a local AutoEq/Equalizer APO text file, preview its response, then save. No downloads or bundled headphone dataset."
            )
        }
        item {
            Button({ importer.launch(arrayOf("text/*", "application/octet-stream")) }) {
                Text("Import AutoEq text")
            }
            TextButton({ vm.saveProfile("${vm.dspSettings.value.preset} copy") }) {
                Text("Save current EQ as profile")
            }
        }
        item {
            Text("Active: ${state.active(route)?.name?:"Personal EQ"}")
            TextButton(vm::useBaseEq) { Text("Use personal EQ · clear this route's assignment") }
        }
        items(state.profiles, key = { it.id }) { p ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text(p.notes, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton({ vm.selectProfile(p.id) }) {
                            Text(if (state.manual == p.id) "Default selected" else "Set default")
                        }
                        IconButton({ rename = p }) { Icon(Icons.Rounded.Edit, "Rename ${p.name}") }
                        IconButton({ vm.deleteProfile(p.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete ${p.name}")
                        }
                    }
                    Text(
                        "Adjust the active profile's individual filters in Equalizer. Personal EQ is preserved.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Automatic output assignments", style = MaterialTheme.typography.titleMedium)
            Text(
                "The actual AudioTrack route selects a category. Android device IDs/addresses are not assumed to be persistent identities.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(OutputCategory.entries) { category ->
            var menu by remember { mutableStateOf(false) }
            val assigned = state.profiles.find { it.id == state.assignments[category] }
            Box {
                TextButton({ menu = true }) {
                    Text("${category.label}: ${assigned?.name?:"Default / personal"}")
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Default / personal") },
                        onClick = {
                            vm.assignProfile(category, null)
                            menu = false
                        },
                    )
                    state.profiles.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                vm.assignProfile(category, p.id)
                                menu = false
                            },
                        )
                    }
                }
            }
        }
    }
    preview?.let { imported ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Preview AutoEq") },
            text = {
                Column {
                    OutlinedTextField(
                        name,
                        { name = it.take(80) },
                        label = { Text("Profile name") },
                    )
                    ResponseGraph(imported.settings, vm.audioEngine.sampleRate)
                    Text(
                        "${imported.settings.activeFilters().size} filters · preamp ${imported.settings.preamp} dB"
                    )
                    Text(imported.notes, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    {
                        vm.saveProfile(name, imported.settings, imported.notes)
                        preview = null
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Save profile")
                }
            },
            dismissButton = { TextButton({ preview = null }) { Text("Cancel") } },
        )
    }
    rename?.let { p ->
        TextEntryDialog("Rename profile", p.name, { rename = null }) {
            vm.renameProfile(p.id, it)
            rename = null
        }
    }
}

@UnstableApi
@Composable
fun AudioOutputScreen(vm: PlayerViewModel) {
    val route by vm.outputRoute.collectAsStateWithLifecycle()
    val devices by vm.outputs.collectAsStateWithLifecycle()
    val source by vm.sourceInfo.collectAsStateWithLifecycle()
    val dsp by vm.dspSettings.collectAsStateWithLifecycle()
    val format by vm.audioEngine.format.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Audio output", style = MaterialTheme.typography.titleLarge)
            Text(
                "Current route: ${route.device?.let{"${it.category.label} · ${it.name}"}?:"Not reported; start playback"}"
            )
        }
        item {
            Text("Source file", style = MaterialTheme.typography.titleMedium)
            Text(
                "${source.codec} · ${source.container}\n${source.rate?.let{"$it Hz"}?:"Rate not reported"} · ${source.bits?.let{"$it-bit"}?:"Bit depth not reported"} · ${source.channels?.let{"$it channels"}?:"Channels not reported"}\n${source.bitrate?.let{"$it bit/s"}?:"Bitrate not reported"}"
            )
        }
        item {
            Text("App processing", style = MaterialTheme.typography.titleMedium)
            Text(
                "16-bit PCM · ${format.rate} Hz · ${format.channels} channels\nEQ: ${if(dsp.enabled)dsp.preset else "bypassed"} · ReplayGain ${dsp.replayMode}"
            )
        }
        item {
            Text("Android AudioTrack client", style = MaterialTheme.typography.titleMedium)
            Text(
                "${route.clientRate?.let{"$it Hz"}?:"Not reported"} · ${route.clientChannels?:"?"} channels · encoding ${route.clientEncoding?:"?"}"
            )
            Text(
                "This is the app's AudioTrack format. The system mixer, Bluetooth transport and DAC may use a different format. Hardware format, exclusive access and bit-perfect output are not verified. There is no direct-output mode.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            HorizontalDivider()
            Text(
                "Available outputs and reported capabilities",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "An empty capability list means Android did not report a fixed list. Connected does not imply active.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(devices, key = { it.id }) { d ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("${d.category.label} · ${d.name}")
                    Text(
                        "Rates: ${d.rates.joinToString().ifEmpty{"Not reported"}}\nChannels: ${d.channels.joinToString().ifEmpty{"Not reported"}}\nEncoding IDs: ${d.encodings.joinToString().ifEmpty{"Not reported"}}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun LyricsScreen(vm: PlayerViewModel, close: () -> Unit) {
    val playback by vm.playback.collectAsStateWithLifecycle()
    val embedded by vm.embeddedLyrics.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val changes by vm.lyrics.changes.collectAsStateWithLifecycle()
    val options by vm.lyrics.options.collectAsStateWithLifecycle(LyricOptions())
    val song = playback.item?.mediaId.orEmpty()
    val offsetFlow = remember(song) { if (song.isBlank()) flowOf(0L) else vm.lyrics.offset(song) }
    val offset by offsetFlow.collectAsStateWithLifecycle(0L)
    var loaded by remember(song) { mutableStateOf<LoadedLyrics?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val dragged by list.interactionSource.collectIsDraggedAsState()
    DisposableEffect(vm) {
        vm.lyricsVisible.value = true
        onDispose { vm.lyricsVisible.value = false }
    }
    LaunchedEffect(song, embedded, folders, changes) {
        loaded = null
        if (song.isNotBlank()) {
            loading = true
            try {
                loaded = vm.lyrics.load(song, embedded, folders)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                vm.message.value = "Lyrics could not be read"
            } finally {
                loading = false
            }
        }
    }
    val lyrics = loaded?.lyrics
    val index = lyrics?.current(playback.position, offset) ?: -1
    val currentTime = lyrics?.lines?.getOrNull(index)?.timeMs
    LaunchedEffect(index, options.autoScroll) {
        if (options.autoScroll && index >= 0) {
            val first = lyrics!!.lines.indexOfFirst { it.timeMs == currentTime }
            list.animateScrollToItem(first.coerceAtLeast(0))
        }
    }
    LaunchedEffect(dragged) {
        if (dragged && options.autoScroll) vm.lyricOptions { it.copy(autoScroll = false) }
    }
    var importFor by remember { mutableStateOf("") }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && importFor.isNotBlank()) vm.importLyrics(importFor, uri)
        }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Lyrics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton({ showOptions = !showOptions }) {
                Icon(Icons.Rounded.Tune, "Lyric settings")
            }
            IconButton(close) { Icon(Icons.Rounded.Close, "Close lyrics") }
        }
        Text(
            playback.item?.mediaMetadata?.title?.toString().orEmpty(),
            style = MaterialTheme.typography.titleMedium,
        )
        Row {
            TextButton(
                {
                    importFor = song
                    importer.launch(arrayOf("text/*", "application/octet-stream"))
                },
                enabled = song.isNotBlank(),
            ) {
                Text("Choose .lrc / text")
            }
            TextButton({ vm.removeLyrics(song) }, enabled = song.isNotBlank()) {
                Text("Remove imported")
            }
        }
        if (showOptions) {
            Text("Text size ${options.font.toInt()}")
            Slider(
                options.font,
                { v -> vm.lyricOptions { it.copy(font = v) } },
                valueRange = 14f..40f,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-scroll", Modifier.weight(1f))
                Switch(options.autoScroll, { v -> vm.lyricOptions { it.copy(autoScroll = v) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton({ vm.lyricOffset(song, offset - 250) }) { Text("−250 ms") }
                Text("Offset $offset ms", Modifier.weight(1f))
                TextButton({ vm.lyricOffset(song, offset + 250) }) { Text("+250 ms") }
            }
            TextButton({ vm.lyricOffset(song, 0) }) { Text("Reset offset") }
            Text(
                "Positive offset delays highlighting. Dragging lyrics turns auto-scroll off.",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            loaded?.source
                ?: if (loading) "Checking local lyrics…"
                else
                    "No lyrics found. Choose a local file. Automatic matching searches sibling .lrc/.txt files only within selected music folders.",
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(lyrics?.lines.orEmpty()) { line ->
                val active = currentTime != null && line.timeMs == currentTime
                Text(
                    line.text.ifBlank { " " },
                    fontSize = options.font.sp,
                    color =
                        if (active) MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (lyrics?.synchronized == true) .7f else 1f
                            ),
                    modifier =
                        Modifier.fillMaxWidth()
                            .semantics { selected = active }
                            .clickable(enabled = line.timeMs != null) {
                                vm.seek((line.timeMs ?: 0) + (lyrics?.offsetMs ?: 0) + offset)
                            },
                )
            }
        }
    }
}

@Composable
fun TextEntryDialog(title: String, initial: String, close: () -> Unit, save: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it.take(100) }, singleLine = true) },
        confirmButton = {
            TextButton({ save(text) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(close) { Text("Cancel") } },
    )
}

@UnstableApi
@Composable
fun SmartPlaylistsSection(vm: PlayerViewModel, open: () -> Unit) {
    val playlists by vm.smartPlaylists.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<SmartPlaylist?>(null) }
    var create by remember { mutableStateOf(false) }
    Column(
        Modifier.heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Smart playlists",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton({ create = true }) { Text("New rule") }
        }
        playlists.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    {
                        vm.filter.value = LibraryFilter(smart = p.rules, smartId = p.id)
                        open()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(p.name)
                }
                IconButton({ edit = p }) { Icon(Icons.Rounded.Edit, "Edit ${p.name}") }
                IconButton({ vm.deleteSmart(p.id) }) {
                    Icon(Icons.Rounded.DeleteOutline, "Delete ${p.name}")
                }
            }
        }
    }
    if (create || edit != null)
        SmartDialog(
            edit,
            {
                create = false
                edit = null
            },
        ) { name, rules ->
            vm.saveSmart(edit?.id, name, rules)
            create = false
            edit = null
        }
}

@Composable
private fun SmartDialog(
    original: SmartPlaylist?,
    close: () -> Unit,
    save: (String, SmartRules) -> Unit,
) {
    var name by remember { mutableStateOf(original?.name.orEmpty()) }
    var artist by remember { mutableStateOf(original?.rules?.artist.orEmpty()) }
    var album by remember { mutableStateOf(original?.rules?.album.orEmpty()) }
    var genre by remember { mutableStateOf(original?.rules?.genre.orEmpty()) }
    var plays by remember { mutableStateOf((original?.rules?.minPlays ?: 0).toString()) }
    var format by remember { mutableStateOf(original?.rules?.format.orEmpty()) }
    var favorite by remember { mutableStateOf(original?.rules?.favoritesOnly ?: false) }
    var added by remember {
        mutableStateOf(
            original
                ?.rules
                ?.addedAfter
                ?.takeIf { it > 0 }
                ?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                }
                .orEmpty()
        )
    }
    val date =
        runCatching {
                if (added.isBlank()) 0L
                else
                    LocalDate.parse(added)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                        .coerceAtLeast(0)
            }
            .getOrNull()
    val count = plays.toIntOrNull()
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Smart playlist · all rules match") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }) }
                item {
                    OutlinedTextField(
                        artist,
                        { artist = it.take(200) },
                        label = { Text("Artist (exact, optional)") },
                    )
                }
                item {
                    OutlinedTextField(
                        album,
                        { album = it.take(200) },
                        label = { Text("Album (exact, optional)") },
                    )
                }
                item {
                    OutlinedTextField(
                        genre,
                        { genre = it.take(200) },
                        label = { Text("Genre (exact, optional)") },
                    )
                }
                item {
                    OutlinedTextField(
                        plays,
                        { plays = it.take(7) },
                        label = { Text("Minimum play count") },
                        isError = count == null || count !in 0..1000000,
                    )
                }
                item {
                    OutlinedTextField(
                        added,
                        { added = it.take(10) },
                        label = { Text("Added since YYYY-MM-DD (optional)") },
                        isError = date == null,
                    )
                }
                item {
                    OutlinedTextField(
                        format,
                        { format = it.take(200) },
                        label = { Text("MIME contains (optional)") },
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Favorites only", Modifier.weight(1f))
                        Switch(favorite, { favorite = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                {
                    save(
                        name,
                        SmartRules(artist, album, genre, count ?: 0, date ?: 0, favorite, format),
                    )
                },
                enabled = name.isNotBlank() && date != null && count != null && count in 0..1000000,
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(close) { Text("Cancel") } },
    )
}
