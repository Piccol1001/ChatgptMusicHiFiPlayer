package com.piccol.hifi

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.piccol.hifi.data.Preferences
import com.piccol.hifi.dsp.*
import com.piccol.hifi.playback.PlaybackService
import java.util.concurrent.TimeUnit
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Uses generated, original tones; never requires a user's music or an Internet connection. */
@RunWith(AndroidJUnit4::class)
@UnstableApi
class PlaybackIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context
        get() = instrumentation.targetContext

    private lateinit var controller: MediaController
    private val files = mutableListOf<Uri>()

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun await(timeout: Long = 60_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end) {
            var result = false
            main { result = condition() }
            if (result) return
            Thread.sleep(100)
        }
        val app = context.applicationContext as HiFiApplication
        fail(
            "Playback condition did not become true within $timeout ms; mode=${app.audioEngine.settings.mode}, frames=${app.audioEngine.framesProcessed}, tap=${app.pcmTap.enabled}/${app.pcmTap.sequence}"
        )
    }

    @Before
    fun connect() {
        instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
        main {
            future =
                MediaController.Builder(
                        context,
                        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
                    )
                    .buildAsync()
        }
        controller = future.get(20, TimeUnit.SECONDS)
        main {
            controller.stop()
            controller.clearMediaItems()
            controller.shuffleModeEnabled = false
            controller.repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    @After
    fun cleanup() {
        main {
            controller.stop()
            controller.clearMediaItems()
            controller.release()
        }
        files.forEach { context.contentResolver.delete(it, null, null) }
    }

    private fun import(
        ext: String,
        asset: String = "tone.$ext",
        name: String = "tone.$ext",
    ): MediaItem {
        val directory = java.io.File(context.filesDir, "integration-test").apply { mkdirs() }
        val file = java.io.File(directory, name)
        file.outputStream().use { output ->
            instrumentation.context.assets.open(asset).use { it.copyTo(output) }
        }
        val uri =
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.testfiles",
                file,
            )
        files += uri
        return MediaItem.Builder().setMediaId(uri.toString()).setUri(uri).build()
    }

    @Test
    fun requiredFormatsDecodeAndAdvanceThroughTheProductionService() {
        for (ext in listOf("mp3", "flac", "wav", "m4a", "ogg", "opus")) {
            val item = import(ext)
            main {
                controller.setMediaItem(item)
                controller.prepare()
                controller.play()
            }
            await {
                controller.isPlaying &&
                    controller.currentPosition > 350 &&
                    controller.duration > 7000
            }
            main {
                assertNull("Decoder error for $ext", controller.playerError)
                controller.pause()
            }
        }
    }

    @Test
    fun queueControlsAndSnapshotUseTheExistingSession() {
        val first = import("mp3")
        val second = import("flac")
        main {
            controller.setMediaItems(listOf(first, second))
            controller.prepare()
            controller.play()
        }
        await { controller.isPlaying && controller.currentPosition > 250 }
        main {
            controller.pause()
            controller.seekTo(2000)
            controller.shuffleModeEnabled = true
            controller.repeatMode = Player.REPEAT_MODE_ALL
        }
        await { controller.currentPosition >= 1900 && !controller.isPlaying }
        main {
            controller.shuffleModeEnabled = false
            controller.seekToNextMediaItem()
        }
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
        while (
            (snapshot.items.size != 1 ||
                snapshot.items.firstOrNull()?.uri != first.mediaId ||
                snapshot.repeat != Player.REPEAT_MODE_ALL) && System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(100)
            snapshot = runBlocking { Preferences(context).snapshot() }
        }
        assertEquals("Service should persist the edited queue", 1, snapshot.items.size)
        assertEquals(first.mediaId, snapshot.items.single().uri)
        assertEquals(Player.REPEAT_MODE_ALL, snapshot.repeat)
    }

    @Test
    fun dspProcessesActualDecodedPcmAndReplayGainMetadata() {
        val app = context.applicationContext as HiFiApplication
        val original = runBlocking { app.dsp.settings.first() }
        val left = FloatArray(8192)
        val right = FloatArray(8192)
        fun rms(): Double {
            val n = app.pcmTap.copyLatest(left, right)
            return if (n < 8192) 0.0
            else sqrt((0 until n).sumOf { left[it].toDouble() * left[it] } / n)
        }
        try {
            runBlocking { app.dsp.update { DspSettings() } }
            app.pcmTap.enabled = true
            val item = import("wav")
            main {
                controller.setMediaItem(item)
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.prepare()
                controller.play()
            }
            await { app.audioEngine.audibleSettings == DspSettings() && app.audioEngine.framesProcessed > 24000 && rms() > .01 }
            val dry = rms()
            for (mode in EqMode.entries) {
                val settings =
                    DspSettings(
                        enabled = true,
                        mode = mode,
                        autoHeadroom = false,
                        ten = TEN_BANDS.map { if (it == 500.0) 6.0 else 0.0 },
                        thirtyOne = THIRD_OCTAVE.map { if (it == 500.0) 6.0 else 0.0 },
                        filters = TEN_BANDS.map { EqFilter(it, if (it == 500.0) 6.0 else 0.0) },
                    )
                val before = app.audioEngine.framesProcessed
                runBlocking { app.dsp.update { settings } }
                await { app.audioEngine.audibleSettings == settings }
                val committed = app.audioEngine.framesProcessed
                await { app.audioEngine.framesProcessed > committed + 24000 && rms() > .01 }
                val expected = responseDb(settings.activeFilters(), 48000, 440.0)
                assertEquals("Actual PCM gain in $mode", expected, 20 * log10(rms() / dry), .5)
            }
            runBlocking { app.dsp.update { DspSettings() } }
            val replay = import("mp3") // replace with the tagged fixture before playback
            val file = java.io.File(context.filesDir, "integration-test/tone.mp3")
            file.outputStream().use { out ->
                instrumentation.context.assets.open("replaygain.mp3").use { it.copyTo(out) }
            }
            main {
                controller.setMediaItem(replay)
                controller.prepare()
                controller.play()
            }
            await { app.replayTags.value.trackGain == -6.0 && rms() > .01 }
            val before = app.audioEngine.framesProcessed
            val dryReplay = rms()
            runBlocking { app.dsp.update { it.copy(replayMode = ReplayMode.TRACK) } }
            await { app.audioEngine.audibleSettings.replayMode == ReplayMode.TRACK }
            val committed = app.audioEngine.framesProcessed
            await { app.audioEngine.framesProcessed > committed + 24000 && rms() > .001 }
            assertEquals(
                "ReplayGain changes actual samples",
                -6.0,
                20 * log10(rms() / dryReplay),
                .5,
            )
        } finally {
            app.pcmTap.enabled = false
            runBlocking { app.dsp.update { original } }
        }
    }

    @Test
    fun aiffPcmStreamsThroughTheExistingDspAndSeeks() {
        val app = context.applicationContext as HiFiApplication
        for (asset in listOf("tone.aiff", "tone24.aiff")) {
            val item = import("aiff", asset, asset)
            main {
                controller.setMediaItem(item)
                controller.prepare()
                controller.play()
            }
            await {
                controller.isPlaying &&
                    controller.currentPosition > 350 &&
                    controller.duration > 7000
            }
            await { app.sourceInfo.value.bits == if (asset == "tone24.aiff") 24 else 16 }
            assertEquals(if (asset == "tone24.aiff") 96000 else 48000, app.audioEngine.sampleRate)
            main { controller.seekTo(4000) }
            await { controller.currentPosition >= 3900 }
            main {
                assertNull(controller.playerError)
                controller.pause()
            }
        }
    }

    @Test
    fun alacBehaviorMatchesTheDeviceDecoderInventory() {
        val item = import("m4a", "alac.m4a", "alac.m4a")
        val format = android.media.MediaFormat.createAudioFormat("audio/alac", 48000, 2)
        val available =
            android.media
                .MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
                .findDecoderForFormat(format) != null
        main {
            controller.setMediaItem(item)
            controller.prepare()
            controller.play()
        }
        if (available) await { controller.isPlaying && controller.currentPosition > 350 }
        else {
            val app = context.applicationContext as HiFiApplication
            await { app.playbackProblem.value != null || controller.playerError != null }
            assertTrue(
                app.playbackProblem.value?.contains("decoder") == true ||
                    controller.playerError != null
            )
        }
        android.util.Log.i("HiFiFormatTest", "ALAC platform decoder available: $available")
    }

    @Test
    fun flacTransitionsReuseAudioTrackAndPreserveExactPcmFrames() {
        checkTransitionFrames(listOf("flac" to true))
    }

    @Test
    fun mp3TransitionsPreserveExactBoundaryFramesOnSupportedDecoders() {
        val decoder =
            android.media
                .MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
                .findDecoderForFormat(
                    android.media.MediaFormat.createAudioFormat("audio/mpeg", 44100, 2)
                )
        val known = android.os.Build.VERSION.SDK_INT == 28 && decoder == "OMX.google.mp3.decoder"
        val force =
            androidx.test.platform.app.InstrumentationRegistry.getArguments()
                .getString("verifyKnownMp3Gapless") == "true"
        org.junit.Assume.assumeFalse(
            "Known API 28 Google MP3 decoder boundary residual: 22–551 frames; see FEATURE_STATUS and TESTING",
            known && !force,
        )
        checkTransitionFrames(listOf("mp3" to true, "mp3" to false))
    }

    private fun checkTransitionFrames(cases: List<Pair<String, Boolean>>) {
        val app = context.applicationContext as HiFiApplication
        val original = runBlocking { app.dsp.settings.first() }
        try {
            runBlocking { app.dsp.update { DspSettings() } }
            for ((ext, distinct) in cases) {
                val first =
                    import(ext, "continuous.$ext", "part1.$ext")
                        .buildUpon()
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle("Live part one")
                                .setAlbumTitle("Continuous recording")
                                .build()
                        )
                        .build()
                val second =
                    import(
                            ext,
                            if (distinct) "continuous2.$ext" else "continuous.$ext",
                            "part2.$ext",
                        )
                        .buildUpon()
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle("Live part two")
                                .setArtist("Different metadata")
                                .build()
                        )
                        .build()
                var isolated = 0L
                if (ext == "mp3") {
                    val before = app.audioEngine.framesProcessed
                    main {
                        controller.setMediaItem(second)
                        controller.prepare()
                        controller.play()
                    }
                    await { controller.playbackState == Player.STATE_ENDED }
                    isolated = app.audioEngine.framesProcessed - before
                    android.util.Log.i(
                        "HiFiGaplessTest",
                        "Isolated MP3 terminal PCM excess: ${isolated-88200} frames",
                    )
                    assertTrue(
                        "Terminal decoder tail must be bounded to one MP3 frame",
                        isolated in 88200L..89352L,
                    )
                }
                val frames = app.audioEngine.framesProcessed
                val tracks = app.output.createdTrackCount
                main {
                    controller.setMediaItems(listOf(first, second))
                    controller.prepare()
                    controller.play()
                }
                await { controller.playbackState == Player.STATE_ENDED }
                val paired = app.audioEngine.framesProcessed - frames
                // Compare to the isolated last track so its terminal decoder tail is not mistaken
                // for inserted samples at the transition between tracks.
                if (ext == "flac") assertEquals("FLAC PCM total", 176400L, paired)
                else {
                    val residual = paired - isolated - 88200L
                    android.util.Log.i(
                        "HiFiGaplessTest",
                        "MP3 boundary residual ($distinct metadata): $residual frames",
                    )
                    assertEquals("MP3 must be sample-exact between tracks", 0L, residual)
                }
                assertEquals(
                    "One AudioTrack must serve the compatible $ext pair",
                    1L,
                    app.output.createdTrackCount - tracks,
                )
                if (ext == "mp3")
                    assertTrue(
                        "MP3 fixture must carry encoder delay/padding",
                        app.sourceInfo.value.encoderDelay + app.sourceInfo.value.encoderPadding > 0,
                    )
            }
        } finally {
            runBlocking { app.dsp.update { original } }
        }
    }

    @Test
    fun importedAutoEqAndEmbeddedLyricsUseTheProductionService() {
        val app = context.applicationContext as HiFiApplication
        val original = runBlocking { app.dsp.settings.first() }
        val profiles = runBlocking { app.profiles.state.first() }
        val left = FloatArray(8192)
        val right = FloatArray(8192)
        fun rms(): Double {
            val n = app.pcmTap.copyLatest(left, right)
            return if (n < 8192) 0.0
            else sqrt((0 until n).sumOf { left[it].toDouble() * left[it] } / n)
        }
        var imported: String? = null
        try {
            runBlocking {
                app.dsp.update { DspSettings() }
                app.profiles.select(null)
            }
            app.pcmTap.enabled = true
            val item = import("mp3", "lyrics.mp3", "lyrics.mp3")
            main {
                controller.setMediaItem(item)
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.prepare()
                controller.play()
            }
            await { app.embeddedLyrics.value?.contains("Second line") == true && app.audioEngine.audibleSettings == DspSettings() && rms() > .01 }
            val dry = rms()
            val correction =
                com.piccol.hifi.audio
                    .parseAutoEq("Filter 1: ON PK Fc 440 Hz Gain -6 dB Q 1")
                    .settings
            runBlocking {
                app.profiles.save("Native correction", correction)
                imported = app.profiles.state.first().profiles.last().id
                app.profiles.select(imported)
            }
            await { app.audioEngine.audibleSettings.preset == "Native correction" }
            val committed = app.audioEngine.framesProcessed
            await { app.audioEngine.framesProcessed > committed + 24000 && rms() > .001 }
            assertEquals(-6.0, 20 * log10(rms() / dry), .5)
            assertEquals(
                1,
                com.piccol.hifi.audio.parseLyrics(app.embeddedLyrics.value!!).current(2500),
            )
            assertNotNull(
                "The actual AudioTrack route should be reported",
                app.output.route.value.device,
            )
        } finally {
            app.pcmTap.enabled = false
            runBlocking {
                imported?.let { app.profiles.delete(it) }
                app.profiles.select(profiles.manual)
                app.dsp.update { original }
            }
        }
    }

    @Test
    fun threeWidgetsRenderAndTheirButtonsControlTheExistingSession() {
        val app = context.applicationContext as HiFiApplication
        val first =
            import("wav")
                .buildUpon()
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Widget tone")
                        .setArtist("Generated fixture")
                        .build()
                )
                .build()
        val second =
            import("flac")
                .buildUpon()
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Second widget tone")
                        .build()
                )
                .build()
        val scope =
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            )
        val snapshot =
            app.widgets.snapshots.stateIn(
                scope,
                kotlinx.coroutines.flow.SharingStarted.Eagerly,
                com.piccol.hifi.widgets.WidgetSnapshot(),
            )
        val host = android.appwidget.AppWidgetHost(context, 1705)
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val views = mutableListOf<android.appwidget.AppWidgetHostView>()
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
            }
        }
        fun find(
            view: android.view.View,
            predicate: (android.view.View) -> Boolean,
        ): android.view.View? {
            if (predicate(view)) return view
            if (view is android.view.ViewGroup)
                for (i in 0 until view.childCount) find(view.getChildAt(i), predicate)?.let {
                    return it
                }
            return null
        }
        fun click(root: android.view.View, label: String) {
            var target = find(root) { it.contentDescription?.toString() == label }
            while (target != null && !target.hasOnClickListeners()) target =
                target.parent as? android.view.View
            assertNotNull("Widget control must expose a click action: $label", target)
            assertTrue(target!!.performClick())
        }
        try {
            shell("appwidget grantbind --package ${context.packageName}")
            main {
                controller.setMediaItems(listOf(first, second))
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.prepare()
                controller.play()
            }
            await { controller.isPlaying && snapshot.value.title == "Widget tone" }
            main {
                host.deleteHost()
                host.startListening()
                val providers =
                    listOf(
                        Triple(com.piccol.hifi.widgets.CompactWidgetReceiver::class.java, 160, 160),
                        Triple(
                            com.piccol.hifi.widgets.StandardWidgetReceiver::class.java,
                            300,
                            120,
                        ),
                        Triple(com.piccol.hifi.widgets.ExpandedWidgetReceiver::class.java, 320, 340),
                    )
                for ((receiver, width, height) in providers) {
                    val id = host.allocateAppWidgetId()
                    val options =
                        android.os.Bundle().apply {
                            putInt(
                                android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                                width,
                            )
                            putInt(
                                android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                                width,
                            )
                            putInt(
                                android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                                height,
                            )
                            putInt(
                                android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                                height,
                            )
                        }
                    assertTrue(
                        manager.bindAppWidgetIdIfAllowed(
                            id,
                            ComponentName(context, receiver),
                            options,
                        )
                    )
                    val info = manager.getAppWidgetInfo(id)
                    assertEquals(0, info.updatePeriodMillis)
                    views += host.createView(context, id, info)
                }
            }
            runBlocking { com.piccol.hifi.widgets.refreshWidgets(context) }
            await {
                views.size == 3 &&
                    views.all { root ->
                        find(root) {
                            it.contentDescription?.toString()?.startsWith("Open Now Playing:") ==
                                true
                        } != null
                    } &&
                    views.drop(1).all { root ->
                        find(root) {
                            it is android.widget.TextView &&
                                it.text.toString().contains("Widget tone", true)
                        } != null
                    }
            }
            main { click(views[1], "Pause") }
            await { !controller.isPlaying && !snapshot.value.playing }
            main { controller.repeatMode = Player.REPEAT_MODE_OFF }
            await {
                snapshot.value.repeat == Player.REPEAT_MODE_OFF &&
                    find(views[2]) { it.contentDescription?.toString() == "Repeat off" } != null
            }
            runBlocking { com.piccol.hifi.widgets.WidgetControls.execute(context, "next") }
            await { controller.currentMediaItemIndex == 1 && snapshot.value.uri == second.mediaId }
            runBlocking { com.piccol.hifi.widgets.WidgetControls.execute(context, "previous") }
            await { controller.currentMediaItemIndex == 0 && snapshot.value.uri == first.mediaId }
            main { click(views[2], "Shuffle off") }
            await { controller.shuffleModeEnabled && snapshot.value.shuffle }
            main { click(views[2], "Repeat off") }
            await {
                controller.repeatMode == Player.REPEAT_MODE_ALL &&
                    snapshot.value.repeat == Player.REPEAT_MODE_ALL
            }
            runBlocking { com.piccol.hifi.widgets.WidgetControls.execute(context, "toggle") }
            await { controller.isPlaying && snapshot.value.playing }
            assertEquals(2, runBlocking { app.preferences.snapshot() }.items.size)
        } finally {
            main {
                host.stopListening()
                host.deleteHost()
            }
            shell("appwidget revokebind --package ${context.packageName}")
            scope.cancel()
        }
    }

    @Test
    fun corruptAudioFailsGracefullyAndTheSameSessionCanPlayAnotherFile() {
        val broken = import("wav", name = "broken.wav")
        java.io
            .File(context.filesDir, "integration-test/broken.wav")
            .writeText("This is not an audio container.")
        main {
            controller.setMediaItem(broken)
            controller.prepare()
            controller.play()
        }
        await { controller.playerError != null }
        val valid = import("wav", name = "recovery.wav")
        main {
            controller.setMediaItem(valid)
            controller.prepare()
            controller.play()
        }
        await { controller.isPlaying && controller.currentPosition > 350 }
        main {
            assertNull(controller.playerError)
            assertEquals(valid.mediaId, controller.currentMediaItem!!.mediaId)
        }
    }
}
