package com.piccol.hifi

import android.content.ComponentName
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.piccol.hifi.playback.PlaybackService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@UnstableApi
class ServiceLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context
        get() = instrumentation.targetContext

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun connect(): ListenableFuture<MediaController> {
        lateinit var future: ListenableFuture<MediaController>
        main {
            future =
                MediaController.Builder(
                        context,
                        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
                    )
                    .buildAsync()
        }
        future.get(30, TimeUnit.SECONDS)
        return future
    }

    private fun await(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 60000
        while (System.currentTimeMillis() < end) {
            var ready = false
            main { ready = condition() }
            if (ready) return
            Thread.sleep(100)
        }
        fail("Service lifecycle condition timed out")
    }

    @Test
    fun savedQueueRestoresPausedAfterTheServiceIsDestroyedAndCreatedAgain() {
        val app = context.applicationContext as HiFiApplication
        val activity =
            instrumentation.startActivitySync(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        val file =
            java.io.File(context.filesDir, "integration-test/restore.wav").apply {
                parentFile!!.mkdirs()
            }
        file.outputStream().use { out ->
            instrumentation.context.assets.open("tone.wav").use { it.copyTo(out) }
        }
        val uri =
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.testfiles",
                file,
            )
        var first: ListenableFuture<MediaController>? = null
        var second: ListenableFuture<MediaController>? = null
        try {
            first = connect()
            val player = first.get()
            main {
                player.setMediaItem(
                    MediaItem.Builder().setUri(uri).setMediaId(uri.toString()).build(),
                    2000,
                )
                player.prepare()
                player.play()
            }
            await { player.isPlaying && player.currentPosition >= 1950 }
            main { player.pause() }
            await { !player.isPlaying }
            val deadline = System.currentTimeMillis() + 60000
            var saved = runBlocking { app.preferences.snapshot() }
            while (
                (saved.items.singleOrNull()?.uri != uri.toString() || saved.position < 1950) &&
                    System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(100)
                saved = runBlocking { app.preferences.snapshot() }
            }
            assertEquals(uri.toString(), saved.items.single().uri)
            assertTrue(saved.position >= 1950)
            main {
                activity.finish()
                first?.let { MediaController.releaseFuture(it) }
            }
            first = null
            context.stopService(Intent(context, PlaybackService::class.java))
            await { !app.serviceRunning.value }
            second = connect()
            val restored = second.get()
            await { restored.mediaItemCount == 1 && restored.currentPosition >= 1950 }
            main {
                assertFalse(restored.isPlaying)
                assertEquals(uri.toString(), restored.currentMediaItem!!.mediaId)
                assertNull(restored.playerError)
                // Replay is verified with no foreground MainActivity and the real service alive.
                assertTrue(
                    androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                        .none { it is MainActivity }
                )
                assertTrue(app.serviceRunning.value)
                restored.play()
            }
            await { restored.isPlaying && restored.currentPosition > 2100 }
            main { restored.clearMediaItems() }
        } finally {
            main {
                first?.let { MediaController.releaseFuture(it) }
                second?.let { future ->
                    runCatching { future.get().clearMediaItems() }
                    MediaController.releaseFuture(future)
                }
                activity.finish()
            }
            file.delete()
        }
    }
}
