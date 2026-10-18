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
import org.junit.Rule
import org.junit.Test

@UnstableApi
class PlayerUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val indexed = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)

    @After
    fun finishOwnActivityBeforeScenarioIdleWait() {
        val activity = compose.activity
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.finish() }
    }

    @Test
    fun eqProfilesVisualizersAndInstalledAboutVersionRemainAccessible() {
        compose.waitUntil(60_000) {
            compose
                .onAllNodesWithContentDescription("Open Equalizer")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithContentDescription("Open Equalizer").performClick()
        compose.onNodeWithText("31 bands").performClick()
        compose.onNodeWithText("Parametric").performClick()
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
        compose.onNodeWithContentDescription("Open Visualizer").performClick()
        val modeList =
            compose.onNode(
                indexed and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
            )
        listOf("Oscilloscope", "Stereo Meter", "Spectrogram", "Circular", "Reactive", "Spectrum")
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
        compose.onNodeWithContentDescription("Exit fullscreen").performClick()
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
        compose.onNodeWithText("Close licenses").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Audio Output").performClick()
        compose.onNodeWithText("Audio output information").performClick()
        compose.onNodeWithText("Audio output").assertExists()
    }
}
