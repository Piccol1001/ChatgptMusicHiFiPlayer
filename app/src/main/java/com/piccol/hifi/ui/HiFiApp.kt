package com.piccol.hifi.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.piccol.hifi.data.*
import com.piccol.hifi.library.readLimited
import kotlinx.coroutines.*

private val Ink = Color(0xFF080A0F)
private val Surface = Color(0xFF151922)
private val Muted = Color(0xFFA4ADBF)
private val accents = listOf(Color(0xFF58A6FF), Color(0xFF7BE0C3), Color(0xFFCCA3FF))

private enum class Screen(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    PLAYER("Playing", Icons.Rounded.Album),
    EQ("Equalizer", Icons.Rounded.Tune),
    VISUALIZER("Visualizer", Icons.Rounded.GraphicEq),
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun HiFiApp(vm: PlayerViewModel, openPlayer: Boolean = false, onPlayerOpened: () -> Unit = {}) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val eqState by vm.dspSettings.collectAsStateWithLifecycle()
    var searchRequest by remember { mutableIntStateOf(0) }
    var createRequest by remember { mutableIntStateOf(0) }
    var importRequest by remember { mutableIntStateOf(0) }
    var presetRequest by remember { mutableIntStateOf(0) }
    var visualOptionsRequest by remember { mutableIntStateOf(0) }
    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    val accent =
        artworkAccent(
            playback.item?.mediaMetadata?.artworkUri?.toString(),
            settings.artworkColor,
            accents[settings.accent.coerceIn(0, 2)],
        )
    val background = if (settings.amoled) Color.Black else Color(0xFF171B23)
    var screen by rememberSaveable { mutableStateOf(Screen.LIBRARY) }
    var visualFullScreen by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var queue by rememberSaveable { mutableStateOf(false) }
    var profiles by rememberSaveable { mutableStateOf(false) }
    var output by rememberSaveable { mutableStateOf(false) }
    var lyrics by rememberSaveable { mutableStateOf(false) }
    var info by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openPlayer) {
        if (openPlayer) {
            screen = Screen.PLAYER
            showSettings = false
            visualFullScreen = false
            onPlayerOpened()
        }
    }
    BackHandler(showSettings) {
        if (settingsPage != null) settingsPage = null else showSettings = false
    }
    val snackbar = remember { SnackbarHostState() }
    val colors =
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF07111E),
            background = background,
            surface = Surface,
            surfaceContainer = Surface,
            onSurface = Color(0xFFF4F6FA),
            onSurfaceVariant = Muted,
            error = Color(0xFFFF6B6B),
            onBackground = Color(0xFFF4F6FA),
            secondary = Muted,
            secondaryContainer = Color(0xFF202633),
            onSecondaryContainer = Color(0xFFF4F6FA),
            surfaceVariant = Color(0xFF202633),
            surfaceContainerHigh = Color(0xFF202633),
            outline = Color(0xFF333D4E),
        )
    val musicAccess =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            vm.refresh()
        }
    val notificationAccess =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    vm.addFolder(uri)
                } catch (_: SecurityException) {
                    vm.message.value =
                        "This folder provider could not retain access. Choose another folder."
                }
            }
        }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.message.value = null
        }
    }
    MaterialTheme(colorScheme = colors, typography = PlayerTypography) {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = background,
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets =
                if (visualFullScreen) WindowInsets(0, 0, 0, 0)
                else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
            topBar = {
                if (!visualFullScreen)
                    Row(
                        Modifier.fillMaxWidth()
                            .background(background)
                            .statusBarsPadding()
                            .heightIn(min = 60.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showSettings || screen == Screen.PLAYER)
                            IconButton({
                                if (showSettings) {
                                    if (settingsPage != null) settingsPage = null
                                    else showSettings = false
                                } else screen = Screen.LIBRARY
                            }) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    if (showSettings) "Back" else "Minimize player",
                                )
                            }
                        Text(
                            if (showSettings) settingsPage ?: "Settings"
                            else if (screen == Screen.PLAYER) "Now Playing" else screen.label,
                            Modifier.weight(1f).padding(start = 8.dp),
                            style =
                                if (showSettings && settingsPage != null)
                                    MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!showSettings) {
                            when (screen) {
                                Screen.LIBRARY -> {
                                    IconButton({ searchRequest++ }) {
                                        Icon(Icons.Rounded.Search, "Search library")
                                    }
                                    IconButton(vm::refresh) {
                                        Icon(Icons.Rounded.Refresh, "Rescan library")
                                    }
                                }
                                Screen.PLAYLISTS -> {
                                    IconButton({ createRequest++ }) {
                                        Icon(Icons.Rounded.Add, "Create playlist")
                                    }
                                    IconButton({ importRequest++ }) {
                                        Icon(Icons.Rounded.FileOpen, "Import playlist")
                                    }
                                }
                                Screen.PLAYER ->
                                    IconButton({ queue = true }) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.QueueMusic,
                                            "Playback queue",
                                        )
                                    }
                                Screen.EQ -> {
                                    Switch(
                                        eqState.enabled,
                                        { enabled -> vm.changeDsp { it.copy(enabled = enabled) } },
                                        Modifier.semantics {
                                            contentDescription = "Equalizer enabled"
                                        },
                                    )
                                    IconButton({ presetRequest++ }) {
                                        Icon(Icons.Rounded.LibraryMusic, "Choose EQ preset")
                                    }
                                }
                                Screen.VISUALIZER -> {
                                    IconButton({ visualFullScreen = true }) {
                                        Icon(Icons.Rounded.Fullscreen, "Fullscreen visualizer")
                                    }
                                    IconButton({ visualOptionsRequest++ }) {
                                        Icon(Icons.Rounded.Tune, "Visualization settings")
                                    }
                                }
                            }
                            IconButton({
                                showSettings = true
                                settingsPage = null
                            }) {
                                Icon(Icons.Rounded.Settings, "Settings")
                            }
                        }
                    }
            },
            bottomBar = {
                if (!visualFullScreen && !showSettings)
                    Column(Modifier.background(background).navigationBarsPadding()) {
                        if (playback.item != null && screen != Screen.PLAYER)
                            MiniPlayer(playback, { screen = Screen.PLAYER }, vm::toggle, vm::next)
                        HorizontalDivider(color = Color.White.copy(alpha = .06f))
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp)
                        ) {
                            Screen.entries.forEach { destination ->
                                val active = screen == destination
                                Column(
                                    Modifier.weight(1f)
                                        .selectable(active, role = Role.Tab) {
                                            screen = destination
                                            showSettings = false
                                            visualFullScreen = false
                                        }
                                        .semantics {
                                            contentDescription = "Open ${destination.label}"
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        destination.icon,
                                        null,
                                        Modifier.size(23.dp),
                                        tint = if (active) accent else Muted,
                                    )
                                    Text(
                                        destination.label,
                                        modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        fontWeight =
                                            if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) accent else Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
            },
        ) { padding ->
            Box(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .then(if (showSettings) Modifier.navigationBarsPadding() else Modifier)
            ) {
                if (showSettings)
                    SettingsScreen(
                        vm,
                        { folderPicker.launch(null) },
                        {
                            if (Build.VERSION.SDK_INT >= 33)
                                notificationAccess.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        { profiles = true },
                        { output = true },
                        settingsPage,
                        { settingsPage = it },
                        {
                            showSettings = false
                            screen = Screen.EQ
                        },
                        {
                            showSettings = false
                            screen = Screen.VISUALIZER
                        },
                    )
                else
                    when (screen) {
                        Screen.LIBRARY ->
                            LibraryScreen(
                                vm,
                                {
                                    musicAccess.launch(
                                        if (Build.VERSION.SDK_INT >= 33)
                                            Manifest.permission.READ_MEDIA_AUDIO
                                        else Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                },
                                { folderPicker.launch(null) },
                                searchRequest,
                            )
                        Screen.PLAYLISTS ->
                            PlaylistsScreen(
                                vm,
                                { screen = Screen.LIBRARY },
                                createRequest,
                                importRequest,
                            )
                        Screen.PLAYER ->
                            NowPlaying(
                                vm,
                                playback,
                                settings.technicalInfo,
                                { queue = true },
                                { info = true },
                                { lyrics = true },
                                { screen = Screen.EQ },
                                { screen = Screen.VISUALIZER },
                                { screen = Screen.LIBRARY },
                            )
                        Screen.EQ -> EqualizerScreen(vm, presetRequest) { profiles = true }
                        Screen.VISUALIZER ->
                            VisualizerScreen(
                                vm,
                                visualFullScreen,
                                visualOptionsRequest,
                                {
                                    visualFullScreen = false
                                    screen = Screen.LIBRARY
                                },
                            ) {
                                visualFullScreen = it
                            }
                    }
            }
        }
        if (profiles)
            ModalBottomSheet({ profiles = false }) {
                Box(Modifier.fillMaxWidth().heightIn(max = 650.dp)) { ProfilesScreen(vm) }
            }
        if (output)
            ModalBottomSheet({ output = false }) {
                Box(Modifier.fillMaxWidth().heightIn(max = 650.dp)) { AudioOutputScreen(vm) }
            }
        if (lyrics)
            Dialog(
                { lyrics = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(Modifier.fillMaxSize()) { LyricsScreen(vm) { lyrics = false } }
            }
        if (queue)
            ModalBottomSheet(onDismissRequest = { queue = false }) { QueueSheet(vm, playback) }
        if (info)
            AlertDialog(
                onDismissRequest = { info = false },
                confirmButton = { TextButton(onClick = { info = false }) { Text("Done") } },
                title = { Text("Audio information") },
                text = { AudioInfo(vm, playback) },
            )
    }
}

@UnstableApi
@Composable
private fun LibraryScreen(
    vm: PlayerViewModel,
    requestAccess: () -> Unit,
    chooseFolder: () -> Unit,
    searchRequest: Int = 0,
) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val groups by vm.libraryGroups.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val scan by vm.scanner.state.collectAsStateWithLifecycle()
    val tracks = vm.tracks.collectAsLazyPagingItems()
    val libraryState = rememberLazyGridState()
    val focus = remember { FocusRequester() }
    var folderPrefix by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(filter.category) { folderPrefix = "" }
    LaunchedEffect(searchRequest) {
        if (searchRequest > 0) {
            libraryState.scrollToItem(0)
            withFrameNanos {}
            focus.requestFocus()
        }
    }
    val grouping =
        filter.category in
            listOf(Category.ALBUMS, Category.ARTISTS, Category.GENRES, Category.FOLDERS) &&
            filter.group == null &&
            filter.playlist == null &&
            filter.smart == null &&
            filter.search.isBlank()
    val albums = grouping && filter.category == Category.ALBUMS
    val folderNodes = remember(groups, folderPrefix) { folderChildren(groups, folderPrefix) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (albums) 2 else 1),
        state = libraryState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TextField(
                filter.search,
                { vm.filter.value = filter.copy(search = it) },
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focus),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Songs, artists, albums") },
                trailingIcon = {
                    if (filter.search.isNotEmpty())
                        IconButton({ vm.filter.value = filter.copy(search = "") }) {
                            Icon(Icons.Rounded.Close, "Clear search")
                        }
                },
                shape = RoundedCornerShape(14.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Category.entries) { c ->
                    CompactTab(
                        c.label,
                        filter.category == c && filter.playlist == null && filter.smart == null,
                        {
                            vm.filter.value =
                                filter.copy(
                                    category = c,
                                    group = null,
                                    playlist = null,
                                    smart = null,
                                    smartId = null,
                                )
                        },
                    )
                }
            }
        }
        if (!vm.scanner.hasMediaPermission() && folders.isEmpty())
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "Your music stays on your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(requestAccess) { Text("Grant Music Access") }
                        TextButton(
                            chooseFolder,
                            colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                        ) {
                            Text("Choose Folder")
                        }
                    }
                }
            }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (filter.group != null || filter.playlist != null || filter.smart != null) {
                    IconButton({
                        vm.filter.value =
                            filter.copy(group = null, playlist = null, smart = null, smartId = null)
                    }) {
                        Icon(Icons.Rounded.ArrowBack, "Back to collection")
                    }
                    Text(
                        filter.group?.substringAfterLast('/')?.ifBlank { filter.group!! }
                            ?: if (filter.smart != null) "Smart collection" else "Playlist",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    var sort by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            { sort = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, null, Modifier.size(18.dp))
                            Text(filter.sort.label)
                        }
                        DropdownMenu(sort, { sort = false }) {
                            Sort.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        vm.filter.value = filter.copy(sort = order)
                                        sort = false
                                    },
                                )
                            }
                        }
                    }
                    var format by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            { format = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                        ) {
                            Text(filter.format.ifBlank { "All formats" })
                        }
                        DropdownMenu(format, { format = false }) {
                            listOf(
                                    "" to "All formats",
                                    "mpeg" to "MP3",
                                    "flac" to "FLAC",
                                    "wav" to "WAV",
                                    "mp4" to "M4A",
                                    "ogg" to "OGG",
                                    "opus" to "Opus",
                                )
                                .forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            vm.filter.value = filter.copy(format = key)
                                            format = false
                                        },
                                    )
                                }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
                IconButton(vm::playAll) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        "Play up to 1,000 matching songs",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (scan.running)
            item(span = { GridItemSpan(maxLineSpan) }) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
        if (
            scan.message.isNotEmpty() &&
                (scan.running ||
                    scan.message.contains("unavailable") ||
                    scan.message.contains("again"))
        )
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    scan.message,
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        if (grouping && groups.isNotEmpty()) {
            if (filter.category == Category.FOLDERS) {
                if (folderPrefix.isNotEmpty() || groups.any { it.name.trim('/').isEmpty() })
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (folderPrefix.isNotEmpty())
                                IconButton({
                                    folderPrefix = folderPrefix.substringBeforeLast('/', "")
                                }) {
                                    Icon(Icons.Rounded.ArrowUpward, "Parent folder")
                                }
                            Text(
                                folderPrefix.ifEmpty { "Storage root" },
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            groups
                                .firstOrNull { it.name.trim('/') == folderPrefix }
                                ?.let { g ->
                                    TextButton({ vm.filter.value = filter.copy(group = g.name) }) {
                                        Text("Songs here")
                                    }
                                }
                        }
                    }
                items(folderNodes, key = { it.path }) { node ->
                    ListItem(
                        headlineContent = {
                            Text(node.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = { Text("${node.count} songs") },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Folder,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                        colors =
                            ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                        modifier =
                            Modifier.clickable {
                                val direct = groups.firstOrNull { it.name.trim('/') == node.path }
                                if (node.hasChildren || direct == null) folderPrefix = node.path
                                else vm.filter.value = filter.copy(group = direct.name)
                            },
                    )
                }
            } else
                itemsIndexed(groups, key = { _, g -> g.name }) { groupIndex, g ->
                    if (albums)
                        Column(
                            Modifier.padding(
                                    start = if (groupIndex % 2 == 0) 16.dp else 0.dp,
                                    end = if (groupIndex % 2 == 1) 16.dp else 0.dp,
                                    bottom = 20.dp,
                                )
                                .clickable { vm.filter.value = filter.copy(group = g.name) }
                        ) {
                            Artwork(g.artwork, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(
                                g.name.ifBlank { "Unknown album" },
                                Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${g.subtitle} · ${g.count} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    else
                        ListItem(
                            headlineContent = {
                                Text(
                                    g.name.ifBlank { "Unknown" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = { Text("${g.count} songs") },
                            leadingContent = {
                                if (filter.category == Category.ARTISTS)
                                    Box(
                                        Modifier.size(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Person, null, tint = Muted)
                                    }
                                else Artwork(g.artwork, Modifier.size(48.dp))
                            },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                            modifier =
                                Modifier.clickable { vm.filter.value = filter.copy(group = g.name) },
                        )
                }
        } else if (tracks.loadState.refresh is LoadState.Error)
            item(span = { GridItemSpan(maxLineSpan) }) {
                MusicEmptyState(
                    "Library unavailable",
                    "Check folder access and try again.",
                    "Retry",
                    { tracks.retry() },
                    Icons.Rounded.FolderOff,
                )
            }
        else if (
            tracks.itemCount == 0 && tracks.loadState.refresh !is LoadState.Loading && !scan.running
        )
            item(span = { GridItemSpan(maxLineSpan) }) {
                MusicEmptyState(
                    if (filter.search.isBlank()) "No music found" else "No matches",
                    if (filter.search.isBlank())
                        "Add a local music folder or grant storage access to start listening."
                    else "Try another song, artist or album.",
                    if (filter.search.isBlank()) "Add Music" else "Clear search",
                    {
                        if (filter.search.isBlank()) chooseFolder()
                        else vm.filter.value = filter.copy(search = "")
                    },
                    Icons.Rounded.LibraryMusic,
                )
            }
        else {
            items(tracks.itemCount) { i ->
                tracks[i]?.let { TrackRow(it, it.uri in favorites, vm, filter.playlist) }
            }
            if (tracks.loadState.append is LoadState.Loading)
                item { CircularProgressIndicator(Modifier.padding(16.dp)) }
        }
    }
}

@UnstableApi
@Composable
private fun TrackRow(track: Track, favorite: Boolean, vm: PlayerViewModel, playlist: Long?) {
    var menu by remember(track.uri) { mutableStateOf(false) }
    var addToPlaylist by remember(track.uri) { mutableStateOf(false) }
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    ListItem(
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(
                    "${track.artist} · ${track.album}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (settings.detailedList)
                    Text(
                        "${track.mime.substringAfter('/').uppercase()} · ${track.size/1024} KiB",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
            }
        },
        leadingContent = { Artwork(track.artwork, Modifier.size(48.dp)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, "Options for ${track.title}")
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Play next") },
                            onClick = {
                                vm.enqueue(track, true)
                                menu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to queue") },
                            onClick = {
                                vm.enqueue(track, false)
                                menu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (favorite) "Remove favorite" else "Favorite") },
                            onClick = {
                                vm.favorite(track.uri)
                                menu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to playlist") },
                            onClick = {
                                addToPlaylist = true
                                menu = false
                            },
                        )
                        if (playlist != null)
                            DropdownMenuItem(
                                text = { Text("Remove from playlist") },
                                onClick = {
                                    vm.removeFromPlaylist(playlist, track.uri)
                                    menu = false
                                },
                            )
                    }
                }
            }
        },
        modifier = Modifier.clickable { vm.play(track) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
    if (addToPlaylist)
        AlertDialog(
            onDismissRequest = { addToPlaylist = false },
            confirmButton = { TextButton(onClick = { addToPlaylist = false }) { Text("Done") } },
            title = { Text("Add to playlist") },
            text = {
                if (playlists.isEmpty()) Text("Create a playlist in the Playlists tab first.")
                else
                    LazyColumn {
                        items(playlists, key = { it.id }) { p ->
                            TextButton(
                                onClick = {
                                    vm.addToPlaylist(p.id, track.uri)
                                    addToPlaylist = false
                                }
                            ) {
                                Text(p.name)
                            }
                        }
                    }
            },
        )
}

@Composable
internal fun Artwork(
    uri: String?,
    modifier: Modifier = Modifier,
    corners: Int = 12,
    fit: Boolean = false,
) {
    Box(
        modifier.clip(RoundedCornerShape(corners.dp)).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Album,
            null,
            Modifier.fillMaxSize(.36f),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = .5f),
        )
        if (uri != null)
            AsyncImage(
                uri,
                "Album artwork",
                Modifier.fillMaxSize(),
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
            )
    }
}

@Composable
private fun MiniPlayer(
    state: PlaybackState,
    open: () -> Unit,
    toggle: () -> Unit,
    next: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = open),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(state.item?.mediaMetadata?.artworkUri?.toString(), Modifier.size(44.dp), 8)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        state.item?.mediaMetadata?.title?.toString().orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        state.item?.mediaMetadata?.artist?.toString().orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                IconButton(toggle) {
                    Icon(
                        if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.playing) "Pause" else "Play",
                    )
                }
                IconButton(next) { Icon(Icons.Rounded.SkipNext, "Next track") }
            }
            LinearProgressIndicator(
                progress = {
                    if (state.duration > 0)
                        (state.position.toFloat() / state.duration).coerceIn(0f, 1f)
                    else 0f
                },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                trackColor = Color.Transparent,
            )
        }
    }
}

