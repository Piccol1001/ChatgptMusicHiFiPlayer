package com.piccol.hifi

import android.content.Context
import androidx.room.Room
import com.piccol.hifi.data.*
import com.piccol.hifi.library.cleanTag
import com.piccol.hifi.library.isAudioFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class LibraryTest {
    private lateinit var db: MusicDatabase
    private val dao get() = db.music()
    @Before fun before() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MusicDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun after() { db.close() }
    private fun tracks(filter: LibraryFilter): List<String> = db.openHelper.readableDatabase.query(libraryQuery(filter)).use { c ->
        buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("uri"))) }
    }
    @Test fun sourceRefreshPreservesFavoritesAndMarksMissingFilesUnavailable() = runBlocking {
        val a = Track("content://a", "A")
        val b = Track("content://b", "B")
        dao.replaceSource("media", listOf(a, b))
        dao.addFavorite(Favorite(a.uri))
        dao.replaceSource("media", listOf(b.copy(title = "Renamed")))
        assertFalse(dao.track(a.uri)!!.available)
        assertEquals("Renamed", dao.track(b.uri)!!.title)
        assertEquals(listOf(a.uri), dao.favorites().first())
        assertEquals(listOf(b.uri), tracks(LibraryFilter()))
    }
    @Test fun playlistMembershipIsIdempotentOrderedAndDeletionKeepsTracks() = runBlocking {
        dao.upsert(listOf(Track("content://a", "Z"), Track("content://b", "A")))
        val id = dao.create(Playlist(name = "Favorites"))
        dao.addToPlaylist(id, "content://a"); dao.addToPlaylist(id, "content://b"); dao.addToPlaylist(id, "content://a")
        assertEquals(listOf("content://a", "content://b"), tracks(LibraryFilter(playlist = id)))
        assertEquals(2, dao.playlists().first().single().count)
        dao.deletePlaylist(id)
        assertTrue(tracks(LibraryFilter(playlist = id)).isEmpty())
        assertNotNull(dao.track("content://a"))
    }
    @Test fun literalSearchDoesNotInterpretSqlWildcardsOrInjection() = runBlocking {
        dao.upsert(listOf(Track("content://a", "100%_pure"), Track("content://b", "100abcdefpure")))
        assertEquals(listOf("content://a"), tracks(LibraryFilter(search = "%_")))
        assertTrue(tracks(LibraryFilter(search = "' OR 1=1 --")).isEmpty())
    }
    @Test fun historyUsesPlayCountAndLastPlayedIndependently() = runBlocking {
        dao.upsert(listOf(Track("content://a", "A"), Track("content://b", "B")))
        dao.recordPlay("content://a", 1); dao.recordPlay("content://a", 2); dao.recordPlay("content://b", 3)
        assertEquals(listOf("content://a", "content://b"), tracks(LibraryFilter(category = Category.MOST)))
        assertEquals(listOf("content://b", "content://a"), tracks(LibraryFilter(category = Category.RECENT)))
    }
    @Test fun metadataFallbackAndFileRecognitionAreConservative() {
        assertEquals("Unknown artist", cleanTag("<unknown>", "Unknown artist"))
        assertTrue(isAudioFile("SONG.FLAC", "application/octet-stream"))
        assertTrue(isAudioFile("no-extension", "audio/mpeg"))
        assertFalse(isAudioFile("cover.jpg", "image/jpeg"))
    }
}
