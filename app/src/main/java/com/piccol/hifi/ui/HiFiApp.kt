package com.piccol.hifi.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.piccol.hifi.data.*

private val Ink = Color(0xFF080B12)
private val Surface = Color(0xFF121926)
private val Muted = Color(0xFF98A7BD)
private val accents = listOf(Color(0xFF58A6FF), Color(0xFF7BE0C3), Color(0xFFCCA3FF))
private enum class Screen(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Rounded.LibraryMusic), PLAYLISTS("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    PLAYER("Playing", Icons.Rounded.Album), EQ("Equalizer", Icons.Rounded.Tune), VISUALIZER("Visualizer", Icons.Rounded.GraphicEq)
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HiFiApp(vm: PlayerViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var queue by rememberSaveable { mutableStateOf(false) }
    var info by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val colors = darkColorScheme(primary = accents[settings.accent.coerceIn(0, 2)], background = Ink, surface = Surface,
        surfaceContainer = Surface, onSurface = Color(0xFFE8EEF8), onBackground = Color(0xFFE8EEF8), secondary = Muted)
    val musicAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    val notificationAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); vm.addFolder(uri) }
            catch (_: SecurityException) { vm.message.value = "This folder provider could not retain access. Choose another folder." }
        }
    }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
    MaterialTheme(colorScheme = colors) {
        Scaffold(containerColor = Ink, snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(title = { Column {
                    Text(if (showSettings) "Settings" else if (screen == Screen.PLAYER) "Now playing" else screen.label, fontWeight = FontWeight.Bold)
                    Text("HiFi Player · on your device", style = MaterialTheme.typography.labelSmall, color = Muted)
                } }, navigationIcon = {
                    if (showSettings) IconButton(onClick = { showSettings = false }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                }, actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Rounded.Refresh, "Refresh music library") }
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Rounded.Settings, "Settings") }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink))
            }, bottomBar = {
                Column {
                    if (playback.item != null && screen != Screen.PLAYER && !showSettings) MiniPlayer(playback, { screen = Screen.PLAYER }, vm::toggle, vm::next)
                    NavigationBar(containerColor = Ink, tonalElevation = 0.dp) {
                        Screen.entries.forEach { destination -> NavigationBarItem(
                            selected = screen == destination && !showSettings,
                            onClick = { screen = destination; showSettings = false },
                            icon = { Icon(destination.icon, destination.label) },
                            label = { Text(destination.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) }) }
                    }
                }
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (showSettings) SettingsScreen(vm, { folderPicker.launch(null) }, {
                    if (Build.VERSION.SDK_INT >= 33) notificationAccess.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) else when (screen) {
                    Screen.LIBRARY -> LibraryScreen(vm, {
                        musicAccess.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
                    }, { folderPicker.launch(null) })
                    Screen.PLAYLISTS -> PlaylistsScreen(vm, { screen = Screen.LIBRARY })
                    Screen.PLAYER -> NowPlaying(vm, playback, settings.technicalInfo, { queue = true }, { info = true })
                    Screen.EQ -> PendingScreen(Icons.Rounded.Tune, "Your sound, precisely tuned", "Equalization is not available in this version. Playback currently uses the standard Android audio path without custom DSP.")
                    Screen.VISUALIZER -> PendingScreen(Icons.Rounded.GraphicEq, "See your sound", "Audio visualizations are not available in this version.")
                }
            }
        }
        if (queue) ModalBottomSheet(onDismissRequest = { queue = false }) { QueueSheet(vm, playback) }
        if (info) AlertDialog(onDismissRequest = { info = false }, confirmButton = { TextButton(onClick = { info = false }) { Text("Done") } },
            title = { Text("Audio information") }, text = { AudioInfo(playback) })
    }
}

