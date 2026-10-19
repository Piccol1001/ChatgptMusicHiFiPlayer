package com.piccol.hifi

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.pm.PackageInfoCompat
import androidx.media3.common.util.UnstableApi
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@UnstableApi
class PlayerUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val indexed = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)

    private lateinit var launchIntent: android.content.Intent

    @Before
    fun retainScenarioLaunchIntent() {
        launchIntent = android.content.Intent(compose.activity.intent)
    }

    @After
    fun finishOwnActivityBeforeScenarioIdleWait() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor =
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            listOf(
                    androidx.test.runner.lifecycle.Stage.RESUMED,
                    androidx.test.runner.lifecycle.Stage.STARTED,
                    androidx.test.runner.lifecycle.Stage.PAUSED,
                    androidx.test.runner.lifecycle.Stage.STOPPED,
                )
                .flatMap { monitor.getActivitiesInStage(it).toList() }
                .filterIsInstance<MainActivity>()
                .distinct()
                .forEach {
                    // ActivityScenario matches lifecycle events by action/data/type of its original
                    // intent.
                    // Delivery assertions are complete; restore only the test harness identity for
                    // teardown.
                    it.intent = android.content.Intent(launchIntent)
                    it.finish()
                }
        }
    }

    @Test
    fun eqProfilesVisualizersAndInstalledAboutVersionRemainAccessible() {
        try {
            compose.waitUntil(60_000) {
                compose
                    .onAllNodesWithContentDescription("Open Audio")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose
                .onAllNodes(
                    hasContentDescription("Open Library") or
                        hasContentDescription("Open Playing") or
                        hasContentDescription("Open Audio")
                )
                .assertCountEquals(3)
            compose.onNodeWithContentDescription("Open Audio").performClick()
            compose.onNodeWithContentDescription("Audio tab Equalizer").assertIsSelected()
            compose.onNodeWithContentDescription("Equalizer enabled").assertExists()
            compose.onNodeWithText("31 bands").performClick()
            compose.onNodeWithText("Parametric").performClick()
            compose.onNode(indexed).performScrollToNode(hasText("Advanced controls"))
            compose.onNodeWithText("Advanced controls").performClick()
            compose.onNode(indexed).performScrollToNode(hasText("AutoEq & output profiles"))
            compose.onNodeWithText("AutoEq & output profiles").performClick()
            compose.onNodeWithText("Headphone & output profiles").assertExists()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitUntil(30_000) {
                compose
                    .onAllNodesWithText("Headphone & output profiles")
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
            compose.onNodeWithContentDescription("Audio tab Visualizer").performClick()
            val modeList =
                compose.onNode(
                    indexed and
                        SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
                )
            listOf(
                    "Oscilloscope",
                    "Stereo Meter",
                    "Spectrogram",
                    "Circular",
                    "Reactive",
                    "Spectrum",
                )
                .forEach { mode ->
                    modeList.performScrollToNode(hasContentDescription(mode))
                    compose.onNodeWithContentDescription(mode).performClick()
                    compose.waitUntil(30_000) {
                        runCatching {
                                compose.onNodeWithContentDescription(mode).assertIsSelected()
                                true
                            }
                            .getOrDefault(false)
                    }
                }
            compose.onNodeWithContentDescription("Fullscreen visualizer").performClick()
            compose.mainClock.advanceTimeBy(3_500)
            compose.onNodeWithContentDescription("Visualization mode").assertDoesNotExist()
            compose.onNodeWithContentDescription("Show visualizer controls").performClick()
            compose.mainClock.advanceTimeBy(200)
            compose.onNodeWithContentDescription("Visualization mode").assertExists()
            compose.onNodeWithContentDescription("Visualizer color theme").assertExists()
            compose.onNodeWithContentDescription("Exit fullscreen").performClick()
            compose.onNodeWithContentDescription("Open Library").performClick()
            compose
                .onNodeWithContentDescription("Library category Playlists")
                .performScrollTo()
                .performClick()
            compose.onNodeWithContentDescription("Create playlist").assertExists()
            compose.onNodeWithContentDescription("Open Library").assertIsSelected()
            listOf("equalizer" to "Equalizer", "visualizer" to "Visualizer").forEach {
                (route, label) ->
                compose.onNodeWithContentDescription("Open Library").performClick()
                compose.runOnUiThread {
                    InstrumentationRegistry.getInstrumentation()
                        .callActivityOnNewIntent(
                            compose.activity,
                            android.content
                                .Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("hifi://$route"),
                                    compose.activity,
                                    MainActivity::class.java,
                                )
                                .addFlags(
                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                ),
                        )
                }
                compose.waitUntil(30_000) {
                    runCatching {
                            compose
                                .onNodeWithContentDescription("Audio tab $label")
                                .assertIsSelected()
                            true
                        }
                        .getOrDefault(false)
                }
                InstrumentationRegistry.getInstrumentation()
                    .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                compose.onNodeWithContentDescription("Open Library").assertIsSelected()
            }
            compose.runOnUiThread {
                InstrumentationRegistry.getInstrumentation()
                    .callActivityOnNewIntent(
                        compose.activity,
                        android.content
                            .Intent(compose.activity, MainActivity::class.java)
                            .putExtra("open_player", true)
                            .addFlags(
                                android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            ),
                    )
            }
            compose.waitUntil(30_000) {
                runCatching {
                        compose.onNodeWithContentDescription("Open Playing").assertIsSelected()
                        true
                    }
                    .getOrDefault(false)
            }
            compose.onNodeWithContentDescription("Settings").performClick()
            var list = compose.onNode(indexed)
            list.performScrollToNode(hasText("About"))
            compose.onNodeWithText("About").performClick()
            compose.onNodeWithText("App Version").assertExists()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            compose
                .onNodeWithText(
                    "${info.versionName} (code ${PackageInfoCompat.getLongVersionCode(info)})"
                )
                .assertExists()
            list = compose.onNode(indexed)
            list.performScrollToNode(hasText("Open-source licenses"))
            compose.onNodeWithText("Open-source licenses").performClick()
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Apache-2.0.txt").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Apache-2.0.txt").performClick()
            compose.waitUntil(60_000) {
                compose
                    .onAllNodes(hasText("Apache License", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithText("Close licenses").performClick()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Audio Output").performClick()
            compose.onNodeWithText("Audio output information").performClick()
            compose.onNodeWithText("Audio output").assertExists()
            compose.runOnUiThread {
                InstrumentationRegistry.getInstrumentation()
                    .callActivityOnNewIntent(
                        compose.activity,
                        android.content
                            .Intent(compose.activity, MainActivity::class.java)
                            .putExtra("route", "visualizer"),
                    )
            }
            compose.onNodeWithContentDescription("Audio tab Visualizer").assertIsSelected()
            compose.onNodeWithText("Audio output").assertDoesNotExist()

            android.util.Log.i(
                "MinimalUi",
                "All navigation, shortcut and installed-version assertions passed",
            )
        } catch (failure: Throwable) {
            android.util.Log.e("MinimalUi", "UI assertion failed before teardown", failure)
            throw failure
        }
    }
}
