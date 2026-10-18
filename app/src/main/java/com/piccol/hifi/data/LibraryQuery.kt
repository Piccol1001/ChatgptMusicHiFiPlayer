package com.piccol.hifi.data

import androidx.sqlite.db.SimpleSQLiteQuery

enum class Category(val label: String) {
    ALL("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders"),
    FAVORITES("Favorites"),
    ADDED("Recently added"),
    RECENT("Recently played"),
    MOST("Most played"),
}

enum class Sort(val label: String, val sql: String) {
    TITLE("Title", "t.title COLLATE NOCASE"),
    ARTIST("Artist", "t.artist COLLATE NOCASE, t.album, t.trackNumber"),
    ALBUM("Album", "t.album COLLATE NOCASE, t.discNumber, t.trackNumber"),
    TRACK("Track number", "t.discNumber, t.trackNumber"),
    DURATION("Duration", "t.duration DESC"),
    ADDED("Date added", "t.added DESC"),
    FORMAT("Format", "t.mime"),
    SIZE("File size", "t.size DESC"),
}

data class LibraryFilter(
    val search: String = "",
    val category: Category = Category.ALL,
    val group: String? = null,
    val sort: Sort = Sort.TITLE,
    val format: String = "",
    val playlist: Long? = null,
    val smart: SmartRules? = null,
    val smartId: String? = null,
)

fun libraryQuery(filter: LibraryFilter, limit: Int? = null): SimpleSQLiteQuery {
    val args = mutableListOf<Any>()
    val conditions = mutableListOf("t.available=1")
    if (filter.search.isNotBlank()) {
        conditions +=
            "(t.title LIKE ? ESCAPE '!' OR t.artist LIKE ? ESCAPE '!' OR t.album LIKE ? ESCAPE '!')"
        val term = "%${filter.search.replace("!", "!!").replace("%", "!%").replace("_", "!_")}%"
        repeat(3) { args += term }
    }
    if (filter.format.isNotEmpty()) {
        conditions += "t.mime LIKE ?"
        args += "%${filter.format}%"
    }
    filter.smart?.let { rules ->
        require(rules.valid())
        listOf("artist" to rules.artist, "album" to rules.album, "genre" to rules.genre).forEach {
            (column, value) ->
            if (value.isNotBlank()) {
                conditions += "t.$column = ? COLLATE NOCASE"
                args += value
            }
        }
        if (rules.minPlays > 0) {
            conditions += "COALESCE((SELECT playCount FROM history WHERE uri=t.uri),0)>=?"
            args += rules.minPlays
        }
        if (rules.addedAfter > 0) {
            conditions += "t.added>=?"
            args += rules.addedAfter
        }
        if (rules.favoritesOnly) conditions += "t.uri IN (SELECT uri FROM favorites)"
        if (rules.format.isNotBlank()) {
            conditions += "t.mime LIKE ? ESCAPE '!'"
            args += "%${rules.format.replace("!","!!").replace("%","!%").replace("_","!_")}%"
        }
    }
    filter.group?.let {
        val column =
            when (filter.category) {
                Category.ALBUMS -> "album"
                Category.ARTISTS -> "artist"
                Category.GENRES -> "genre"
                else -> "folder"
            }
        conditions += "t.$column=?"
        args += it
    }
    when (filter.category) {
        Category.FAVORITES -> conditions += "t.uri IN (SELECT uri FROM favorites)"
        Category.RECENT,
        Category.MOST -> conditions += "t.uri IN (SELECT uri FROM history)"
        else -> Unit
    }
    filter.playlist?.let {
        conditions += "t.uri IN (SELECT uri FROM playlist_entries WHERE playlistId=?)"
        args += it
    }
    val order =
        when {
            filter.playlist != null -> {
                args += filter.playlist
                "(SELECT position FROM playlist_entries WHERE playlistId=? AND uri=t.uri)"
            }
            filter.category == Category.ADDED -> "t.added DESC"
            filter.category == Category.RECENT ->
                "(SELECT lastPlayed FROM history WHERE uri=t.uri) DESC"
            filter.category == Category.MOST ->
                "(SELECT playCount FROM history WHERE uri=t.uri) DESC"
            else -> filter.sort.sql
        }
    val cap = limit?.let { " LIMIT ${it.coerceIn(1, 1000)}" }.orEmpty()
    return SimpleSQLiteQuery(
        "SELECT t.* FROM tracks t WHERE ${conditions.joinToString(" AND ")} ORDER BY $order, t.uri$cap",
        args.toTypedArray(),
    )
}
