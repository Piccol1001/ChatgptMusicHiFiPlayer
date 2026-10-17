package com.piccol.hifi

import com.piccol.hifi.data.*
import com.piccol.hifi.widgets.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetStateTest {
    @Test
    fun eventSnapshotPersistsAndClockHonorsPauseSeekSpeedAndDuration() = runBlocking {
        val s =
            WidgetSnapshot(
                "content://music/1",
                "Song",
                "Artist",
                "Album",
                playing = true,
                position = 3000,
                duration = 20000,
                speed = 1.5f,
                elapsed = 1000,
            )
        assertEquals(6000L, s.currentPosition(3000))
        assertEquals(20000L, s.currentPosition(30000))
        assertEquals(3000L, s.copy(playing = false).currentPosition(30000))
        assertEquals(3000L, s.currentPosition(500))
        val context = RuntimeEnvironment.getApplication()
        val repo = WidgetRepository(context)
        repo.save(s)
        assertEquals(s, WidgetRepository(context).snapshots.first())
        assertEquals(s, decodeWidget(encodeWidget(s)))
        repo.seed(
            QueueSnapshot(
                listOf(SavedItem(s.uri, s.title, s.artist, s.album, null)),
                position = 9000,
            ),
            force = true,
        )
        assertEquals(9000L, repo.snapshots.first().position)
        assertFalse(repo.snapshots.first().playing)
        assertEquals(WidgetSnapshot(), decodeWidget("not JSON"))
    }
}