@UnstableApi
@Composable
private fun LibraryScreen(vm: PlayerViewModel, requestAccess: () -> Unit, chooseFolder: () -> Unit) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val scan by vm.scanner.state.collectAsStateWithLifecycle()
    val tracks = vm.tracks.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        if (!vm.scanner.hasMediaPermission()) {
            Card(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Column(Modifier.padding(16.dp)) {
                Text("Bring your music with you", style = MaterialTheme.typography.titleMedium)
                Text("Allow music access or select a folder. Your files stay on your device.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Row { TextButton(onClick = requestAccess) { Text("Allow music") }; TextButton(onClick = chooseFolder) { Text("Choose folder") } }
            } }
        }
        OutlinedTextField(value = filter.search, onValueChange = { vm.filter.value = filter.copy(search = it) }, singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("Songs, artists, albums") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp),
            trailingIcon = { if (filter.search.isNotEmpty()) IconButton(onClick = { vm.filter.value = filter.copy(search = "") }) { Icon(Icons.Rounded.Close, "Clear search") } })
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { category -> FilterChip(selected = filter.category == category && filter.playlist == null,
                onClick = { vm.filter.value = filter.copy(category = category, group = null, playlist = null) }, label = { Text(category.label) }) }
        }
        if (filter.playlist != null || filter.group != null) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(filter.group ?: "Playlist songs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { vm.filter.value = filter.copy(group = null, playlist = null) }) { Text("Clear") }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            var sortMenu by remember { mutableStateOf(false) }
            var formatMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, null); Spacer(Modifier.width(6.dp)); Text(filter.sort.label) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) { Sort.entries.forEach { sort ->
                    DropdownMenuItem(text = { Text(sort.label) }, onClick = { vm.filter.value = filter.copy(sort = sort); sortMenu = false })
                } }
            }
            Box {
                TextButton(onClick = { formatMenu = true }) { Text(filter.format.ifBlank { "All formats" }) }
                DropdownMenu(expanded = formatMenu, onDismissRequest = { formatMenu = false }) {
                    listOf("" to "All formats", "mpeg" to "MP3", "flac" to "FLAC", "wav" to "WAV", "mp4" to "M4A", "ogg" to "OGG", "opus" to "Opus").forEach { (value, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { vm.filter.value = filter.copy(format = value); formatMenu = false })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = vm::playAll) { Icon(Icons.Rounded.PlayCircle, "Play up to 1,000 matching songs") }
        }
        if (scan.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (scan.message.isNotEmpty()) Text(scan.message, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Muted)
        if (groups.isNotEmpty() && filter.group == null && filter.playlist == null) {
            LazyColumn { items(groups, key = { it }) { group -> ListItem(headlineContent = { Text(group.ifBlank { "Unknown" }) },
                leadingContent = { Icon(if (filter.category == Category.FOLDERS) Icons.Rounded.Folder else Icons.Rounded.Album, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { vm.filter.value = filter.copy(group = group) }, colors = ListItemDefaults.colors(containerColor = Ink)) } }
        } else if (tracks.loadState.refresh is LoadState.Error) {
            EmptyState("Library could not be loaded", "Try refreshing your music library.") { TextButton(onClick = { tracks.retry() }) { Text("Retry") } }
        } else if (tracks.itemCount == 0 && tracks.loadState.refresh !is LoadState.Loading && !scan.running) {
            EmptyState("A little quiet in here", "Add local music, choose another category, or clear your search.") { TextButton(onClick = chooseFolder) { Text("Choose music folder") } }
        } else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(tracks.itemCount) { index -> tracks[index]?.let { track -> TrackRow(track, track.uri in favorites, vm, filter.playlist) } }
            if (tracks.loadState.append is LoadState.Loading) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
        }
    }
}

@UnstableApi
@Composable
private fun TrackRow(track: Track, favorite: Boolean, vm: PlayerViewModel, playlist: Long?) {
    var menu by remember(track.uri) { mutableStateOf(false) }
    var addToPlaylist by remember(track.uri) { mutableStateOf(false) }
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    ListItem(headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${track.artist} · ${track.album}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Artwork(track.artwork, Modifier.size(48.dp)) },
        trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time(track.duration), style = MaterialTheme.typography.labelSmall, color = Muted)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Options for ${track.title}") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Play next") }, onClick = { vm.enqueue(track, true); menu = false })
                    DropdownMenuItem(text = { Text("Add to queue") }, onClick = { vm.enqueue(track, false); menu = false })
                    DropdownMenuItem(text = { Text(if (favorite) "Remove favorite" else "Favorite") }, onClick = { vm.favorite(track.uri); menu = false })
                    DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { addToPlaylist = true; menu = false })
                    if (playlist != null) DropdownMenuItem(text = { Text("Remove from playlist") }, onClick = { vm.removeFromPlaylist(playlist, track.uri); menu = false })
                }
            }
        } }, modifier = Modifier.clickable { vm.play(track) }, colors = ListItemDefaults.colors(containerColor = Ink))
    if (addToPlaylist) AlertDialog(onDismissRequest = { addToPlaylist = false }, confirmButton = { TextButton(onClick = { addToPlaylist = false }) { Text("Done") } },
        title = { Text("Add to playlist") }, text = {
            if (playlists.isEmpty()) Text("Create a playlist in the Playlists tab first.")
            else LazyColumn { items(playlists, key = { it.id }) { p -> TextButton(onClick = { vm.addToPlaylist(p.id, track.uri); addToPlaylist = false }) { Text(p.name) } } }
        })
}

