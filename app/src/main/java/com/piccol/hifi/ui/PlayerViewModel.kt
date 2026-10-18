package com.piccol.hifi.ui

import android.app.Application
import android.content.ComponentName
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.paging.*
import com.piccol.hifi.HiFiApplication
import com.piccol.hifi.audio.*
import com.piccol.hifi.data.*
import com.piccol.hifi.dsp.*
import com.piccol.hifi.playback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PlaybackState(
    val connected: Boolean = false,
    val item: MediaItem? = null,
    val queue: List<MediaItem> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HiFiApplication
    private val dao = app.database.music()
    val preferences = app.preferences
    val scanner = app.scanner
    val dspRepository = app.dsp
    val audioEngine = app.audioEngine
    val pcmTap = app.pcmTap
    val replayTags = app.replayTags
    val profileState =
        app.profiles.state.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileState())
    val outputRoute = app.output.route
    val outputs = app.output.available
    val playingSignal = app.playbackIsPlaying
    val widgetAppearance =
        app.widgets.appearance.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.piccol.hifi.widgets.WidgetAppearance(),
        )
    val sourceInfo = app.sourceInfo
    val embeddedLyrics = app.embeddedLyrics
    val lyrics = app.lyrics
    val lyricsVisible = MutableStateFlow(false)
    val smartPlaylists =
        app.smartPlaylists.playlists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dspSettings =
        app.effectiveDsp.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DspSettings(),
        )
    val customPresets =
        app.dsp.custom.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val presetFavorites =
        app.dsp.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val visualSettings =
        app.visualSettings.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.piccol.hifi.visualization.VisualSettings(),
        )
    val visualization = app.visualization
    val recentPresets =
        app.dsp.recent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val filter = MutableStateFlow(LibraryFilter())
    val message = MutableStateFlow<String?>(null)
    val favorites =
        dao.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists =
        dao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings =
        preferences.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Settings(),
        )
    val folders =
        preferences.folders.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptySet(),
        )
    val tracks =
        filter
            .debounce(180)
            .flatMapLatest { value ->
                Pager(PagingConfig(pageSize = 60, enablePlaceholders = false)) {
                        dao.page(libraryQuery(value))
                    }
                    .flow
            }
            .cachedIn(viewModelScope)
    val groups =
        filter
            .map { it.category }
            .distinctUntilChanged()
            .flatMapLatest {
                when (it) {
                    Category.ALBUMS -> dao.albums()
                    Category.ARTISTS -> dao.artists()
                    Category.GENRES -> dao.genres()
                    Category.FOLDERS -> dao.folders()
                    else -> flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val controller = MutableStateFlow<MediaController?>(null)
    private val future =
        MediaController.Builder(
                application,
                SessionToken(application, ComponentName(application, PlaybackService::class.java)),
            )
            .buildAsync()
    val playback =
        controller
            .flatMapLatest { player ->
                if (player == null) flowOf(PlaybackState())
                else
                    callbackFlow {
                        var cachedQueue = emptyList<MediaItem>()
                        fun update(refreshQueue: Boolean = false) {
                            if (refreshQueue)
                                cachedQueue =
                                    (0 until player.mediaItemCount).map(player::getMediaItemAt)
                            trySend(
                                PlaybackState(
                                    true,
                                    player.currentMediaItem,
                                    cachedQueue,
                                    player.currentMediaItemIndex,
                                    player.isPlaying,
                                    player.playbackState == Player.STATE_BUFFERING,
                                    player.currentPosition.coerceAtLeast(0),
                                    player.duration.takeIf { it > 0 } ?: 0,
                                    player.shuffleModeEnabled,
                                    player.repeatMode,
                                    app.playbackProblem.value
                                        ?: player.playerError?.let {
                                            "This file could not be played (${it.errorCodeName}). Check storage access or try another file."
                                        },
                                )
                            )
                        }
                        val listener =
                            object : Player.Listener {
                                override fun onEvents(player: Player, events: Player.Events) {
                                    update(
                                        events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                                            events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)
                                    )
                                }
                            }
                        player.addListener(listener)
                        update(true)
                        val ticker = launch {
                            while (isActive) {
                                delay(if (lyricsVisible.value) 100 else 500)
                                if (player.isPlaying) update()
                            }
                        }
                        awaitClose {
                            ticker.cancel()
                            player.removeListener(listener)
                        }
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), PlaybackState())

    private var observingMedia = false

    private fun observeMedia() {
        if (scanner.hasMediaPermission() && !observingMedia)
            runCatching {
                    app.contentResolver.registerContentObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        true,
                        observer,
                    )
                }
                .onSuccess { observingMedia = true }
        else if (!scanner.hasMediaPermission() && observingMedia) {
            app.contentResolver.unregisterContentObserver(observer)
            observingMedia = false
        }
    }

    private var scanJob: Job? = null
    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scanJob?.cancel()
                scanJob =
                    viewModelScope.launch {
                        delay(1000)
                        refreshNow()
                    }
            }
        }

    init {
        future.addListener(
            {
                try {
                    controller.value = future.get()
                } catch (_: Exception) {
                    message.value = "Playback service could not connect. Reopen the app to retry."
                }
            },
            ContextCompat.getMainExecutor(application),
        )
        observeMedia()
        viewModelScope.launch {
            app.smartPlaylists.playlists.collect { list ->
                filter.value.smartId?.let { id ->
                    val selected = list.find { it.id == id }
                    filter.value =
                        filter.value.copy(smart = selected?.rules, smartId = selected?.id)
                }
            }
        }
        refresh()
    }

    private fun task(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                message.value = e.message?.take(160) ?: "Invalid values"
            } catch (_: Exception) {
                message.value =
                    "The operation could not finish. Check storage availability and try again."
            }
        }
    }

    private suspend fun refreshNow() {
        try {
            scanner.scan()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            message.value = "Library refresh failed. Reconnect storage and try again."
        }
    }

    fun refresh() {
        observeMedia()
        scanJob?.cancel()
        scanJob = viewModelScope.launch { refreshNow() }
    }

    fun addFolder(uri: Uri) = task {
        preferences.addFolder(uri.toString())
        scanner.scan()
    }

    fun removeFolder(uri: String) = task {
        preferences.removeFolder(uri)
        dao.markUnavailable(uri)
        runCatching {
            app.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    fun play(track: Track) {
        controller.value?.let {
            it.setMediaItems(listOf(track.mediaItem()))
            it.prepare()
            it.play()
        } ?: run { message.value = "Connecting to player…" }
    }

    fun playAll() = task {
        val list = dao.selected(libraryQuery(filter.value, limit = 1000))
        if (list.isEmpty()) {
            message.value = "No accessible music found."
            return@task
        }
        controller.value?.let {
            it.setMediaItems(list.map(Track::mediaItem))
            it.prepare()
            it.play()
        }
        if (list.size == 1000)
            message.value = "Queued the first 1,000 songs. Add more from the library as needed."
    }

    fun enqueue(track: Track, next: Boolean) {
        controller.value?.let {
            if (next && it.mediaItemCount > 0)
                it.addMediaItem(it.currentMediaItemIndex + 1, track.mediaItem())
            else it.addMediaItem(track.mediaItem())
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            message.value = if (next) "Playing next: ${track.title}" else "Added to queue"
        }
    }

    fun toggle() {
        controller.value?.let {
            if (it.isPlaying) it.pause()
            else {
                if (it.playbackState == Player.STATE_IDLE) it.prepare()
                it.play()
            }
        }
    }

    fun next() {
        controller.value?.seekToNextMediaItem()
    }

    fun previous() {
        controller.value?.let {
            if (it.currentPosition > 3_000) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun seek(position: Long) {
        controller.value?.seekTo(position.coerceAtLeast(0))
    }

    fun shuffle() {
        controller.value?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun repeat() {
        controller.value?.let {
            it.repeatMode =
                when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
        }
    }

    fun move(from: Int, to: Int) {
        controller.value?.let {
            if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount)
                it.moveMediaItem(from, to)
        }
    }

    fun remove(index: Int) {
        controller.value?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) }
    }

    fun selectQueue(index: Int) {
        controller.value?.let {
            if (index in 0 until it.mediaItemCount) {
                it.seekToDefaultPosition(index)
                it.play()
            }
        }
    }

    fun favorite(uri: String) = task {
        if (uri in favorites.value) dao.removeFavorite(uri) else dao.addFavorite(Favorite(uri))
    }

    fun createPlaylist(name: String, saveQueue: Boolean = false) = task {
        val clean = name.trim().take(100)
        if (clean.isEmpty()) return@task
        val id = dao.create(Playlist(name = clean))
        if (saveQueue) playback.value.queue.forEach { item -> dao.addToPlaylist(id, item.mediaId) }
    }

    fun renamePlaylist(id: Long, name: String) = task {
        if (name.isNotBlank()) dao.rename(id, name.trim().take(100))
    }

    fun deletePlaylist(id: Long) = task { dao.deletePlaylist(id) }

    fun addToPlaylist(id: Long, uri: String) = task {
        dao.addToPlaylist(id, uri)
        message.value = "Added to playlist"
    }

    fun removeFromPlaylist(id: Long, uri: String) = task { dao.removeEntry(id, uri) }

    fun speed(value: Float) = task { preferences.speed(value) }

    fun detailedList(value: Boolean) = task { preferences.detailedList(value) }

    fun widgetAppearance(
        transform:
            (com.piccol.hifi.widgets.WidgetAppearance) -> com.piccol.hifi.widgets.WidgetAppearance
    ) = task { app.widgets.appearance(transform) }

    fun technical(value: Boolean) = task { preferences.technical(value) }

    fun appearance(
        amoled: Boolean? = null,
        artworkColor: Boolean? = null,
        gradient: Boolean? = null,
        corners: Int? = null,
    ) = task { preferences.appearance(amoled, artworkColor, gradient, corners) }

    val libraryGroups =
        filter
            .map { it.category }
            .distinctUntilChanged()
            .flatMapLatest {
                if (
                    it in
                        listOf(Category.ALBUMS, Category.ARTISTS, Category.GENRES, Category.FOLDERS)
                )
                    dao.groupCards(it.name)
                else flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun accent(value: Int) = task { preferences.accent(value) }

    fun clearHistory() = task { dao.clearHistory() }

    fun changeVisual(
        transform:
            (
                com.piccol.hifi.visualization.VisualSettings
            ) -> com.piccol.hifi.visualization.VisualSettings
    ) = task { app.visualSettings.update(transform) }

    private val dspMutex = Mutex()

    suspend fun trackInfo(uri: String) = dao.track(uri)

    fun changeDsp(transform: (DspSettings) -> DspSettings) = task {
        dspMutex.withLock {
            val base = app.dsp.settings.first()
            val active = app.profiles.state.first().active(app.output.route.value)
            val next =
                transform(effectiveDsp(base, app.profiles.state.first(), app.output.route.value))
            require(next.valid())
            if (active == null) app.dsp.update { next }
            else {
                app.profiles.edit(active.id) { next }
                app.dsp.update {
                    it.copy(
                        replayMode = next.replayMode,
                        replayPreamp = next.replayPreamp,
                        preventClipping = next.preventClipping,
                    )
                }
            }
        }
    }

    fun useBaseEq() = task {
        app.output.route.value.device?.category?.let { app.profiles.assign(it, null) }
        app.profiles.select(null)
    }

    fun saveProfile(name: String, settings: DspSettings? = null, notes: String = "") = task {
        app.profiles.save(name, settings ?: app.effectiveDsp.first(), notes)
    }

    fun renameProfile(id: String, name: String) = task { app.profiles.rename(id, name) }

    fun deleteProfile(id: String) = task { app.profiles.delete(id) }

    fun selectProfile(id: String?) = task { app.profiles.select(id) }

    fun assignProfile(category: OutputCategory, id: String?) = task {
        app.profiles.assign(category, id)
    }

    fun importLyrics(song: String, uri: Uri) = task { app.lyrics.import(song, uri) }

    fun removeLyrics(song: String) = task { app.lyrics.remove(song) }

    fun lyricOffset(song: String, value: Long) = task { app.lyrics.offset(song, value) }

    fun lyricOptions(transform: (LyricOptions) -> LyricOptions) = task {
        app.lyrics.options(transform)
    }

    fun saveSmart(id: String?, name: String, rules: SmartRules) = task {
        app.smartPlaylists.save(id, name, rules)
    }

    fun deleteSmart(id: String) = task { app.smartPlaylists.delete(id) }

    suspend fun exportPlaylist(id: Long): String =
        exportM3u(dao.selected(libraryQuery(LibraryFilter(playlist = id))))

    fun importPlaylist(name: String, text: String) = task {
        val parsed = parseM3u(text)
        val tracks = parsed.uris.mapNotNull { dao.track(it)?.takeIf { t -> t.available } }
        require(tracks.isNotEmpty()) { "No indexed songs in this playlist" }
        dao.importPlaylist(
            name.trim().ifBlank { "Imported playlist" }.take(100),
            tracks.map { it.uri },
        )
        message.value =
            "Imported ${tracks.size} songs; ${parsed.unresolved+parsed.uris.size-tracks.size} unresolved entries"
    }

    fun applyPreset(preset: EqPreset) = task {
        val active = app.profiles.state.first().active(app.output.route.value)
        if (active == null) app.dsp.apply(preset)
        else {
            app.profiles.edit(active.id) {
                preset.settings.copy(enabled = true, preset = active.name)
            }
            app.dsp.rememberPreset(preset.id)
        }
    }

    fun savePreset(name: String, settings: DspSettings? = null) = task {
        app.dsp.save(name, settings ?: app.effectiveDsp.first())
    }

    fun renamePreset(id: String, name: String) = task { app.dsp.rename(id, name) }

    fun deletePreset(id: String) = task { app.dsp.delete(id) }

    fun favoritePreset(id: String) = task { app.dsp.favorite(id) }

    fun importPresets(text: String) = task {
        app.dsp.importPresets(text)
        message.value = "Presets imported"
    }

    override fun onCleared() {
        if (observingMedia) app.contentResolver.unregisterContentObserver(observer)
        MediaController.releaseFuture(future)
        super.onCleared()
    }
}
