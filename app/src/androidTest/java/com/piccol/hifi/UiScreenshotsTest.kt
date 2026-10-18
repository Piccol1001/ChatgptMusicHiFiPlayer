package com.piccol.hifi

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.piccol.hifi.data.*
import com.piccol.hifi.dsp.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*

/** Explicit fixture capture on the separate hifi-ui-validation AVD; never live music data. */
@UnstableApi
class UiScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context
        get() = instrumentation.targetContext

    private val app
        get() = context.applicationContext as HiFiApplication

    private var livePlayback = false

    private fun node(
        predicate: (android.view.accessibility.AccessibilityNodeInfo) -> Boolean
    ): android.view.accessibility.AccessibilityNodeInfo? {
        fun visit(
            n: android.view.accessibility.AccessibilityNodeInfo
        ): android.view.accessibility.AccessibilityNodeInfo? {
            if (predicate(n)) return n
            for (i in 0 until n.childCount) n.getChild(i)?.let {
                visit(it)?.let { found ->
                    return found
                }
            }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }

    private fun deviceWait(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 60_000
        while (!condition()) {
            compose.mainClock.advanceTimeBy(1_000)
            check(android.os.SystemClock.elapsedRealtime() < deadline) {
                "Device UI condition timed out"
            }
            Thread.sleep(250)
        }
    }

    private fun deviceClick(label: String, description: Boolean = false) {
        println("UI_CLICK: $label")
        compose.mainClock.advanceTimeBy(1_000)
        deviceWait {
            val found = node {
                (if (description) it.contentDescription else it.text)
                    ?.toString()
                    ?.contains(label) == true
            }
            val bounds = android.graphics.Rect()
            found?.getBoundsInScreen(bounds)
            if (
                found != null &&
                    !bounds.isEmpty &&
                    bounds.centerX() in
                        0 until compose.activity.resources.displayMetrics.widthPixels
            ) {
                shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
                true
            } else {
                val right =
                    android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
                        .ACTION_SCROLL_RIGHT
                        .id
                node { n -> n.actionList.any { it.id == right } }?.performAction(right)
                false
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        Thread.sleep(500)
    }

    private fun nav(label: String) {
        if (livePlayback) deviceClick("Open $label", true)
        else compose.onNodeWithContentDescription("Open $label").performClick()
    }

    private fun capture(name: String) {
        if (
            InstrumentationRegistry.getArguments().getString("eqRefresh") == "true" &&
                name.take(2) !in setOf("10", "11", "12", "13", "14", "15", "16", "17", "18", "99")
        )
            return
        println("UI_CAPTURE: $name")
        deviceWait {
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() ==
                context.packageName
        }
        // Native accessibility actions still need explicit frames from the Compose test clock.
        compose.mainClock.advanceTimeBy(1_000)
        // Continuous PCM/progress frames need not become Compose-idle to be captured.
        instrumentation.runOnMainSync { compose.activity.window.decorView.apply { invalidate() } }
        Thread.sleep(700)
        compose.mainClock.advanceTimeBy(64)
        Thread.sleep(300)
        shell("mkdir -p /sdcard/Download/hifi-ui-validation/v3")
        shell("screencap -p /sdcard/Download/hifi-ui-validation/v3/$name.png")
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }

    @Test
    fun captureEmptyPopulatedAudioAndAdaptiveScreens() {
        Assume.assumeTrue(
            "Capture only on the explicitly prepared isolated validation AVD",
            InstrumentationRegistry.getArguments().getString("uiValidation") == "true" &&
                shell("getprop ro.kernel.qemu.avd_name").trim() in
                    setOf("hifi-ui-validation", "hifi-ui-validation-30"),
        )
        compose.waitUntil(60_000) {
            compose
                .onAllNodesWithContentDescription("Open Library")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        nav("Library")
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText("No music found").fetchSemanticsNodes().isNotEmpty()
        }
        capture("01-library-empty")
        nav("Playlists")
        compose.onNodeWithText("Create your first playlist").assertExists()
        capture("02-playlists-empty")
        nav("Playing")
        compose.onNodeWithText("Ready to listen?").assertExists()
        capture("03-playing-empty")
        nav("Visualizer")
        compose.onNodeWithText("Play music to start visualizing").assertExists()
        capture("04-visualizer-idle")
        val directory = File(context.filesDir, "integration-test/ui-design").apply { mkdirs() }
        val tracks =
            (0..5).map { i ->
                val cover = File(directory, "cover-$i.png")
                val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val bases = intArrayOf(0xff173e67.toInt(), 0xff633b36.toInt(), 0xff214f46.toInt())
                canvas.drawColor(bases[i % 3])
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = Color.argb(110, 220, 240, 255)
                for (j in 0..7) {
                    paint.strokeWidth = 3f + j * 2
                    canvas.drawLine(0f, 50f + j * 42, 400f, 10f + j * 32, paint)
                }
                paint.color = Color.argb(100, 255, 220, 160)
                canvas.drawCircle(265f, 210f, 90f, paint)
                cover.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val file = File(directory, "signal-$i.flac")
                instrumentation.context.assets.open("tone.flac").use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.testfiles", file)
                        .toString()
                val artwork =
                    FileProvider.getUriForFile(context, "${context.packageName}.testfiles", cover)
                        .toString()
                Track(
                    uri,
                    listOf(
                        "Signal / One",
                        "Warm frequency",
                        "Room in blue",
                        "Quiet current",
                        "After the tone",
                        "Signal / Two",
                    )[i],
                    artist = if (i < 3) "Generated Audio" else "Studio Fixtures",
                    album = if (i < 3) "Signal studies" else "Room studies",
                    genre = "Test tones",
                    duration = 8000,
                    mime = "audio/flac",
                    folder = if (i < 3) "Music/Signal/" else "Music/Room/",
                    artwork = artwork,
                    source = "ui-validation",
                )
            }
        val priorDsp = runBlocking { app.dsp.settings.first() }
        val ids = mutableListOf<Long>()
        lateinit var controller: MediaController
        var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        try {
            runBlocking {
                app.database.music().upsert(tracks)
                for (name in listOf("Signal sessions", "Studio collection")) {
                    val id = app.database.music().create(Playlist(name = name))
                    ids += id
                    tracks.take(3).forEach { app.database.music().addToPlaylist(id, it.uri) }
                }
            }
            nav("Library")
            compose.waitUntil(60_000) {
                compose.onAllNodesWithText("Signal / One").fetchSemanticsNodes().isNotEmpty()
            }
            capture("05-library-songs")
            compose.onNodeWithText("Albums").performClick()
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Signal studies").fetchSemanticsNodes().isNotEmpty()
            }
            capture("06-library-albums")
            nav("Playlists")
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Signal sessions").fetchSemanticsNodes().isNotEmpty()
            }
            capture("07-playlists-populated")
            compose.runOnUiThread {
                future =
                    MediaController.Builder(
                            context,
                            SessionToken(
                                context,
                                ComponentName(
                                    context,
                                    com.piccol.hifi.playback.PlaybackService::class.java,
                                ),
                            ),
                        )
                        .buildAsync()
            }
            controller = future!!.get(30, TimeUnit.SECONDS)
            compose.runOnUiThread {
                controller.setMediaItems(
                    tracks.map { t ->
                        MediaItem.Builder()
                            .setMediaId(t.uri)
                            .setUri(t.uri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(t.title)
                                    .setArtist(t.artist)
                                    .setAlbumTitle(t.album)
                                    .setArtworkUri(android.net.Uri.parse(t.artwork))
                                    .build()
                            )
                            .build()
                    }
                )
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.prepare()
                controller.play()
            }
            compose.waitUntil(60_000) {
                var playing = false
                compose.runOnUiThread { playing = controller.isPlaying }
                playing
            }
            livePlayback = true
            nav("Playing")
            capture("08-now-playing-before-wait")
            deviceWait { node { it.contentDescription?.toString() == "Pause" } != null }
            capture("08-now-playing")
            nav("Library")
            capture("09-library-mini-player")
            runBlocking {
                app.dsp.update { old ->
                    old.copy(
                        enabled = true,
                        preset = "Custom",
                        ten = listOf(3.0, 2.0, 1.0, 0.0, -1.0, -1.0, 0.0, 1.0, 2.0, 2.0),
                        thirtyOne = List(31) { i -> 2.5 * kotlin.math.cos(i * .18) },
                        filters =
                            TEN_BANDS.mapIndexed { i, f ->
                                EqFilter(frequency = f, gain = if (i < 3) 2.0 else -1.0)
                            },
                    )
                }
            }
            nav("Equalizer")
            deviceClick("10 bands")
            capture("10-eq-ten")
            deviceClick("31 bands")
            capture("11-eq-thirty-one")
            deviceClick("Parametric")
            capture("12-eq-parametric")
            nav("Visualizer")
            deviceWait { app.pcmTap.sequence > 10 }
            capture("13-visualizer-playing")
            nav("Library")
            shell("wm size 840x1680")
            deviceWait { compose.activity.resources.configuration.screenWidthDp <= 330 }
            capture("14-library-small")
            shell("settings put system font_scale 1.5")
            deviceWait { compose.activity.resources.configuration.fontScale >= 1.4f }
            capture("15-library-large-font")
            nav("Equalizer")
            deviceClick("31 bands")
            capture("16-eq-large-font")
            shell("settings put system font_scale 1.0")
            shell("wm size reset")
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            deviceWait {
                compose.activity.resources.configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
            }
            nav("Playing")
            capture("17-now-playing-landscape")
            nav("Equalizer")
            capture("18-eq-landscape")
        } finally {
            runCatching { capture("99-final-state") }
            shell("settings put system font_scale 1.0")
            shell("wm size reset")
            shell("wm density reset")
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            if (future?.isDone == true)
                compose.runOnUiThread {
                    runCatching {
                        future!!.get().stop()
                        future!!.get().clearMediaItems()
                    }
                    MediaController.releaseFuture(future!!)
                }
            runBlocking {
                app.dsp.update { priorDsp }
                ids.forEach { app.database.music().deletePlaylist(it) }
                app.database.runInTransaction {
                    tracks.forEach { t ->
                        for (table in listOf("favorites", "history", "tracks")) app.database
                            .openHelper
                            .writableDatabase
                            .execSQL("DELETE FROM $table WHERE uri=?", arrayOf(t.uri))
                    }
                }
            }
            directory.deleteRecursively()
            // Finish before ActivityScenario waits for native idle: continuous render ticks
            // can otherwise starve its cleanup on an unaccelerated emulator.
            val activity = compose.activity
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
