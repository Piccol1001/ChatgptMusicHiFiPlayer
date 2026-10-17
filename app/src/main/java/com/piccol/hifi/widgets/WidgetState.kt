package com.piccol.hifi.widgets

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import com.piccol.hifi.data.QueueSnapshot
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject

data class WidgetSnapshot(
    val uri: String = "",
    val title: String = "Choose your music",
    val artist: String = "Open HiFi Player",
    val album: String = "",
    val artwork: String? = null,
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = 0,
    val speed: Float = 1f,
    val elapsed: Long = 0,
) {
    fun currentPosition(now: Long = SystemClock.elapsedRealtime()): Long =
        (position + if (playing) (now - elapsed).coerceAtLeast(0).times(speed).toLong() else 0)
            .coerceIn(0, duration.coerceAtLeast(position))
}

data class WidgetAppearance(val transparent: Boolean = false, val accent: Int = 0)

private val Context.widgetStore by preferencesDataStore("widgets")

@OptIn(FlowPreview::class)
class WidgetRepository(private val context: Context) {
    private val store = context.widgetStore
    private val data =
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val snapshots =
        data.map { decodeWidget(it[stringPreferencesKey("snapshot")]) }.distinctUntilChanged()
    val appearance =
        data
            .map {
                WidgetAppearance(
                    it[booleanPreferencesKey("transparent")] ?: false,
                    (it[intPreferencesKey("accent")] ?: 0).coerceIn(0, 2),
                )
            }
            .distinctUntilChanged()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = Channel<WidgetSnapshot>(Channel.CONFLATED)

    init {
        scope.launch {
            updates.receiveAsFlow().debounce(200).collect { snapshot ->
                try {
                    save(snapshot)
                    refreshWidgets(context)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    /* A failed widget refresh must not stop playback. */
                }
            }
        }
    }

    fun submit(snapshot: WidgetSnapshot) {
        updates.trySend(snapshot)
    }

    suspend fun save(snapshot: WidgetSnapshot) {
        store.edit { it[stringPreferencesKey("snapshot")] = encodeWidget(snapshot) }
    }

    suspend fun appearance(transform: (WidgetAppearance) -> WidgetAppearance) {
        store.edit {
            val s =
                transform(
                    WidgetAppearance(
                        it[booleanPreferencesKey("transparent")] ?: false,
                        it[intPreferencesKey("accent")] ?: 0,
                    )
                )
            it[booleanPreferencesKey("transparent")] = s.transparent
            it[intPreferencesKey("accent")] = s.accent.coerceIn(0, 2)
        }
        refreshWidgets(context)
    }

    suspend fun seed(queue: QueueSnapshot, force: Boolean = false) {
        val old = snapshots.first()
        if (force || old.uri.isBlank()) {
            if (queue.items.isEmpty()) {
                if (force) save(WidgetSnapshot())
                return
            }
            val item = queue.items[queue.index]
            save(
                WidgetSnapshot(
                    item.uri,
                    item.title,
                    item.artist,
                    item.album,
                    item.artwork,
                    position = queue.position,
                    duration = if (item.uri == old.uri) old.duration else 0,
                    shuffle = queue.shuffle,
                    repeat = queue.repeat,
                )
            )
        }
    }
}

fun Player.widgetSnapshot(): WidgetSnapshot {
    val metadata = currentMediaItem?.mediaMetadata
    return WidgetSnapshot(
        currentMediaItem?.mediaId.orEmpty(),
        metadata?.title?.toString().orEmpty().ifBlank { "Choose your music" },
        metadata?.artist?.toString().orEmpty(),
        metadata?.albumTitle?.toString().orEmpty(),
        metadata?.artworkUri?.toString(),
        isPlaying,
        currentPosition.coerceAtLeast(0),
        duration.takeIf { it > 0 } ?: 0,
        shuffleModeEnabled,
        repeatMode,
        playbackParameters.speed,
        SystemClock.elapsedRealtime(),
    )
}

fun encodeWidget(s: WidgetSnapshot) =
    JSONObject()
        .apply {
            put("uri", s.uri)
            put("title", s.title)
            put("artist", s.artist)
            put("album", s.album)
            put("art", s.artwork)
            put("playing", s.playing)
            put("position", s.position)
            put("duration", s.duration)
            put("shuffle", s.shuffle)
            put("repeat", s.repeat)
            put("speed", s.speed)
            put("elapsed", s.elapsed)
        }
        .toString()

fun decodeWidget(text: String?): WidgetSnapshot =
    try {
        val o = JSONObject(text ?: "{}")
        WidgetSnapshot(
            o.optString("uri"),
            o.optString("title", "Choose your music"),
            o.optString("artist", "Open HiFi Player"),
            o.optString("album"),
            o.optString("art").takeIf { it.isNotBlank() && it != "null" },
            o.optBoolean("playing"),
            o.optLong("position").coerceAtLeast(0),
            o.optLong("duration").coerceAtLeast(0),
            o.optBoolean("shuffle"),
            o.optInt("repeat").coerceIn(0, 2),
            o.optDouble("speed", 1.0).toFloat().takeIf { it.isFinite() && it in .5f..2f } ?: 1f,
            o.optLong("elapsed").coerceAtLeast(0),
        )
    } catch (_: Exception) {
        WidgetSnapshot()
    }