@UnstableApi
@Composable
private fun NowPlaying(
    vm: PlayerViewModel,
    state: PlaybackState,
    technical: Boolean,
    queue: () -> Unit,
    info: () -> Unit,
    lyrics: () -> Unit,
    eq: () -> Unit,
    visual: () -> Unit,
    browse: () -> Unit,
) {
    if (state.item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MusicEmptyState(
                "Ready to listen?",
                "Choose music from your library to start playing.",
                "Browse Library",
                browse,
            )
        }
        return
    }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val source by vm.sourceInfo.collectAsStateWithLifecycle()
    val metadata = state.item.mediaMetadata
    val accent = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(
                if (settings.artworkGradient)
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = .14f), background, background)
                    )
                else Brush.verticalGradient(listOf(background, background))
            )
    ) {
        val landscape = maxWidth > maxHeight
        val artSize =
            if (landscape) minOf(maxWidth * .4f, maxHeight - 32.dp).coerceAtLeast(96.dp)
            else
                minOf(maxWidth * .8f, (maxHeight - 320.dp).coerceAtLeast(160.dp))
                    .coerceAtMost(380.dp)
        val art: @Composable () -> Unit = {
            Artwork(
                metadata.artworkUri?.toString(),
                Modifier.size(artSize),
                settings.artworkCorners,
                true,
            )
        }
        val sourceBadge: @Composable () -> Unit = {
            Surface(
                color = accent.copy(alpha = .09f),
                shape = RoundedCornerShape(8.dp),
                onClick = info,
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.GraphicEq, null, Modifier.size(16.dp), tint = accent)
                    Text(
                        if (technical)
                            listOfNotNull(
                                    source.codec.substringAfterLast('/').uppercase().takeUnless {
                                        it.isBlank() || it == "NOT REPORTED"
                                    },
                                    source.bits?.let { "$it-bit" },
                                    source.rate?.let { "${it/1000.0} kHz" },
                                )
                                .joinToString(" · ")
                                .ifBlank { "Audio information" }
                        else "Audio information",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = accent)
                }
            }
        }
        val controls: @Composable () -> Unit = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (landscape) 4.dp else 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            metadata.title?.toString().orEmpty(),
                            style =
                                if (landscape) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.headlineSmall,
                            maxLines = if (landscape) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            metadata.artist?.toString().orEmpty(),
                            style =
                                if (landscape) MaterialTheme.typography.bodySmall
                                else MaterialTheme.typography.bodyLarge,
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            metadata.albumTitle?.toString().orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton({ vm.favorite(state.item.mediaId) }) {
                        Icon(
                            if (state.item.mediaId in favorites) Icons.Rounded.Favorite
                            else Icons.Rounded.FavoriteBorder,
                            "Toggle favorite",
                            tint = if (state.item.mediaId in favorites) accent else Muted,
                        )
                    }
                }
                if (!landscape) sourceBadge()
                var scrub by remember(state.item.mediaId) { mutableStateOf<Float?>(null) }
                Column {
                    Slider(
                        value =
                            scrub
                                ?: state.position
                                    .toFloat()
                                    .coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                        onValueChange = { scrub = it },
                        onValueChangeFinished = {
                            scrub?.let { vm.seek(it.toLong()) }
                            scrub = null
                        },
                        valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                        enabled = state.duration > 0,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            time(scrub?.toLong() ?: state.position),
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted,
                        )
                        Text(
                            time(state.duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(vm::shuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            "Shuffle ${if(state.shuffle)"on" else "off"}",
                            tint = if (state.shuffle) accent else Muted,
                        )
                    }
                    IconButton(vm::previous, Modifier.size(52.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous track", Modifier.size(30.dp))
                    }
                    FilledIconButton(
                        vm::toggle,
                        Modifier.size(if (landscape) 56.dp else 68.dp),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Icon(
                            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.playing) "Pause" else "Play",
                            Modifier.size(34.dp),
                        )
                    }
                    IconButton(vm::next, Modifier.size(52.dp)) {
                        Icon(Icons.Rounded.SkipNext, "Next track", Modifier.size(30.dp))
                    }
                    IconButton(vm::repeat) {
                        Icon(
                            if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                            else Icons.Rounded.Repeat,
                            "Repeat ${when(state.repeat){1->"one"
2->"all"
else->"off"}}",
                            tint = if (state.repeat != 0) accent else Muted,
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = .06f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PlayerAction(Icons.Rounded.Lyrics, "Lyrics", lyrics, Modifier.weight(1f))
                    PlayerAction(Icons.Rounded.Tune, "Equalizer", eq, Modifier.weight(1f))
                    PlayerAction(Icons.Rounded.GraphicEq, "Visualizer", visual, Modifier.weight(1f))
                    PlayerAction(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        "Queue",
                        queue,
                        Modifier.weight(1f),
                    )
                    PlayerAction(Icons.Rounded.Info, "Audio info", info, Modifier.weight(1f))
                }
                if (landscape) sourceBadge()
                state.error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (landscape)
            Row(
                Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                art()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
            }
        else
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                art()
                Box(Modifier.widthIn(max = 480.dp)) { controls() }
            }
    }
}

