package com.piccol.hifi.widgets

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.piccol.hifi.HiFiApplication
import com.piccol.hifi.playback.PlaybackService
import com.piccol.hifi.playback.mediaItem
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*

val widgetCommand = ActionParameters.Key<String>("command")

@UnstableApi
class WidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetControls.execute(context, parameters[widgetCommand] ?: "toggle")
    }
}

@UnstableApi
object WidgetControls {
    /**
     * Uses a controller of the authoritative service. Never creates an ExoPlayer or a media
     * session.
     */
    suspend fun execute(context: Context, command: String) =
        withContext(Dispatchers.Main.immediate) {
            // Glance callbacks can supply ReceiverRestrictedContext, which cannot bind services.
            val appContext = context.applicationContext
            val future =
                MediaController.Builder(
                        appContext,
                        SessionToken(
                            appContext,
                            ComponentName(appContext, PlaybackService::class.java),
                        ),
                    )
                    .buildAsync()
            try {
                val player =
                    withTimeout(15000) {
                        suspendCancellableCoroutine<MediaController> { continuation ->
                            future.addListener(
                                {
                                    try {
                                        if (continuation.isActive) continuation.resume(future.get())
                                    } catch (e: Exception) {
                                        if (continuation.isActive)
                                            continuation.resumeWithException(e)
                                    }
                                },
                                ContextCompat.getMainExecutor(appContext),
                            )
                        }
                    }
                val app = context.applicationContext as HiFiApplication
                if (player.mediaItemCount == 0) {
                    val queue = app.preferences.snapshot()
                    if (player.mediaItemCount == 0 && queue.items.isNotEmpty()) {
                        player.setMediaItems(
                            queue.items.map { it.mediaItem() },
                            queue.index,
                            queue.position,
                        )
                        player.shuffleModeEnabled = queue.shuffle
                        player.repeatMode = queue.repeat
                        player.prepare()
                    }
                }
                when (command) {
                    "toggle" ->
                        if (player.isPlaying) player.pause()
                        else if (player.mediaItemCount > 0) {
                            if (player.playbackState == Player.STATE_IDLE) player.prepare()
                            player.play()
                        }
                    "next" -> player.seekToNextMediaItem()
                    "previous" ->
                        if (player.currentPosition > 3000) player.seekTo(0)
                        else player.seekToPreviousMediaItem()
                    "shuffle" -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                    "repeat" ->
                        player.repeatMode =
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                /* A later tap can reconnect; no second player is created. */
            } finally {
                MediaController.releaseFuture(future)
            }
        }
}