@Composable
private fun Artwork(uri: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Surface), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Album, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = .65f), modifier = Modifier.fillMaxSize(.5f))
        if (uri != null) AsyncImage(model = uri, contentDescription = "Album artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun MiniPlayer(state: PlaybackState, open: () -> Unit, toggle: () -> Unit, next: () -> Unit) {
    Surface(color = Surface, modifier = Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = open)) {
        Column {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(state.item?.mediaMetadata?.artworkUri?.toString(), Modifier.size(42.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(state.item?.mediaMetadata?.title?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(state.item?.mediaMetadata?.artist?.toString().orEmpty(), maxLines = 1, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = toggle) { Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Play") }
                IconButton(onClick = next) { Icon(Icons.Rounded.SkipNext, "Next track") }
            }
            LinearProgressIndicator(progress = { if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

@UnstableApi
@Composable
private fun NowPlaying(vm: PlayerViewModel, state: PlaybackState, technical: Boolean, queue: () -> Unit, info: () -> Unit) {
    if (state.item == null) { EmptyState("Ready when you are", "Choose a song from your library to start listening."); return }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val metadata = state.item.mediaMetadata
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val art: @Composable () -> Unit = { Artwork(metadata.artworkUri?.toString(), Modifier.fillMaxWidth().aspectRatio(1f)) }
        val controls: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(20.dp))
                Text(metadata.title?.toString().orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(metadata.artist?.toString().orEmpty(), color = Muted, style = MaterialTheme.typography.titleMedium)
                Text(metadata.albumTitle?.toString().orEmpty(), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (technical) Text("Android audio · ${if (state.buffering) "Buffering" else "Standard playback"}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                var scrub by remember(state.item.mediaId) { mutableStateOf<Float?>(null) }
                Slider(value = scrub ?: state.position.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { scrub = it }, onValueChangeFinished = { scrub?.let { vm.seek(it.toLong()) }; scrub = null },
                    valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(time(scrub?.toLong() ?: state.position), color = Muted, style = MaterialTheme.typography.labelSmall)
                    Text(time(state.duration), color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::shuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle ${if (state.shuffle) "on" else "off"}", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else Muted) }
                    IconButton(onClick = vm::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous track", Modifier.size(34.dp)) }
                    FilledIconButton(onClick = vm::toggle, modifier = Modifier.size(72.dp)) { Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "Pause" else "Play", Modifier.size(38.dp)) }
                    IconButton(onClick = vm::next) { Icon(Icons.Rounded.SkipNext, "Next track", Modifier.size(34.dp)) }
                    IconButton(onClick = vm::repeat) { Icon(if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat ${when (state.repeat) { 1 -> "one"; 2 -> "all"; else -> "off" }}", tint = if (state.repeat != 0) MaterialTheme.colorScheme.primary else Muted) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { vm.favorite(state.item.mediaId) }) { Icon(if (state.item.mediaId in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Toggle favorite") }
                    IconButton(onClick = queue) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Playback queue") }
                    IconButton(onClick = info) { Icon(Icons.Rounded.Info, "Audio information") }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (landscape) Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(end = 24.dp)) { art() }
            Box(Modifier.weight(1f)) { LazyColumn { item { controls() } } }
        } else LazyColumn(contentPadding = PaddingValues(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { item { art() }; item { controls() } }
    }
}

@UnstableApi
@Composable
private fun QueueSheet(vm: PlayerViewModel, state: PlaybackState) {
    var save by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Up next · ${state.queue.size}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { save = true }, enabled = state.queue.isNotEmpty()) { Text("Save playlist") }
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(state.queue.size) { i ->
            val item = state.queue[i]
            ListItem(headlineContent = { Text(item.mediaMetadata.title?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(if (i == state.index) "Current track" else item.mediaMetadata.artist?.toString().orEmpty()) },
                trailingContent = { Row {
                    IconButton(onClick = { vm.move(i, i - 1) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { vm.move(i, i + 1) }, enabled = i < state.queue.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { vm.remove(i) }) { Icon(Icons.Rounded.Close, "Remove from queue") }
                } }, modifier = Modifier.clickable { vm.selectQueue(i) })
        }
    }
    if (save) NameDialog("Save queue as playlist", "", { save = false }) { vm.createPlaylist(it, true); save = false }
}

@UnstableApi
@Composable
private fun PlaylistsScreen(vm: PlayerViewModel, open: () -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var create by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<PlaylistSummary?>(null) }
    var delete by remember { mutableStateOf<PlaylistSummary?>(null) }
    Column {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Made by you", style = MaterialTheme.typography.headlineSmall)
            FilledTonalButton(onClick = { create = true }) { Icon(Icons.Rounded.Add, null); Text("New") }
        }
        if (playlists.isEmpty()) EmptyState("Every mood has a soundtrack", "Create a playlist, then add songs from the library.")
        LazyColumn { items(playlists, key = { it.id }) { p ->
            ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${p.count} songs · ${time(p.duration)}") },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) }, trailingContent = { Row {
                    IconButton(onClick = { rename = p }) { Icon(Icons.Rounded.Edit, "Rename playlist") }
                    IconButton(onClick = { delete = p }) { Icon(Icons.Rounded.DeleteOutline, "Delete playlist") }
                } }, colors = ListItemDefaults.colors(containerColor = Ink), modifier = Modifier.clickable { vm.filter.value = LibraryFilter(playlist = p.id); open() })
        } }
    }
    if (create) NameDialog("New playlist", "", { create = false }) { vm.createPlaylist(it); create = false }
    rename?.let { p -> NameDialog("Rename playlist", p.name, { rename = null }) { vm.renamePlaylist(p.id, it); rename = null } }
    delete?.let { p -> AlertDialog(onDismissRequest = { delete = null }, title = { Text("Delete ${p.name}?") }, text = { Text("Your music files will stay on your device.") },
        confirmButton = { TextButton(onClick = { vm.deletePlaylist(p.id); delete = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancel") } }) }
}

@UnstableApi
@Composable
private fun SettingsScreen(vm: PlayerViewModel, addFolder: () -> Unit, notifications: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Audio", style = MaterialTheme.typography.titleLarge) }
        item {
            Text("Playback speed · ${"%.2f".format(settings.speed)}×")
            var speed by remember(settings.speed) { mutableFloatStateOf(settings.speed) }
            Slider(value = speed, onValueChange = { speed = it }, onValueChangeFinished = { vm.speed(speed) }, valueRange = .5f..2f, steps = 5)
            Text("Gapless transitions use Media3's standard pipeline for compatible files. Crossfade and custom DSP are unavailable.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item { TextButton(onClick = notifications) { Text("Allow playback notifications") } }
        item { HorizontalDivider(); Text("Library", style = MaterialTheme.typography.titleLarge) }
        item { Button(onClick = addFolder) { Icon(Icons.Rounded.CreateNewFolder, null); Spacer(Modifier.width(8.dp)); Text("Add music folder") } }
        items(folders.toList(), key = { it }) { folder -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(android.net.Uri.decode(folder.substringAfterLast('/')), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = { vm.removeFolder(folder) }) { Icon(Icons.Rounded.Close, "Remove folder access") }
        } }
        item { TextButton(onClick = vm::refresh) { Text("Scan library") } }
        item { HorizontalDivider(); Text("Appearance", style = MaterialTheme.typography.titleLarge) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Compact audio information", Modifier.weight(1f)); Switch(checked = settings.technicalInfo, onCheckedChange = vm::technical) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("Blue", "Mint", "Violet").forEachIndexed { i, name ->
            FilterChip(selected = settings.accent == i, onClick = { vm.accent(i) }, label = { Text(name) })
        } } }
        item { HorizontalDivider(); Text("Privacy & data", style = MaterialTheme.typography.titleLarge) }
        item { Text("Your library, playlists, favorites, queue and settings are stored locally. No accounts, analytics, ads or network permission.", color = Muted) }
        item { TextButton(onClick = vm::clearHistory) { Text("Clear playback history") } }
        item { HorizontalDivider(); Text("HiFi Player 0.1.0", style = MaterialTheme.typography.titleMedium); Text("Native Android · Media3 playback\nOutput quality depends on the decoder, Android mixer and connected device. Bit-perfect output is not verified.", color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, dismiss: () -> Unit, save: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(name, { name = it.take(100) }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { save(name) }, enabled = name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun AudioInfo(state: PlaybackState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.item?.mediaMetadata?.title?.toString().orEmpty(), fontWeight = FontWeight.Bold)
        Text("Duration: ${time(state.duration)}")
        Text("Custom DSP: off\nOutput format: not verified\nSource codec details will be available in a later version.")
        Text("High-resolution source files do not guarantee high-resolution hardware output.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PendingScreen(icon: ImageVector, title: String, text: String) {
    EmptyState(title, text) { Icon(icon, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun EmptyState(title: String, description: String, action: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp)); Text(description, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp)); action()
    }
}

fun time(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
}
