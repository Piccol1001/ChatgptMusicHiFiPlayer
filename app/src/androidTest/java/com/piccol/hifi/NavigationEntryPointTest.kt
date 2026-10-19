package com.piccol.hifi

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.media3.common.util.UnstableApi
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** Actual Android task/intent dispatch, outside ActivityScenario's single-activity lifecycle. */
@UnstableApi
class NavigationEntryPointTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val flags =
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

    private fun selected(label: String) {
        fun find(node: AccessibilityNodeInfo): Boolean {
            if (
                node.contentDescription?.toString() == label &&
                    (node.isSelected || node.parent?.isSelected == true)
            )
                return true
            for (i in 0 until node.childCount) node.getChild(i)?.let { if (find(it)) return true }
            return false
        }
        val deadline = SystemClock.elapsedRealtime() + 60_000
        while (true) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root?.packageName?.toString() == context.packageName && find(root)) return
            check(SystemClock.elapsedRealtime() < deadline) {
                "Actual launch never selected $label; current root=${root?.packageName}"
            }
            Thread.sleep(200)
        }
    }

    @After
    fun finishOwnActivities() {
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED)
                .flatMap { monitor.getActivitiesInStage(it).toList() }
                .filterIsInstance<MainActivity>()
                .distinct()
                .forEach { it.finish() }
        }
    }

    @Test
    fun legacyAudioLinksAndWidgetLaunchUseTheExistingActivity() {
        val main =
            instrumentation.startActivitySync(
                Intent(context, MainActivity::class.java)
                    .putExtra("route", "library")
                    .addFlags(flags)
            ) as MainActivity
        selected("Open Library")
        listOf("hifi://equalizer" to "Equalizer", "hifi://audio/visualizer" to "Visualizer")
            .forEach { (uri, tab) ->
                // Resolve through the real manifest rather than specifying a component.
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        .setPackage(context.packageName)
                        .addFlags(flags)
                assertEquals(
                    MainActivity::class.java.name,
                    context.packageManager.resolveActivity(intent, 0)?.activityInfo?.name,
                )
                context.startActivity(intent)
                selected("Audio tab $tab")
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                selected("Open Library")
            }
        context.startActivity(
            Intent(context, MainActivity::class.java).putExtra("open_player", true).addFlags(flags)
        )
        selected("Open Playing")
        instrumentation.runOnMainSync {
            val resumed =
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
            check(resumed.size == 1 && resumed.single() === main) {
                "Shortcut created a second MainActivity"
            }
        }
    }
}
