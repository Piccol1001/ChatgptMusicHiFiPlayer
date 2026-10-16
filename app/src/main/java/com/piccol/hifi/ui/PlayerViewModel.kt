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
import com.piccol.hifi.data.*
import com.piccol.hifi.playback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

data class PlaybackState(
    val connected: Boolean = false, val item: MediaItem? = null, val queue: List<MediaItem> = emptyList(),
    val index: Int = 0, val playing: Boolean = false, val buffering: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF, val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HiFiApplication
    private val dao = app.database.music()
    val preferences = app.preferences
    val scanner = app.scanner
    val filter = MutableStateFlow(LibraryFilter())
    val message = MutableStateFlow<String?>(null)
    val favorites = dao.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists = dao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())
    val folders = preferences.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val tracks = filter.debounce(180).flatMapLatest { value -> Pager(PagingConfig(pageSize = 60, enablePlaceholders = false)) { dao.page(libraryQuery(value)) }.flow }.cachedIn(viewModelScope)
    val groups = filter.map { it.category }.distinctUntilChanged().flatMapLatest {
        when (it) { Category.ALBUMS -> dao.albums(); Category.ARTISTS -> dao.artists(); Category.GENRES -> dao.genres(); Category.FOLDERS -> dao.folders(); else -> flowOf(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val controller = MutableStateFlow<MediaController?>(null)
    private val future = MediaController.Builder(application, SessionToken(application, ComponentName(application, PlaybackService::class.java))).buildAsync()
    val playback = controller.flatMapLatest { player ->
        if (player == null) flowOf(PlaybackState()) else callbackFlow {
            fun update() {
                trySend(PlaybackState(true, player.currentMediaItem, (0 until player.mediaItemCount).map(player::getMediaItemAt),
                    player.currentMediaItemIndex, player.isPlaying, player.playbackState == Player.STATE_BUFFERING,
                    player.currentPosition.coerceAtLeast(0), player.duration.takeIf { it > 0 } ?: 0,
                    player.shuffleModeEnabled, player.repeatMode, player.playerError?.let { "This file could not be played (${it.errorCodeName}). Check storage access or try another file." }))
            }
            val listener = object : Player.Listener { override fun onEvents(player: Player, events: Player.Events) { update() } }
            player.addListener(listener)
            update()
            val ticker = launch { while (isActive) { delay(500); update() } }
            awaitClose { ticker.cancel(); player.removeListener(listener) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    private var scanJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { scanJob?.cancel(); scanJob = viewModelScope.launch { delay(1000); refreshNow() } }
    }
    init {
        future.addListener({
            try { controller.value = future.get() }
            catch (_: Exception) { message.value = "Playback service could not connect. Reopen the app to retry." }
        }, ContextCompat.getMainExecutor(application))
        application.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        refresh()
    }

    private fun task(block: suspend () -> Unit) { viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { message.value = "The operation could not finish. Check storage availability and try again." }
    } }
    private suspend fun refreshNow() { try { scanner.scan() } catch (e: CancellationException) { throw e } catch (_: Exception) { message.value = "Library refresh failed. Reconnect storage and try again." } }
    fun refresh() { scanJob?.cancel(); scanJob = viewModelScope.launch { refreshNow() } }
    fun addFolder(uri: Uri) = task { preferences.addFolder(uri.toString()); scanner.scan() }
    fun removeFolder(uri: String) = task {
        preferences.removeFolder(uri); dao.markUnavailable(uri)
        runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(uri), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    fun play(track: Track) { controller.value?.let { it.setMediaItems(listOf(track.mediaItem())); it.prepare(); it.play() } ?: run { message.value = "Connecting to player…" } }
    fun playAll() = task {
        val list = dao.selected(libraryQuery(filter.value, limit = 1000))
        if (list.isEmpty()) { message.value = "No accessible music found."; return@task }
        controller.value?.let { it.setMediaItems(list.map(Track::mediaItem)); it.prepare(); it.play() }
        if (list.size == 1000) message.value = "Queued the first 1,000 songs. Add more from the library as needed."
    }
    fun enqueue(track: Track, next: Boolean) {
        controller.value?.let {
            if (next && it.mediaItemCount > 0) it.addMediaItem(it.currentMediaItemIndex + 1, track.mediaItem()) else it.addMediaItem(track.mediaItem())
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            message.value = if (next) "Playing next: ${track.title}" else "Added to queue"
        }
    }
    fun toggle() { controller.value?.let { if (it.isPlaying) it.pause() else { if (it.playbackState == Player.STATE_IDLE) it.prepare(); it.play() } } }
    fun next() { controller.value?.seekToNextMediaItem() }
    fun previous() { controller.value?.let { if (it.currentPosition > 3_000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seek(position: Long) { controller.value?.seekTo(position.coerceAtLeast(0)) }
    fun shuffle() { controller.value?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun repeat() { controller.value?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }
    fun move(from: Int, to: Int) { controller.value?.let { if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) } }
    fun remove(index: Int) { controller.value?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) } }
    fun selectQueue(index: Int) { controller.value?.let { if (index in 0 until it.mediaItemCount) { it.seekToDefaultPosition(index); it.play() } } }
    fun favorite(uri: String) = task { if (uri in favorites.value) dao.removeFavorite(uri) else dao.addFavorite(Favorite(uri)) }
    fun createPlaylist(name: String, saveQueue: Boolean = false) = task {
        val clean = name.trim().take(100)
        if (clean.isEmpty()) return@task
        val id = dao.create(Playlist(name = clean))
        if (saveQueue) playback.value.queue.forEach { item -> dao.addToPlaylist(id, item.mediaId) }
    }
    fun renamePlaylist(id: Long, name: String) = task { if (name.isNotBlank()) dao.rename(id, name.trim().take(100)) }
    fun deletePlaylist(id: Long) = task { dao.deletePlaylist(id) }
    fun addToPlaylist(id: Long, uri: String) = task { dao.addToPlaylist(id, uri); message.value = "Added to playlist" }
    fun removeFromPlaylist(id: Long, uri: String) = task { dao.removeEntry(id, uri) }
    fun speed(value: Float) = task { preferences.speed(value) }
    fun technical(value: Boolean) = task { preferences.technical(value) }
    fun accent(value: Int) = task { preferences.accent(value) }
    fun clearHistory() = task { dao.clearHistory() }
    override fun onCleared() {
        app.contentResolver.unregisterContentObserver(observer)
        MediaController.releaseFuture(future)
        super.onCleared()
    }
}
