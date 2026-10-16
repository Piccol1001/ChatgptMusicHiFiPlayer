package com.piccol.hifi.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.piccol.hifi.HiFiApplication
import com.piccol.hifi.MainActivity
import com.piccol.hifi.data.QueueSnapshot
import kotlinx.coroutines.*

@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as HiFiApplication
    private var restoring = true
    private var lastRecorded: String? = null
    private var saveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val launch = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(launch).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (!restoring) scheduleSave()
                if (player.isPlaying) {
                    val id = player.currentMediaItem?.mediaId
                    if (id != null && id != lastRecorded) {
                        lastRecorded = id
                        scope.launch(Dispatchers.IO) { app.database.music().recordPlay(id, System.currentTimeMillis()) }
                    }
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { lastRecorded = null }
        })
        scope.launch {
            val saved = app.preferences.snapshot()
            // A user command received during restoration takes precedence over the saved queue.
            if (player.mediaItemCount == 0 && saved.items.isNotEmpty()) {
                player.setMediaItems(saved.items.map { it.mediaItem() }, saved.index, saved.position)
                player.shuffleModeEnabled = saved.shuffle
                player.repeatMode = saved.repeat
                player.prepare()
            }
            restoring = false
            app.preferences.settings.collect { player.setPlaybackSpeed(it.speed) }
        }
        scope.launch {
            while (isActive) { delay(5_000); if (player.isPlaying && !restoring) persist() }
        }
    }

    private fun snapshot() = QueueSnapshot((0 until player.mediaItemCount).map { player.getMediaItemAt(it).saved() },
        player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0), player.shuffleModeEnabled, player.repeatMode)

    private suspend fun persist() { app.preferences.saveQueue(snapshot()) }
    private fun scheduleSave() {
        saveJob?.cancel()
        val value = snapshot()
        saveJob = scope.launch { delay(250); app.preferences.saveQueue(value) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) stopSelf()
    }
    override fun onDestroy() {
        val value = snapshot()
        scope.cancel()
        // Commit the final state before process teardown; normally this is a tiny local write.
        runBlocking(Dispatchers.IO) { withTimeoutOrNull(1500) { app.preferences.saveQueue(value) } }
        session?.release()
        player.release()
        super.onDestroy()
    }
}
