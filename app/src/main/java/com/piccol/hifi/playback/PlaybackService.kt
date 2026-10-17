package com.piccol.hifi.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.*
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.piccol.hifi.HiFiApplication
import com.piccol.hifi.MainActivity
import com.piccol.hifi.audio.*
import com.piccol.hifi.data.QueueSnapshot
import com.piccol.hifi.dsp.*
import com.piccol.hifi.widgets.widgetSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app
        get() = application as HiFiApplication

    private var restoring = true
    private var lastRecorded: String? = null
    private var saveJob: Job? = null
    private val gainTags = mutableMapOf<String, String>()
    private var decoderGainActive = false
    private var sourceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        app.serviceRunning.value = true
        val renderers =
            object : DefaultRenderersFactory(this) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink =
                    PcmOnlyAudioSink(
                        DefaultAudioSink.Builder(context)
                            .setEnableFloatOutput(false)
                            .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                            .setAudioTrackProvider { config, attributes, sessionId, context ->
                                DefaultAudioSink.AudioTrackProvider.DEFAULT.getAudioTrack(
                                        config,
                                        attributes,
                                        sessionId,
                                        context,
                                    )
                                    .also(app.output::attach)
                            }
                            .setAudioProcessors(
                                arrayOf(EqualizerProcessor(app.audioEngine, app.pcmTap))
                            )
                            .build()
                    )
            }
        val extractors = LocalExtractors()
        player =
            ExoPlayer.Builder(this, renderers)
                .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractors))
                .build()
                .apply {
                    trackSelectionParameters =
                        trackSelectionParameters
                            .buildUpon()
                            .setAudioOffloadPreferences(
                                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                                    .setAudioOffloadMode(
                                        TrackSelectionParameters.AudioOffloadPreferences
                                            .AUDIO_OFFLOAD_MODE_DISABLED
                                    )
                                    .build()
                            )
                            .build()
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true,
                    )
                    setHandleAudioBecomingNoisy(true)
                    setWakeMode(C.WAKE_MODE_LOCAL)
                }
        val launch =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).putExtra("open_player", true),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        session = MediaSession.Builder(this, player).setSessionActivity(launch).build()
        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    app.playbackIsPlaying.value = player.isPlaying
                    app.widgets.submit(player.widgetSnapshot())
                    if (
                        events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                            events.contains(Player.EVENT_TRACKS_CHANGED)
                    ) {
                        gainTags.clear()
                        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                            app.embeddedLyrics.value = null
                            app.sourceInfo.value = SourceInfo()
                            app.playbackProblem.value = null
                        }
                        sourceJob?.cancel()
                        decoderGainActive = false
                        player.currentTracks.groups.forEach { group ->
                            for (i in 0 until group.length) if (group.isTrackSelected(i)) {
                                val format = group.getTrackFormat(i)
                                readGainMetadata(format.metadata)
                                val uri = player.currentMediaItem?.mediaId
                                if (uri != null)
                                    sourceJob =
                                        scope.launch {
                                            try {
                                                app.sourceInfo.value =
                                                    sourceInfo(this@PlaybackService, uri, format)
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (_: Exception) {
                                                /* Unreadable technical tags remain unknown. */
                                            }
                                        }
                                if (format.sampleMimeType == MimeTypes.AUDIO_OPUS)
                                    format.initializationData.firstOrNull()?.let { header ->
                                        if (
                                            header.size >= 18 &&
                                                (header[16].toInt() != 0 || header[17].toInt() != 0)
                                        )
                                            decoderGainActive = true
                                    }
                            }
                        }
                        val audio =
                            player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                        if (
                            events.contains(Player.EVENT_TRACKS_CHANGED) &&
                                audio.isNotEmpty() &&
                                audio.none { it.isSelected } &&
                                audio.none { it.isSupported }
                        ) {
                            app.playbackProblem.value =
                                "This device has no compatible decoder for ${audio.first().getTrackFormat(0).sampleMimeType}. Choose another format."
                            player.stop()
                        }
                        publishReplayTags()
                    }
                    if (!restoring) scheduleSave()
                    if (player.isPlaying) {
                        val id = player.currentMediaItem?.mediaId
                        if (id != null && id != lastRecorded) {
                            lastRecorded = id
                            scope.launch(Dispatchers.IO) {
                                app.database.music().recordPlay(id, System.currentTimeMillis())
                            }
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    lastRecorded = null
                }

                override fun onMetadata(metadata: Metadata) {
                    readGainMetadata(metadata)
                    publishReplayTags()
                }
            }
        )
        scope.launch {
            combine(app.effectiveDsp, app.replayTags) { settings, tags -> settings to tags }
                .collect { (settings, tags) ->
                    withContext(Dispatchers.Default) { app.audioEngine.update(settings, tags) }
                }
        }
        scope.launch {
            val saved = app.preferences.snapshot()
            // A user command received during restoration takes precedence over the saved queue.
            if (player.mediaItemCount == 0 && saved.items.isNotEmpty()) {
                player.setMediaItems(
                    saved.items.map { it.mediaItem() },
                    saved.index,
                    saved.position,
                )
                player.shuffleModeEnabled = saved.shuffle
                player.repeatMode = saved.repeat
                player.prepare()
            }
            restoring = false
            app.preferences.settings.collect { player.setPlaybackSpeed(it.speed) }
        }
        scope.launch {
            while (isActive) {
                delay(5_000)
                if (player.isPlaying && !restoring) persist()
            }
        }
    }

    private fun readGainMetadata(metadata: Metadata?) {
        if (metadata == null) return
        for (i in 0 until metadata.length()) when (val entry = metadata[i]) {
            is VorbisComment -> {
                gainTags[entry.key.uppercase()] = entry.value
                if (entry.key.uppercase() in listOf("LYRICS", "UNSYNCEDLYRICS"))
                    app.embeddedLyrics.value = entry.value.take(262144)
            }
            is BinaryFrame ->
                if (entry.id == "USLT")
                    app.embeddedLyrics.value = decodeUslt(entry.data)?.take(262144)
            is TextInformationFrame ->
                if (entry.id == "TXXX")
                    entry.description?.let { key ->
                        val value = entry.values.firstOrNull().orEmpty()
                        gainTags[key.uppercase()] = value
                        if (key.uppercase() in listOf("LYRICS", "USLT", "UNSYNCEDLYRICS"))
                            app.embeddedLyrics.value = value.take(262144)
                    }
        }
    }

    private fun publishReplayTags() {
        app.replayTags.value = parseReplayTags(gainTags).copy(decoderGainActive = decoderGainActive)
    }

    private fun snapshot() =
        QueueSnapshot(
            (0 until player.mediaItemCount).map { player.getMediaItemAt(it).saved() },
            player.currentMediaItemIndex.coerceAtLeast(0),
            player.currentPosition.coerceAtLeast(0),
            player.shuffleModeEnabled,
            player.repeatMode,
        )

    private suspend fun persist() {
        app.preferences.saveQueue(snapshot())
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        val value = snapshot()
        saveJob =
            scope.launch {
                delay(250)
                app.preferences.saveQueue(value)
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (
            !player.playWhenReady ||
                player.mediaItemCount == 0 ||
                player.playbackState == Player.STATE_ENDED
        )
            stopSelf()
    }

    override fun onDestroy() {
        app.playbackIsPlaying.value = false
        app.output.clear()
        val value = snapshot()
        val finalWidget = player.widgetSnapshot().copy(playing = false)
        scope.cancel()
        // Commit the final state before process teardown; normally this is a tiny local write.
        runBlocking(Dispatchers.IO) { withTimeoutOrNull(1500) { app.preferences.saveQueue(value) } }
        app.serviceRunning.value = false
        app.widgets.submit(finalWidget)
        session?.release()
        player.release()
        super.onDestroy()
    }
}
