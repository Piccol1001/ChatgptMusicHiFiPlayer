package com.piccol.hifi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.util.UnstableApi
import org.junit.Rule
import org.junit.Test

@UnstableApi
class PlayerUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun eqProfilesVisualizersAndOutputScreensNavigateWithoutPlayback() {
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText("Equalizer").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Equalizer").performClick()
        compose.onNodeWithText("31 bands").performClick()
        compose.onNodeWithText("Parametric").performClick()
        compose.onNodeWithText("AutoEq & output profiles").performClick()
        compose.onNodeWithText("Headphone & output profiles").assertExists()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(30_000) {
            compose
                .onAllNodesWithText("Headphone & output profiles")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        compose.onNodeWithText("Visualizer").performClick()
        listOf("Oscilloscope", "Stereo meters", "Spectrogram", "Circular", "Reactive", "Spectrum")
            .forEach { mode ->
                compose.onNodeWithContentDescription(mode).performClick()
                compose.waitUntil(30_000) {
                    runCatching {
                            compose.onNodeWithContentDescription(mode).assertIsOn()
                            true
                        }
                        .getOrDefault(false)
                }
            }
        compose.onNodeWithContentDescription("Fullscreen visualizer").performClick()
        compose.onNodeWithContentDescription("Exit fullscreen").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        val settingsList =
            compose.onNode(
                SemanticsMatcher.keyIsDefined(
                    androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
                )
            )
        settingsList.performScrollToNode(hasText("Open-source licenses"))
        compose.onNodeWithText("Open-source licenses").performClick()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Apache-2.0.txt").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Close licenses").performClick()
        settingsList.performScrollToNode(hasText("Audio output information"))
        compose.onNodeWithText("Audio output information").performClick()
        compose.onNodeWithText("Audio output").assertExists()
    }
}
