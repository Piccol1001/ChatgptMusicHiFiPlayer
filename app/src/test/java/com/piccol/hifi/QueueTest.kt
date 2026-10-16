package com.piccol.hifi

import com.piccol.hifi.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QueueTest {
    @Test fun snapshotRoundTripRetainsDuplicatesAndPlaybackModes() {
        val song = SavedItem("content://media/external/audio/media/5", "Title", "Artist", "Album", null)
        val snapshot = QueueSnapshot(listOf(song, song), 1, 31_000, true, 2)
        assertEquals(snapshot, decodeQueue(encodeQueue(snapshot)))
    }
    @Test fun malformedSnapshotDoesNotPreventStartup() {
        assertEquals(QueueSnapshot(), decodeQueue("{broken"))
        assertEquals(QueueSnapshot(), decodeQueue(null))
    }
    @Test fun remoteUrisAreRejectedAndInvalidIndicesClamped() {
        val saved = QueueSnapshot(listOf(SavedItem("https://example.com/song", "", "", "", null)), 99, -50, repeat = 99)
        val actual = decodeQueue(encodeQueue(saved))
        assertTrue(actual.items.isEmpty()); assertEquals(0, actual.index); assertEquals(0L, actual.position); assertEquals(2, actual.repeat)
    }
}
