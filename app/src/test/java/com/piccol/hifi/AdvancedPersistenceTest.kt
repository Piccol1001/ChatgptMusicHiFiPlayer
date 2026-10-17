package com.piccol.hifi

import androidx.room.Room
import com.piccol.hifi.audio.*
import com.piccol.hifi.data.*
import com.piccol.hifi.dsp.*
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
class AdvancedPersistenceTest {
    @Test
    fun importedLyricsAndTimingOptionsSurviveRepositoryRecreation() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = LyricsRepository(context)
        val file =
            java.io.File(context.cacheDir, "lyrics.lrc").apply { writeText("[00:01.00]Local line") }
        val song = "content://lyrics/test"
        repo.import(song, android.net.Uri.fromFile(file))
        repo.offset(song, 250)
        repo.options { it.copy(font = 28f, autoScroll = false) }
        val restored = LyricsRepository(context)
        assertEquals(
            "Local line",
            restored.load(song, "Embedded line", emptySet())!!.lyrics.lines.single().text,
        )
        assertEquals(250L, restored.offset(song).first())
        assertEquals(28f, restored.options.first().font, 0f)
        assertFalse(restored.options.first().autoScroll)
        file.writeText(" ")
        assertTrue(runCatching { repo.import(song, android.net.Uri.fromFile(file)) }.isFailure)
        assertEquals(
            "Local line",
            restored.load(song, null, emptySet())!!.lyrics.lines.single().text,
        )
        restored.remove(song)
        assertEquals(
            "Embedded line",
            restored.load(song, "Embedded line", emptySet())!!.lyrics.lines.single().text,
        )
    }

    @Test
    fun profileRoutesPreservePersonalEqAndReplayGain() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = ProfileRepository(context)
        repo.state.first().profiles.forEach { repo.delete(it.id) }
        val base = DspSettings(enabled = true, preamp = -2.0, replayMode = ReplayMode.TRACK)
        val correction = parseAutoEq("Filter 1: ON PK Fc 1000 Hz Gain -4 dB Q 1").settings
        repo.save("Headphones", correction)
        val id = repo.state.first().profiles.single().id
        repo.assign(OutputCategory.WIRED, id)
        val state = ProfileRepository(context).state.first()
        val speaker =
            OutputRoute(
                OutputDevice(
                    1,
                    "Speaker",
                    OutputCategory.SPEAKER,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                )
            )
        val wired = speaker.copy(device = speaker.device!!.copy(category = OutputCategory.WIRED))
        assertEquals(base, effectiveDsp(base, state, speaker))
        assertEquals(ReplayMode.TRACK, effectiveDsp(base, state, wired).replayMode)
        assertEquals(-4.0, effectiveDsp(base, state, wired).filters[0].gain, 0.0)
        repo.rename(id, "New name")
        assertEquals("New name", repo.state.first().profiles.single().name)
        repo.delete(id)
        assertTrue(repo.state.first().assignments.isEmpty())
    }

    @Test
    fun smartRulesReevaluateActualFavoritesHistoryAndDates() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db =
            Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val dao = db.music()
            dao.upsert(
                listOf(
                    Track(
                        "content://a/1",
                        "A",
                        "Artist",
                        genre = "Jazz",
                        added = 2000,
                        mime = "audio/flac",
                    ),
                    Track("content://a/2", "B", "Other", genre = "Rock", added = 1000),
                )
            )
            val rules =
                SmartRules(
                    artist = "artist",
                    genre = "Jazz",
                    minPlays = 2,
                    addedAfter = 1500,
                    favoritesOnly = true,
                    format = "flac",
                )
            fun query() = libraryQuery(LibraryFilter(smart = rules))
            assertTrue(dao.selected(query()).isEmpty())
            dao.addFavorite(Favorite("content://a/1"))
            dao.recordPlay("content://a/1", 3000)
            assertTrue(dao.selected(query()).isEmpty())
            dao.recordPlay("content://a/1", 4000)
            assertEquals(listOf("A"), dao.selected(query()).map { it.title })
            dao.removeFavorite("content://a/1")
            assertTrue(dao.selected(query()).isEmpty())
            val encoded = encodeSmart(listOf(SmartPlaylist("id", "Jazz", rules)))
            assertEquals(rules, decodeSmart(encoded).single().rules)
        } finally {
            db.close()
        }
    }

    @Test
    fun tenThousandSongIndexFiltersAndUnavailableFilesPreserveUserRelationships() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db =
            Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val dao = db.music()
            dao.upsert(
                (0 until 10000).map {
                    Track(
                        "content://load/$it",
                        "Track $it",
                        artist = "Artist ${it%100}",
                        genre = "Genre ${it%10}",
                        mime = "audio/flac",
                    )
                }
            )
            assertEquals(1000, dao.selected(libraryQuery(LibraryFilter(), 1000)).size)
            assertEquals(
                "Track 9999",
                dao.selected(libraryQuery(LibraryFilter(search = "Track 9999"))).single().title,
            )
            dao.addFavorite(Favorite("content://load/42"))
            dao.recordPlay("content://load/42", 1234)
            val query =
                libraryQuery(
                    LibraryFilter(
                        smart =
                            SmartRules(
                                artist = "Artist 42",
                                minPlays = 1,
                                favoritesOnly = true,
                                format = "flac",
                            )
                    )
                )
            assertEquals("content://load/42", dao.selected(query).single().uri)
            dao.markUnavailable("media")
            assertTrue(dao.selected(query).isEmpty())
            assertTrue(
                "Missing files must retain favorites",
                dao.favorites().first().contains("content://load/42"),
            )
            dao.upsert(
                listOf(
                    Track(
                        "content://load/42",
                        "Restored",
                        artist = "Artist 42",
                        mime = "audio/flac",
                    )
                )
            )
            assertEquals("Restored", dao.selected(query).single().title)
        } finally {
            db.close()
        }
    }
}
