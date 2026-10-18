package com.piccol.hifi

import android.content.Context
import androidx.room.Room
import com.piccol.hifi.data.*
import com.piccol.hifi.ui.folderChildren
import com.piccol.hifi.ui.installedAppVersion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class RedesignModelTest {
    @Test
    fun aboutReadsInstalledPackageMetadataRatherThanBuildLabel() {
        val context = RuntimeEnvironment.getApplication() as Context
        @Suppress("DEPRECATION")
        val info =
            context.packageManager.getPackageInfo(context.packageName, 0).apply {
                versionName = "9.8.7"
                versionCode = 987
            }
        shadowOf(context.packageManager).installPackage(info)
        assertEquals("9.8.7 (code 987)", installedAppVersion(context))
    }

    @Test
    fun folderHierarchyKeepsRealCountsAndSeparatesSimilarPrefixes() {
        val groups =
            listOf(
                LibraryGroup("Music/Jazz/Live/", "Artist", null, 2),
                LibraryGroup("Music/Jazz/", "Artist", null, 3),
                LibraryGroup("Music/Rock/", "Artist", null, 4),
                LibraryGroup("Musical/", "Artist", null, 7),
            )
        val roots = folderChildren(groups, "")
        assertEquals(listOf("Music", "Musical"), roots.map { it.label })
        assertEquals(9, roots.first().count)
        assertEquals(listOf("Jazz", "Rock"), folderChildren(groups, "Music").map { it.label })
        assertEquals(listOf("Live"), folderChildren(groups, "Music/Jazz").map { it.label })
        assertTrue(folderChildren(groups, "Music").first().hasChildren)
    }

    @Test
    fun realGroupAndPlaylistCoverProjectionsPreserveExistingRows() = runBlocking {
        val context = RuntimeEnvironment.getApplication() as Context
        val database =
            Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val dao = database.music()
            dao.upsert(
                listOf(
                    Track(
                        "content://test/1",
                        "One",
                        artist = "Ada",
                        album = "Lines",
                        artwork = "content://cover/1",
                        duration = 1000,
                    ),
                    Track(
                        "content://test/2",
                        "Two",
                        artist = "Ada",
                        album = "Lines",
                        duration = 2000,
                    ),
                )
            )
            val groups = dao.groupCards("ALBUMS").first()
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().count)
            assertEquals("content://cover/1", groups.single().artwork)
            val playlist = dao.create(Playlist(name = "Existing"))
            dao.insertEntry(PlaylistEntry(playlist, "content://test/1", 0))
            val actual = dao.playlists().first().single()
            assertEquals("content://cover/1", actual.artwork)
            assertEquals(1, actual.count)
            assertEquals(1000L, actual.duration)
            assertNotNull(dao.track("content://test/2"))
        } finally {
            database.close()
        }
    }
}