@Composable
private fun PlayerAction(
    icon: ImageVector,
    label: String,
    click: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = click)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, label, Modifier.size(21.dp), tint = Muted)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@UnstableApi
@Composable
private fun QueueSheet(vm: PlayerViewModel, state: PlaybackState) {
    var save by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Up next · ${state.queue.size}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { save = true }, enabled = state.queue.isNotEmpty()) {
            Text("Save playlist")
        }
    }
    LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = 480.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(state.queue.size) { i ->
            val item = state.queue[i]
            ListItem(
                headlineContent = {
                    Text(
                        item.mediaMetadata.title?.toString().orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        if (i == state.index) "Current track"
                        else item.mediaMetadata.artist?.toString().orEmpty()
                    )
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { vm.move(i, i - 1) }, enabled = i > 0) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "Move up")
                        }
                        IconButton(
                            onClick = { vm.move(i, i + 1) },
                            enabled = i < state.queue.lastIndex,
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "Move down")
                        }
                        IconButton(onClick = { vm.remove(i) }) {
                            Icon(Icons.Rounded.Close, "Remove from queue")
                        }
                    }
                },
                modifier = Modifier.clickable { vm.selectQueue(i) },
            )
        }
    }
    if (save)
        NameDialog("Save queue as playlist", "", { save = false }) {
            vm.createPlaylist(it, true)
            save = false
        }
}

