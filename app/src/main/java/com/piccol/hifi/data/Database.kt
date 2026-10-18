package com.piccol.hifi.data

import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks", indices = [Index("source"), Index("album"), Index("artist")])
data class Track(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String = "Unknown artist",
    val album: String = "Unknown album",
    val albumArtist: String = "",
    val genre: String = "Unknown genre",
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val duration: Long = 0,
    val size: Long = 0,
    val added: Long = 0,
    val mime: String = "",
    val folder: String = "",
    val artwork: String? = null,
    val source: String = "media",
    val available: Boolean = true,
)

@Entity(tableName = "favorites") data class Favorite(@PrimaryKey val uri: String)

@Entity(tableName = "history")
data class History(@PrimaryKey val uri: String, val lastPlayed: Long, val playCount: Int)

@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "uri"],
    foreignKeys =
        [
            ForeignKey(
                entity = Playlist::class,
                parentColumns = ["id"],
                childColumns = ["playlistId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("playlistId")],
)
data class PlaylistEntry(val playlistId: Long, val uri: String, val position: Int)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val count: Int,
    val duration: Long,
    val artwork: String? = null,
)

data class LibraryGroup(
    val name: String,
    val subtitle: String,
    val artwork: String?,
    val count: Int,
)

@Dao
abstract class MusicDao {
    @RawQuery(
        observedEntities = [Track::class, Favorite::class, History::class, PlaylistEntry::class]
    )
    abstract fun page(query: SupportSQLiteQuery): PagingSource<Int, Track>

    @RawQuery abstract suspend fun selected(query: SupportSQLiteQuery): List<Track>

    @Query("SELECT * FROM tracks WHERE uri = :uri") abstract suspend fun track(uri: String): Track?

    @Upsert abstract suspend fun upsert(tracks: List<Track>)

    @Query("UPDATE tracks SET available = 0 WHERE source = :source")
    abstract suspend fun markUnavailable(source: String)

    @Transaction
    open suspend fun replaceSource(source: String, tracks: List<Track>) {
        markUnavailable(source)
        upsert(tracks)
    }

    @Query(
        """SELECT CASE :category WHEN 'ALBUMS' THEN album WHEN 'ARTISTS' THEN artist WHEN 'GENRES' THEN genre ELSE folder END AS name,
        MIN(artist) AS subtitle, MIN(artwork) AS artwork, COUNT(*) AS count FROM tracks WHERE available=1
        GROUP BY CASE :category WHEN 'ALBUMS' THEN album WHEN 'ARTISTS' THEN artist WHEN 'GENRES' THEN genre ELSE folder END ORDER BY name COLLATE NOCASE"""
    )
    abstract fun groupCards(category: String): Flow<List<LibraryGroup>>

    @Query("SELECT uri FROM favorites") abstract fun favorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE uri = :uri")
    abstract suspend fun removeFavorite(uri: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun initializeHistory(history: History)

    @Query("UPDATE history SET lastPlayed=:now, playCount=playCount+1 WHERE uri=:uri")
    abstract suspend fun incrementHistory(uri: String, now: Long)

    @Transaction
    open suspend fun recordPlay(uri: String, now: Long) {
        initializeHistory(History(uri, now, 0))
        incrementHistory(uri, now)
    }

    @Query("SELECT DISTINCT album FROM tracks WHERE available = 1 ORDER BY album COLLATE NOCASE")
    abstract fun albums(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM tracks WHERE available = 1 ORDER BY artist COLLATE NOCASE")
    abstract fun artists(): Flow<List<String>>

    @Query("SELECT DISTINCT genre FROM tracks WHERE available = 1 ORDER BY genre COLLATE NOCASE")
    abstract fun genres(): Flow<List<String>>

    @Query("SELECT DISTINCT folder FROM tracks WHERE available = 1 ORDER BY folder COLLATE NOCASE")
    abstract fun folders(): Flow<List<String>>

    @Query(
        "SELECT p.id,p.name,COUNT(t.uri) AS count,COALESCE(SUM(t.duration),0) AS duration, MIN(t.artwork) AS artwork FROM playlists p LEFT JOIN playlist_entries e ON p.id=e.playlistId LEFT JOIN tracks t ON t.uri=e.uri GROUP BY p.id ORDER BY p.name COLLATE NOCASE"
    )
    abstract fun playlists(): Flow<List<PlaylistSummary>>

    @Insert abstract suspend fun create(playlist: Playlist): Long

    @Query("UPDATE playlists SET name=:name WHERE id=:id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id=:id") abstract suspend fun deletePlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(position),-1)+1 FROM playlist_entries WHERE playlistId=:id")
    abstract suspend fun nextPosition(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertEntry(entry: PlaylistEntry)

    @Transaction
    open suspend fun addToPlaylist(id: Long, uri: String) {
        insertEntry(PlaylistEntry(id, uri, nextPosition(id)))
    }

    @Query("DELETE FROM playlist_entries WHERE playlistId=:id AND uri=:uri")
    abstract suspend fun removeEntry(id: Long, uri: String)

    @Transaction
    open suspend fun importPlaylist(name: String, uris: List<String>): Long {
        val id = create(Playlist(name = name))
        uris.distinct().forEachIndexed { index, uri -> insertEntry(PlaylistEntry(id, uri, index)) }
        return id
    }

    @Query("DELETE FROM history") abstract suspend fun clearHistory()
}

@Database(
    entities =
        [Track::class, Favorite::class, History::class, Playlist::class, PlaylistEntry::class],
    version = 1,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun music(): MusicDao
}
