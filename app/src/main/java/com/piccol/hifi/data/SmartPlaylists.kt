package com.piccol.hifi.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.*
import org.json.*

data class SmartRules(
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val minPlays: Int = 0,
    val addedAfter: Long = 0,
    val favoritesOnly: Boolean = false,
    val format: String = "",
) {
    fun valid() =
        listOf(artist, album, genre, format).all { it.length <= 200 } &&
            minPlays in 0..1000000 &&
            addedAfter >= 0
}

data class SmartPlaylist(val id: String, val name: String, val rules: SmartRules)

private val Context.smartStore by preferencesDataStore("smart_playlists")

class SmartPlaylists(context: Context) {
    private val store = context.smartStore
    val playlists =
        store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { decodeSmart(it[stringPreferencesKey("rules")]) }
            .distinctUntilChanged()

    suspend fun save(id: String?, name: String, rules: SmartRules) {
        require(name.isNotBlank() && rules.valid())
        store.edit {
            val old = decodeSmart(it[stringPreferencesKey("rules")])
            require(id != null || old.size < 100)
            val next =
                SmartPlaylist(id ?: UUID.randomUUID().toString(), name.trim().take(80), rules)
            it[stringPreferencesKey("rules")] =
                encodeSmart(old.filterNot { p -> p.id == next.id } + next)
        }
    }

    suspend fun delete(id: String) {
        store.edit {
            it[stringPreferencesKey("rules")] =
                encodeSmart(
                    decodeSmart(it[stringPreferencesKey("rules")]).filterNot { p -> p.id == id }
                )
        }
    }
}

fun encodeSmart(list: List<SmartPlaylist>) =
    JSONArray()
        .apply {
            list.forEach { p ->
                put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("artist", p.rules.artist)
                        put("album", p.rules.album)
                        put("genre", p.rules.genre)
                        put("plays", p.rules.minPlays)
                        put("added", p.rules.addedAfter)
                        put("favorites", p.rules.favoritesOnly)
                        put("format", p.rules.format)
                    }
                )
            }
        }
        .toString()

fun decodeSmart(text: String?): List<SmartPlaylist> =
    try {
        val a = JSONArray(text ?: "[]")
        require(a.length() <= 100)
        (0 until a.length()).map {
            a.getJSONObject(it).let { p ->
                val rules =
                    SmartRules(
                        p.optString("artist"),
                        p.optString("album"),
                        p.optString("genre"),
                        p.optInt("plays"),
                        p.optLong("added"),
                        p.optBoolean("favorites"),
                        p.optString("format"),
                    )
                require(rules.valid())
                SmartPlaylist(p.getString("id"), p.getString("name"), rules)
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

/** M3U8 uses indexed content URIs so imports cannot silently resolve to an unrelated title. */
fun exportM3u(tracks: List<Track>): String = buildString {
    append("#EXTM3U\n")
    tracks.forEach { t ->
        append(
            "#EXTINF:${t.duration/1000},${t.artist.replace('\n',' ')} - ${t.title.replace('\n',' ')}\n${t.uri}\n"
        )
    }
}

data class M3uEntries(val uris: List<String>, val unresolved: Int)

fun parseM3u(text: String): M3uEntries {
    require(text.toByteArray().size <= 2097152) { "Playlist exceeds 2 MiB" }
    val entries =
        text
            .removePrefix("\uFEFF")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .toList()
    require(entries.size <= 10000) { "At most 10,000 playlist entries" }
    return M3uEntries(
        entries.filter { it.startsWith("content://") }.distinct(),
        entries.count { !it.startsWith("content://") },
    )
}