@UnstableApi
@Composable
private fun PlaylistsScreen(
    vm: PlayerViewModel,
    open: () -> Unit,
    createRequest: Int = 0,
    importRequest: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf<PlaylistSummary?>(null) }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    try {
                        val text =
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)!!.use {
                                    readLimited(it, 2097152)
                                }
                            }
                        vm.importPlaylist("Imported playlist", text)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        vm.message.value = "Could not read playlist (maximum 2 MiB)"
                    }
                }
        }
    val exporter =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("audio/x-mpegurl")
        ) { uri ->
            val playlist = exporting
            if (uri != null && playlist != null)
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val text = vm.exportPlaylist(playlist.id)
                            context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use {
                                it.write(text)
                            }
                        }
                        vm.message.value = "Playlist exported"
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        vm.message.value = "Could not export playlist"
                    }
                }
        }
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var create by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<PlaylistSummary?>(null) }
    var delete by remember { mutableStateOf<PlaylistSummary?>(null) }
    val importTypes =
        arrayOf(
            "audio/x-mpegurl",
            "application/x-mpegurl",
            "application/vnd.apple.mpegurl",
            "text/*",
            "application/octet-stream",
        )
    LaunchedEffect(createRequest) { if (createRequest > 0) create = true }
    LaunchedEffect(importRequest) { if (importRequest > 0) importer.launch(importTypes) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(132.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeading(
                "Your playlists",
                if (playlists.isEmpty()) "Make room for your favorites"
                else "${playlists.size} collections",
            ) {
                TextButton({ importer.launch(importTypes) }) {
                    Icon(Icons.Rounded.FileOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
            }
        }
        if (playlists.isEmpty())
            item(span = { GridItemSpan(maxLineSpan) }) {
                MusicEmptyState(
                    "Create your first playlist",
                    "Organize your favorite songs into collections.",
                    "Create Playlist",
                    { create = true },
                    Icons.AutoMirrored.Rounded.QueueMusic,
                )
            }
        items(playlists, key = { it.id }) { p ->
            var menu by remember(p.id) { mutableStateOf(false) }
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                    vm.filter.value = LibraryFilter(playlist = p.id)
                    open()
                }
            ) {
                Box {
                    Artwork(p.artwork, Modifier.fillMaxWidth().aspectRatio(1f), 14)
                    Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        FilledTonalIconButton(
                            { menu = true },
                            colors =
                                IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = .65f),
                                    contentColor = Color.White,
                                ),
                        ) {
                            Icon(Icons.Rounded.MoreHoriz, "Options for ${p.name}")
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export playlist") },
                                onClick = {
                                    menu = false
                                    exporting = p
                                    exporter.launch("${p.name}.m3u8")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menu = false
                                    rename = p
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menu = false
                                    delete = p
                                },
                            )
                        }
                    }
                }
                Text(
                    p.name,
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${p.count} tracks · ${time(p.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeading("Smart collections", "Always up to date")
        }
        items(
            listOf(
                Triple("Recently Played", Category.RECENT, Icons.Rounded.History),
                Triple("Most Played", Category.MOST, Icons.Rounded.BarChart),
                Triple("Favorites", Category.FAVORITES, Icons.Rounded.FavoriteBorder),
                Triple("Recently Added", Category.ADDED, Icons.Rounded.LibraryAdd),
            )
        ) { (name, category, icon) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                onClick = {
                    vm.filter.value = LibraryFilter(category = category)
                    open()
                },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SmartPlaylistsSection(vm, open) }
    }

    if (create)
        NameDialog("New playlist", "", { create = false }) {
            vm.createPlaylist(it)
            create = false
        }
    rename?.let { p ->
        NameDialog("Rename playlist", p.name, { rename = null }) {
            vm.renamePlaylist(p.id, it)
            rename = null
        }
    }
    delete?.let { p ->
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text("Delete ${p.name}?") },
            text = { Text("Your music files will stay on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deletePlaylist(p.id)
                        delete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancel") } },
        )
    }
}

