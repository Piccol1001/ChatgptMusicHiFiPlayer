package com.piccol.hifi

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.piccol.hifi.data.Preferences
import com.piccol.hifi.playback.PlaybackService
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Uses generated, original tones; never requires a user's music or an Internet connection. */
@RunWith(AndroidJUnit4::class)
@UnstableApi
class PlaybackIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private lateinit var controller: MediaController
    private val files = mutableListOf<Uri>()
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun await(timeout: Long = 20_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end) {
            var result = false
            main { result = condition() }
            if (result) return
            Thread.sleep(100)
        }
        fail("Playback condition did not become true within $timeout ms")
    }
    @Before fun connect() {
        instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        main { future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync() }
        controller = future.get(20, TimeUnit.SECONDS)
        main { controller.stop(); controller.clearMediaItems(); controller.shuffleModeEnabled = false; controller.repeatMode = Player.REPEAT_MODE_OFF }
    }
    @After fun cleanup() {
        main { controller.stop(); controller.clearMediaItems(); controller.release() }
        files.forEach { context.contentResolver.delete(it, null, null) }
    }
    private fun import(ext: String): MediaItem {
        val directory = java.io.File(context.filesDir, "integration-test").apply { mkdirs() }
        val file = java.io.File(directory, "tone.$ext")
        file.outputStream().use { output -> instrumentation.context.assets.open("tone.$ext").use { it.copyTo(output) } }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.testfiles", file)
        files += uri
        return MediaItem.Builder().setMediaId(uri.toString()).setUri(uri).build()
    }
    @Test fun requiredFormatsDecodeAndAdvanceThroughTheProductionService() {
        for (ext in listOf("mp3", "flac", "wav", "m4a", "ogg", "opus")) {
            val item = import(ext)
            main { controller.setMediaItem(item); controller.prepare(); controller.play() }
            await { controller.isPlaying && controller.currentPosition > 350 && controller.duration > 7000 }
            main { assertNull("Decoder error for $ext", controller.playerError); controller.pause() }
        }
    }
    @Test fun queueControlsAndSnapshotUseTheExistingSession() {
        val first = import("mp3"); val second = import("flac")
        main { controller.setMediaItems(listOf(first, second)); controller.prepare(); controller.play() }
        await { controller.isPlaying && controller.currentPosition > 250 }
        main { controller.pause(); controller.seekTo(2000); controller.shuffleModeEnabled = true; controller.repeatMode = Player.REPEAT_MODE_ALL }
        await { controller.currentPosition >= 1900 && !controller.isPlaying }
        main { controller.shuffleModeEnabled = false; controller.seekToNextMediaItem() }
        await { controller.currentMediaItemIndex == 1 }
        main { controller.seekToPreviousMediaItem() }
        await { controller.currentMediaItemIndex == 0 }
        main { controller.moveMediaItem(0, 1) }
        await { controller.currentMediaItemIndex == 1 }
        main { controller.removeMediaItem(0) }
        await { controller.mediaItemCount == 1 }
        // Controller timeline changes are optimistic. Wait for the service's debounced
        // disk commit rather than assume it has acknowledged commands after a fixed delay.
        var snapshot = runBlocking { Preferences(context).snapshot() }
        val deadline = System.currentTimeMillis() + 20_000
        while ((snapshot.items.size != 1 || snapshot.items.firstOrNull()?.uri != first.mediaId || snapshot.repeat != Player.REPEAT_MODE_ALL)
            && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            snapshot = runBlocking { Preferences(context).snapshot() }
        }
        assertEquals("Service should persist the edited queue", 1, snapshot.items.size)
        assertEquals(first.mediaId, snapshot.items.single().uri)
        assertEquals(Player.REPEAT_MODE_ALL, snapshot.repeat)
    }
}