@UnstableApi
@Composable
private fun SettingsScreen(
    vm: PlayerViewModel,
    addFolder: () -> Unit,
    notifications: () -> Unit,
    profiles: () -> Unit,
    output: () -> Unit,
    section: String?,
    choose: (String?) -> Unit,
    openEq: () -> Unit,
    openVisual: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val categories =
        listOf(
            Triple("Playback", "Speed and notifications", Icons.Rounded.PlayCircle),
            Triple("Audio Output", "Route, source format and profiles", Icons.Rounded.Headphones),
            Triple("Equalizer", "Presets, ReplayGain and AutoEq", Icons.Rounded.Tune),
            Triple("Visualizer", "Analysis, color and motion", Icons.Rounded.GraphicEq),
            Triple("Library", "Folders and music access", Icons.Rounded.Folder),
            Triple("Appearance", "Theme, artwork and song rows", Icons.Rounded.Palette),
            Triple("Widgets", "Home-screen player controls", Icons.Rounded.Widgets),
            Triple("Privacy & Data", "Local storage and playback history", Icons.Rounded.Security),
            Triple("About", "App version and open-source licenses", Icons.Rounded.Info),
        )
    key(section) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (section == null) {
                items(categories) { (title, description, icon) ->
                    SettingRow(title, description, icon, { choose(title) })
                }
            } else
                when (section) {
                    "Playback" -> {
                        item {
                            SettingRow(
                                "Playback speed",
                                "${"%.2f".format(settings.speed)}×",
                                Icons.Rounded.Speed,
                            )
                        }
                        item {
                            var speed by
                                remember(settings.speed) { mutableFloatStateOf(settings.speed) }
                            Slider(
                                speed,
                                { speed = it },
                                Modifier.padding(horizontal = 24.dp),
                                onValueChangeFinished = { vm.speed(speed) },
                                valueRange = .5f..2f,
                                steps = 5,
                            )
                        }
                        if (Build.VERSION.SDK_INT >= 33)
                            item {
                                SettingRow(
                                    "Playback notifications",
                                    "Control music from Android notifications",
                                    Icons.Rounded.Notifications,
                                    notifications,
                                )
                            }
                        else
                            item {
                                SettingRow(
                                    "Playback notifications",
                                    "Managed by Android",
                                    Icons.Rounded.Notifications,
                                )
                            }
                        item {
                            SettingRow(
                                "Track transitions",
                                "FLAC frame continuity verified. Sample-exact MP3 gapless remains incomplete on the tested legacy decoder; crossfade is unavailable.",
                                Icons.Rounded.SkipNext,
                            )
                        }
                    }
                    "Audio Output" -> {
                        item {
                            SettingRow(
                                "Audio output information",
                                "Actual route, source and processing format",
                                Icons.Rounded.Headphones,
                                output,
                            )
                        }
                        item {
                            SettingRow(
                                "Headphone & output EQ profiles",
                                "Local corrections and route-category assignments",
                                Icons.Rounded.Tune,
                                profiles,
                            )
                        }
                    }
                    "Equalizer" -> {
                        item {
                            SettingRow(
                                "Open equalizer",
                                "10-band, parametric and 31-band DSP",
                                Icons.Rounded.Tune,
                                openEq,
                            )
                        }
                        item {
                            SettingRow(
                                "AutoEq & output profiles",
                                "Import a local correction file",
                                Icons.Rounded.Headphones,
                                profiles,
                            )
                        }
                        item {
                            SettingRow(
                                "ReplayGain",
                                "Track/album gain and peak protection are in the equalizer controls",
                                Icons.Rounded.VolumeUp,
                                openEq,
                            )
                        }
                    }
                    "Visualizer" -> {
                        item {
                            SettingRow(
                                "Open visualizer",
                                "Six real PCM/FFT modes with settings",
                                Icons.Rounded.GraphicEq,
                                openVisual,
                            )
                        }
                        item { MotionSettings(vm) }
                    }
                    "Library" -> {
                        item {
                            SettingRow(
                                "Add music folder",
                                "Keep access to a selected local folder",
                                Icons.Rounded.CreateNewFolder,
                                addFolder,
                            )
                        }
                        item {
                            SettingRow(
                                "Scan library",
                                "Refresh local music and availability",
                                Icons.Rounded.Refresh,
                                vm::refresh,
                            )
                        }
                        items(folders.toList(), key = { it }) { folder ->
                            SettingRow(
                                android.net.Uri.decode(folder.substringAfterLast('/')),
                                "Selected folder",
                                Icons.Rounded.Folder,
                                trailing = {
                                    IconButton({ vm.removeFolder(folder) }) {
                                        Icon(Icons.Rounded.Close, "Remove folder access")
                                    }
                                },
                            )
                        }
                    }
                    "Appearance" -> {
                        item {
                            SettingRow(
                                "AMOLED black",
                                "Use a black background instead of dark gray",
                                Icons.Rounded.DarkMode,
                                trailing = {
                                    Switch(settings.amoled, { vm.appearance(amoled = it) })
                                },
                            )
                        }
                        item {
                            SettingRow(
                                "Accent color",
                                "Selected controls and progress",
                                Icons.Rounded.Palette,
                            )
                        }
                        item {
                            Row(
                                Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("Blue", "Mint", "Violet").forEachIndexed { i, label ->
                                    CompactTab(label, settings.accent == i, { vm.accent(i) })
                                }
                            }
                        }
                        item {
                            SettingRow(
                                "Artwork accent",
                                "Sample local album artwork; use selected accent when unavailable",
                                Icons.Rounded.ColorLens,
                                trailing = {
                                    Switch(
                                        settings.artworkColor,
                                        { vm.appearance(artworkColor = it) },
                                    )
                                },
                            )
                        }
                        item {
                            SettingRow(
                                "Artwork background",
                                "Subtle gradient behind Now Playing; turn off for a plain background",
                                Icons.Rounded.Gradient,
                                trailing = {
                                    Switch(
                                        settings.artworkGradient,
                                        { vm.appearance(gradient = it) },
                                    )
                                },
                            )
                        }
                        item {
                            SettingRow(
                                "Artwork corners",
                                "${settings.artworkCorners} dp on Now Playing",
                                Icons.Rounded.CropSquare,
                            )
                        }
                        item {
                            Slider(
                                settings.artworkCorners.toFloat(),
                                { vm.appearance(corners = it.toInt()) },
                                Modifier.padding(horizontal = 24.dp),
                                valueRange = 0f..32f,
                                steps = 7,
                            )
                        }
                        item {
                            SettingRow(
                                "Compact song rows",
                                "Turn off to show extra file details",
                                Icons.Rounded.ViewList,
                                trailing = {
                                    Switch(!settings.detailedList, { vm.detailedList(!it) })
                                },
                            )
                        }
                        item {
                            SettingRow(
                                "Detailed audio badge",
                                "Show reported source format on Now Playing",
                                Icons.Rounded.HighQuality,
                                trailing = { Switch(settings.technicalInfo, vm::technical) },
                            )
                        }
                        item { MotionSettings(vm) }
                    }
                    "Widgets" ->
                        item { Box(Modifier.padding(horizontal = 16.dp)) { WidgetSettings(vm) } }
                    "Privacy & Data" -> {
                        item {
                            SettingRow(
                                "Completely offline",
                                "No accounts, ads, analytics, microphone or network permission. Music files stay on your device.",
                                Icons.Rounded.Security,
                            )
                        }
                        item {
                            SettingRow(
                                "Local data",
                                "Library, playlists, favorites, lyrics and settings are stored locally. Android backup/transfer is excluded.",
                                Icons.Rounded.Storage,
                            )
                        }
                        item {
                            SettingRow(
                                "Clear playback history",
                                "Keeps your music files, playlists and favorites",
                                Icons.Rounded.History,
                                vm::clearHistory,
                            )
                        }
                    }
                    "About" -> {
                        item {
                            Column(
                                Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AboutSettings()
                            }
                        }
                        item { Box(Modifier.padding(horizontal = 12.dp)) { LicenseSettings() } }
                    }
                }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    dismiss: () -> Unit,
    save: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { name = it.take(100) },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { save(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun AudioInfo(vm: PlayerViewModel, state: PlaybackState) {
    val dsp by vm.dspSettings.collectAsStateWithLifecycle()
    val format by vm.audioEngine.format.collectAsStateWithLifecycle()
    val source by vm.sourceInfo.collectAsStateWithLifecycle()
    val route by vm.outputRoute.collectAsStateWithLifecycle()
    val track by
        produceState<Track?>(null, state.item?.mediaId) {
            value = state.item?.mediaId?.let { vm.trackInfo(it) }
        }
    Column(
        Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(state.item?.mediaMetadata?.title?.toString().orEmpty(), fontWeight = FontWeight.Bold)
        Text(
            "${state.item?.mediaMetadata?.artist?.toString().orEmpty()} · ${state.item?.mediaMetadata?.albumTitle?.toString().orEmpty()}"
        )
        track?.let {
            Text("${it.genre} · track ${it.trackNumber} / disc ${it.discNumber}\n${it.size} bytes")
        }
        Text("Duration: ${time(state.duration)}")
        Text(
            "Source: ${source.codec} · ${source.container}\n${source.bits?.let{"$it-bit"}?:"Bit depth not reported"} · ${source.rate?.let{"$it Hz"}?:"Rate not reported"} · ${source.channels?:"?"} channels\n${source.bitrate?.let{"$it bit/s"}?:"Bitrate not reported"}"
        )
        Text(
            "EQ: ${if(dsp.enabled)dsp.preset else "bypassed"}\nReplayGain: ${dsp.replayMode}\nProcessing: 16-bit PCM · ${format.rate} Hz"
        )
        Text(
            "Actual route: ${route.device?.let{"${it.category.label} · ${it.name}"}?:"Not reported"}\nAudioTrack client: ${route.clientRate?.let{"$it Hz"}?:"Not reported"} · ${route.clientChannels?:"?"} channels"
        )
        Text(
            "Android may resample after the app's AudioTrack. Hardware sample format, exclusive mode and bit-perfect output are not verified.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PendingScreen(icon: ImageVector, title: String, text: String) {
    EmptyState(title, text) {
        Icon(icon, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(title: String, description: String, action: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(description, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        action()
    }
}

fun time(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return if (seconds >= 3600)
        "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}
